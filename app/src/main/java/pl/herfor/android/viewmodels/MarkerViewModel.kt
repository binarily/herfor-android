package pl.herfor.android.viewmodels

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.Marker
import pl.herfor.android.database.MarkerDatabase
import pl.herfor.android.objects.*
import pl.herfor.android.utils.getAccidentTypes
import pl.herfor.android.utils.getSeverities
import kotlin.concurrent.thread

class MarkerViewModel(application: Application) : AndroidViewModel(application) {


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

    //All markers
    val markerDao = MarkerDatabase.getDatabase(getApplication()).markerDao()
    val existingMarkers = markerDao.getAll()
    //Map of markers on map
    val mapMarkers = HashMap<String, Marker>()

    //Grade DAO
    val gradeDao = MarkerDatabase.getDatabase(getApplication()).gradeDao()

    //Settings
    var insideLocationArea = true
    var started = false
    var buttonState = RightButtonMode.DISABLED

    private val sharedPreferences: SharedPreferences =
        application.getSharedPreferences("preferences", Context.MODE_PRIVATE)
    var visibleSeverities = sharedPreferences.getSeverities()
    var visibleAccidentTypes = sharedPreferences.getAccidentTypes()

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