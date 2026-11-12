package it.scado.data

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Prodotti di un frigo. Le scritture non attendono il server: Firestore le
 * salva subito in locale e le sincronizza appena c'è rete.
 */
class ProductRepository(private val db: FirebaseFirestore) {

    private fun col(hid: String) =
        db.collection("households").document(hid).collection("products")

    fun products(hid: String): Flow<List<Product>> = callbackFlow {
        val reg = col(hid).orderBy("expiryEpochDay").addSnapshotListener { snap, err ->
            if (err != null) {
                Log.w("ProductRepo", "products", err)
                return@addSnapshotListener
            }
            trySend(snap?.documents?.mapNotNull { it.toProduct() } ?: emptyList())
        }
        awaitClose { reg.remove() }
    }

    suspend fun fetchOnce(hid: String): List<Product> =
        col(hid).get().await().documents.mapNotNull { it.toProduct() }

    fun add(
        hid: String,
        name: String,
        expiryEpochDay: Long,
        reminderDays: Collection<Int>,
        uid: String,
        userName: String,
    ) {
        col(hid).document().set(
            mapOf(
                "name" to name,
                "expiryEpochDay" to expiryEpochDay,
                "reminderDays" to reminderDays.distinct().sorted(),
                "addedBy" to uid,
                "addedByName" to userName,
                "createdAt" to FieldValue.serverTimestamp(),
            )
        )
    }

    fun update(hid: String, id: String, name: String, expiryEpochDay: Long, reminderDays: Collection<Int>) {
        col(hid).document(id).update(
            mapOf(
                "name" to name,
                "expiryEpochDay" to expiryEpochDay,
                "reminderDays" to reminderDays.distinct().sorted(),
            )
        )
    }

    fun delete(hid: String, id: String) {
        col(hid).document(id).delete()
    }

    fun restore(hid: String, p: Product) {
        col(hid).document(p.id).set(
            mapOf(
                "name" to p.name,
                "expiryEpochDay" to p.expiryEpochDay,
                "reminderDays" to p.reminderDays,
                "addedBy" to p.addedBy,
                "addedByName" to p.addedByName,
                "createdAt" to FieldValue.serverTimestamp(),
            )
        )
    }
}

private fun DocumentSnapshot.toProduct(): Product? {
    val name = getString("name") ?: return null
    val expiry = getLong("expiryEpochDay") ?: return null
    val reminders = (get("reminderDays") as? List<*>)
        ?.mapNotNull { (it as? Number)?.toInt() }
        ?: emptyList()
    return Product(id, name, expiry, reminders, getString("addedBy"), getString("addedByName"))
}
