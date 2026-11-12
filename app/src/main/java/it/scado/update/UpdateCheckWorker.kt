package it.scado.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import it.scado.notify.Notifications
import java.util.concurrent.TimeUnit

/** Una volta al giorno controlla se c'è una nuova versione e, se sì, lo notifica (una volta sola). */
class UpdateCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val update = try {
            Updater.availableUpdate()
        } catch (e: Exception) {
            return Result.retry()
        } ?: return Result.success()

        val prefs = applicationContext.getSharedPreferences("updates", Context.MODE_PRIVATE)
        if (prefs.getInt(KEY_NOTIFIED, 0) < update.versionCode) {
            Notifications.showUpdate(applicationContext, update.versionName)
            prefs.edit().putInt(KEY_NOTIFIED, update.versionCode).apply()
        }
        return Result.success()
    }

    companion object {
        private const val KEY_NOTIFIED = "notified_version"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "scado_update_check", ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
