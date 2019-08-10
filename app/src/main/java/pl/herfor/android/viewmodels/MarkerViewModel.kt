package pl.herfor.android.viewmodels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.GsonBuilder
import pl.herfor.android.objects.MarkerData
import pl.herfor.android.objects.MarkersLookupRequest
import pl.herfor.android.objects.Point
import pl.herfor.android.objects.SeverityType
import pl.herfor.android.retrofits.MarkerRetrofit
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MarkerViewModel : ViewModel() {

    //Observables
    val newMarkerObservable: MutableLiveData<MarkerData> by lazy {
        MutableLiveData<MarkerData>()
    }
    val removeMarkerObservable: MutableLiveData<MarkerData> by lazy {
        MutableLiveData<MarkerData>()
    }
    val connectionStatusObservable: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    val addMarkerStatusObservable: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    //Markers
    private val markersHashMap: HashMap<Point, MarkerData> = HashMap()

    //Settings
    var locationEnabled = false
    var insideLocationArea = true

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
                connectionStatusObservable.value = false
            }

            override fun onResponse(call: Call<List<MarkerData>>?, response: Response<List<MarkerData>>?) {
                connectionStatusObservable.value = true
                response?.body()?.forEach { marker ->
                    when (marker.properties.severityType) {
                        SeverityType.NONE -> {
                            markersHashMap.remove(marker.location)
                            removeMarkerObservable.value = marker
                        }
                        else -> {
                            markersHashMap[marker.location] = marker
                            newMarkerObservable.value = marker
                        }
                    }
                }
            }
        }
    }

    private fun markersAddCallback(): Callback<MarkerData> {
        return object : Callback<MarkerData> {
            override fun onFailure(call: Call<MarkerData>, t: Throwable) {
                Log.e("ADD", t.localizedMessage)
                addMarkerStatusObservable.value = false
            }

            override fun onResponse(call: Call<MarkerData>, response: Response<MarkerData>) {
                val marker = response.body()
                if (marker != null) {
                    addMarkerStatusObservable.value = true
                    markersHashMap[marker.location] = marker
                    newMarkerObservable.value = marker
                }
            }

        }
    }
}