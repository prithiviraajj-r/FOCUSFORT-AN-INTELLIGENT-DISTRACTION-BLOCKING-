package com.example.focusfortvs.blocking

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockPolicyResolver @Inject constructor(
    private val normalPolicy: NormalPolicy,
    private val officePolicy: OfficePolicy,
    private val studentPolicy: StudentPolicy,
    private val detoxPolicy: DetoxPolicy,
) {
    fun policyFor(mode: String): BlockPolicy {
        return when (mode.trim().lowercase()) {
            "normal" -> normalPolicy
            "office" -> officePolicy
            "student" -> studentPolicy
            "digital detox", "detox" -> detoxPolicy
            else -> normalPolicy
        }
    }
}