package pl.herfor.android.modules

import android.location.Location
import com.google.android.gms.location.DetectedActivity
import com.nhaarman.mockitokotlin2.given
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.get
import org.koin.test.KoinTest
import org.koin.test.mock.declareMock
import org.mockito.internal.verification.VerificationModeFactory
import pl.herfor.android.appModule
import pl.herfor.android.interfaces.ContextRepository

class NotificationGeofenceModuleTest : KoinTest {

    private lateinit var module: NotificationGeofenceModule

    @Before
    fun setUp() {
        startKoin { modules(appModule) }
        module = get()
        declareMock<ContextRepository> {
            given(this.getContext()).willReturn(mock())
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun shouldCreateGeofenceWithActivityRadius() {
        //given
        declareMock<PreferencesModule> {
            given(this.getCurrentActivity()).willReturn(DetectedActivity.ON_FOOT)
        }
        val locationMock = mock<Location>()
        declareMock<LocationModule> {
            given(this.getCurrentLocation()).willReturn(locationMock)
            given(this.getGeofencingClient()).willReturn(mock())
        }
        declareMock<IntentModule> { }
        whenever(locationMock.latitude).thenReturn(10.0)
        whenever(locationMock.longitude).thenReturn(20.0)

        //when
        module.registerFullGeofence()

        //then
        verify(get<PreferencesModule>()).getCurrentActivity()
        verify(get<LocationModule>()).getGeofencingClient()
        verify(locationMock).latitude
    }

    @Test
    fun shouldCreateGeofenceWithFixedRadius() {
        //given
        declareMock<PreferencesModule> {
            given(this.getCurrentActivity()).willReturn(DetectedActivity.ON_FOOT)
        }
        val locationMock = mock<Location>()
        declareMock<LocationModule> {
            given(this.getCurrentLocation()).willReturn(locationMock)
            given(this.getGeofencingClient()).willReturn(mock())
        }
        declareMock<IntentModule> { }
        whenever(locationMock.latitude).thenReturn(10.0)
        whenever(locationMock.longitude).thenReturn(20.0)

        //when
        module.registerInitialGeofence()

        //then
        verify(get<PreferencesModule>(), VerificationModeFactory.times(0)).getCurrentActivity()
        verify(get<LocationModule>()).getGeofencingClient()
        verify(locationMock).latitude
    }
}