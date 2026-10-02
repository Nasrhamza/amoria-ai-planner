package com.example.amoriaiaplanner.feature_profile.data

import com.example.amoriaiaplanner.feature_profile.model.AppUser
import com.example.amoriaiaplanner.feature_profile.model.UserProfile
import com.example.amoriaiaplanner.feature_profile.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ProfileRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun ensureUserDocumentsExist() {
        val firebaseUser = auth.currentUser ?: return
        val uid = firebaseUser.uid

        val userDoc = db.collection("users").document(uid).get().await()
        if (!userDoc.exists()) {
            val provider = firebaseUser.providerData
                .map { it.providerId }
                .firstOrNull { it != "firebase" }
                ?: "password"

            val appUser = AppUser(
                uid = uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName ?: "",
                provider = provider,
                role = UserRole.USER,
                isBlocked = false,
                emailVerified = firebaseUser.isEmailVerified
            )

            db.collection("users").document(uid).set(appUser).await()
        }

        val profileDoc = db.collection("profiles").document(uid).get().await()
        if (!profileDoc.exists()) {
            val profile = UserProfile(uid = uid)
            db.collection("profiles").document(uid).set(profile).await()
        }
    }

    suspend fun getCurrentUserProfile(): UserProfile? {
        val uid = auth.currentUser?.uid ?: return null
        val snapshot = db.collection("profiles").document(uid).get().await()
        return snapshot.toObject(UserProfile::class.java)
    }

    suspend fun getCurrentAppUser(): AppUser? {
        val uid = auth.currentUser?.uid ?: return null
        return getAppUserByUid(uid)
    }

    suspend fun getAppUserByUid(uid: String): AppUser? {
        if (uid.isBlank()) return null
        val snapshot = db.collection("users").document(uid).get().await()
        return snapshot.toObject(AppUser::class.java)
    }

    suspend fun getCurrentUserRole(): String {
        return getCurrentAppUser()?.role ?: UserRole.USER
    }

    suspend fun isCurrentUserBlocked(): Boolean {
        return getCurrentAppUser()?.isBlocked == true
    }

    suspend fun isCurrentUserAdmin(): Boolean {
        return getCurrentUserRole() == UserRole.ADMIN
    }

    suspend fun completeForcedPasswordChange() {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("No signed-in account.")
        db.collection("users")
            .document(uid)
            .update("mustChangePassword", false)
            .await()
    }

    suspend fun getProfileByUid(uid: String): UserProfile? {
        if (uid.isBlank()) return null
        val snapshot = db.collection("profiles").document(uid).get().await()
        return snapshot.toObject(UserProfile::class.java)
    }

    suspend fun isProfileCompleted(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val userDoc = db.collection("users").document(uid).get().await()
        return userDoc.getBoolean("profileCompleted") == true
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("profiles")
            .document(uid)
            .set(profile.copy(uid = uid, updatedAt = System.currentTimeMillis()))
            .await()

        db.collection("users")
            .document(uid)
            .update(
                mapOf(
                    "profileCompleted" to true,
                    "onboardingCompleted" to true
                )
            ).await()
    }
}
