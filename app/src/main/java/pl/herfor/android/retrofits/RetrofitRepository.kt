package pl.herfor.android.retrofits

import android.util.Log
import pl.herfor.android.objects.*
import pl.herfor.android.objects.enums.Severity
import pl.herfor.android.utils.Constants
import pl.herfor.android.viewmodels.MarkerViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitRepository(val model: MarkerViewModel) {
    //Retrofit
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://192.168.0.127:8080/")
        .addConverterFactory(GsonConverterFactory.create(Constants.GSON))
        .build()
    private val markerRetrofit = retrofit.create(MarkerRetrofit::class.java)
    private val gradeRetrofit = retrofit.create(GradeRetrofit::class.java)

    fun loadMarker(
        id: String,
        callback: Callback<MarkerData> = singleMarkerForNotificationCallback()
    ) {
        markerRetrofit.getMarker(id).enqueue(callback)
    }

    fun loadVisibleMarkersChangedSince(request: MarkersLookupRequest) {
        markerRetrofit.listMarkersNearbySince(request).enqueue(markersCallback())
    }

    fun submitMarker(request: MarkerAddRequest) {
        markerRetrofit.addMarker(request).enqueue(markersAddCallback())
    }

    fun submitGrade(
        request: MarkerGradeRequest,
        callback: Callback<MarkerGrade> = gradeCallback()
    ) {
        gradeRetrofit.create(request).enqueue(callback)
    }

    private fun markersCallback(): Callback<List<MarkerData>> {
        return object : Callback<List<MarkerData>> {
            override fun onFailure(call: Call<List<MarkerData>>?, t: Throwable?) {
                model.connectionStatus.value = false
            }

            override fun onResponse(
                call: Call<List<MarkerData>>?,
                response: Response<List<MarkerData>>?
            ) {
                model.connectionStatus.value = true
                response?.body()?.forEach { marker ->
                    when (marker.properties.severity) {
                        Severity.NONE -> {
                            model.threadSafeDelete(marker)
                        }
                        else -> {
                            model.threadSafeInsert(marker)
                        }
                    }
                }
            }
        }
    }

    private fun markersAddCallback(): Callback<MarkerData> {
        return object : Callback<MarkerData> {
            override fun onFailure(call: Call<MarkerData>, t: Throwable) {
                model.submittingMarkerStatus.value = false
            }

            override fun onResponse(call: Call<MarkerData>, response: Response<MarkerData>) {
                val marker = response.body()
                if (marker?.id != null) {
                    model.submittingMarkerStatus.value = true
                    marker.properties.notificationStatus = NotificationStatus.Dismissed
                    model.threadSafeInsert(marker)
                }
            }

        }
    }

    private fun singleMarkerForNotificationCallback(): Callback<MarkerData> {
        return object : Callback<MarkerData> {
            override fun onFailure(call: Call<MarkerData>, t: Throwable) {
                model.connectionStatus.value = false
                model.markerFromNotificationStatus.value = null
            }

            override fun onResponse(call: Call<MarkerData>, response: Response<MarkerData>) {
                val marker = response.body()
                when (marker?.properties?.severity) {
                    Severity.NONE -> {
                        model.threadSafeDelete(marker)
                        model.markerFromNotificationStatus.value = null
                    }
                    null -> {
                        Log.e(
                            this.javaClass.name,
                            "Received marker with no severity, showing error"
                        )
                        model.markerFromNotificationStatus.value = null
                    }
                    else -> {
                        model.threadSafeInsert(marker)
                        model.markerFromNotificationStatus.value = marker.id
                    }
                }
            }

        }
    }

    private fun gradeCallback(): Callback<MarkerGrade> {
        return object : Callback<MarkerGrade> {
            override fun onFailure(call: Call<MarkerGrade>, t: Throwable) {
                model.gradeSubmissionStatus.value = false
            }

            override fun onResponse(call: Call<MarkerGrade>, response: Response<MarkerGrade>) {
                val grade = response.body()
                if (grade != null) {
                    model.threadSafeInsert(grade)
                    model.gradeSubmissionStatus.value = true
                    model.currentlyShownGrade.value = grade.grade
                } else {
                    model.gradeSubmissionStatus.value = false
                }
            }

        }
    }

}