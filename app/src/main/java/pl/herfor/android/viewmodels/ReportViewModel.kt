package pl.herfor.android.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.android.gms.maps.model.Marker
import org.koin.core.KoinComponent
import org.koin.core.inject
import pl.herfor.android.database.AppDatabase
import pl.herfor.android.modules.PreferencesModule
import pl.herfor.android.objects.Report
import pl.herfor.android.objects.ReportGrade
import pl.herfor.android.objects.enums.*
import pl.herfor.android.utils.DoubleTrigger
import kotlin.concurrent.thread

class ReportViewModel(context: Context) : AndroidViewModel(context as Application), KoinComponent {
    private val preferences: PreferencesModule by inject()

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
        MutableLiveData<MutableList<Severity>>(preferences.getSeverities())
    }
    val visibleAccidents: MutableLiveData<MutableList<Accident>> by lazy {
        MutableLiveData<MutableList<Accident>>(preferences.getAccidents())
    }
    val homeSilentZoneName: MutableLiveData<String> by lazy {
        MutableLiveData<String>(preferences.getSilentZoneData(SilentZone.HOME).locationName)
    }
    val workSilentZoneName: MutableLiveData<String> by lazy {
        MutableLiveData<String>(preferences.getSilentZoneData(SilentZone.WORK).locationName)
    }
    val silentZoneToggled: MutableLiveData<SilentZone> by lazy {
        MutableLiveData<SilentZone>()
    }

    //Grade DAO
    private val reportDao = AppDatabase.getDatabase(context).reportDao()
    private val gradeDao = AppDatabase.getDatabase(context).gradeDao()

    //All markers
    val filteredReports by lazy {
        Transformations.switchMap(
            DoubleTrigger(visibleSeverities, visibleAccidents)
        ) {
            reportDao.getFiltered(it.first, it.second)
        }
    }
    //Map of markers on map
    val mapMarkers = HashMap<String, Marker>()

    //Settings
    var insideLocationArea = true
    var buttonState = RightButtonMode.DISABLED

    //TODO: move to module
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