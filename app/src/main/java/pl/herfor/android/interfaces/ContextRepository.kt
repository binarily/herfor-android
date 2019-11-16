package pl.herfor.android.interfaces

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.tasks.Task
import pl.herfor.android.database.AppDatabase
import pl.herfor.android.database.daos.ReportDao
import pl.herfor.android.database.daos.ReportGradeDao

interface ContextRepository {
    fun getCurrentLocation(): Task<Location>
    fun getLocationName(latitude: Double, longitude: Double): String
    fun getLifecycleOwner(): LifecycleOwner
    fun getContext(): Context
    fun getActivity(): Activity
    fun showToast(resourceId: Int, duration: Int)
    fun getSharedPreferences(): SharedPreferences
    fun getDatabase(): AppDatabase
    fun getString(id: Int): String
    fun getGeofencingClient(): GeofencingClient
    fun getReportDao(): ReportDao
    fun getGradeDao(): ReportGradeDao
}