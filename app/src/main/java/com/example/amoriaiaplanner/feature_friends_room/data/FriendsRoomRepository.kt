package com.example.amoriaiaplanner.feature_friends_room.data

import com.example.amoriaiaplanner.feature_friends_room.model.FriendRoom
import com.example.amoriaiaplanner.feature_friends_room.model.RoomMember
import com.example.amoriaiaplanner.feature_friends_room.model.RoomStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import kotlin.random.Random
import com.example.amoriaiaplanner.feature_ai.data.LmStudioRepository
import com.example.amoriaiaplanner.feature_ai.model.PlaceSuggestion
import com.example.amoriaiaplanner.feature_friends_room.model.FriendsQuizAnswer

class FriendsRoomRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val lmRepo: LmStudioRepository = LmStudioRepository()
) {

    fun currentUid(): String = auth.currentUser?.uid.orEmpty()
    fun currentEmail(): String = auth.currentUser?.email.orEmpty()

    suspend fun createRoom(): FriendRoom {
        val uid = currentUid()
        val email = currentEmail()
        if (uid.isBlank()) throw IllegalStateException("User not authenticated")

        val userDoc = db.collection("users").document(uid).get().await()
        val activeRoomId = userDoc.getString("activeRoomId").orEmpty()
        val displayName = userDoc.getString("displayName").orEmpty()

        if (activeRoomId.isNotBlank()) {
            val existingRoom = getRoomById(activeRoomId)
            if (existingRoom != null && existingRoom.status == RoomStatus.WAITING) {
                return existingRoom
            }
        }

        val roomRef = db.collection("friend_rooms").document()
        val code = (100000..999999).random().toString()

        val room = FriendRoom(
            roomId = roomRef.id,
            code = code,
            hostUid = uid,
            hostEmail = email,
            status = RoomStatus.WAITING,
            maxMembers = 5,
            memberCount = 1,
            createdAt = System.currentTimeMillis()
        )

        val member = RoomMember(
            uid = uid,
            email = email,
            displayName = displayName,
            isHost = true
        )

        val batch = db.batch()
        batch.set(roomRef, room)
        batch.set(roomRef.collection("members").document(uid), member)
        batch.update(db.collection("users").document(uid), "activeRoomId", room.roomId)
        batch.commit().await()

        return room
    }

    suspend fun joinRoomByCode(enteredCode: String): FriendRoom {
        val uid = currentUid()
        val email = currentEmail()
        val cleanCode = enteredCode.trim()

        if (uid.isBlank()) throw IllegalStateException("User not authenticated")
        if (cleanCode.length != 6) throw IllegalArgumentException("Room code must be 6 digits")

        val userDoc = db.collection("users").document(uid).get().await()
        val displayName = userDoc.getString("displayName").orEmpty()

        val snapshot = db.collection("friend_rooms")
            .whereEqualTo("code", cleanCode)
            .whereEqualTo("status", RoomStatus.WAITING)
            .limit(1)
            .get()
            .await()

        if (snapshot.isEmpty) {
            throw IllegalArgumentException("Invalid room code or the quiz has already started.")
        }

        val roomDoc = snapshot.documents.first()
        val roomId = roomDoc.id
        val roomRef = db.collection("friend_rooms").document(roomId)
        val memberRef = roomRef.collection("members").document(uid)
        val userRef = db.collection("users").document(uid)

        db.runTransaction { transaction ->
            val room = transaction.get(roomRef).toObject(FriendRoom::class.java)
                ?: throw IllegalStateException("Room not found")

            if (room.status != RoomStatus.WAITING) throw IllegalStateException("Room already started")
            
            val existingMember = transaction.get(memberRef)
            if (!existingMember.exists()) {
                if (room.memberCount >= room.maxMembers) throw IllegalStateException("This room is full.")
                
                val member = RoomMember(
                    uid = uid,
                    email = email,
                    displayName = displayName,
                    isHost = false
                )
                transaction.set(memberRef, member)
                transaction.update(roomRef, "memberCount", room.memberCount + 1)
            }
            transaction.update(userRef, "activeRoomId", roomId)
        }.await()

        return getRoomById(roomId) ?: throw IllegalStateException("Join failed")
    }

    suspend fun startRoom(roomId: String) {
        val uid = currentUid()
        val roomRef = db.collection("friend_rooms").document(roomId)

        db.runTransaction { transaction ->
            val room = transaction.get(roomRef).toObject(FriendRoom::class.java) ?: return@runTransaction
            if (room.hostUid != uid) throw IllegalStateException("Only the host can start the quiz.")
            if (room.status != RoomStatus.WAITING) return@runTransaction

            val now = System.currentTimeMillis()
            transaction.update(roomRef, mapOf(
                "status" to RoomStatus.STARTED,
                "quizStartedAt" to now,
                "quizEndsAt" to now + 180_000L,
                "suggestionsGenerated" to false
            ))
        }.await()
    }

    suspend fun leaveRoom(roomId: String) {
        val uid = currentUid()
        if (uid.isBlank() || roomId.isBlank()) return

        val roomRef = db.collection("friend_rooms").document(roomId)
        val memberRef = roomRef.collection("members").document(uid)
        val userRef = db.collection("users").document(uid)

        db.runTransaction { transaction ->
            val roomSnapshot = transaction.get(roomRef)
            if (!roomSnapshot.exists()) {
                transaction.update(userRef, "activeRoomId", "")
                return@runTransaction
            }
            val room = roomSnapshot.toObject(FriendRoom::class.java) ?: return@runTransaction

            if (room.hostUid == uid) {
                // Host leaves -> Cancel room for everyone
                transaction.update(roomRef, "status", RoomStatus.CANCELLED)
            } else {
                // Member leaves -> Remove from sub-collection and update count
                transaction.delete(memberRef)
                transaction.update(roomRef, "memberCount", (room.memberCount - 1).coerceAtLeast(1))
            }
            transaction.update(userRef, "activeRoomId", "")
        }.await()
    }

    suspend fun getRoomById(roomId: String): FriendRoom? {
        if (roomId.isBlank()) return null
        return db.collection("friend_rooms").document(roomId).get().await().toObject(FriendRoom::class.java)
    }

    fun observeCurrentUserActiveRoomId(onChanged: (String?) -> Unit, onError: (Exception) -> Unit): ListenerRegistration {
        val uid = currentUid()
        return db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) onError(error)
            else onChanged(snapshot?.getString("activeRoomId"))
        }
    }

    fun observeRoom(roomId: String, onChanged: (FriendRoom?) -> Unit, onError: (Exception) -> Unit): ListenerRegistration {
        return db.collection("friend_rooms").document(roomId).addSnapshotListener { snapshot, error ->
            if (error != null) onError(error)
            else onChanged(snapshot?.toObject(FriendRoom::class.java))
        }
    }

    fun observeMembers(roomId: String, onChanged: (List<RoomMember>) -> Unit, onError: (Exception) -> Unit): ListenerRegistration {
        return db.collection("friend_rooms").document(roomId).collection("members").orderBy("joinedAt").addSnapshotListener { snapshot, error ->
            if (error != null) onError(error)
            else onChanged(snapshot?.documents?.mapNotNull { it.toObject(RoomMember::class.java) }.orEmpty())
        }
    }

    suspend fun saveQuizAnswer(roomId: String, answer: FriendsQuizAnswer) {
        val uid = currentUid()
        val roomRef = db.collection("friend_rooms").document(roomId)
        val batch = db.batch()
        batch.set(roomRef.collection("quiz_answers").document(uid), answer.copy(uid = uid, email = currentEmail(), finishedAt = System.currentTimeMillis()))
        batch.update(roomRef.collection("members").document(uid), "hasFinishedQuiz", true)
        batch.commit().await()
    }

    suspend fun getQuizAnswers(roomId: String): List<FriendsQuizAnswer> {
        return db.collection("friend_rooms").document(roomId).collection("quiz_answers").get().await().documents.mapNotNull { it.toObject(FriendsQuizAnswer::class.java) }
    }

    suspend fun shouldGenerateSuggestions(roomId: String): Boolean {
        val room = getRoomById(roomId) ?: return false
        if (room.status != RoomStatus.STARTED) return false
        val membersSnapshot = db.collection("friend_rooms").document(roomId).collection("members").get().await()
        val members = membersSnapshot.documents.mapNotNull { it.toObject(RoomMember::class.java) }
        return members.all { it.hasFinishedQuiz } || (room.quizEndsAt > 0L && System.currentTimeMillis() >= room.quizEndsAt)
    }

    suspend fun generateAndSaveGroupSuggestions(roomId: String, extraPreference: String = "", nearbyPlaces: List<com.example.amoriaiaplanner.feature_ai.model.NearbyPlace> = emptyList()): List<PlaceSuggestion> {
        val roomRef = db.collection("friend_rooms").document(roomId)
        val room = getRoomById(roomId) ?: throw IllegalStateException("Room not found")
        if (room.suggestionsGenerated) return getSavedSuggestions(roomId)
        val answers = getQuizAnswers(roomId)
        if (answers.isEmpty()) throw IllegalStateException("No answers yet.")
        val suggestions = lmRepo.suggestForFriendsRoom(answers, room.memberCount, extraPreference, nearbyPlaces)
        db.runTransaction { transaction ->
            val suggestionsRef = roomRef.collection("suggestions")
            suggestions.forEachIndexed { i, s -> transaction.set(suggestionsRef.document("s_${i + 1}"), s) }
            transaction.update(roomRef, mapOf("suggestionsGenerated" to true, "status" to RoomStatus.COMPLETED))
        }.await()
        return suggestions
    }

    suspend fun getSavedSuggestions(roomId: String): List<PlaceSuggestion> {
        return db.collection("friend_rooms").document(roomId).collection("suggestions").get().await().documents.mapNotNull { it.toObject(PlaceSuggestion::class.java) }
    }

    fun observeSavedSuggestions(
        roomId: String,
        onChanged: (List<PlaceSuggestion>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection("friend_rooms")
            .document(roomId)
            .collection("suggestions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val suggestions = snapshot?.documents
                    ?.mapNotNull { it.toObject(PlaceSuggestion::class.java) }
                    .orEmpty()

                onChanged(suggestions)
            }
    }

    suspend fun deleteRoomCompletely(roomId: String) {
        val roomRef = db.collection("friend_rooms").document(roomId)
        val members = roomRef.collection("members").get().await()
        val answers = roomRef.collection("quiz_answers").get().await()
        val suggestions = roomRef.collection("suggestions").get().await()
        val batch = db.batch()
        members.documents.forEach { doc ->
            val uid = doc.getString("uid").orEmpty()
            if (uid.isNotBlank()) batch.update(db.collection("users").document(uid), "activeRoomId", "")
            batch.delete(doc.reference)
        }
        answers.documents.forEach { doc ->
            batch.delete(doc.reference)
        }
        suggestions.documents.forEach { doc ->
            batch.delete(doc.reference)
        }
        batch.delete(roomRef)
        batch.commit().await()
    }
}
