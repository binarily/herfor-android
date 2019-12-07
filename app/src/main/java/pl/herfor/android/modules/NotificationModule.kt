package pl.herfor.android.modules

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.firebase.messaging.FirebaseMessaging
import org.koin.core.KoinComponent
import org.koin.core.inject
import pl.herfor.android.R
import pl.herfor.android.interfaces.ContextRepository
import pl.herfor.android.services.ActivityRecognitionService
import pl.herfor.android.utils.Constants

class NotificationModule : KoinComponent {
    val context: ContextRepository by inject()

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getContext().getString(R.string.channel_name)
            val descriptionText = context.getContext().getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel =
                NotificationChannel(Constants.NOTIFICATION_CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }

            val notificationManager: NotificationManager =
                context.getContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        } else {
            Log.d(this.javaClass.name, "Notification channel not necessary on this platform.")
        }
    }

    fun registerToNotifications() {
        FirebaseMessaging.getInstance().subscribeToTopic("report-new")
        FirebaseMessaging.getInstance().subscribeToTopic("report-update")
        FirebaseMessaging.getInstance().subscribeToTopic("report-remove")
    }

    fun receiveGeofenceRadiusUpdates() {
        val pendingIntent = PendingIntent.getService(
            context.getContext(),
            0,
            Intent(context.getContext(), ActivityRecognitionService::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        ActivityRecognition.getClient(context.getContext())
            .requestActivityTransitionUpdates(
                ActivityTransitionRequest(Constants.TRANSITIONS),
                pendingIntent
            )
    }

}