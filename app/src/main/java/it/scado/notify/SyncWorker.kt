package it.scado.notify

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import it.scado.ScadoApp
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Ogni qualche ora scarica i prodotti e riprogramma le notifiche, così i
 * prodotti aggiunti dall'altro membro della famiglia vengono ricordati anche
 * se l'app non è stata aperta.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val c = (applicationContext as ScadoApp).container
        val uid = c.auth.currentUser?.uid ?: return Result.success()
        return try {
            val hid = c.householdRepo.fetchHouseholdId(uid)
            if (hid == null) {
                ReminderScheduler.cancelAll(applicationContext)
            } else {
                val products = c.productRepo.fetchOnce(hid)
                ReminderScheduler.sync(applicationContext, hid, products, c.settingsRepo.settings.first())
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "scado_sync", ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
