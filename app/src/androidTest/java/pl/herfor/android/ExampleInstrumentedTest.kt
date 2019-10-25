package pl.herfor.android

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

    //TODO: black-box tests
    //Examples:
    // * tests on livedata - if we put a marker, then we should get marker on map
    // * tests on retrofit - retrofit receives some data (mocked), some livedata get triggered
    // * tests on map - map shows location, button changes in given way/details get shown
    // * tests on filters - filter gets changed, markers appear on livedata
    // * tests on addition/grading - addition gets triggered, retrofit gets the data
    // * tests on notifications - notification is received, data is added/notification appears/proper marker is opened

    //TODO: integration tests
    //Examples:
    // * sideload to server and check if stuff gets on list
    // * sideload to server and check if we get error
    // * NO SIDELOADING TO DB: that is done in integration tests on server side
}
