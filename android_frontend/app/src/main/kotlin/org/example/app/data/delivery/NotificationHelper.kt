package org.example.app.data.delivery

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.example.app.R

/**
 * Posts local notifications for simulated delivery status changes.
 *
 * No external services used.
 */
object NotificationHelper {
    private const val CHANNEL_ID = "delivery_status"
    private const val CHANNEL_NAME = "Delivery updates"

    // PUBLIC_INTERFACE
    fun ensureChannel(context: Context) {
        /** Create the notification channel on Android O+ (safe to call repeatedly). */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for delivery status changes"
        }
        manager.createNotificationChannel(channel)
    }

    // PUBLIC_INTERFACE
    fun postStageChanged(context: Context, orderId: String, restaurantName: String, stage: DeliveryStage) {
        /** Post a local notification for a stage transition. */
        ensureChannel(context)

        val title = context.getString(R.string.delivery_notification_title, restaurantName)
        val text = context.getString(R.string.delivery_notification_body, stage.displayLabel())

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_delivery)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Android 13+ requires runtime POST_NOTIFICATIONS permission, which may be denied.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        // Stable notification id per order so subsequent updates replace the previous one.
        val notificationId = ("delivery_" + orderId).hashCode()

        // Even with the permission check, be defensive in case OEMs throw SecurityException.
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Ignore: notifications are optional UX sugar for this simulated flow.
        }
    }
}
