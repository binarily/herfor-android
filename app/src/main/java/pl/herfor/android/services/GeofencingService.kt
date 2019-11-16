package pl.herfor.android.services

import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import org.koin.core.KoinComponent
import org.koin.core.inject
import pl.herfor.android.modules.NotificationGeofenceModule
import pl.herfor.android.modules.PreferencesModule
import pl.herfor.android.utils.Constants
import pl.herfor.android.workers.NotificationWorker

class GeofencingService : IntentService("GeofencingService"), KoinComponent {
    val notificationGeofence: NotificationGeofenceModule by inject()
    val preferences: PreferencesModule by inject()

    companion object {
        const val GEOFENCE_HOME = "home"
        const val GEOFENCE_WORK = "work"
        const val GEOFENCE_FOLLOW = "follow"

        internal fun geofencePendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, GeofencingService::class.java)

            return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            Log.e("Geofencing", geofencingEvent.errorCode.toString())
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            for (geofence in triggeringGeofences) {
                when (geofence.requestId) {
                    GEOFENCE_HOME, GEOFENCE_WORK -> turnOnNotifications()
                    GEOFENCE_FOLLOW -> triggerNotificationCheck()
                }
            }
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            for (geofence in triggeringGeofences) {
                when (geofence.requestId) {
                    GEOFENCE_HOME, GEOFENCE_WORK -> turnOffNotifications()
                    else -> Log.e("Geofences", "Incorrect transition")
                }
            }
        }
    }

    private fun turnOffNotifications() {
        preferences.setSilentZoneNotificationCondition(false)
    }

    private fun turnOnNotifications() {
        preferences.setSilentZoneNotificationCondition(true)
    }

    private fun triggerNotificationCheck() {
        val parameterMap = HashMap<String, String>()
        parameterMap[Constants.NOTIFICATION_MESSAGE_ACTION_KEY] = Constants.ACTION_REFRESH
        val workData = workDataOf(*parameterMap.toList().toTypedArray())
        val notificationWorkRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInputData(workData)
            .addTag(Constants.NOTIFICATION_WORKER_TAG)
            .build()
        WorkManager.getInstance(this).enqueue(notificationWorkRequest)

        notificationGeofence.registerFullGeofence()
    }
}
