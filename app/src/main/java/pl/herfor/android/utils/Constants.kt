package pl.herfor.android.utils

import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.DetectedActivity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.aaronhe.threetengson.ThreeTenGsonAdapter

class Constants {
    companion object {
        const val DEV_MODE = false
        const val SERVER_LOCATION = "https://obs-czerniakk.appspot.com/"

        const val NOTIFICATION_CHANNEL_ID = "obs-notifications"
        const val NOTIFICATION_WORKER_TAG = "notifications"
        const val ZOOM_LEVEL = 15.0F
        const val BUTTON_ANIMATION_DURATION = 200
        const val RIGHT_BUTTON_STATE_KEY = "rightButtonState"
        const val CHIP_ID_KEY = "chipId"
        const val NOTIFICATION_MESSAGE_SEVERITY_KEY = "severity"
        const val NOTIFICATION_MESSAGE_ID_KEY = "id"
        const val NOTIFICATION_MESSAGE_REPORT_KEY = "marker"
        const val INTENT_REPORT_ID_KEY = "reportId"

        const val NOTIFICATION_MESSAGE_ACTION_KEY = "action"
        const val ACTION_NEW = "report-new"
        const val ACTION_UPDATE = "report-update"
        const val ACTION_REMOVE = "report-remove"
        const val ACTION_REFRESH = "report-refresh"

        val TRANSITIONS = listOf<ActivityTransition>(
            ActivityTransition.Builder().setActivityType(DetectedActivity.ON_FOOT)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder().setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder().setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder().setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        )

        val GSON: Gson = ThreeTenGsonAdapter.registerOffsetDateTime(GsonBuilder()).create()
    }
}