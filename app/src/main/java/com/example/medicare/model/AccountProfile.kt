package com.example.medicare.model

import com.google.firebase.firestore.DocumentSnapshot

data class AccountProfile(
    val uid: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val status: AccountStatus,
) {
    companion object {
        fun fromSnapshot(uid: String, snapshot: DocumentSnapshot): AccountProfile = AccountProfile(
            uid = uid,
            fullName = snapshot.getString("fullName").orEmpty(),
            email = snapshot.getString("email").orEmpty(),
            phone = snapshot.getString("phone").orEmpty(),
            status = AccountStatus.fromStoredValue(
                snapshot.getString("accountStatus"),
            ),
        )
    }
}
