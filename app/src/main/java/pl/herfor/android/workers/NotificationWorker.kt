package pl.herfor.android.workers

import android.app.PendingIntent
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.coroutineScope
import org.koin.core.KoinComponent
import org.koin.core.inject
import pl.herfor.android.R
import pl.herfor.android.interfaces.ContextRepository
import pl.herfor.android.modules.DatabaseModule
import pl.herfor.android.modules.IntentModule
import pl.herfor.android.modules.LocationModule
import pl.herfor.android.modules.PreferencesModule
import pl.herfor.android.objects.Report
import pl.herfor.android.objects.enums.NotificationStatus
import pl.herfor.android.objects.enums.Severity
import pl.herfor.android.retrofits.RetrofitRepository
import pl.herfor.android.utils.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.roundToLong
import kotlin.random.Random


class NotificationWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams), KoinComponent {
    private val appContext: ContextRepository by inject()
    private val retrofit: RetrofitRepository by inject()
    private val preferences: PreferencesModule by inject()
    private val intents: IntentModule by inject()
    private val location: LocationModule by inject()
    private val database: DatabaseModule by inject()

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
                Log.e(this.javaClass.name, "Received message with no action, ignoring...")
            }
        }

        // Indicate whether the task finished successfully with the Result
        return@coroutineScope Result.success()
    }

    private fun handleNewReport(report: Report) {
        database.getReportDao().insert(report)
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

        if (alreadyGraded(report.id) || alreadyShown(report.id) || !shouldShowNotification(report)) {
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
        if (Constants.DEV_MODE) {
            return true
        }
        val severities = preferences.getSeverities()
        val accidentTypes = preferences.getAccidents()
        val displayNotifications = preferences.getSilentZoneNotificationCondition()

        if (!displayNotifications || !report.isVisible(severities, accidentTypes)) {
            return false
        }

        when {
            severities.contains(Severity.GREEN) -> when (report.properties.severity) {
                Severity.GREEN -> if (Random.nextInt(0, 100) < 50) return false
                Severity.YELLOW -> if (Random.nextInt(0, 100) < 100) return false
                Severity.RED -> if (Random.nextInt(0, 100) < 100) return false
                Severity.NONE -> return false
            }
            severities.contains(Severity.YELLOW) -> when (report.properties.severity) {
                Severity.GREEN -> if (Random.nextInt(0, 100) < 30) return false
                Severity.YELLOW -> if (Random.nextInt(0, 100) < 80) return false
                Severity.RED -> if (Random.nextInt(0, 100) < 100) return false
                Severity.NONE -> return false
            }
            severities.contains(Severity.RED) -> when (report.properties.severity) {
                Severity.GREEN -> if (Random.nextInt(0, 100) < 15) return false
                Severity.YELLOW -> if (Random.nextInt(0, 100) < 50) return false
                Severity.RED -> if (Random.nextInt(0, 100) < 100) return false
                Severity.NONE -> return false
            }
        }

        val distance = calculateDistance(report)

        val currentActivity =
            preferences.getCurrentActivity()

        if (distance == -1L || distance > currentActivity.toDetectedActivityDistance()) {
            return false
        }

        return true
    }

    private fun alreadyShown(id: String): Boolean {
        val marker = database.getReportDao().getOne(id).value
            ?: return false

        return marker.properties.notificationStatus != NotificationStatus.NotShown
    }

    private fun alreadyGraded(id: String): Boolean {
        val grades = database.getGradeDao().getGradesByReportIdSync(id)
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
