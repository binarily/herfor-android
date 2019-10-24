package pl.herfor.android.services

import android.app.IntentService
import android.content.Intent
import pl.herfor.android.contexts.MarkerContext
import pl.herfor.android.objects.enums.Grade
import pl.herfor.android.retrofits.RetrofitRepository
import pl.herfor.android.viewmodels.MarkerViewModel


const val GRADE_RELEVANT_ACTION = "pl.herfor.android.services.action.GRADE_RELEVANT"
const val GRADE_NOT_RELEVANT_ACTION = "pl.herfor.android.services.action.GRADE_NOT_RELEVANT"

const val MARKER_ID_PARAM = "pl.herfor.android.services.extra.MARKER_ID"

class NotificationGradingService : IntentService("NotificationGradingService") {
    val context = MarkerContext(applicationContext)
    val model = MarkerViewModel(application)
    val repository = RetrofitRepository(model)

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            GRADE_RELEVANT_ACTION -> {
                val markerId = intent.getStringExtra(MARKER_ID_PARAM)
                handleGrade(markerId, Grade.RELEVANT)
            }
            GRADE_NOT_RELEVANT_ACTION -> {
                val markerId = intent.getStringExtra(MARKER_ID_PARAM)
                handleGrade(markerId, Grade.NOT_RELEVANT)
            }
        }
    }

    private fun handleGrade(markerId: String, grade: Grade) {
        //TODO: send, save, dismiss notification
    }

}
