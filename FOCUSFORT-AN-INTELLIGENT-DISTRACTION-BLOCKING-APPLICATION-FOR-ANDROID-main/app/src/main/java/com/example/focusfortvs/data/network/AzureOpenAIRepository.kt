package com.example.focusfortvs.data.network

import android.util.Log
import org.json.JSONObject
import javax.inject.Inject

class AzureOpenAIRepository @Inject constructor(
    private val api: AzureOpenAIApi
) {
    private val endpoint = "end"
    private val apiKey =




        "BuildConfig.AZURE_OPENAI_KEY"

    private fun buildFullUrl(): String {
        val baseUrl = endpoint.trimEnd('/')
        return if (!baseUrl.contains("/deployments/")) {
            "$baseUrl/openai/deployments/gpt-4o/chat/completions?api-version=2024-02-15-preview"
        } else {
            baseUrl
        }
    }

    private suspend fun sendMessage(messages: List<ChatMessage>, maxTokens: Int = 1000, temperature: Double = 0.0): String? {
        return try {
            val request = ChatRequest(messages = messages, max_tokens = maxTokens, temperature = temperature)
            val response = api.generateQuiz(buildFullUrl(), apiKey, request)
            response.choices.firstOrNull()?.message?.content
        } catch (e: Exception) {
            Log.e("AzureOpenAI", "API call failed", e)
            null
        }
    }

    /** Step 1: Validate image relevance against the study topic. Returns true if relevant. */
    suspend fun validateImageRelevance(base64Image: String, studyTopic: String): Boolean {
        val systemPrompt = """
            You are a strict, expert academic evaluator. 
            Your only job is to evaluate if the provided image genuinely teaches or relates to the topic: "$studyTopic".
            
            CRITICAL RULES:
            1. Do NOT approve an image just because it contains a matching keyword.
            2. If there is ANY domain mismatch (e.g. topic is "Java" but image is "Python", "C++", or general articles), you MUST reject it.
            
            Answer ONLY with YES or NO. No explanation.
        """.trimIndent()
        
        val userPrompt = "Analyze this image. Does its core conceptual meaning and overall subject matter match the topic: \"$studyTopic\"?"

        val messages = listOf(
            ChatMessage(
                role = "system",
                content = listOf(ContentItem(type = "text", text = systemPrompt))
            ),
            ChatMessage(
                role = "user",
                content = listOf(
                    ContentItem(type = "text", text = userPrompt),
                    ContentItem(type = "image_url", image_url = ImageUrl(url = "data:image/jpeg;base64,$base64Image"))
                )
            )
        )
        val response = sendMessage(messages, maxTokens = 5, temperature = 0.0)?.trim()?.uppercase() ?: return false
        Log.d("AzureOpenAI", "Relevance check: $response")
        return response.startsWith("YES")
    }

    /** Step 2: Generate topic-grounded quiz from the image. Call only after relevance passes. */
    suspend fun generateQuizFromImage(base64Image: String, studyTopic: String): QuizResponse? {
        val systemPrompt = """
            You are an expert academic tutor acting as an automated examiner for FocusFort. Your job is to verify that a student genuinely understood the study material in the provided image.

            ### STEP 1: DETECT THE SUBJECT & APPLY THE CORRECT METHODOLOGY
            Analyze the image, identify the subject matter, and strictly follow the specific testing methodology for that category:

            - CATEGORY A: GRAMMAR & LANGUAGES
              - METHODOLOGY (Application): Generate brand new, creative sentences that force the student to APPLY the grammar rule or vocabulary taught in the image.
              - NEGATIVE CONSTRAINT: DO NOT ask for dictionary definitions of words used in the instructional text. DO NOT ask the student to regurgitate the rule verbatim.

            - CATEGORY B: MATH & EQUATIONS
              - METHODOLOGY (Application): Provide a NEW mathematical problem that uses the exact same formula or methodology taught in the image, but with DIFFERENT numbers or variables.
              - NEGATIVE CONSTRAINT: DO NOT test them on the exact same numbers used in the image's example.

            - CATEGORY C: PROGRAMMING & CODING
              - METHODOLOGY (Analysis): Provide modified code snippets based on the text, ask for output prediction, or test the logic of the specific concept taught.
              - NEGATIVE CONSTRAINT: DO NOT ask for visual trivia (e.g., variable names, string values, or line numbers). DO NOT test concepts from language boilerplate unless explicitly taught.

            - CATEGORY D: FACTUAL SUBJECTS (HISTORY, SCIENCE, GEOGRAPHY, ETC.)
              - METHODOLOGY (Analysis): Test conceptual understanding, cause-and-effect, and relationships based ONLY on the facts present in the text.
              - NEGATIVE CONSTRAINT: DO NOT bring in outside facts. DO NOT ask questions that require merely reading a word directly off the image.

            ### STEP 2: GENERAL RULES (APPLIES TO ALL CATEGORIES)
            - Ensure the question difficulty perfectly matches the grade level of the provided text.
            - Do not mention that you are analyzing an image. Write the questions as if you are a teacher giving a natural pop quiz.
            - Generate exactly 3 questions.

            ### OUTPUT FORMAT:
            Output strictly valid JSON with this exact schema (no markdown, no code blocks):
            {
              "detected_subject": "e.g., English Grammar, Python Programming, 8th Grade History",
              "applied_methodology": "Which Category rule applied and how",
              "questions": [
                {
                  "question": "The generated question following the specific category methodology",
                  "options": ["Option A", "Option B", "Option C", "Option D"],
                  "correct_answer": "Option A",
                  "explanation": "Brief explanation of why this answer is correct."
                }
              ]
            }
        """.trimIndent()
        
        val userPrompt = "Analyze the study material in this image. Detect the subject, apply the correct category methodology, and generate 3 quiz questions. Return ONLY the raw JSON object."

        val messages = listOf(
            ChatMessage(
                role = "system",
                content = listOf(ContentItem(type = "text", text = systemPrompt))
            ),
            ChatMessage(
                role = "user",
                content = listOf(
                    ContentItem(type = "text", text = userPrompt),
                    ContentItem(type = "image_url", image_url = ImageUrl(url = "data:image/jpeg;base64,$base64Image"))
                )
            )
        )
        val jsonContent = sendMessage(messages, temperature = 0.0) ?: return null
        return parseQuizJson(jsonContent)
    }
    
    /**
     * Parses the AI JSON response into QuizResponse.
     * Supports both:
     *   - new format: correct_answer (string) matched to its index in options
     *   - legacy format: answerIndex (int) fallback
     */
    private fun parseQuizJson(json: String): QuizResponse? {
        try {
            val startIndex = json.indexOf('{')
            val endIndex = json.lastIndexOf('}')
            if (startIndex == -1 || endIndex == -1) return null

            val cleanJson = json.substring(startIndex, endIndex + 1)
            val root = JSONObject(cleanJson)
            val questionsArray = root.getJSONArray("questions")

            val quizQuestions = mutableListOf<QuizQuestion>()
            for (i in 0 until questionsArray.length()) {
                val qObj = questionsArray.getJSONObject(i)
                val questionText = qObj.getString("question")

                val optionsArray = qObj.getJSONArray("options")
                val options = mutableListOf<String>()
                for (j in 0 until optionsArray.length()) {
                    options.add(optionsArray.getString(j))
                }

                // Resolve correct answer index:
                // Priority 1: correct_answer string → find its position in options
                // Priority 2: answerIndex integer (legacy fallback)
                val resolvedIndex: Int = if (qObj.has("correct_answer")) {
                    val correctAnswer = qObj.getString("correct_answer").trim()
                    val idx = options.indexOfFirst { it.trim().equals(correctAnswer, ignoreCase = true) }
                    if (idx >= 0) idx else qObj.optInt("answerIndex", 0)
                } else {
                    qObj.optInt("answerIndex", 0)
                }

                quizQuestions.add(QuizQuestion(questionText, options, resolvedIndex))
            }

            return QuizResponse(quizQuestions)
        } catch (e: Exception) {
            Log.e("AzureOpenAI", "Failed to parse JSON: $e")
            return null
        }
    }
    /**
     * Evaluates an Office Mode access request via AI.
     *
     * @param appName   Human-readable app label (e.g., "WhatsApp")
     * @param reason    User's stated reason for needing the app
     * @return          [OfficeEvaluationResult.Approved] or [OfficeEvaluationResult.Rejected]
     */
    suspend fun evaluateOfficeRequest(appName: String, reason: String): OfficeEvaluationResult {
        val currentDateTime = java.text.SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        
        val systemPrompt = """
            You are an AI productivity guardian for FocusFort Office Mode.
            An employee is trying to access a blocked app during active work hours.
            
            Your job: evaluate if the stated reason justifies accessing the requested app.
            
            TEMPORAL CONTEXT:
            You will be provided with the user's exact current date and time. Use this to determine if the request makes sense. 
            However, act like a smart, responsible manager: understand real-world logistics. For example, ordering lunch 30-45 minutes before lunch break is logical because delivery takes time.
            
            EVALUATION LOGIC:
            First, analyze the requested app and ask yourself: "If I approve this app, can the user use it for entertainment, leisure, or distraction?"
            
            IF YES (e.g., WhatsApp, Instagram, YouTube, Games, Social Media):
            - BE EXTREMELY STRICT.
            - Only approve if the user's reason is a genuine, pressing work requirement or a real emergency (e.g., "Need to send client the invoice link via WhatsApp", "Boss messaged me on Teams").
            - Check for honesty, logic, and professional context.
            - Reject anything recreational, vague, or non-urgent.
            
            IF NO (e.g., Word, Excel, Recharge apps, Bus/Travel Booking, Utility apps, Food Delivery apps):
            - BE REASONABLY BALANCED.
            - Since the app provides no entertainment value, you only need to check if the user is honest and the reason is logical.
            - If the reason honestly corresponds to the app's actual core function (e.g., "I want to enter data" for Excel, "My data pack is over, I want to recharge" for a Recharge app, "Need to order food for lunch" for Zomato), APPROVE IT.
            - Take the current time into account, but accommodate reasonable prep/delivery times.
            - If the reason does NOT logically relate to the app's function, REJECT IT.
            
            GENERAL RULES:
            - EMOTIONAL RESILIENCE: Do NOT be manipulated by emotional, desperate, or dramatic language (e.g., "my boss will fire me", "I need this immediately!"). Evaluate the logic coldly and responsibly.
            - Never approve recreational use.
            - NUDGE MESSAGES: When rejecting, you MUST generate a highly specific, logical `nudgeMessage` explaining exactly *why* the request was denied based on the app's function, the stated reason, or the time. Do not just say "get back to work". Be clear, direct, and logically sound.
            
            OUTPUT FORMAT — return ONLY valid JSON, no markdown:
            For approved: {"status":"APPROVED"}
            For rejected: {"status":"REJECTED","nudgeMessage":"Your specific, logical denial explanation here."}
        """.trimIndent()

        val userMessage = """
            CURRENT DATE & TIME: $currentDateTime
            
            App Requested: $appName
            Stated Reason: "$reason"
            
            Evaluate and return JSON only.
        """.trimIndent()

        val messages = listOf(
            ChatMessage(role = "system", content = listOf(ContentItem(type = "text", text = systemPrompt))),
            ChatMessage(role = "user", content = listOf(ContentItem(type = "text", text = userMessage)))
        )

        return try {
            val raw = sendMessage(messages, maxTokens = 150, temperature = 0.3) ?: run {
                Log.e("AzureOpenAI", "Office eval: null response")
                return OfficeEvaluationResult.Rejected("Our evaluation service is unavailable. Please try again.")
            }
            parseOfficeEvalJson(raw)
        } catch (e: Exception) {
            Log.e("AzureOpenAI", "Office eval exception", e)
            OfficeEvaluationResult.Rejected("Evaluation failed. Please try again.")
        }
    }

    private fun parseOfficeEvalJson(json: String): OfficeEvaluationResult {
        return try {
            val start = json.indexOf('{')
            val end = json.lastIndexOf('}')
            if (start == -1 || end == -1) return OfficeEvaluationResult.Rejected("Invalid response from AI.")
            val clean = json.substring(start, end + 1)
            val obj = org.json.JSONObject(clean)
            val status = obj.optString("status", "REJECTED").uppercase()
            if (status == "APPROVED") {
                OfficeEvaluationResult.Approved
            } else {
                val nudge = obj.optString("nudgeMessage", "Access denied. Stay focused on your work.")
                OfficeEvaluationResult.Rejected(nudge)
            }
        } catch (e: Exception) {
            Log.e("AzureOpenAI", "Failed to parse office eval JSON: $e")
            OfficeEvaluationResult.Rejected("Could not parse evaluation response.")
        }
    }

    suspend fun evaluateEssentialApps(appNames: List<String>): List<AppAdvisoryResult> {
        val appListString = appNames.joinToString(", ")
        val systemPrompt = """
            You are an intelligent focus-coach acting as an App Advisor for a user about to enter a "Digital Detox" mode.
            The user has selected the following apps as "Essential Apps" that they want to be allowed during their detox session.
            Your job is to evaluate each app and determine if it is genuinely essential (e.g., communication, productivity, critical utilities) or potentially distracting (e.g., games, social media, entertainment).
            
            OUTPUT STRICT JSON:
            Return a JSON array of objects. Each object must have:
            - "appName": The exact string of the app name provided.
            - "isEssential": Boolean. true if it's a genuine utility (Phone, WhatsApp, Messaging, Maps, Food Delivery, Banking), false if it is for entertainment (Games, TikTok, Instagram, Netflix).
            - "reason": A short 1-sentence advisory message to the user. If true, say something encouraging like "Essential for communication." If false, give a firm but polite warning, e.g., "This is an entertainment app. It is highly distracting for focus time. Did you select this by mistake?"
            
            Example:
            [
              { "appName": "WhatsApp", "isEssential": true, "reason": "Essential for communication." },
              { "appName": "Candy Crush", "isEssential": false, "reason": "This is a gaming app. It is highly distracting for focus time. Did you select this by mistake?" }
            ]
        """.trimIndent()
        
        val userPrompt = "Evaluate these apps: $appListString"
        val messages = listOf(
            ChatMessage(role = "system", content = listOf(ContentItem(type = "text", text = systemPrompt))),
            ChatMessage(role = "user", content = listOf(ContentItem(type = "text", text = userPrompt)))
        )

        return try {
            val raw = sendMessage(messages, maxTokens = 500, temperature = 0.2) ?: return emptyList()
            parseAppAdvisoryJson(raw, appNames)
        } catch (e: Exception) {
            Log.e("AzureOpenAI", "App advisory exception", e)
            emptyList()
        }
    }

    private fun parseAppAdvisoryJson(json: String, requestedApps: List<String>): List<AppAdvisoryResult> {
        return try {
            val start = json.indexOf('[')
            val end = json.lastIndexOf(']')
            if (start == -1 || end == -1) return emptyList()
            val clean = json.substring(start, end + 1)
            val jsonArray = org.json.JSONArray(clean)
            
            val results = mutableListOf<AppAdvisoryResult>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                results.add(
                    AppAdvisoryResult(
                        appName = obj.optString("appName", "Unknown"),
                        isEssential = obj.optBoolean("isEssential", true),
                        reason = obj.optString("reason", "")
                    )
                )
            }
            // Ensure all requested apps have an entry
            val matchedNames = results.map { it.appName.lowercase() }.toSet()
            requestedApps.forEach { req ->
                if (!matchedNames.contains(req.lowercase())) {
                    results.add(AppAdvisoryResult(req, true, "Assumed essential by default."))
                }
            }
            results
        } catch (e: Exception) {
            Log.e("AzureOpenAI", "Failed to parse app advisory JSON: $e")
            emptyList()
        }
    }

    /**
     * Validates whether the entered study topic title is a legitimate academic
     * or learning subject. Returns a [TopicValidationResult] with either
     * [TopicValidationResult.Valid] or [TopicValidationResult.Invalid] + a reason.
     *
     * This is a text-only call — no image required.
     * Examples that PASS: "Biology – Cell Division", "Python OOP", "World War II", "Calculus – Derivatives"
     * Examples that FAIL: "floor", "my room", "selfie", "lunch", "abc"
     */
    suspend fun validateStudyTopic(topic: String): TopicValidationResult {
        val systemPrompt = """
            You are an academic topic validator for a student focus app called FocusFort.
            
            A student is entering the topic they claim to be studying. Your job is to determine 
            if it is a REAL and LEGITIMATE study subject — something that could genuinely appear 
            in a school, college, or self-learning curriculum.
            
            RULES:
            - VALID topics: Any named academic subject, chapter, concept, skill, or discipline.
              Examples: "Biology – Cell Division", "Python OOP", "World War II", "Calculus", 
              "Financial Accounting", "English Grammar", "Data Structures", "Physics – Optics"
            - INVALID topics: Random objects, places, food, nonsense, selfies, games, personal 
              activities, or anything that clearly cannot be a study subject.
              Examples: "floor", "my room", "cat", "lunch", "gaming", "abc", "xyz", empty names
            - Be REASONABLY lenient — "math" alone is valid, "science project" is valid.
            - Do NOT accept vague emotional phrases or non-academic personal plans.
            
            OUTPUT FORMAT — return ONLY valid JSON, no markdown:
            If valid:   {"valid": true}
            If invalid: {"valid": false, "reason": "One sentence explaining why this is not a valid study topic."}
        """.trimIndent()

        val userPrompt = "Study topic entered by student: \"$topic\"\n\nIs this a legitimate study subject? Return JSON only."

        val messages = listOf(
            ChatMessage(role = "system", content = listOf(ContentItem(type = "text", text = systemPrompt))),
            ChatMessage(role = "user", content = listOf(ContentItem(type = "text", text = userPrompt)))
        )

        return try {
            val raw = sendMessage(messages, maxTokens = 80, temperature = 0.0)
            parseTopicValidationJson(raw)
        } catch (e: Exception) {
            Log.e("AzureOpenAI", "Topic validation exception", e)
            // On network failure, fail open so we don't block the student
            TopicValidationResult.Valid
        }
    }

    private fun parseTopicValidationJson(json: String?): TopicValidationResult {
        if (json == null) return TopicValidationResult.Valid
        return try {
            val start = json.indexOf('{')
            val end = json.lastIndexOf('}')
            if (start == -1 || end == -1) return TopicValidationResult.Valid
            val obj = JSONObject(json.substring(start, end + 1))
            val isValid = obj.optBoolean("valid", true)
            if (isValid) {
                TopicValidationResult.Valid
            } else {
                val reason = obj.optString("reason", "This doesn't appear to be a valid study subject. Please enter a real topic.")
                TopicValidationResult.Invalid(reason)
            }
        } catch (e: Exception) {
            Log.e("AzureOpenAI", "Failed to parse topic validation JSON: $e")
            TopicValidationResult.Valid
        }
    }
}

sealed class TopicValidationResult {
    object Valid : TopicValidationResult()
    data class Invalid(val reason: String) : TopicValidationResult()
}


sealed class OfficeEvaluationResult {
    object Approved : OfficeEvaluationResult()
    data class Rejected(val nudgeMessage: String) : OfficeEvaluationResult()
}

data class QuizResponse(
    val questions: List<QuizQuestion>
)

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int
)

data class AppAdvisoryResult(
    val appName: String,
    val isEssential: Boolean,
    val reason: String
)
