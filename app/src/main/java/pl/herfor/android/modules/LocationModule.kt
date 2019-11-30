package pl.herfor.android.modules

import android.annotation.SuppressLint
import android.location.Address
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import org.koin.core.KoinComponent
import pl.herfor.android.R
import pl.herfor.android.interfaces.ContextRepository
import java.io.IOException
import java.util.concurrent.ExecutionException

class LocationModule(private val context: ContextRepository) : KoinComponent {
    private val locationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.getContext())
    private val geocoder = Geocoder(context.getContext())

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(): Location? {
        return try {
            Tasks.await(locationProviderClient.lastLocation)
        } catch (e: ExecutionException) {
            null
        }
    }

    fun getLocationName(latitude: Double, longitude: Double): String {
        val matches: MutableList<Address>?
        try {
            matches = geocoder.getFromLocation(latitude, longitude, 1)
        } catch (e: IOException) {
            return context.getString(R.string.geocoder_error)
        }
        val bestMatch = if (matches.isEmpty()) null else matches[0]
        return bestMatch?.thoroughfare ?: context.getString(R.string.geocoder_unknown)
    }

    fun getGeofencingClient(): GeofencingClient {
        return LocationServices.getGeofencingClient(context.getContext())
    }

}