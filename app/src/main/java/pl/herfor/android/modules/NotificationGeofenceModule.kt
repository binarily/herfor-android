package pl.herfor.android.modules

import android.location.Location
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import org.koin.core.KoinComponent
import org.koin.core.inject
import pl.herfor.android.interfaces.ContextRepository
import pl.herfor.android.services.GeofencingService
import pl.herfor.android.utils.toDetectedActivityDistance

class NotificationGeofenceModule : KoinComponent {

    private val context: ContextRepository by inject()
    private val preferences: PreferencesModule by inject()

    fun registerFullGeofence() {
        val radius = preferences.getCurrentActivity().toDetectedActivityDistance()
        registerGeofence(radius)
    }

    fun registerInitialGeofence() {
        registerGeofence(100f)
    }

    private fun registerGeofence(radius: Float) {
        context.getCurrentLocation().addOnSuccessListener { location ->
            context.getGeofencingClient().addGeofences(
                GeofencingRequest.Builder().apply {
                    setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
                    addGeofences(listOf(createGeofenceRequest(location, radius)))
                }
                    .build(),
                GeofencingService.geofencePendingIntent(context.getContext())
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