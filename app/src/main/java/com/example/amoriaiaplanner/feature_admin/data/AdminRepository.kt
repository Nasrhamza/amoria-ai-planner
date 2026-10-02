package com.example.amoriaiaplanner.feature_admin.data

import android.content.Context
import com.example.amoriaiaplanner.core.config.AppFeatureConfigRepository
import com.example.amoriaiaplanner.feature_admin.model.AdminFeatureSettings
import com.example.amoriaiaplanner.feature_admin.model.AdminDashboardStats
import com.example.amoriaiaplanner.feature_admin.model.AdminRoomItem
import com.example.amoriaiaplanner.feature_admin.model.AdminUserItem
import com.example.amoriaiaplanner.feature_friends_room.model.FriendRoom
import com.example.amoriaiaplanner.feature_friends_room.model.RoomStatus
import com.example.amoriaiaplanner.feature_profile.data.ProfileRepository
import com.example.amoriaiaplanner.feature_profile.model.AppUser
import com.example.amoriaiaplanner.feature_profile.model.UserProfile
import com.example.amoriaiaplanner.feature_profile.model.UserRole
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AdminRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val profileRepo: ProfileRepository = ProfileRepository(),
    private val featureConfigRepo: AppFeatureConfigRepository = AppFeatureConfigRepository(db)
) {

    private suspend fun requireAdminUser(): AppUser {
        val user = profileRepo.getCurrentAppUser()
            ?: throw SecurityException("Admin access requires a signed-in account.")

        if (user.isBlocked) throw SecurityException("This account is blocked.")
        if (user.role != UserRole.ADMIN) throw SecurityException("Admin access required.")
        return user
    }

    suspend fun loadCurrentAdminUser(): AppUser = requireAdminUser()

    suspend fun loadDashboardStats(): AdminDashboardStats {
        requireAdminUser()

        val usersSnapshot = db.collection("users").get().await()
        val roomsSnapshot = db.collection("friend_rooms").get().await()
        val suggestionsSnapshot = runCatching {
            db.collectionGroup("suggestions").get().await()
        }.getOrNull()

        val activeRooms = roomsSnapshot.documents.count { doc ->
            val room = doc.toObject(FriendRoom::class.java)
            room?.status in setOf(RoomStatus.WAITING, RoomStatus.STARTED)
        }

        return AdminDashboardStats(
            totalUsers = usersSnapshot.size(),
            totalRooms = roomsSnapshot.size(),
            activeRooms = activeRooms,
            totalSuggestions = suggestionsSnapshot?.size(),
            totalRoomMembers = roomsSnapshot.documents.sumOf { doc ->
                doc.toObject(FriendRoom::class.java)?.memberCount ?: 0
            },
            averageMembersPerRoom = roomsSnapshot.documents
                .mapNotNull { it.toObject(FriendRoom::class.java)?.memberCount }
                .takeIf { it.isNotEmpty() }
                ?.average()
                ?: 0.0
        )
    }

    suspend fun loadUsers(): List<AdminUserItem> {
        requireAdminUser()

        return db.collection("users")
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                doc.toObject(AppUser::class.java)?.let { user ->
                    AdminUserItem(
                        uid = doc.id,
                        email = user.email,
                        displayName = user.displayName,
                        role = user.role,
                        isBlocked = user.isBlocked,
                        mustChangePassword = user.mustChangePassword,
                        emailVerified = user.emailVerified,
                        profileCompleted = user.profileCompleted,
                        onboardingCompleted = user.onboardingCompleted,
                        provider = user.provider,
                        createdAt = user.createdAt
                    )
                }
            }
            .sortedWith(compareByDescending<AdminUserItem> { it.createdAt }.thenBy { it.email.lowercase() })
    }

    suspend fun loadRooms(): List<AdminRoomItem> {
        requireAdminUser()

        return db.collection("friend_rooms")
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                doc.toObject(FriendRoom::class.java)?.let { room ->
                    AdminRoomItem(
                        roomId = doc.id,
                        code = room.code,
                        hostUid = room.hostUid,
                        hostEmail = room.hostEmail,
                        status = room.status,
                        memberCount = room.memberCount,
                        maxMembers = room.maxMembers,
                        suggestionsGenerated = room.suggestionsGenerated,
                        createdAt = room.createdAt
                    )
                }
            }
            .sortedWith(compareByDescending<AdminRoomItem> { it.createdAt }.thenBy { it.code })
    }

    suspend fun toggleUserBlocked(userId: String, blocked: Boolean) {
        val current = requireAdminUser()
        if (userId.isBlank()) throw IllegalArgumentException("Invalid user.")
        if (userId == current.uid) throw IllegalStateException("You cannot block your own account.")

        db.collection("users")
            .document(userId)
            .update("isBlocked", blocked)
            .await()
    }

    suspend fun updateUserRole(userId: String, role: String) {
        val current = requireAdminUser()
        if (userId.isBlank()) throw IllegalArgumentException("Invalid user.")
        if (userId == current.uid) {
            throw IllegalStateException("You cannot change your own administrator role.")
        }
        if (role !in setOf(UserRole.USER, UserRole.ADMIN)) {
            throw IllegalArgumentException("Invalid user role.")
        }

        val userRef = db.collection("users").document(userId)
        if (!userRef.get().await().exists()) {
            throw IllegalStateException("The selected user no longer exists.")
        }
        userRef.update("role", role).await()
    }

    suspend fun createUser(
        context: Context,
        email: String,
        password: String,
        displayName: String,
        role: String
    ) {
        requireAdminUser()

        val cleanEmail = email.trim()
        val cleanName = displayName.trim()
        require(cleanEmail.isNotBlank()) { "Email is required." }
        require(password.length >= 8) { "Temporary password must contain at least 8 characters." }
        require(role in setOf(UserRole.USER, UserRole.ADMIN)) { "Invalid account role." }

        val appContext = context.applicationContext
        val secondaryApp = FirebaseApp.getApps(appContext)
            .firstOrNull { it.name == ADMIN_CREATION_APP }
            ?: FirebaseApp.initializeApp(
                appContext,
                FirebaseApp.getInstance().options,
                ADMIN_CREATION_APP
            )
            ?: throw IllegalStateException("Unable to initialize account creation.")

        val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)
        secondaryAuth.signOut()

        val authResult = secondaryAuth
            .createUserWithEmailAndPassword(cleanEmail, password)
            .await()
        val createdUser = authResult.user
            ?: throw IllegalStateException("Firebase did not return the created account.")

        try {
            if (cleanName.isNotBlank()) {
                createdUser.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(cleanName)
                        .build()
                ).await()
            }
            createdUser.sendEmailVerification().await()

            val now = System.currentTimeMillis()
            val appUser = AppUser(
                uid = createdUser.uid,
                email = cleanEmail,
                displayName = cleanName,
                provider = "password",
                role = role,
                isBlocked = false,
                mustChangePassword = role == UserRole.ADMIN,
                emailVerified = false,
                createdAt = now
            )

            val batch = db.batch()
            batch.set(db.collection("users").document(createdUser.uid), appUser)
            batch.set(
                db.collection("profiles").document(createdUser.uid),
                UserProfile(uid = createdUser.uid)
            )
            batch.commit().await()
        } catch (error: Exception) {
            runCatching { createdUser.delete().await() }
            throw error
        } finally {
            secondaryAuth.signOut()
        }
    }

    suspend fun loadFeatureSettings(): AdminFeatureSettings {
        requireAdminUser()
        return featureConfigRepo.load()
    }

    suspend fun updateFeatureSettings(settings: AdminFeatureSettings) {
        val admin = requireAdminUser()
        featureConfigRepo.save(settings, updatedBy = admin.uid)
    }

    suspend fun updateRoomStatus(roomId: String, status: String) {
        requireAdminUser()
        if (roomId.isBlank()) throw IllegalArgumentException("Invalid room.")
        if (status !in VALID_ROOM_STATUSES) {
            throw IllegalArgumentException("Invalid room status.")
        }

        val roomRef = db.collection("friend_rooms").document(roomId)
        if (!roomRef.get().await().exists()) {
            throw IllegalStateException("The selected room no longer exists.")
        }
        roomRef.update("status", status).await()
    }

    suspend fun deleteRoomCompletely(roomId: String) {
        requireAdminUser()

        if (roomId.isBlank()) throw IllegalArgumentException("Invalid room.")

        val roomRef = db.collection("friend_rooms").document(roomId)
        val membersSnapshot = roomRef.collection("members").get().await()
        val answersSnapshot = roomRef.collection("quiz_answers").get().await()
        val suggestionsSnapshot = roomRef.collection("suggestions").get().await()

        val userRefsToClear = membersSnapshot.documents.mapNotNull { doc ->
            val uid = doc.getString("uid").orEmpty().ifBlank { doc.id }
            if (uid.isBlank()) return@mapNotNull null

            val userRef = db.collection("users").document(uid)
            val userDocument = runCatching { userRef.get().await() }.getOrNull()
            userRef.takeIf {
                userDocument?.exists() == true &&
                    userDocument.getString("activeRoomId") == roomId
            }
        }

        userRefsToClear.chunked(MAX_BATCH_WRITES).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { userRef -> batch.update(userRef, "activeRoomId", "") }
            batch.commit().await()
        }

        val nestedDocuments = buildList {
            addAll(membersSnapshot.documents.map { it.reference })
            addAll(answersSnapshot.documents.map { it.reference })
            addAll(suggestionsSnapshot.documents.map { it.reference })
        }
        nestedDocuments.chunked(MAX_BATCH_WRITES).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach(batch::delete)
            batch.commit().await()
        }

        roomRef.delete().await()
    }

    private companion object {
        const val ADMIN_CREATION_APP = "amoria-admin-user-creation"
        const val MAX_BATCH_WRITES = 400
        val VALID_ROOM_STATUSES = setOf(
            RoomStatus.WAITING,
            RoomStatus.STARTED,
            RoomStatus.COMPLETED,
            RoomStatus.CANCELLED
        )
    }
}
