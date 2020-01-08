package pl.herfor.android.instrumented

import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("pl.herfor.android", appContext.packageName)
    }

    //TODO: 1. When the user submits the report and it is accepted, the database has a new report and LiveData shows it.
    //TODO: 2. When the user submits the report and the error happens, a toast appears.
    //TODO: 3. When the user submits the grade and it is accepted, a toast appears.
    //TODO: 4. When the user submits the grade and the error happens, a toast appears.
    //TODO: 5. When a report is to be displayed and street name can't be fetched, an error text appears instead.
    //TODO: 6. When the user changes the filter, the results from database are different.
}
