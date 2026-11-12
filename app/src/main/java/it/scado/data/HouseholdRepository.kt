package it.scado.data

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

/**
 * Gestisce i "frigo" (households). Struttura su Firestore:
 *
 *  users/{uid}                         -> { displayName, householdId }
 *  households/{hid}                    -> { name, members[], memberNames{}, inviteCode, ... }
 *  households/{hid}/products/{pid}     -> i prodotti del frigo
 *  households/{hid}/joins/{uid}        -> prova che chi è entrato conosceva il codice
 *  invites/{code}                      -> { householdId }
 *
 * Le regole in firestore.rules fanno sì che solo i membri vedano il contenuto.
 */
class HouseholdRepository(private val db: FirebaseFirestore) {

    private fun users() = db.collection("users")
    private fun households() = db.collection("households")
    private fun invites() = db.collection("invites")

    fun userProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val reg = users().document(uid).addSnapshotListener { snap, err ->
            if (err != null) {
                Log.w(TAG, "userProfile", err)
                return@addSnapshotListener
            }
            trySend(
                snap?.takeIf { it.exists() }?.let {
                    UserProfile(uid, it.getString("displayName"), it.getString("householdId"))
                }
            )
        }
        awaitClose { reg.remove() }
    }

    fun household(hid: String): Flow<Household?> = callbackFlow {
        val reg = households().document(hid).addSnapshotListener { snap, err ->
            if (err != null) {
                Log.w(TAG, "household", err)
                return@addSnapshotListener
            }
            trySend(snap?.toHousehold())
        }
        awaitClose { reg.remove() }
    }

    suspend fun fetchHouseholdId(uid: String): String? =
        users().document(uid).get().await().getString("householdId")

    suspend fun createHousehold(uid: String, userName: String, name: String): String {
        val ref = households().document()
        repeat(5) {
            val code = randomCode()
            val batch = db.batch()
            batch.set(
                ref, mapOf(
                    "name" to name.trim(),
                    "members" to listOf(uid),
                    "memberNames" to mapOf(uid to userName),
                    "inviteCode" to code,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.set(invites().document(code), mapOf("householdId" to ref.id))
            batch.set(
                users().document(uid),
                mapOf("householdId" to ref.id, "displayName" to userName),
                SetOptions.merge()
            )
            try {
                batch.commit().await()
                return ref.id
            } catch (e: FirebaseFirestoreException) {
                // Codice già usato da un altro frigo: ne provo un altro.
                if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
            }
        }
        error("Impossibile creare il frigo, riprova.")
    }

    suspend fun joinHousehold(uid: String, userName: String, rawCode: String): String {
        val code = rawCode.trim().uppercase()
        val invite = invites().document(code).get().await()
        val hid = invite.getString("householdId")
            ?: throw IllegalArgumentException("Codice non valido.")
        val householdRef = households().document(hid)
        val batch = db.batch()
        batch.set(
            householdRef.collection("joins").document(uid),
            mapOf("code" to code, "at" to FieldValue.serverTimestamp())
        )
        batch.update(
            householdRef, mapOf(
                "members" to FieldValue.arrayUnion(uid),
                "memberNames.$uid" to userName,
            )
        )
        batch.set(
            users().document(uid),
            mapOf("householdId" to hid, "displayName" to userName),
            SetOptions.merge()
        )
        batch.commit().await()
        return hid
    }

    suspend fun leaveHousehold(uid: String, hid: String) {
        val batch = db.batch()
        batch.update(
            households().document(hid), mapOf(
                "members" to FieldValue.arrayRemove(uid),
                "memberNames.$uid" to FieldValue.delete(),
            )
        )
        batch.set(users().document(uid), mapOf("householdId" to null), SetOptions.merge())
        batch.commit().await()
    }

    private fun DocumentSnapshot.toHousehold(): Household? {
        if (!exists()) return null
        @Suppress("UNCHECKED_CAST")
        return Household(
            id = id,
            name = getString("name") ?: "Il nostro frigo",
            members = (get("members") as? List<String>) ?: emptyList(),
            memberNames = (get("memberNames") as? Map<String, String>) ?: emptyMap(),
            inviteCode = getString("inviteCode") ?: "",
        )
    }

    companion object {
        private const val TAG = "HouseholdRepo"
        private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private val random = SecureRandom()
        fun randomCode(): String = (1..6).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")
    }
}
