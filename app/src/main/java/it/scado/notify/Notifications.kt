package it.scado.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import it.scado.MainActivity
import it.scado.R
import it.scado.util.Dates
import it.scado.util.Emoji

object Notifications {
    const val CHANNEL_ID = "scadenze"
    const val UPDATES_CHANNEL_ID = "aggiornamenti"
    private const val UPDATE_NOTIFICATION_ID = 1

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, "Scadenze", NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Promemoria dei cibi in scadenza" }
        val updates = NotificationChannel(
            UPDATES_CHANNEL_ID, "Aggiornamenti", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Nuove versioni di Scado" }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannels(listOf(channel, updates))
    }

    @SuppressLint("MissingPermission")
    fun showUpdate(context: Context, versionName: String) {
        if (!canNotify(context)) return
        val notification = NotificationCompat.Builder(context, UPDATES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF1B8A6B.toInt())
            .setContentTitle("Nuova versione di Scado")
            .setContentText("La versione $versionName è pronta: apri l'app per aggiornare.")
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(UPDATE_NOTIFICATION_ID, notification)
    }

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    @SuppressLint("MissingPermission")
    fun showExpiry(context: Context, id: Int, name: String, expiryEpochDay: Long) {
        if (!canNotify(context)) return
        val pending = openAppIntent(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF1B8A6B.toInt())
            .setContentTitle("${Emoji.forName(name)} $name")
            .setContentText("${Dates.relative(expiryEpochDay)} · ${Dates.format(expiryEpochDay)}")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
