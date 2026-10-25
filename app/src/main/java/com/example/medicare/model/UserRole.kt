package com.example.medicare.model

/** Roles are stored in the secured Firestore user profile. */
enum class UserRole {
    ADMIN,
    USER,
    DOCTOR;

    companion object {
        fun fromClaim(value: Any?): UserRole? = when (value?.toString()?.uppercase()) {
            "ADMIN" -> ADMIN
            "USER", "PATIENT" -> USER
            "DOCTOR" -> DOCTOR
            else -> null
        }
    }
}
