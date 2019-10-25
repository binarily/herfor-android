package pl.herfor.android.viewmodels

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.Marker
import pl.herfor.android.database.MarkerDatabase
import pl.herfor.android.objects.MarkerData
import pl.herfor.android.objects.MarkerGrade
import pl.herfor.android.objects.enums.Accident
import pl.herfor.android.objects.enums.Grade
import pl.herfor.android.objects.enums.RightButtonMode
import pl.herfor.android.objects.enums.Severity
import pl.herfor.android.utils.DoubleTrigger
import pl.herfor.android.utils.getAccidentTypes
import pl.herfor.android.utils.getSeverities
import kotlin.concurrent.thread

class MarkerViewModel(context: Context) : ViewModel() {
    //Observables
    val addMarkerToMap: MutableLiveData<MarkerData> by lazy {
        MutableLiveData<MarkerData>()
    }
    val removeMarkerFromMap: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }
    val updateMarkerOnMap: MutableLiveData<MarkerData> by lazy {
        MutableLiveData<MarkerData>()
    }

    val connectionStatus: MutableLiveData<Boolean?> by lazy {
        MutableLiveData<Boolean?>()
    }
    val submittingMarkerStatus: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    val markerFromNotificationStatus: MutableLiveData<String?> by lazy {
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
    val currentlyShownMarker: MutableLiveData<MarkerData> by lazy {
        MutableLiveData<MarkerData>()
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
    val markerDao = MarkerDatabase.getDatabase(context).markerDao()
    val filteredMarkers by lazy {
        Transformations.switchMap(
            DoubleTrigger(visibleSeverities, visibleAccidentTypes)
        ) {
            markerDao.getFiltered(it.first, it.second)
        }
    }
    //Map of markers on map
    val mapMarkers = HashMap<String, Marker>()

    //Grade DAO
    val gradeDao = MarkerDatabase.getDatabase(context).gradeDao()

    //Settings
    var insideLocationArea = true
    var started = false
    var buttonState = RightButtonMode.DISABLED

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("preferences", Context.MODE_PRIVATE)


    internal fun threadSafeInsert(markerData: MarkerData) {
        thread {
            markerDao.insert(markerData)
        }
    }

    internal fun threadSafeDelete(markerData: MarkerData) {
        thread {
            markerDao.delete(markerData)
        }
    }

    internal fun threadSafeInsert(markerGrade: MarkerGrade) {
        thread {
            gradeDao.insert(markerGrade)
        }
    }

}