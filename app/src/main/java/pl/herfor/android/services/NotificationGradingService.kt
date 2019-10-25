package pl.herfor.android.services

import android.app.IntentService
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.tasks.Tasks
import pl.herfor.android.R
import pl.herfor.android.contexts.MarkerContext
import pl.herfor.android.objects.MarkerGrade
import pl.herfor.android.objects.MarkerGradeRequest
import pl.herfor.android.objects.enums.Grade
import pl.herfor.android.retrofits.RetrofitRepository
import pl.herfor.android.utils.toPoint
import pl.herfor.android.viewmodels.MarkerViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class NotificationGradingService : IntentService("NotificationGradingService") {
    val context = MarkerContext(applicationContext)
    val model = MarkerViewModel(application)
    val repository = RetrofitRepository(model)

    companion object {
        const val GRADE_RELEVANT_ACTION = "pl.herfor.android.services.action.GRADE_RELEVANT"
        const val GRADE_NOT_RELEVANT_ACTION = "pl.herfor.android.services.action.GRADE_NOT_RELEVANT"

        const val MARKER_ID_PARAM = "pl.herfor.android.services.extra.MARKER_ID"
    }

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
        val location = Tasks.await(context.getCurrentLocation()).toPoint()
        val request = MarkerGradeRequest(markerId, location, grade)
        repository.submitGrade(request, gradeNotificationCallback())
    }


    private fun gradeNotificationCallback(): Callback<MarkerGrade> {
        return object : Callback<MarkerGrade> {
            override fun onFailure(call: Call<MarkerGrade>, t: Throwable) {
                context.showToast(R.string.grade_error_toast, Toast.LENGTH_SHORT)
            }

            override fun onResponse(call: Call<MarkerGrade>, response: Response<MarkerGrade>) {
                val grade = response.body()
                if (grade != null) {
                    model.threadSafeInsert(grade)
                    NotificationManagerCompat.from(context.getContext())
                        .cancel(grade.markerId, 0)
                    context.showToast(R.string.grade_thanks_toast, Toast.LENGTH_SHORT)
                } else {
                    context.showToast(R.string.grade_error_toast, Toast.LENGTH_SHORT)
                }
            }

        }
    }

}
