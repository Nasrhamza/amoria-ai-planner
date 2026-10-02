package com.example.amoriaiaplanner.feature_account.data

import com.example.amoriaiaplanner.feature_friends_room.data.FriendsRoomRepository
import com.example.amoriaiaplanner.feature_profile.model.AppUser
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AccountRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val friendsRoomRepository: FriendsRoomRepository = FriendsRoomRepository()
) {
    suspend fun loadAccount(): AppUser {
        val firebaseUser = auth.currentUser
            ?: throw IllegalStateException("No authenticated user.")

        return db.collection("users")
            .document(firebaseUser.uid)
            .get()
            .await()
            .toObject(AppUser::class.java)
            ?: AppUser(
                uid = firebaseUser.uid,
                email = firebaseUser.email.orEmpty(),
                displayName = firebaseUser.displayName.orEmpty(),
                provider = primaryProvider()
            )
    }

    fun primaryProvider(): String {
        return auth.currentUser
            ?.providerData
            ?.map { it.providerId }
            ?.firstOrNull { it != "firebase" }
            ?: "password"
    }

    suspend fun updatePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser
            ?: throw IllegalStateException("No authenticated user.")
        val email = user.email
            ?: throw IllegalStateException("This account has no email address.")

        if (primaryProvider() != "password") {
            throw IllegalStateException("Password is managed by your sign-in provider.")
        }
        require(currentPassword.isNotBlank()) { "Current password is required." }
        require(newPassword.length >= 6) { "New password must contain at least 6 characters." }

        user.reauthenticate(
            EmailAuthProvider.getCredential(email, currentPassword)
        ).await()
        user.updatePassword(newPassword).await()
    }

    suspend fun deleteAccount(currentPassword: String) {
        val user = auth.currentUser
            ?: throw IllegalStateException("No authenticated user.")

        if (primaryProvider() == "password") {
            val email = user.email
                ?: throw IllegalStateException("This account has no email address.")
            require(currentPassword.isNotBlank()) {
                "Current password is required to delete the account."
            }
            user.reauthenticate(
                EmailAuthProvider.getCredential(email, currentPassword)
            ).await()
        } else {
            val lastSignIn = user.metadata?.lastSignInTimestamp ?: 0L
            val recentlySignedIn = System.currentTimeMillis() - lastSignIn < RECENT_LOGIN_WINDOW_MS
            if (!recentlySignedIn) {
                throw IllegalStateException(
                    "Please log out, sign in again, then delete the account."
                )
            }
        }

        val userRef = db.collection("users").document(user.uid)
        val activeRoomId = userRef.get().await().getString("activeRoomId").orEmpty()
        if (activeRoomId.isNotBlank()) {
            runCatching { friendsRoomRepository.leaveRoom(activeRoomId) }
        }

        val batch = db.batch()
        batch.delete(db.collection("profiles").document(user.uid))
        batch.delete(db.collection("invite_codes").document(user.uid))
        batch.delete(userRef)
        batch.commit().await()

        user.delete().await()
    }

    private companion object {
        const val RECENT_LOGIN_WINDOW_MS = 10 * 60 * 1000L
    }
}
