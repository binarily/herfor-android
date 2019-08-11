package pl.herfor.android.contexts

import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import pl.herfor.android.contracts.MarkerContract

class MarkerContext(private val context: AppCompatActivity) : MarkerContract.Context {
    private val geocoder = Geocoder(context)
    private val locationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    override fun getLocationProvider(): FusedLocationProviderClient {
        return locationProviderClient
    }

    override fun getGeocoder(): Geocoder {
        return geocoder
    }

    override fun getAppContext(): AppCompatActivity {
        return context
    }

}