package pl.herfor.android.services

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityRecognitionResult
import pl.herfor.android.contexts.MarkerContext

class ActivityRecognitionService : IntentService("ActivityRecognitionService") {

    override fun onHandleIntent(intent: Intent?) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            val context = MarkerContext(applicationContext)
            val result = ActivityRecognitionResult.extractResult(intent)
            context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
                .edit()
                .putInt("currentActivity", result.mostProbableActivity.type)
                .apply()
            Log.d(
                this.javaClass.name,
                "Current most probable activity: ${result.mostProbableActivity}"
            )
        }
    }

}
