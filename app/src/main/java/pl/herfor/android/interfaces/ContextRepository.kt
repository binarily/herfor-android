package pl.herfor.android.interfaces

import android.app.Activity
import android.content.Context
import android.location.Location
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.tasks.Task

interface ContextRepository {
    fun getCurrentLocation(): Task<Location>
    fun getLocationName(latitude: Double, longitude: Double): String
    fun getLifecycleOwner(): LifecycleOwner
    fun getContext(): Context
    fun getActivity(): Activity
}