package pl.herfor.android.modules

import android.content.SharedPreferences
import android.location.Location
import com.google.android.gms.location.DetectedActivity
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.mockito.Answers
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.robolectric.RobolectricTestRunner
import pl.herfor.android.contexts.AppContext
import pl.herfor.android.objects.SilentZoneData
import pl.herfor.android.objects.enums.Accident
import pl.herfor.android.objects.enums.Severity
import pl.herfor.android.objects.enums.SilentZone

@RunWith(RobolectricTestRunner::class)
class PreferencesModuleTest {

    private val context = mock<AppContext>()
    private val preferences = mock<SharedPreferences>(defaultAnswer = Answers.RETURNS_DEEP_STUBS)
    private lateinit var module: PreferencesModule

    @Before
    fun setUp() {
        whenever(context.getSharedPreferences()).thenReturn(preferences)
        module = PreferencesModule(context)
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun shouldReturnSeverities() {
        //given
        whenever(preferences.getBoolean(eq("severity.GREEN"), any())).thenReturn(true)
        whenever(preferences.getBoolean(eq("severity.YELLOW"), any())).thenReturn(false)
        whenever(preferences.getBoolean(eq("severity.RED"), any())).thenReturn(true)

        //when
        val severityList = module.getSeverities()

        //then
        assertThat(severityList).hasSize(2)
        assertThat(severityList).containsExactly(Severity.GREEN, Severity.RED)
    }

    @Test
    fun shouldReturnAccidents() {
        //given
        whenever(preferences.getBoolean(eq("accident.BUS"), any())).thenReturn(true)
        whenever(preferences.getBoolean(eq("accident.RAIL"), any())).thenReturn(false)
        whenever(preferences.getBoolean(eq("accident.TRAM"), any())).thenReturn(false)
        whenever(preferences.getBoolean(eq("accident.PEDESTRIAN"), any())).thenReturn(true)
        whenever(preferences.getBoolean(eq("accident.BIKE"), any())).thenReturn(false)
        whenever(preferences.getBoolean(eq("accident.METRO"), any())).thenReturn(true)

        //when
        val accidentList = module.getAccidents()

        //then
        assertThat(accidentList).hasSize(3)
        assertThat(accidentList).containsExactly(Accident.BUS, Accident.PEDESTRIAN, Accident.METRO)
    }

    @Test
    fun shouldReturnUserId() {
        //given
        whenever(preferences.getString(eq("registrationId"), eq(null))).thenReturn("1234")

        //when
        val userId = module.getUserId()

        //then
        assertThat(userId).isEqualTo("1234")
    }

    @Test
    fun shouldReturnSilentZoneNotificationCondition() {
        //given
        whenever(preferences.getBoolean(eq("displayNotifications"), any())).thenReturn(true)

        //when
        val condition = module.getSilentZoneNotificationCondition()

        //then
        assertThat(condition).isEqualTo(true)
    }

    @Test
    fun shouldReturnCurrentActivity() {
        //given
        whenever(
            preferences.getInt(
                eq("currentActivity"),
                any()
            )
        ).thenReturn(DetectedActivity.ON_BICYCLE)

        //when
        val condition = module.getCurrentActivity()

        //then
        assertThat(condition).isEqualTo(DetectedActivity.ON_BICYCLE)
    }

    @Test
    fun shouldReturnSilentZoneData() {
        //given
        whenever(preferences.getBoolean(eq("silentZone.HOME"), anyBoolean())).thenReturn(true)
        whenever(preferences.getFloat(eq("silentZone.HOME.location.latitude"), any())).thenReturn(
            20.0f
        )
        whenever(preferences.getFloat(eq("silentZone.HOME.location.longitude"), any())).thenReturn(
            25.0f
        )
        whenever(preferences.getString(eq("silentZone.HOME.locationName"), anyString())).thenReturn(
            "Location name"
        )

        //when
        val silentZoneData = module.getSilentZoneData(SilentZone.HOME)

        //then
        assertThat(silentZoneData.enabled).isTrue()
        assertThat(silentZoneData.location?.latitude).isEqualTo(20.0)
        assertThat(silentZoneData.location?.longitude).isEqualTo(25.0)
        assertThat(silentZoneData.locationName).isEqualTo("Location name")
    }

    @Test
    fun shouldInsertAccident() {
        //given

        //when
        module.setAccident(Accident.METRO, false)

        //then
        verify(preferences.edit().putBoolean("accident.METRO", false)).apply()
    }

    @Test
    fun shouldInsertSeverity() {
        //given

        //when
        module.setSeverity(Severity.RED, true)

        //then
        verify(preferences.edit().putBoolean("severity.RED", true)).apply()
    }

    @Test
    fun shouldInsertUserId() {
        //given

        //when
        module.setUserId("1234")

        //then
        verify(preferences.edit().putString("registrationId", "1234")).apply()
    }

    @Test
    fun shouldInsertSilentZoneNotificationCondition() {
        //given

        //when
        module.setSilentZoneNotificationCondition(false)

        //then
        verify(preferences.edit().putBoolean("displayNotifications", false)).apply()
    }

    @Test
    fun shouldInsertCurrentActivity() {
        //given

        //when
        module.setCurrentActivity(DetectedActivity.IN_VEHICLE)

        //then
        verify(preferences.edit().putInt("currentActivity", DetectedActivity.IN_VEHICLE)).apply()
    }

    @Test
    fun shouldInsertSilentZoneData() {
        val location = Location("dummyprovider")
        location.latitude = 20.0
        location.longitude = 25.0

        //given
        val silentZoneData = SilentZoneData(true, location, "Location Name")

        //when
        module.setSilentZoneData(SilentZone.WORK, silentZoneData)

        //then
        verify(
            preferences.edit()
                .putBoolean("silentZone.WORK", true)
                .putFloat("silentZone.WORK.location.latitude", 20.0f)
                .putFloat("silentZone.WORK.location.longitude", 25.0f)
                .putString("silentZone.WORK.locationName", "Location Name")
        ).apply()
    }


}