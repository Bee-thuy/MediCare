package com.example.medicare.data.auth

import com.example.medicare.model.AccountProfile
import com.example.medicare.model.AccountStatus
import com.example.medicare.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun resolveSession(callback: (SessionState) -> Unit) {
        val user = auth.currentUser ?: run {
            callback(SessionState.SignedOut)
            return
        }
        user.reload()
            .addOnSuccessListener { loadProfile(user.uid, user.isEmailVerified, callback) }
            .addOnFailureListener {
                callback(SessionState.Error("Không thể xác thực phiên đăng nhập. Vui lòng thử lại."))
            }
    }

    fun signIn(email: String, password: String, callback: (SessionState) -> Unit) {
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { resolveSession(callback) }
            .addOnFailureListener { callback(SessionState.Error("Email hoặc mật khẩu không đúng.")) }
    }

    /** Public registration can create USER only; Firestore Rules lock the type afterwards. */
    fun registerPatient(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        callback: (Result<Unit>) -> Unit,
    ) {
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val user = result.user ?: run {
                    callback(Result.failure(IllegalStateException("Không thể tạo tài khoản.")))
                    return@addOnSuccessListener
                }
                val profile = hashMapOf(
                    "fullName" to fullName.trim(),
                    "email" to email.trim(),
                    "phone" to phone.trim(),
                    "type" to UserRole.USER.name,
                    "accountStatus" to AccountStatus.ACTIVE.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                firestore.collection(USERS_COLLECTION).document(user.uid).set(profile)
                    .addOnSuccessListener {
                        user.sendEmailVerification()
                            .addOnSuccessListener { callback(Result.success(Unit)) }
                            .addOnFailureListener {
                                callback(Result.failure(IllegalStateException("Tài khoản đã tạo nhưng chưa gửi được email xác minh.")))
                            }
                    }
                    .addOnFailureListener {
                        user.delete()
                        signOut()
                        callback(Result.failure(IllegalStateException("Không thể tạo hồ sơ. Hãy kiểm tra Firestore Rules.")))
                    }
            }
            .addOnFailureListener { error ->
                val message = if (error is FirebaseAuthUserCollisionException) {
                    "Email này đã được đăng ký."
                } else {
                    "Không thể tạo tài khoản. Vui lòng thử lại sau."
                }
                callback(Result.failure(IllegalStateException(message)))
            }
    }

    fun sendPasswordReset(email: String, callback: (Result<Unit>) -> Unit) {
        auth.sendPasswordResetEmail(email.trim())
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener {
                callback(Result.failure(IllegalStateException("Không thể gửi email đặt lại mật khẩu.")))
            }
    }

    fun signOut() = auth.signOut()

    private fun loadProfile(uid: String, isEmailVerified: Boolean, callback: (SessionState) -> Unit) {
        firestore.collection(USERS_COLLECTION).document(uid).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    signOut()
                    callback(SessionState.Error("Không tìm thấy hồ sơ tài khoản."))
                    return@addOnSuccessListener
                }
                val role = UserRole.fromClaim(snapshot.getString("type"))
                if (role == null) {
                    signOut()
                    callback(SessionState.Error("Tài khoản chưa được cấp loại hợp lệ."))
                    return@addOnSuccessListener
                }
                if (role == UserRole.USER && !isEmailVerified) {
                    callback(SessionState.Error(EMAIL_VERIFICATION_REQUIRED))
                    return@addOnSuccessListener
                }
                val profile = AccountProfile.fromSnapshot(uid, snapshot)
                when (profile.status) {
                    AccountStatus.ACTIVE -> callback(SessionState.Authenticated(role, profile))
                    AccountStatus.DISABLED -> {
                        signOut()
                        callback(SessionState.Error("Tài khoản đã bị vô hiệu hóa."))
                    }
                    AccountStatus.PENDING -> callback(SessionState.Error("Tài khoản đang chờ được kích hoạt."))
                }
            }
            .addOnFailureListener {
                callback(SessionState.Error("Không thể tải hồ sơ tài khoản. Hãy kiểm tra mạng hoặc Firestore Rules."))
            }
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val EMAIL_VERIFICATION_REQUIRED = "Vui lòng xác minh email trước khi đăng nhập."
    }
}

sealed class SessionState {
    data object SignedOut : SessionState()
    data class Authenticated(val role: UserRole, val profile: AccountProfile) : SessionState()
    data class Error(val message: String) : SessionState()
}
