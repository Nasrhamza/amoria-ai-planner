package com.example.amoriaiaplanner.feature_auth.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    fun currentUser() = auth.currentUser

    fun isLoggedIn(): Boolean = auth.currentUser != null

    fun logout() = auth.signOut()

    /** Email/Password signup + sends verification email + signs out (forces verify then login). */
    suspend fun signupAndSendVerification(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).await()
        auth.currentUser?.sendEmailVerification()?.await()
        auth.signOut()
    }

    /**
     * Email/Password login.
     * Returns true when the Firebase session is established.
     * Verification is handled as an app hint, not a hard block.
     */
    suspend fun loginAndRequireVerified(email: String, password: String): Boolean {
        auth.signInWithEmailAndPassword(email, password).await()
        auth.currentUser?.reload()?.await()
        return auth.currentUser != null
    }

    /** Resend verification by re-authenticating with email/password (needs password). */
    suspend fun resendVerificationWithCredentials(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
        auth.currentUser?.sendEmailVerification()?.await()
        auth.signOut()
    }

    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    suspend fun updateCurrentUserPassword(newPassword: String) {
        val user = auth.currentUser
            ?: throw FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "No signed-in account.")
        user.updatePassword(newPassword).await()
    }

    /**
     * Tries to check if email exists.
     * NOTE: May always return true/success if "Email Enumeration Protection" is ON in Firebase Console.
     */
    suspend fun checkIfEmailExists(email: String): Boolean {
        return try {
            val result = auth.fetchSignInMethodsForEmail(email).await()
            result.signInMethods?.isNotEmpty() == true
        } catch (e: Exception) {
            false
        }
    }

    /** Google login using Firebase credential. */
    suspend fun loginWithGoogleIdToken(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
        auth.currentUser?.reload()?.await()
    }

    /** Used by AppNav to decide if current session exists. */
    suspend fun isCurrentSessionAllowed(): Boolean {
        auth.currentUser?.reload()?.await()
        return auth.currentUser != null
    }

    // Friendly messages (used by ViewModels)
    fun friendlyError(e: Exception): String {
        return when (e) {
            is FirebaseAuthInvalidUserException -> "No account found with this email."
            is FirebaseAuthInvalidCredentialsException -> "Wrong email or password."
            is FirebaseAuthUserCollisionException -> "This email is already registered."
            is FirebaseAuthWeakPasswordException -> "Weak password. Use 6+ characters."
            is FirebaseAuthRecentLoginRequiredException ->
                "For security, sign out and log in again before changing the password."
            is FirebaseNetworkException -> "Network error. Check your connection."
            is FirebaseTooManyRequestsException -> "Too many attempts. Please try again later."
            else -> e.message ?: "Authentication failed."
        }
    }
}
