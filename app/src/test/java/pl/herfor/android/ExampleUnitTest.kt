package pl.herfor.android

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    //TODO: single-method tests
    //NOT on livedata and its observers
    //Tests on pure correctness
    //Examples:
    // * test on severity change - if severity is the same, nothing happens; if severity changes so do sharedprefs
    // * test on right button - right combination of data changes button accordingly
    // * test on database converters - that they convert correctly
}
