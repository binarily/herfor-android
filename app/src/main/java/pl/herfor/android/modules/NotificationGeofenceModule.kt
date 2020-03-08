package pl.herfor.android.modules

import android.location.Location
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import org.koin.core.KoinComponent
import org.koin.core.inject
import kotlin.concurrent.thread

class NotificationGeofenceModule : KoinComponent {

    private val preferences: PreferencesModule by inject()
    private val location: LocationModule by inject()
    private val intent: IntentModule by inject()
    private val businessLogic: BusinessLogicModule by inject()

    fun registerFullGeofence() {
        val radius = businessLogic.getDetectedActivity(preferences.getCurrentActivity())
        registerGeofence(radius)
    }

    fun registerInitialGeofence() {
        registerGeofence(100f)
    }

    private fun registerGeofence(radius: Float) {
        thread {
            val currentLocation = location.getCurrentLocation()
            if (currentLocation == null) {
                thread {
                    Thread.sleep(5000)
                    registerGeofence(radius)
                }
                return@thread
            }

            location.getGeofencingClient().addGeofences(
                GeofencingRequest.Builder().apply {
                    setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
                    addGeofences(listOf(createGeofenceRequest(currentLocation, radius)))
                }
                    .build(),
                intent.geofenceIntent()
            )
        }
    }

    private fun createGeofenceRequest(location: Location, radius: Float): Geofence {
        return Geofence.Builder()
            .setRequestId("FOLLOW")
            .setCircularRegion(
                location.latitude,
                location.longitude,
                radius
            )
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build()
    }
}