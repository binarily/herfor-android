package pl.herfor.android.modules

import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.*
import org.junit.Before
import org.junit.Test
import org.mockito.Answers
import pl.herfor.android.contexts.AppContext
import pl.herfor.android.objects.enums.Accident
import pl.herfor.android.objects.enums.Severity

class PreferencesModuleTest {

    private val context = mock<AppContext>()
    private val preferences = mock<SharedPreferences>(defaultAnswer = Answers.RETURNS_DEEP_STUBS)
    private lateinit var module: PreferencesModule

    @Before
    fun setUp() {
        whenever(context.getSharedPreferences()).thenReturn(preferences)
        module = PreferencesModule(context)
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
}