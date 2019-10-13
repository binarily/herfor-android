package pl.herfor.android.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.Marker
import pl.herfor.android.database.MarkerDatabase
import pl.herfor.android.objects.*
import pl.herfor.android.retrofits.MarkerRetrofit
import pl.herfor.android.utils.Constants
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
    val severityFilterChanged: MutableLiveData<SeverityType> by lazy {
        MutableLiveData<SeverityType>()
    }
    val accidentFilterChanged: MutableLiveData<AccidentType> by lazy {
        MutableLiveData<AccidentType>()
    }
    val markerFromNotificationStatus: MutableLiveData<String?> by lazy {
        MutableLiveData<String?>()
    }

    //All markers
    val markerDatabase = MarkerDatabase.getDatabase(getApplication()).markerDao()
    val existingMarkers = markerDatabase.getAll()
    //Map of markers on map
    val mapMarkers = HashMap<String, Marker>()

    //Settings
    var locationEnabled = false
    var insideLocationArea = true
    var started = false
    var visibleSeverities = arrayListOf(SeverityType.GREEN, SeverityType.YELLOW, SeverityType.RED)
    var visibleAccidentTypes = arrayListOf(
        AccidentType.BIKE, AccidentType.BUS, AccidentType.METRO,
        AccidentType.PEDESTRIAN, AccidentType.RAIL, AccidentType.TRAM
    )

    //Retrofit
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://192.168.0.126:8080/")
        .addConverterFactory(GsonConverterFactory.create(Constants.GSON))
        .build()
    private val service = retrofit.create(MarkerRetrofit::class.java)

    fun loadAllMarkers() {
        service.listMarkers().enqueue(markersCallback())
    }

    fun loadSingleMarkerForNotification(id: String) {
        service.getMarker(id).enqueue(singleMarkerForNotificationCallback())
    }

    fun loadAllVisibleMarkers(request: MarkersLookupRequest) {
        service.listMarkersNearby(request).enqueue(markersCallback())
    }

    fun loadVisibleMarkersChangedSince(request: MarkersLookupRequest) {
        service.listMarkersNearbySince(request).enqueue(markersCallback())
    }

    fun addMarker(request: MarkerAddRequest) {
        service.addMarker(request).enqueue(markersAddCallback())
    }

    private fun threadSafeInsert(markerData: MarkerData) {
        thread {
            markerDatabase.insert(markerData)
        }
    }

    private fun threadSafeDelete(markerData: MarkerData) {
        thread {
            markerDatabase.delete(markerData)
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
                    when (marker.properties.severityType) {
                        SeverityType.NONE -> {
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
                when (marker?.properties?.severityType) {
                    SeverityType.NONE -> {
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
}