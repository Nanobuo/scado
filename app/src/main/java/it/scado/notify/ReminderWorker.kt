package it.scado.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/** Mostra la notifica, dopo aver controllato che il prodotto sia ancora nel frigo. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (FirebaseAuth.getInstance().currentUser == null) return Result.success()
        val hid = inputData.getString(ReminderScheduler.KEY_HID) ?: return Result.success()
        val pid = inputData.getString(ReminderScheduler.KEY_PID) ?: return Result.success()
        var name = inputData.getString(ReminderScheduler.KEY_NAME) ?: ""
        val expiry = inputData.getLong(ReminderScheduler.KEY_EXPIRY, Long.MIN_VALUE)

        val doc = withTimeoutOrNull(15_000) {
            runCatching {
                FirebaseFirestore.getInstance()
                    .collection("households").document(hid)
                    .collection("products").document(pid)
                    .get().await()
            }.getOrNull()
        }
        if (doc != null) {
            if (!doc.exists()) return Result.success()          // già consumato
            if (doc.getLong("expiryEpochDay") != expiry) return Result.success() // data cambiata
            name = doc.getString("name") ?: name
        }
        // Se non riesco a verificare (offline) avviso comunque: meglio un avviso in più.
        Notifications.showExpiry(applicationContext, (hid + pid).hashCode(), name, expiry)
        return Result.success()
    }
}
