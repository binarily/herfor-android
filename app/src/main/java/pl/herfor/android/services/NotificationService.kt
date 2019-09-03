package pl.herfor.android.services

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import pl.herfor.android.R
import pl.herfor.android.utils.Constants.Companion.NOTIFICATION_CHANNEL_ID
import kotlin.random.Random

class NotificationService : FirebaseMessagingService() {
    override fun onMessageReceived(p0: RemoteMessage) {

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("OBS! notification")
            .setContentText("Received from server")
            .setSmallIcon(R.drawable.ic_checked)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Latitude: ${p0.data["latitude"]}, longitude: ${p0.data["longitude"]}")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            notify(Random(System.currentTimeMillis()).nextInt(), builder.build())
        }
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }
}