package pl.herfor.android.services

import android.app.IntentService
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityRecognitionResult
import org.koin.android.ext.android.inject
import pl.herfor.android.modules.PreferencesModule

class ActivityRecognitionService : IntentService("ActivityRecognitionService") {
    private val preferences: PreferencesModule by inject()

    override fun onHandleIntent(intent: Intent?) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            val result = ActivityRecognitionResult.extractResult(intent)
            preferences.setCurrentActivity(result.mostProbableActivity.type)
            Log.d(
                this.javaClass.name,
                "Current most probable activity: ${result.mostProbableActivity}"
            )
        }
    }

}
