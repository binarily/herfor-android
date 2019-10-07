package pl.herfor.android.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.coroutineScope
import pl.herfor.android.R
import pl.herfor.android.activities.MapsActivity
import pl.herfor.android.contexts.MarkerContext
import pl.herfor.android.objects.MarkerData
import pl.herfor.android.utils.Constants
import pl.herfor.android.utils.getAccidentTypes
import pl.herfor.android.utils.getSeverities
import pl.herfor.android.utils.toLocation
import kotlin.math.roundToLong
import kotlin.random.Random

class NotificationWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = coroutineScope {
        val markerContext = MarkerContext(applicationContext)

        val intent = Intent(applicationContext, MapsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        intent.putExtra(
            Constants.INTENT_MARKER_ID_KEY,
            inputData.getString(Constants.NOTIFICATION_MESSAGE_ID_KEY)
        )
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

        val marker = Constants.GSON.fromJson(
            inputData.getString(Constants.NOTIFICATION_MESSAGE_MARKER_KEY),
            MarkerData::class.java
        )

        //TODO: check severity and accident type for whether it's worth showing
        val sharedPreferences =
            applicationContext.getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val severities = sharedPreferences.getSeverities()
        val accidentTypes = sharedPreferences.getAccidentTypes()
        if (!(accidentTypes.contains(marker.properties.accidentType) && severities.contains(marker.properties.severityType))) {
            return@coroutineScope Result.success()
        }

        val markerLocation = marker.location.toLocation()
        var distance = -1L
        markerContext.getCurrentLocation().addOnSuccessListener {
            distance =
                markerLocation.distanceTo(it).roundToLong()
        }

        //TODO: make this dependent on current activity
        if (distance == -1L || distance > 100) {
            return@coroutineScope Result.success()
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
        return@coroutineScope Result.success()
    }
}
