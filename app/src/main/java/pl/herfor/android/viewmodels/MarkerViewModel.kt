package pl.herfor.android.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.Marker
import com.google.gson.GsonBuilder
import pl.herfor.android.objects.*
import pl.herfor.android.retrofits.MarkerRetrofit
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MarkerViewModel : ViewModel() {

    //Observables
    val addMarkerToMap: MutableLiveData<MarkerData> by lazy {
        MutableLiveData<MarkerData>()
    }
    val removeMarkerFromMap: MutableLiveData<MarkerData> by lazy {
        MutableLiveData<MarkerData>()
    }
    val connectionStatus: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
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

    //All markers
    val markersHashMap: HashMap<Point, MarkerData> = HashMap()
    //Map of markers on map
    val mapMarkers = HashMap<String, Marker>()

    //Settings
    var locationEnabled = false
    var insideLocationArea = true
    var visibleSeverities = arrayListOf(SeverityType.GREEN, SeverityType.YELLOW, SeverityType.RED)
    var visibleAccidentTypes = arrayListOf(
        AccidentType.BIKE, AccidentType.BUS, AccidentType.METRO,
        AccidentType.PEDESTRIAN, AccidentType.RAIL, AccidentType.TRAM
    )

    //Retrofit
    private val gson = GsonBuilder()
        .setDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz")
        .create()
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://192.168.0.127:8080/")
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    private val service = retrofit.create(MarkerRetrofit::class.java)

    fun loadAllMarkers() {
        service.listMarkers().enqueue(markersCallback())
    }

    fun loadAllVisibleMarkers(request: MarkersLookupRequest) {
        service.listMarkersNearby(request).enqueue(markersCallback())
    }

    fun loadVisibleMarkersChangedSince(request: MarkersLookupRequest) {
        service.listMarkersNearbySince(request).enqueue(markersCallback())
    }

    fun addMarker(marker: MarkerData) {
        service.addMarker(marker).enqueue(markersAddCallback())
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
                            markersHashMap.remove(marker.location)
                            removeMarkerFromMap.value = marker
                        }
                        else -> {
                            if (!markersHashMap.containsKey(marker.location)) {
                                markersHashMap[marker.location] = marker
                                if (visibleSeverities.contains(marker.properties.severityType) && visibleAccidentTypes.contains(
                                        marker.properties.accidentType
                                    )
                                ) {
                                    addMarkerToMap.value = marker
                                }
                            }
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
                if (marker != null) {
                    submittingMarkerStatus.value = true
                    markersHashMap[marker.location] = marker
                    addMarkerToMap.value = marker
                }
            }

        }
    }
}