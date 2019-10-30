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
import pl.herfor.android.contexts.AppContext
import pl.herfor.android.objects.Report
import pl.herfor.android.objects.enums.NotificationStatus
import pl.herfor.android.objects.enums.Severity
import pl.herfor.android.retrofits.RetrofitRepository
import pl.herfor.android.services.NotificationDeletedService
import pl.herfor.android.services.NotificationGradingService
import pl.herfor.android.utils.*
import pl.herfor.android.viewmodels.ReportViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.ExecutionException
import kotlin.math.roundToLong
import kotlin.random.Random


class NotificationWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    private val appContext = AppContext(applicationContext)
    private val model = ReportViewModel(appContext.getContext())
    private val retrofit = RetrofitRepository(model)
    private val openAppIntent =
        PendingIntent.getActivity(
            appContext.getContext(),
            Random.nextInt(),
            Intent(appContext.getContext(), MapsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(
                    Constants.INTENT_REPORT_ID_KEY,
                    inputData.getString(Constants.NOTIFICATION_MESSAGE_ID_KEY)
                )
            },
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    private val relevantGradeIntent =
        PendingIntent.getService(
            appContext.getContext(),
            Random.nextInt(),
            Intent(appContext.getContext(), NotificationGradingService::class.java).apply {
                putExtra(
                    NotificationGradingService.REPORT_ID_PARAM,
                    inputData.getString(Constants.NOTIFICATION_MESSAGE_ID_KEY)
                )
                action = NotificationGradingService.GRADE_RELEVANT_ACTION
            },
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    private val notRelevantGradeIntent =
        PendingIntent.getService(
            appContext.getContext(),
            Random.nextInt(),
            Intent(appContext.getContext(), NotificationGradingService::class.java).apply {
                putExtra(
                    NotificationGradingService.REPORT_ID_PARAM,
                    inputData.getString(Constants.NOTIFICATION_MESSAGE_ID_KEY)
                )
                action = NotificationGradingService.GRADE_NOT_RELEVANT_ACTION
            },
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    private val sharedPreferences =
        appContext.getSharedPreferences("preferences", Context.MODE_PRIVATE)

    override suspend fun doWork(): Result = coroutineScope {

        when (inputData.getString(Constants.NOTIFICATION_MESSAGE_ACTION_KEY)) {
            Constants.ACTION_NEW -> {
                val report = Constants.GSON.fromJson(
                    inputData.getString(Constants.NOTIFICATION_MESSAGE_REPORT_KEY),
                    Report::class.java
                )
                handleNewReport(report)
            }
            Constants.ACTION_UPDATE -> {
                handleUpdateReport()
            }
            Constants.ACTION_REMOVE -> {
                handleRemoveReport()
            }
            else -> {
                Log.e(this.javaClass.name, "Received message with no action, ignoring...")
            }
        }

        // Indicate whether the task finished successfully with the Result
        return@coroutineScope Result.success()
    }

    private fun handleNewReport(report: Report) {
        appContext.getDatabase().reportDao().insert(report)
        val distance = calculateDistance(report, appContext)
        if (!shouldShowNotification(report)) {
            return
        }

        val location =
            appContext.getLocationName(
                report.location.latitude,
                report.location.longitude
            )

        createNotification(location, distance, report, openAppIntent)

        appContext.getDatabase().reportDao()
            .updateNotificationStatus(NotificationStatus.Shown, report.id)
        return
    }

    private fun handleUpdateReport() {
        val id = inputData.getString(Constants.NOTIFICATION_MESSAGE_ID_KEY) ?: return
        val newSeverity = inputData.getString(Constants.NOTIFICATION_MESSAGE_SEVERITY_KEY) ?: return

        val report = appContext.getDatabase().reportDao().getOne(id).value
        if (report != null) {
            appContext.getDatabase().reportDao()
                .updateSeverity(Severity.valueOf(newSeverity), id)
        } else {
            retrofit.loadReport(id, newReportCallback())
            return
        }

        if (alreadyGraded(report.id) || alreadyShown(report.id) || !shouldShowNotification(report)) {
            return
        }

        val location =
            appContext.getLocationName(
                report.location.latitude,
                report.location.longitude
            )
        val distance = calculateDistance(report, appContext)
        createNotification(location, distance, report, openAppIntent)

        appContext.getDatabase().reportDao()
            .updateNotificationStatus(NotificationStatus.Shown, report.id)
    }

    private fun handleRemoveReport() {
        with(NotificationManagerCompat.from(appContext.getContext())) {
            val id = inputData.getString(Constants.NOTIFICATION_MESSAGE_ID_KEY)
            if (id != null) {
                cancel(id, 0)
                appContext.getDatabase().reportDao().deleteById(id)
            }
        }
    }

    //Condition methods
    private fun shouldShowNotification(marker: Report): Boolean {
        val severities = sharedPreferences.getSeverities()
        val accidentTypes = sharedPreferences.getAccidentTypes()
        if (!marker.isVisible(severities, accidentTypes)) {
            return false
        }

        when {
            severities.contains(Severity.GREEN) -> when (marker.properties.severity) {
                Severity.GREEN -> if (Random.nextInt(0, 100) < 50) return false
                Severity.YELLOW -> if (Random.nextInt(0, 100) < 100) return false
                Severity.RED -> if (Random.nextInt(0, 100) < 100) return false
                Severity.NONE -> return false
            }
            severities.contains(Severity.YELLOW) -> when (marker.properties.severity) {
                Severity.GREEN -> if (Random.nextInt(0, 100) < 30) return false
                Severity.YELLOW -> if (Random.nextInt(0, 100) < 80) return false
                Severity.RED -> if (Random.nextInt(0, 100) < 100) return false
                Severity.NONE -> return false
            }
            severities.contains(Severity.RED) -> when (marker.properties.severity) {
                Severity.GREEN -> if (Random.nextInt(0, 100) < 15) return false
                Severity.YELLOW -> if (Random.nextInt(0, 100) < 50) return false
                Severity.RED -> if (Random.nextInt(0, 100) < 100) return false
                Severity.NONE -> return false
            }
        }

        val distance = calculateDistance(marker, appContext)

        val currentActivity =
            sharedPreferences.getInt("currentActivity", DetectedActivity.STILL)

        if (distance == -1L || distance > currentActivity.toDetectedActivityDistance()) {
            return false
        }

        return true
    }

    private fun alreadyShown(id: String): Boolean {
        val marker = appContext.getDatabase().reportDao().getOne(id).value
            ?: return false

        return marker.properties.notificationStatus != NotificationStatus.NotShown
    }

    private fun alreadyGraded(id: String): Boolean {
        val grades = appContext.getDatabase().gradeDao().getGradesByReportIdSync(id)
        return grades.isNotEmpty()
    }

    //Helper methods
    private fun createNotification(
        location: String,
        distance: Long,
        marker: Report,
        pendingIntent: PendingIntent
    ) {
        val notification =
            NotificationCompat.Builder(
                appContext.getContext(),
                Constants.NOTIFICATION_CHANNEL_ID
            )
                .setContentTitle(location)
                .setContentText(
                    appContext.getContext().getString(R.string.distance_string).format(
                        distance
                    )
                )
                .setWhen(marker.properties.creationDate.toEpochSecond() * 1000)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(
                    BitmapFactory.decodeResource(
                        appContext.getContext().resources,
                        marker.properties.getGlyph()
                    )
                )
                .addAction(
                    R.drawable.ic_relevant_check,
                    appContext.getString(R.string.relevant_button_string), relevantGradeIntent
                )
                .addAction(
                    R.drawable.ic_irrelevant_cross,
                    appContext.getString(R.string.irrelevant_button_string),
                    notRelevantGradeIntent
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

        notification.deleteIntent =
            PendingIntent.getService(
                appContext.getContext(), 0,
                Intent(appContext.getContext(), NotificationDeletedService::class.java).apply {
                    putExtra(Constants.NOTIFICATION_MESSAGE_ID_KEY, marker.id)
                }, 0
            )

        with(NotificationManagerCompat.from(appContext.getContext())) {
            notify(marker.id, 0, notification)
        }

    }

    private fun calculateDistance(
        marker: Report,
        appContext: AppContext
    ): Long {
        val markerLocation = marker.location.toLocation()
        return try {
            Tasks.await(appContext.getCurrentLocation())
                .distanceTo(markerLocation).roundToLong()
        } catch (e: ExecutionException) {
            -1L
        }
    }

    //Callbacks
    private fun newReportCallback(): Callback<Report> {
        return object : Callback<Report> {
            override fun onFailure(call: Call<Report>, t: Throwable) {
                Log.d("NotificationWorker", "Received update to non-existing marker, ignoring...")
            }

            override fun onResponse(call: Call<Report>, response: Response<Report>) {
                if (response.body() != null) {
                    handleNewReport(response.body()!!)
                }
            }
        }
    }
}
