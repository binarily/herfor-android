package pl.herfor.android.viewmodels

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.Marker
import pl.herfor.android.database.MarkerDatabase
import pl.herfor.android.objects.*
import pl.herfor.android.retrofits.GradeRetrofit
import pl.herfor.android.retrofits.MarkerRetrofit
import pl.herfor.android.utils.Constants
import pl.herfor.android.utils.getAccidentTypes
import pl.herfor.android.utils.getSeverities
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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
    //TODO: replace with LiveData for severity list
    val severityFilterChanged: MutableLiveData<Severity> by lazy {
        MutableLiveData<Severity>()
    }
    //TODO: replace with LiveData for accidentList
    val accidentFilterChanged: MutableLiveData<Accident> by lazy {
        MutableLiveData<Accident>()
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

    val sharedPreferences = application.getSharedPreferences("preferences", Context.MODE_PRIVATE)
    var visibleSeverities = sharedPreferences.getSeverities()
    var visibleAccidentTypes = sharedPreferences.getAccidentTypes()

    //Retrofit
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://192.168.0.126:8080/")
        .addConverterFactory(GsonConverterFactory.create(Constants.GSON))
        .build()
    private val markerRetrofit = retrofit.create(MarkerRetrofit::class.java)
    private val gradeRetrofit = retrofit.create(GradeRetrofit::class.java)

    fun loadSingleMarkerForNotification(id: String) {
        markerRetrofit.getMarker(id).enqueue(singleMarkerForNotificationCallback())
    }

    fun loadVisibleMarkersChangedSince(request: MarkersLookupRequest) {
        markerRetrofit.listMarkersNearbySince(request).enqueue(markersCallback())
    }

    fun submitMarker(request: MarkerAddRequest) {
        markerRetrofit.addMarker(request).enqueue(markersAddCallback())
    }

    fun submitGrade(request: MarkerGradeRequest) {
        gradeRetrofit.create(request).enqueue(gradeCallback())
    }

    private fun threadSafeInsert(markerData: MarkerData) {
        thread {
            markerDao.insert(markerData)
        }
    }

    private fun threadSafeDelete(markerData: MarkerData) {
        thread {
            markerDao.delete(markerData)
        }
    }

    private fun threadSafeInsert(markerGrade: MarkerGrade) {
        thread {
            gradeDao.insert(markerGrade)
        }
    }

    private fun markersCallback(): Callback<List<MarkerData>> {
        return object : Callback<List<MarkerData>> {
            override fun onFailure(call: Call<List<MarkerData>>?, t: Throwable?) {
                connectionStatus.value = false
            }

            override fun onResponse(
                call: Call<List<MarkerData>>?,
                response: Response<List<MarkerData>>?
            ) {
                connectionStatus.value = true
                response?.body()?.forEach { marker ->
                    when (marker.properties.severity) {
                        Severity.NONE -> {
                            threadSafeDelete(marker)
                        }
                        else -> {
                            threadSafeInsert(marker)
                        }
                    }
                }
            }
        }
    }

    private fun markersAddCallback(): Callback<MarkerData> {
        return object : Callback<MarkerData> {
            override fun onFailure(call: Call<MarkerData>, t: Throwable) {
                submittingMarkerStatus.value = false
            }

            override fun onResponse(call: Call<MarkerData>, response: Response<MarkerData>) {
                val marker = response.body()
                if (marker?.id != null) {
                    submittingMarkerStatus.value = true
                    threadSafeInsert(marker)
                }
            }

        }
    }

    private fun singleMarkerForNotificationCallback(): Callback<MarkerData> {
        return object : Callback<MarkerData> {
            override fun onFailure(call: Call<MarkerData>, t: Throwable) {
                connectionStatus.value = false
                markerFromNotificationStatus.value = null
            }

            override fun onResponse(call: Call<MarkerData>, response: Response<MarkerData>) {
                val marker = response.body()
                when (marker?.properties?.severity) {
                    Severity.NONE -> {
                        threadSafeDelete(marker)
                        markerFromNotificationStatus.value = null
                    }
                    null -> {
                        Log.e(
                            this.javaClass.name,
                            "Received marker with no severity, showing error"
                        )
                        markerFromNotificationStatus.value = null
                    }
                    else -> {
                        threadSafeInsert(marker)
                        markerFromNotificationStatus.value = marker.id
                    }
                }
            }

        }
    }

    private fun gradeCallback(): Callback<MarkerGrade> {
        return object : Callback<MarkerGrade> {
            override fun onFailure(call: Call<MarkerGrade>, t: Throwable) {
                connectionStatus.value = false
            }

            override fun onResponse(call: Call<MarkerGrade>, response: Response<MarkerGrade>) {
                val grade = response.body()
                if (grade != null) {
                    threadSafeInsert(grade)
                }
            }

        }
    }
}