package pl.herfor.android.contexts

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
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

}