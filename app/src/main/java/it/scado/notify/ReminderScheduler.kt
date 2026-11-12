package it.scado.notify

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import it.scado.data.AppSettings
import it.scado.data.Product
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Trasforma la lista dei prodotti in notifiche programmate (una per ogni
 * promemoria di ogni prodotto). È idempotente: si può chiamare ogni volta che
 * la lista cambia, aggiunge ciò che manca e cancella ciò che non serve più.
 */
object ReminderScheduler {
    private const val TAG = "scado_reminder"
    private const val NAME_PREFIX = "name:"

    const val KEY_HID = "hid"
    const val KEY_PID = "pid"
    const val KEY_NAME = "name"
    const val KEY_EXPIRY = "expiry"

    suspend fun sync(context: Context, householdId: String, products: List<Product>, settings: AppSettings) {
        val wm = WorkManager.getInstance(context)
        val now = ZonedDateTime.now()
        val zone = ZoneId.systemDefault()
        val wanted = HashSet<String>()

        for (p in products) {
            for (daysBefore in p.reminderDays.distinct()) {
                val at = LocalDate.ofEpochDay(p.expiryEpochDay - daysBefore)
                    .atTime(settings.notifyHour, settings.notifyMinute)
                    .atZone(zone)
                if (!at.isAfter(now)) continue

                // Il nome contiene tutto ciò che determina l'orario: se qualcosa cambia,
                // il vecchio lavoro viene cancellato e ne nasce uno nuovo.
                val name = "rem_${householdId}_${p.id}_${p.expiryEpochDay}_${daysBefore}_" +
                        "${settings.notifyHour}_${settings.notifyMinute}"
                wanted += name

                val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setInitialDelay(Duration.between(now, at).toMillis(), TimeUnit.MILLISECONDS)
                    .setInputData(
                        workDataOf(
                            KEY_HID to householdId,
                            KEY_PID to p.id,
                            KEY_NAME to p.name,
                            KEY_EXPIRY to p.expiryEpochDay,
                        )
                    )
                    .addTag(TAG)
                    .addTag(NAME_PREFIX + name)
                    .build()
                wm.enqueueUniqueWork(name, ExistingWorkPolicy.KEEP, request)
            }
        }

        for (info in wm.getWorkInfosByTagFlow(TAG).first()) {
            if (info.state.isFinished) continue
            val name = info.tags.firstOrNull { it.startsWith(NAME_PREFIX) }
                ?.removePrefix(NAME_PREFIX) ?: continue
            if (name !in wanted) wm.cancelUniqueWork(name)
        }
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
    }
}
