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
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.coroutineScope
import pl.herfor.android.R
import pl.herfor.android.activities.MapsActivity
import pl.herfor.android.contexts.MarkerContext
import pl.herfor.android.objects.MarkerData
import pl.herfor.android.objects.NotificationStatus
import pl.herfor.android.objects.enums.Severity
import pl.herfor.android.services.NotificationDeletedService
import pl.herfor.android.utils.*
import java.util.concurrent.ExecutionException
import kotlin.math.roundToLong
import kotlin.random.Random


class NotificationWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    private val markerContext = MarkerContext(applicationContext)
    private val openAppIntent
            : PendingIntent =
        PendingIntent.getActivity(
            markerContext.getContext(),
            Random.nextInt(),
            Intent(markerContext.getContext(), MapsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(
                    Constants.INTENT_MARKER_ID_KEY,
                    inputData.getString(Constants.NOTIFICATION_MESSAGE_ID_KEY)
                )
            },
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    private val sharedPreferences =
        markerContext.getSharedPreferences("preferences", Context.MODE_PRIVATE)

    override suspend fun doWork(): Result = coroutineScope {

        when (inputData.getString(Constants.NOTIFICATION_MESSAGE_ACTION_KEY)) {
            Constants.ACTION_NEW -> {
                val marker = Constants.GSON.fromJson(
                    inputData.getString(Constants.NOTIFICATION_MESSAGE_MARKER_KEY),
                    MarkerData::class.java
                )

                markerContext.getDatabase().markerDao().insert(marker)
                val distance = calculateDistance(marker, markerContext)
                if (!shouldShowNotification(marker)) {
                    return@coroutineScope Result.success()
                }

                val location =
                    markerContext.getLocationName(
                        marker.location.latitude,
                        marker.location.longitude
                    )
                //TODO: don't show already shown notifications

                createNotification(location, distance, marker, openAppIntent)

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
                createNotification(location, distance, marker, openAppIntent)

                markerContext.getDatabase().markerDao()
                    .updateNotificationStatus(NotificationStatus.Shown, marker.id)
            }
            Constants.ACTION_REMOVE -> {
                with(NotificationManagerCompat.from(markerContext.getContext())) {
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

        //TODO: check if already graded: don't show if so
        return true
    }

    private fun calculateDistance(
        marker: MarkerData,
        markerContext: MarkerContext
    ): Long {
        val markerLocation = marker.location.toLocation()
        return try {
            Tasks.await(markerContext.getCurrentLocation())
                .distanceTo(markerLocation).roundToLong()
        } catch (e: ExecutionException) {
            -1L
        }
    }

    private fun createNotification(
        location: String,
        distance: Long,
        marker: MarkerData,
        pendingIntent: PendingIntent
    ) {
        val notification =
            NotificationCompat.Builder(
                markerContext.getContext(),
                Constants.NOTIFICATION_CHANNEL_ID
            )
                .setContentTitle(location)
                .setContentText(
                    markerContext.getContext().getString(R.string.distance_string).format(
                        distance
                    )
                )
                //TODO: fix this (shows date in 1970)
                .setWhen(marker.properties.creationDate.toEpochSecond())
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(
                    BitmapFactory.decodeResource(
                        markerContext.getContext().resources,
                        marker.properties.getGlyph()
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

        notification.deleteIntent =
            PendingIntent.getService(
                markerContext.getContext(), 0,
                Intent(markerContext.getContext(), NotificationDeletedService::class.java).apply {
                    putExtra(Constants.NOTIFICATION_MESSAGE_ID_KEY, marker.id)
                }, 0
            )

        with(NotificationManagerCompat.from(markerContext.getContext())) {
            notify(marker.id, 0, notification)
        }
    }
}
