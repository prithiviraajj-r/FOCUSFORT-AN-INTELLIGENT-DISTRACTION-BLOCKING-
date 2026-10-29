package com.example.focusfortvs.ui.student

import kotlin.math.floor
import kotlin.math.max

/**
 * Focus Compliance Score Engine
 *
 * Calculates a total score out of 100 across four dimensions:
 *   A. Quiz Accuracy   - 40 pts
 *   B. Topic Relevance - 20 pts (passed from AI)
 *   C. Behavior        - 20 pts (penalised for overlay lingering)
 *   D. Quiz Speed      - 20 pts (decay after 30 seconds)
 */
object ScoringEngine {

    fun quizAccuracyScore(correctAnswers: Int, totalQuestions: Int): Int {
        if (totalQuestions == 0) return 0
        return ((correctAnswers.toFloat() / totalQuestions) * 40).toInt()
    }

    fun relevanceScore(aiRelevancePoints: Int): Int = aiRelevancePoints.coerceIn(0, 20)

    fun behaviorScore(behaviorPenalty: Int): Int = max(0, 20 - (behaviorPenalty * 10))

    fun speedScore(quizCompletionTimeInSeconds: Long): Int {
        return if (quizCompletionTimeInSeconds <= 30L) {
            20
        } else {
            val decay = floor((quizCompletionTimeInSeconds - 30).toDouble() / 3.0).toInt()
            max(0, 20 - decay)
        }
    }

    fun totalScore(
        correctAnswers: Int,
        totalQuestions: Int,
        aiRelevancePoints: Int,
        behaviorPenalty: Int,
        quizCompletionTimeInSeconds: Long,
    ): FocusComplianceScore {
        val accuracy = quizAccuracyScore(correctAnswers, totalQuestions)
        val relevance = relevanceScore(aiRelevancePoints)
        val behavior = behaviorScore(behaviorPenalty)
        val speed = speedScore(quizCompletionTimeInSeconds)
        val total = accuracy + relevance + behavior + speed
        return FocusComplianceScore(
            accuracyScore = accuracy,
            relevanceScore = relevance,
            behaviorScore = behavior,
            speedScore = speed,
            total = total,
            passed = total >= 70,
        )
    }
}

data class FocusComplianceScore(
    val accuracyScore: Int,
    val relevanceScore: Int,
    val behaviorScore: Int,
    val speedScore: Int,
    val total: Int,
    val passed: Boolean,
)
