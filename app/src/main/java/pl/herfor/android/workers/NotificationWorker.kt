package pl.herfor.android.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.location.Location
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.GsonBuilder
import kotlinx.coroutines.coroutineScope
import pl.herfor.android.R
import pl.herfor.android.activities.MapsActivity
import pl.herfor.android.contexts.MarkerContext
import pl.herfor.android.objects.MarkerData
import pl.herfor.android.utils.Constants
import kotlin.math.roundToLong
import kotlin.random.Random

class NotificationWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = coroutineScope {
        val markerContext = MarkerContext(applicationContext)

        val intent = Intent(applicationContext, MapsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        intent.putExtra("id", inputData.getString("id"))
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(applicationContext, 0, intent, 0)

        val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create()
        val marker = gson.fromJson(inputData.getString("marker"), MarkerData().javaClass)

        val markerLocation = Location("")
        markerLocation.latitude = marker.location.latitude
        markerLocation.longitude = marker.location.longitude
        var distance = 0L
        markerContext.getCurrentLocation().addOnSuccessListener {
            distance =
                markerLocation.distanceTo(it).roundToLong()
        }

        val location =
            markerContext.getLocationName(marker.location.latitude, marker.location.longitude)

        val builder =
            NotificationCompat.Builder(applicationContext, Constants.NOTIFICATION_CHANNEL_ID)
                .setContentTitle(location)
                .setContentText(
                    applicationContext.getString(R.string.distance_string).format(
                        distance
                    )
                )
                .setWhen(marker.properties.creationDate.time)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(
                    BitmapFactory.decodeResource(
                        applicationContext.resources,
                        marker.properties.getGlyph()
                    )
                )
                /*.setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(
                            BitmapFactory.decodeResource(
                                applicationContext.resources,
                                R.drawable.common_google_signin_btn_icon_dark
                            )
                        ) //TODO: mapka
                )*/
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

        with(NotificationManagerCompat.from(applicationContext)) {
            notify(Random(System.currentTimeMillis()).nextInt(), builder.build())
        }

        // Indicate whether the task finished successfully with the Result
        Result.success()
    }
}
