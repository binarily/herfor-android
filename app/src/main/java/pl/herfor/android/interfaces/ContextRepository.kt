package pl.herfor.android.interfaces

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.location.FusedLocationProviderClient

interface ContextRepository {
    fun getLifecycleOwner(): LifecycleOwner
    fun getContext(): Context
    fun getActivity(): Activity
    fun showToast(resourceId: Int, duration: Int)
    fun getSharedPreferences(): SharedPreferences
    fun getString(id: Int): String
    fun getLocationProvider(): FusedLocationProviderClient
    fun checkForPlayServices(): Boolean
    fun getLocationPermissionState(): Boolean
}