package pl.herfor.android.viewmodels

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.Marker
import pl.herfor.android.database.AppDatabase
import pl.herfor.android.objects.Report
import pl.herfor.android.objects.ReportGrade
import pl.herfor.android.objects.enums.Accident
import pl.herfor.android.objects.enums.Grade
import pl.herfor.android.objects.enums.RightButtonMode
import pl.herfor.android.objects.enums.Severity
import pl.herfor.android.utils.DoubleTrigger
import pl.herfor.android.utils.getAccidentTypes
import pl.herfor.android.utils.getSeverities
import kotlin.concurrent.thread

class ReportViewModel(context: Context) : ViewModel() {
    //Observables
    val addReportToMap: MutableLiveData<Report> by lazy {
        MutableLiveData<Report>()
    }
    val removeReportFromMap: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }
    val updateReportOnMap: MutableLiveData<Report> by lazy {
        MutableLiveData<Report>()
    }

    val connectionStatus: MutableLiveData<Boolean?> by lazy {
        MutableLiveData<Boolean?>()
    }
    val submittingReportStatus: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    val reportFromNotificationStatus: MutableLiveData<String?> by lazy {
        MutableLiveData<String?>()
    }
    val gradeSubmissionStatus: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    //View observers
    val severityFilterChanged: MutableLiveData<Severity> by lazy {
        MutableLiveData<Severity>()
    }
    val accidentFilterChanged: MutableLiveData<Accident> by lazy {
        MutableLiveData<Accident>()
    }
    val currentlyShownReport: MutableLiveData<Report> by lazy {
        MutableLiveData<Report>()
    }
    val currentlyShownGrade: MutableLiveData<Grade> by lazy {
        MutableLiveData<Grade>()
    }
    val visibleSeverities: MutableLiveData<MutableList<Severity>> by lazy {
        MutableLiveData<MutableList<Severity>>(sharedPreferences.getSeverities())
    }
    val visibleAccidentTypes: MutableLiveData<MutableList<Accident>> by lazy {
        MutableLiveData<MutableList<Accident>>(sharedPreferences.getAccidentTypes())
    }

    //All markers
    val reportDao = AppDatabase.getDatabase(context).reportDao()
    val filteredReports by lazy {
        Transformations.switchMap(
            DoubleTrigger(visibleSeverities, visibleAccidentTypes)
        ) {
            reportDao.getFiltered(it.first, it.second)
        }
    }
    //Map of markers on map
    val mapMarkers = HashMap<String, Marker>()

    //Grade DAO
    val gradeDao = AppDatabase.getDatabase(context).gradeDao()

    //Settings
    var insideLocationArea = true
    var started = false
    var buttonState = RightButtonMode.DISABLED

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("preferences", Context.MODE_PRIVATE)


    internal fun threadSafeInsert(report: Report) {
        thread {
            reportDao.insert(report)
        }
    }

    internal fun threadSafeDelete(report: Report) {
        thread {
            reportDao.delete(report)
        }
    }

    internal fun threadSafeInsert(reportGrade: ReportGrade) {
        thread {
            gradeDao.insert(reportGrade)
        }
    }

}