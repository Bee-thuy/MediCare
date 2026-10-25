package com.example.medicare.model

enum class AccountStatus {
    ACTIVE,
    PENDING,
    DISABLED;

    companion object {
        fun fromStoredValue(value: String?): AccountStatus = when (value?.uppercase()) {
            "ACTIVE" -> ACTIVE
            "DISABLED" -> DISABLED
            else -> PENDING
        }
    }
}
