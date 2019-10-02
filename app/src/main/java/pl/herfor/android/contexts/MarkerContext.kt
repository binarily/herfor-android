package pl.herfor.android.contexts

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import pl.herfor.android.R
import pl.herfor.android.interfaces.ContextRepository
import java.io.IOException

class MarkerContext(private var context: Context) : ContextRepository {
    private var geocoder = Geocoder(context)
    private var locationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override fun getCurrentLocation(): Task<Location> {
        return locationProviderClient.lastLocation
    }

    override fun getLocationName(latitude: Double, longitude: Double): String {
        val geoCoder = geocoder
        val matches: MutableList<Address>?
        try {
            matches = geoCoder.getFromLocation(latitude, longitude, 1)
        } catch (e: IOException) {
            return context.getString(R.string.geocoder_error)
        }
        val bestMatch = if (matches.isEmpty()) null else matches[0]
        return bestMatch?.thoroughfare ?: context.getString(R.string.geocoder_unknown)
    }

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

    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
        return context.getSharedPreferences(name, mode)
    }
}