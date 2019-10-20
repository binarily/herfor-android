package pl.herfor.android.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.DetectedActivity
import kotlinx.coroutines.coroutineScope
import pl.herfor.android.R
import pl.herfor.android.activities.MapsActivity
import pl.herfor.android.contexts.MarkerContext
import pl.herfor.android.objects.MarkerData
import pl.herfor.android.objects.NotificationStatus
import pl.herfor.android.objects.Severity
import pl.herfor.android.services.NotificationDeletedService
import pl.herfor.android.utils.*
import kotlin.math.roundToLong
import kotlin.random.Random


class NotificationWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    private val intent = Intent(applicationContext, MapsActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        putExtra(
            Constants.INTENT_MARKER_ID_KEY,
            inputData.getString(Constants.NOTIFICATION_MESSAGE_ID_KEY)
        )
    }
    private val pendingIntent
            : PendingIntent =
        PendingIntent.getActivity(
            applicationContext,
            Random.nextInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    private val sharedPreferences =
        applicationContext.getSharedPreferences("preferences", Context.MODE_PRIVATE)
    private val markerContext = MarkerContext(applicationContext)

    override suspend fun doWork(): Result = coroutineScope {

        when (inputData.getString(Constants.NOTIFICATION_MESSAGE_ACTION_KEY)) {
            Constants.ACTION_NEW -> {
                val marker = Constants.GSON.fromJson(
                    inputData.getString(Constants.NOTIFICATION_MESSAGE_MARKER_KEY),
                    MarkerData::class.java
                )

                val distance = calculateDistance(marker, markerContext)
                if (!shouldShowNotification(marker)) {
                    return@coroutineScope Result.success()
                }

                val location =
                    markerContext.getLocationName(
                        marker.location.latitude,
                        marker.location.longitude
                    )

                createNotification(location, distance, marker, pendingIntent)

                markerContext.getDatabase().markerDao()
                    .updateNotificationStatus(NotificationStatus.Shown, marker.id)
            }
            Constants.ACTION_UPDATE -> {
                val id = inputData.getString(Constants.NOTIFICATION_MESSAGE_ID_KEY)
                val newSeverity = inputData.getString(Constants.NOTIFICATION_MESSAGE_SEVERITY_KEY)
                if (id == null || newSeverity == null) {
                    return@coroutineScope Result.failure()
                }

                markerContext.getDatabase().markerDao()
                    .updateSeverity(Severity.valueOf(newSeverity), id)
                val marker = markerContext.getDatabase().markerDao().getOne(id).value
                    ?: return@coroutineScope Result.failure()
                val distance = calculateDistance(marker, markerContext)

                when (marker.properties.notificationStatus) {
                    NotificationStatus.Dismissed -> return@coroutineScope Result.success()
                    NotificationStatus.NotShown -> {
                        if (!shouldShowNotification(marker)) {
                            return@coroutineScope Result.success()
                        }
                    }
                }

                val location =
                    markerContext.getLocationName(
                        marker.location.latitude,
                        marker.location.longitude
                    )
                createNotification(location, distance, marker, pendingIntent)
            }
            Constants.ACTION_REMOVE -> {
                with(NotificationManagerCompat.from(applicationContext)) {
                    val id = inputData.getString(Constants.NOTIFICATION_MESSAGE_ID_KEY)
                    if (id != null) {
                        cancel(id, 0)
                        markerContext.getDatabase().markerDao().deleteById(id)
                    }
                }
            }
            else -> {
                Log.e(this.javaClass.name, "Received message with no action, ignoring...")
            }
        }

        // Indicate whether the task finished successfully with the Result
        return@coroutineScope Result.success()
    }

    private fun shouldShowNotification(marker: MarkerData): Boolean {
        val severities = sharedPreferences.getSeverities()
        val accidentTypes = sharedPreferences.getAccidentTypes()
        if (!marker.isVisible(severities, accidentTypes)) {
            return false
        }

        val distance = calculateDistance(marker, markerContext)

        val currentActivity =
            sharedPreferences.getInt("currentActivity", DetectedActivity.STILL)

        if (distance == -1L || distance > currentActivity.toDetectedActivityDistance()) {
            return false
        }
        return true
    }

    private fun calculateDistance(
        marker: MarkerData,
        markerContext: MarkerContext
    ): Long {
        val markerLocation = marker.location.toLocation()
        var distance = -1L
        markerContext.getCurrentLocation().addOnSuccessListener {
            distance =
                markerLocation.distanceTo(it).roundToLong()
        }
        return distance
    }

    private fun createNotification(
        location: String,
        distance: Long,
        marker: MarkerData,
        pendingIntent: PendingIntent
    ) {
        val notification =
            NotificationCompat.Builder(
                applicationContext,
                Constants.NOTIFICATION_CHANNEL_ID
            )
                .setContentTitle(location)
                .setContentText(
                    applicationContext.getString(R.string.distance_string).format(
                        distance
                    )
                )
                .setWhen(marker.properties.creationDate.toEpochSecond())
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(
                    BitmapFactory.decodeResource(
                        applicationContext.resources,
                        marker.properties.getGlyph()
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

        notification.deleteIntent =
            PendingIntent.getService(applicationContext, 0,
                Intent(applicationContext, NotificationDeletedService::class.java).apply {
                    putExtra(Constants.NOTIFICATION_MESSAGE_ID_KEY, marker.id)
                }, 0
            )

        with(NotificationManagerCompat.from(applicationContext)) {
            notify(marker.id, 0, notification)
        }
    }
}
