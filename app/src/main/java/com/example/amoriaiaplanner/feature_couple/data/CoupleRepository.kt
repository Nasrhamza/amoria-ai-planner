package com.example.amoriaiaplanner.feature_couple.data

import com.example.amoriaiaplanner.feature_couple.model.Couple
import com.example.amoriaiaplanner.feature_couple.model.InviteCode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class CoupleRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun currentUid(): String = auth.currentUser?.uid.orEmpty()
    fun currentEmail(): String = auth.currentUser?.email.orEmpty()

    suspend fun ensureUserInviteCode(): String {
        val uid = currentUid()
        if (uid.isBlank()) throw IllegalStateException("User not authenticated")

        val existing = db.collection("invite_codes").document(uid).get().await()
        if (existing.exists()) {
            val existingCode = existing.getString("code").orEmpty()
            if (existingCode.isNotBlank()) return existingCode
        }

        val newCode = generateUniqueCode()
        val inviteCode = InviteCode(
            ownerUid = uid,
            code = newCode,
            status = "active"
        )
        db.collection("invite_codes").document(uid).set(inviteCode).await()
        return newCode
    }

    suspend fun refreshMyCode(): String {
        val uid = currentUid()
        if (uid.isBlank()) throw IllegalStateException("User not authenticated")

        val newCode = generateUniqueCode()
        db.collection("invite_codes")
            .document(uid)
            .set(
                InviteCode(
                    ownerUid = uid,
                    code = newCode,
                    status = "active",
                    updatedAt = System.currentTimeMillis()
                )
            )
            .await()

        return newCode
    }

    suspend fun findCodeOwner(enteredCode: String): Pair<String, String>? {
        val snapshot = db.collection("invite_codes")
            .whereEqualTo("code", enteredCode)
            .whereEqualTo("status", "active")
            .get()
            .await()

        val doc = snapshot.documents.firstOrNull() ?: return null
        val ownerUid = doc.getString("ownerUid").orEmpty()
        if (ownerUid.isBlank()) return null

        val userDoc = db.collection("users").document(ownerUid).get().await()
        val ownerEmail = userDoc.getString("email").orEmpty()

        return ownerUid to ownerEmail
    }

    suspend fun linkWithPartnerCode(enteredCode: String): Couple {
        val myUid = currentUid()
        val myEmail = currentEmail()

        if (myUid.isBlank()) throw IllegalStateException("User not authenticated")
        if (enteredCode.length != 6) throw IllegalArgumentException("Code must be 6 digits")

        val found = findCodeOwner(enteredCode) ?: throw IllegalArgumentException("Invalid partner code")
        val partnerUid = found.first
        val partnerEmail = found.second

        if (partnerUid == myUid) {
            throw IllegalArgumentException("You cannot use your own code")
        }

        val myUserDoc = db.collection("users").document(myUid).get().await()
        val partnerUserDoc = db.collection("users").document(partnerUid).get().await()

        val myCoupleId = myUserDoc.getString("coupleId").orEmpty()
        val partnerCoupleId = partnerUserDoc.getString("coupleId").orEmpty()

        if (myCoupleId.isNotBlank()) throw IllegalStateException("You are already linked")
        if (partnerCoupleId.isNotBlank()) throw IllegalStateException("This partner is already linked")

        val coupleRef = db.collection("couples").document()
        val couple = Couple(
            coupleId = coupleRef.id,
            user1Uid = myUid,
            user2Uid = partnerUid,
            user1Email = myEmail,
            user2Email = partnerEmail,
            status = "active"
        )

        coupleRef.set(couple).await()

        db.collection("users").document(myUid)
            .update(mapOf("coupleId" to couple.coupleId))
            .await()

        db.collection("users").document(partnerUid)
            .update(mapOf("coupleId" to couple.coupleId))
            .await()

        return couple
    }

    suspend fun getCurrentCouple(): Couple? {
        val uid = currentUid()
        if (uid.isBlank()) return null

        val userDoc = db.collection("users").document(uid).get().await()
        val coupleId = userDoc.getString("coupleId").orEmpty()
        if (coupleId.isBlank()) return null

        val coupleDoc = db.collection("couples").document(coupleId).get().await()
        return coupleDoc.toObject(Couple::class.java)
    }

    fun observeCurrentUserCoupleId(
        onChanged: (String?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        val uid = currentUid()
        if (uid.isBlank()) throw IllegalStateException("User not authenticated")

        return db.collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val coupleId = snapshot?.getString("coupleId")
                onChanged(coupleId)
            }
    }

    suspend fun getCoupleById(coupleId: String): Couple? {
        if (coupleId.isBlank()) return null
        val doc = db.collection("couples").document(coupleId).get().await()
        return doc.toObject(Couple::class.java)
    }

    suspend fun unlinkCurrentCouple() {
        val myUid = currentUid()
        if (myUid.isBlank()) throw IllegalStateException("User not authenticated")

        val myUserDoc = db.collection("users").document(myUid).get().await()
        val coupleId = myUserDoc.getString("coupleId").orEmpty()
        if (coupleId.isBlank()) return

        val coupleDoc = db.collection("couples").document(coupleId).get().await()
        val couple = coupleDoc.toObject(Couple::class.java) ?: return

        db.collection("users").document(couple.user1Uid)
            .update(mapOf("coupleId" to ""))
            .await()

        db.collection("users").document(couple.user2Uid)
            .update(mapOf("coupleId" to ""))
            .await()

        db.collection("couples").document(coupleId).delete().await()
    }

    private suspend fun generateUniqueCode(): String {
        repeat(20) {
            val code = Random.nextInt(100000, 1000000).toString()
            val exists = db.collection("invite_codes")
                .whereEqualTo("code", code)
                .whereEqualTo("status", "active")
                .get()
                .await()

            if (exists.isEmpty) return code
        }
        throw IllegalStateException("Unable to generate unique code")
    }
}