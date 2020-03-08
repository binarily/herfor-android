package pl.herfor.android.contexts

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import pl.herfor.android.interfaces.ContextRepository

class AppContext(private val context: Context) : ContextRepository {

    override fun getLifecycleOwner(): LifecycleOwner {
        return context as LifecycleOwner
    }

    override fun getContext(): Context {
        return context
    }

    override fun getActivity(): Activity {
        return context as Activity
    }

    override fun showToast(resourceId: Int, duration: Int) {
        Toast.makeText(context, resourceId, duration).show()
    }

    override fun getSharedPreferences(): SharedPreferences {
        return context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
    }

    override fun getString(id: Int): String {
        return context.getString(id)
    }

    override fun getLocationProvider(): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    override fun checkForPlayServices(): Boolean {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }

    override fun getLocationPermissionState(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}