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
import pl.herfor.android.retrofits.RetrofitRepository
import pl.herfor.android.services.NotificationDeletedService
import pl.herfor.android.services.NotificationGradingService
import pl.herfor.android.utils.*
import pl.herfor.android.viewmodels.MarkerViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.ExecutionException
import kotlin.math.roundToLong
import kotlin.random.Random


class NotificationWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    private val markerContext = MarkerContext(applicationContext)
    private val model = MarkerViewModel(markerContext.getContext())
    private val retrofit = RetrofitRepository(model)
    private val openAppIntent =
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
    private val relevantGradeIntent =
        PendingIntent.getService(
            markerContext.getContext(),
            Random.nextInt(),
            Intent(markerContext.getContext(), NotificationGradingService::class.java).apply {
                putExtra(
                    NotificationGradingService.MARKER_ID_PARAM,
                    inputData.getString(Constants.NOTIFICATION_MESSAGE_ID_KEY)
                )
                action = NotificationGradingService.GRADE_RELEVANT_ACTION
            },
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    private val notRelevantGradeIntent =
        PendingIntent.getService(
            markerContext.getContext(),
            Random.nextInt(),
            Intent(markerContext.getContext(), NotificationGradingService::class.java).apply {
                putExtra(
                    NotificationGradingService.MARKER_ID_PARAM,
                    inputData.getString(Constants.NOTIFICATION_MESSAGE_ID_KEY)
                )
                action = NotificationGradingService.GRADE_NOT_RELEVANT_ACTION
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
                handleNewMarker(marker)
            }
            Constants.ACTION_UPDATE -> {
                handleUpdateMarker()
            }
            Constants.ACTION_REMOVE -> {
                handleRemoveMarker()
            }
            else -> {
                Log.e(this.javaClass.name, "Received message with no action, ignoring...")
            }
        }

        // Indicate whether the task finished successfully with the Result
        return@coroutineScope Result.success()
    }

    private fun handleRemoveMarker() {
        with(NotificationManagerCompat.from(markerContext.getContext())) {
            val id = inputData.getString(Constants.NOTIFICATION_MESSAGE_ID_KEY)
            if (id != null) {
                cancel(id, 0)
                markerContext.getDatabase().markerDao().deleteById(id)
            }
        }
    }

    private fun handleUpdateMarker() {
        val id = inputData.getString(Constants.NOTIFICATION_MESSAGE_ID_KEY) ?: return
        val newSeverity = inputData.getString(Constants.NOTIFICATION_MESSAGE_SEVERITY_KEY) ?: return

        val marker = markerContext.getDatabase().markerDao().getOne(id).value
        if (marker != null) {
            markerContext.getDatabase().markerDao()
                .updateSeverity(Severity.valueOf(newSeverity), id)
        } else {
            retrofit.loadMarker(id, newMarkerCallback())
            return
        }

        if (alreadyGraded(marker.id) || alreadyShown(marker.id) || !shouldShowNotification(marker)) {
            return
        }

        val location =
            markerContext.getLocationName(
                marker.location.latitude,
                marker.location.longitude
            )
        val distance = calculateDistance(marker, markerContext)
        createNotification(location, distance, marker, openAppIntent)

        markerContext.getDatabase().markerDao()
            .updateNotificationStatus(NotificationStatus.Shown, marker.id)
    }

    private fun newMarkerCallback(): Callback<MarkerData> {
        return object : Callback<MarkerData> {
            override fun onFailure(call: Call<MarkerData>, t: Throwable) {
                Log.d("NotificationWorker", "Received update to non-existing marker, ignoring...")
            }

            override fun onResponse(call: Call<MarkerData>, response: Response<MarkerData>) {
                if (response.body() != null) {
                    handleNewMarker(response.body()!!)
                }
            }
        }
    }

    private fun handleNewMarker(marker: MarkerData) {
        markerContext.getDatabase().markerDao().insert(marker)
        val distance = calculateDistance(marker, markerContext)
        if (!shouldShowNotification(marker)) {
            return
        }

        val location =
            markerContext.getLocationName(
                marker.location.latitude,
                marker.location.longitude
            )

        createNotification(location, distance, marker, openAppIntent)

        markerContext.getDatabase().markerDao()
            .updateNotificationStatus(NotificationStatus.Shown, marker.id)
        return
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
        return try {
            Tasks.await(markerContext.getCurrentLocation())
                .distanceTo(markerLocation).roundToLong()
        } catch (e: ExecutionException) {
            -1L
        }
    }

    private fun alreadyShown(id: String): Boolean {
        val marker = markerContext.getDatabase().markerDao().getOne(id).value
            ?: return false

        return marker.properties.notificationStatus != NotificationStatus.NotShown
    }

    private fun alreadyGraded(id: String): Boolean {
        val grades = markerContext.getDatabase().gradeDao().getGradesByMarkerIdSync(id)
        return grades.isNotEmpty()
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
                .setWhen(marker.properties.creationDate.toEpochSecond() * 1000)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(
                    BitmapFactory.decodeResource(
                        markerContext.getContext().resources,
                        marker.properties.getGlyph()
                    )
                )
                .addAction(
                    R.drawable.ic_relevant_check,
                    markerContext.getString(R.string.relevant_button_string), relevantGradeIntent
                )
                .addAction(
                    R.drawable.ic_irrelevant_cross,
                    markerContext.getString(R.string.irrelevant_button_string),
                    notRelevantGradeIntent
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
