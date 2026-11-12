package it.scado.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** Preferenze locali del telefono: promemoria suggeriti e orario delle notifiche. */
class SettingsRepository(private val context: Context) {
    private val keyDays = stringSetPreferencesKey("default_reminder_days")
    private val keyHour = intPreferencesKey("notify_hour")
    private val keyMinute = intPreferencesKey("notify_minute")

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        val defaults = AppSettings()
        AppSettings(
            defaultReminderDays = p[keyDays]?.mapNotNull { it.toIntOrNull() }?.toSet()
                ?: defaults.defaultReminderDays,
            notifyHour = p[keyHour] ?: defaults.notifyHour,
            notifyMinute = p[keyMinute] ?: defaults.notifyMinute,
        )
    }

    suspend fun setDefaultReminderDays(days: Set<Int>) {
        context.dataStore.edit { it[keyDays] = days.map(Int::toString).toSet() }
    }

    suspend fun setNotifyTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[keyHour] = hour
            it[keyMinute] = minute
        }
    }
}
