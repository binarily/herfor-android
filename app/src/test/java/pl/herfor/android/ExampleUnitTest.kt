package pl.herfor.android

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun presenter_severityChange_isRemoved() {
        TODO()
    }

    @Test
    fun presenter_severityChange_isAdded() {
        TODO()
    }

    @Test
    fun presenter_accidentChange_isRemoved() {
        TODO()
    }

    @Test
    fun presenter_accidentChange_isAdded() {
        TODO()
    }

    //TODO: single-method tests
    //NOT on livedata and its observers
    //Tests on pure correctness
    //Examples:
    // * test on severity change - if severity is the same, nothing happens; if severity changes so do sharedprefs
    // * test on right button - right combination of data changes button accordingly
    // * test on database converters - that they convert correctly
}
