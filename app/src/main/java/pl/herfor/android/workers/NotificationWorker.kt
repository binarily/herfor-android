package pl.herfor.android.workers

import android.app.PendingIntent
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.crashlytics.android.Crashlytics
import kotlinx.coroutines.coroutineScope
import org.koin.core.KoinComponent
import org.koin.core.inject
import pl.herfor.android.R
import pl.herfor.android.interfaces.ContextRepository
import pl.herfor.android.modules.*
import pl.herfor.android.objects.Report
import pl.herfor.android.objects.enums.NotificationStatus
import pl.herfor.android.objects.enums.Severity
import pl.herfor.android.retrofits.RetrofitRepository
import pl.herfor.android.utils.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.roundToLong


class NotificationWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams), KoinComponent {
    private val appContext: ContextRepository by inject()
    private val retrofit: RetrofitRepository by inject()
    private val preferences: PreferencesModule by inject()
    private val intents: IntentModule by inject()
    private val location: LocationModule by inject()
    private val database: DatabaseModule by inject()
    private val businessLogic: BusinessLogicModule by inject()

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
            Constants.ACTION_REFRESH -> {
                handleRefresh()
            }
            else -> {
                Crashlytics.log(Log.ERROR, "Notification worker", "Message with no action")
            }
        }

        // Indicate whether the task finished successfully with the Result
        return@coroutineScope Result.success()
    }

    private fun handleNewReport(report: Report) {
        if (database.getReportDao().getOneNow(report.id).isEmpty()) {
            database.getReportDao().insert(report)
        }
        handleReport(report)
        return
    }

    private fun handleUpdateReport() {
        val id = inputData.getString(Constants.NOTIFICATION_MESSAGE_ID_KEY) ?: return
        val newSeverity = inputData.getString(Constants.NOTIFICATION_MESSAGE_SEVERITY_KEY) ?: return

        val report = database.getReportDao().getOne(id).value
        if (report != null) {
            database.getReportDao()
                .updateSeverity(Severity.valueOf(newSeverity), id)
        } else {
            retrofit.loadReport(id, newReportCallback())
            return
        }

        if (businessLogic.alreadyGraded(report.id) || businessLogic.alreadyShown(report.id) || !shouldShowNotification(
                report
            )
        ) {
            return
        }

        val location =
            location.getLocationName(
                report.location.latitude,
                report.location.longitude
            )
        val distance = calculateDistance(report)
        createNotification(location, distance, report, intents.openAppIntent(id))

        database.getReportDao()
            .updateNotificationStatus(NotificationStatus.Shown, report.id)
    }

    private fun handleRemoveReport() {
        with(NotificationManagerCompat.from(appContext.getContext())) {
            val id = inputData.getString(Constants.NOTIFICATION_MESSAGE_ID_KEY)
            if (id != null) {
                cancel(id, 0)
                database.getReportDao().deleteById(id)
            }
        }
    }

    private fun handleRefresh() {
        val currentActivity = preferences.getCurrentActivity()
        val radius = currentActivity.toDetectedActivityDistance()
        val location = location.getCurrentLocation() ?: return
        val northEast = location.toNorthEast(radius.toDouble() / 2)
        val southWest = location.toSouthWest(radius.toDouble() / 2)

        val reports = database.getReportDao().getFromLocation(
            northEast.longitude, southWest.longitude,
            southWest.latitude, northEast.latitude
        ).value

        if (reports != null) {
            for (report in reports) {
                handleReport(report)
            }
        }
    }

    private fun handleReport(report: Report) {
        val distance = calculateDistance(report)
        if (!shouldShowNotification(report)) {
            return
        }

        val location =
            location.getLocationName(
                report.location.latitude,
                report.location.longitude
            )

        createNotification(location, distance, report, intents.openAppIntent(report.id))

        database.getReportDao()
            .updateNotificationStatus(NotificationStatus.Shown, report.id)
    }

    //Condition methods
    private fun shouldShowNotification(report: Report): Boolean {
        if (!businessLogic.showNotificationByFilter(report)) {
            return false
        }

        val distance = calculateDistance(report)

        val currentActivity =
            preferences.getCurrentActivity()

        if (distance == -1L || distance > currentActivity.toDetectedActivityDistance()) {
            return false
        }

        return true
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
                    appContext.getString(R.string.relevant_button_string),
                    intents.relevantGradeIntent(marker.id)
                )
                .addAction(
                    R.drawable.ic_irrelevant_cross,
                    appContext.getString(R.string.irrelevant_button_string),
                    intents.notRelevantGradeIntent(marker.id)
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

        notification.deleteIntent = intents.notificationDeleteIntent(marker.id)

        with(NotificationManagerCompat.from(appContext.getContext())) {
            notify(marker.id, 0, notification)
        }

    }

    private fun calculateDistance(
        marker: Report
    ): Long {
        val markerLocation = marker.location.toLocation()
        return location.getCurrentLocation()?.distanceTo(markerLocation)?.roundToLong() ?: -1L
    }

    //Callbacks
    private fun newReportCallback(): Callback<Report> {
        return object : Callback<Report> {
            override fun onFailure(call: Call<Report>, t: Throwable) {
                Crashlytics.log(Log.DEBUG, "Notification worker", "Update to non-existing marker")
            }

            override fun onResponse(call: Call<Report>, response: Response<Report>) {
                if (response.isSuccessful) {
                    if (response.body() != null) {
                        handleNewReport(response.body()!!)
                    }
                } else {
                    Crashlytics.log(
                        Log.DEBUG,
                        "Notification worker",
                        "Update to non-existing marker"
                    )
                }
            }
        }
    }
}
