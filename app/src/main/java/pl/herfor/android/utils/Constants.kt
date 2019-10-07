package pl.herfor.android.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder

class Constants {
    companion object {
        const val NOTIFICATION_CHANNEL_ID = "obs-notifications"
        const val NOTIFICATION_WORKER_TAG = "notifications"
        const val ZOOM_LEVEL = 15.0F
        const val BUTTON_ANIMATION_DURATION = 200
        const val RIGHT_BUTTON_STATE_KEY = "rightButtonState"
        const val CHIP_ID_KEY = "chipId"
        const val NOTIFICATION_MESSAGE_ID_KEY = "id"
        const val NOTIFICATION_MESSAGE_MARKER_KEY = "marker"
        const val NOTIFICATION_MESSAGE_LATITUDE_KEY = "latitude"
        const val NOTIFICATION_MESSAGE_LONGITUDE_KEY = "longitude"
        const val INTENT_MARKER_ID_KEY = "markerId"
        val GSON: Gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create()

    }
}