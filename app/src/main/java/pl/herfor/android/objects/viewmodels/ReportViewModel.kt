package pl.herfor.android.objects.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.Marker
import org.koin.core.KoinComponent
import org.koin.core.inject
import pl.herfor.android.modules.DatabaseModule
import pl.herfor.android.modules.PreferencesModule
import pl.herfor.android.objects.Report
import pl.herfor.android.objects.enums.*
import pl.herfor.android.utils.DoubleTrigger

class ReportViewModel : ViewModel(), KoinComponent {
    private val preferences: PreferencesModule by inject()
    private val database: DatabaseModule by inject()

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

    //Reports that should be on the map
    val filteredReports by lazy {
        Transformations.switchMap(
            DoubleTrigger(visibleSeverities, visibleAccidents)
        ) {
            database.getReportDao().getFiltered(it.first, it.second)
        }
    }

    //Reports that are on the map
    val mapMarkers = HashMap<String, Marker>()

    //Settings
    var insideLocationArea = true
    var buttonState = RightButtonMode.DISABLED

}