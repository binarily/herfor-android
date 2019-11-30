package pl.herfor.android.modules

import android.location.Location
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import org.koin.core.KoinComponent
import org.koin.core.inject
import pl.herfor.android.objects.SilentZoneData
import pl.herfor.android.objects.enums.SilentZone
import kotlin.concurrent.thread

class SilentZoneGeofenceModule : KoinComponent {

    private val preferences: PreferencesModule by inject()
    private val location: LocationModule by inject()
    private val intent: IntentModule by inject()

    fun isRunning(silentZone: SilentZone): Boolean {
        return preferences.getSilentZoneData(silentZone).enabled
    }

    fun enableZone(silentZone: SilentZone, display: MutableLiveData<String>) {
        thread {
            val currentLocation = location.getCurrentLocation()
            if (currentLocation == null) {
                //TODO: error handling
                return@thread
            }
            val locationName =
                location.getLocationName(currentLocation.latitude, currentLocation.longitude)
            createZone(silentZone, SilentZoneData(true, currentLocation, locationName), display)
        }
    }

    fun disableZone(silentZone: SilentZone) {
        preferences.setSilentZoneData(silentZone, SilentZoneData.DISABLED)
        location.getGeofencingClient().removeGeofences(listOf(silentZone.name))
    }

    fun reregisterZone(silentZone: SilentZone, display: MutableLiveData<String>) {
        if (isRunning(silentZone)) {
            val data = preferences.getSilentZoneData(silentZone)
            createZone(silentZone, data, display)
        }
    }

    private fun createZone(
        silentZone: SilentZone,
        silentZoneData: SilentZoneData, display: MutableLiveData<String>
    ) {
        location.getGeofencingClient()
            .addGeofences(
                createGeofence(silentZone, silentZoneData.location!!),
                intent.geofenceIntent()
            )?.run {
                addOnSuccessListener {
                    preferences.setSilentZoneData(silentZone, silentZoneData)
                    display.value = silentZoneData.locationName
                }
            }
    }

    private fun createGeofence(silentZone: SilentZone, location: Location): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(listOf(createGeofenceRequest(silentZone, location)))
        }.build()
    }

    private fun createGeofenceRequest(silentZone: SilentZone, location: Location): Geofence {
        return Geofence.Builder()
            .setRequestId(silentZone.name)
            .setCircularRegion(
                location.latitude,
                location.longitude,
                200f
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()
    }
}