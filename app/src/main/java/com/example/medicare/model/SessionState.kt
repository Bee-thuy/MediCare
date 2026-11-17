package com.example.medicare.model

sealed class SessionState {
    data object SignedOut : SessionState()
    data class Authenticated(val role: UserRole, val profile: AccountProfile) : SessionState()
    data class Error(val message: String) : SessionState()
}
