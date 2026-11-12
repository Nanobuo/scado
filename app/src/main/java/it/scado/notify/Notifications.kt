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

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, "Scadenze", NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Promemoria dei cibi in scadenza" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun showExpiry(context: Context, id: Int, name: String, expiryEpochDay: Long) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
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
