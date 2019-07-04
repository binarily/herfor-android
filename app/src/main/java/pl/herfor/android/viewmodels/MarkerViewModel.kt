package pl.herfor.android.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import pl.herfor.android.objects.Marker
import pl.herfor.android.retrofits.MarkerRetrofit
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class MarkerViewModel : ViewModel() {
    private val markers: MutableLiveData<List<Marker>> by lazy {
        MutableLiveData<List<Marker>>().also {
            loadMarkers()
        }
    }

    fun getMarkers(): LiveData<List<Marker>> {
        return markers
    }

    private fun loadMarkers() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.0.127:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(MarkerRetrofit::class.java)

        val listOfMarkers = service.listMarkers().enqueue(object : Callback<List<Marker>> {
            override fun onFailure(call: Call<List<Marker>>?, t: Throwable?) {
                Log.v("retrofit", "call failed")
            }

            override fun onResponse(call: Call<List<Marker>>?, response: Response<List<Marker>>?) {
                markers.value = response!!.body()!!
            }

        })
    }
}