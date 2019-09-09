package pl.herfor.android.services

import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.GsonBuilder
import pl.herfor.android.R
import pl.herfor.android.objects.MarkerData
import pl.herfor.android.utils.Constants.Companion.NOTIFICATION_CHANNEL_ID
import kotlin.random.Random

class NotificationService : FirebaseMessagingService() {
    override fun onMessageReceived(p0: RemoteMessage) {

        val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create()

        val marker = gson.fromJson(p0.data["marker"], MarkerData().javaClass)

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("OBS! notification") // TODO: Address
            .setContentText("Received from server") //  TODO: Podstawowy opis (tj. żółty autobus)
            .setWhen(marker.properties.creationDate.time)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(BitmapFactory.decodeResource(resources, marker.properties.getGlyph()))
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(
                        BitmapFactory.decodeResource(
                            resources,
                            R.drawable.common_google_signin_btn_icon_dark
                        )
                    ) //TODO: mapka
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(this)) {
            notify(Random(System.currentTimeMillis()).nextInt(), builder.build())
        }
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }
}