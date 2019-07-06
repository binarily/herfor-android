package pl.herfor.android.viewmodels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import pl.herfor.android.objects.Marker
import pl.herfor.android.objects.MarkersLookupRequest
import pl.herfor.android.objects.Point
import pl.herfor.android.retrofits.MarkerRetrofit
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MarkerViewModel : ViewModel() {
    private val observableMarker: MutableLiveData<Marker> by lazy {
        MutableLiveData<Marker>()
    }

    private val markersHashMap: HashMap<Point, Marker> = HashMap<Point, Marker>()

    fun getMarker(): MutableLiveData<Marker> {
        return observableMarker
    }

    fun loadMarkers() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.0.127:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(MarkerRetrofit::class.java)

        service.listMarkers().enqueue(object : Callback<List<Marker>> {
            override fun onFailure(call: Call<List<Marker>>?, t: Throwable?) {
                Log.v("retrofit", "call failed on loadMarkers")
            }

            override fun onResponse(call: Call<List<Marker>>?, response: Response<List<Marker>>?) {
                response?.body()?.forEach { marker ->
                    if (!markersHashMap.containsKey(marker.location)) {
                        observableMarker.value = marker
                    }
                }
            }
        })
    }

    fun loadMarkersVisibleOnCamera(request: MarkersLookupRequest) {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.0.127:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(MarkerRetrofit::class.java)

        service.listMarkersNearby(request).enqueue(object : Callback<List<Marker>> {
            override fun onFailure(call: Call<List<Marker>>?, t: Throwable?) {
                Log.v("retrofit", "call failed on loadMarkersVisibleOnCamera")
            }

            override fun onResponse(call: Call<List<Marker>>?, response: Response<List<Marker>>?) {
                response?.body()?.forEach { marker ->
                    if (!markersHashMap.containsKey(marker.location)) {
                        markersHashMap[marker.location] = marker
                        observableMarker.value = marker
                    }
                }
            }
        })
    }
}