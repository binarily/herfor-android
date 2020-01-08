package pl.herfor.android.views


import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pl.herfor.android.R

@LargeTest
@RunWith(AndroidJUnit4::class)
class UserInterfaceTests {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MapsActivity::class.java)

    @Test
    fun filterSheetTest() {
        mActivityTestRule.activity
        // Assertions are done by the way of conducting operations on views that are visible
        // The test fails if any of the views mentioned is not visible (i.e. the UI operation is incorrect)
        Thread.sleep(5000)

        val floatingActionButton2 = onView(
            allOf(
                withId(R.id.filterButton),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.activity_main),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        floatingActionButton2.perform(click())
        Thread.sleep(1000)

        val chip = onView(
            allOf(
                withText(R.string.green),
                childAtPosition(
                    allOf(
                        withId(R.id.filterSeverityChipGroup),
                        childAtPosition(
                            withId(R.id.add_sheet),
                            1
                        )
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        chip.perform(click())
        Thread.sleep(1000)

        val chip2 = onView(
            allOf(
                withText(R.string.green),
                childAtPosition(
                    allOf(
                        withId(R.id.filterSeverityChipGroup),
                        childAtPosition(
                            withId(R.id.add_sheet),
                            1
                        )
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        chip2.perform(click())
        Thread.sleep(1000)

        val materialTextView = onView(
            allOf(
                withId(R.id.homeSilentZoneTextView),
                childAtPosition(
                    allOf(
                        withId(R.id.add_sheet),
                        childAtPosition(
                            withId(R.id.design_bottom_sheet),
                            0
                        )
                    ),
                    4
                ),
                isDisplayed()
            )
        )
        materialTextView.perform(click())
        Thread.sleep(1000)


        val materialTextView2 = onView(
            allOf(
                withId(R.id.homeSilentZoneTextView),
                childAtPosition(
                    allOf(
                        withId(R.id.add_sheet),
                        childAtPosition(
                            withId(R.id.design_bottom_sheet),
                            0
                        )
                    ),
                    4
                ),
                isDisplayed()
            )
        )
        materialTextView2.perform(click())
        Thread.sleep(1000)


        val view = onView(
            allOf(
                withId(R.id.touch_outside),
                childAtPosition(
                    allOf(
                        withId(R.id.coordinator),
                        childAtPosition(
                            withId(R.id.container),
                            0
                        )
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        view.perform(pressBack())
        Thread.sleep(3000)
    }

    @Test
    fun swipeBackTest() {
        mActivityTestRule.activity
        val map = onView(allOf(withId(R.id.map)))
        map.perform(swipeRight())
        Thread.sleep(1000)

        val floatingActionButton3 = onView(
            allOf(
                withId(R.id.addButton),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.activity_main),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        )

        floatingActionButton3.perform(click())
        Thread.sleep(1000)
    }

    @Test
    fun additionTests() {
        mActivityTestRule.activity
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)


        val floatingActionButton = onView(
            allOf(
                withId(R.id.addButton),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.activity_main),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        floatingActionButton.perform(click())


        val button = onView(
            allOf(
                withId(R.id.submitReportButton), withText("Dodaj"),
                childAtPosition(
                    allOf(
                        withId(R.id.add_sheet),
                        childAtPosition(
                            withId(R.id.activity_main),
                            2
                        )
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        button.check(matches(not(isEnabled())))

        val chip = onView(
            allOf(
                withText("Tramwaj"),
                childAtPosition(
                    allOf(
                        withId(R.id.addChipGroup),
                        childAtPosition(
                            withId(R.id.add_sheet),
                            1
                        )
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        chip.perform(click())
        button.check(matches(isEnabled()))

        val chip2 = onView(
            allOf(
                withText("Tramwaj"),
                childAtPosition(
                    allOf(
                        withId(R.id.addChipGroup),
                        childAtPosition(
                            withId(R.id.add_sheet),
                            1
                        )
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        chip2.perform(click())
        button.check(matches(not(isEnabled())))

        val chip3 = onView(
            allOf(
                withText("Autobus"),
                childAtPosition(
                    allOf(
                        withId(R.id.addChipGroup),
                        childAtPosition(
                            withId(R.id.add_sheet),
                            1
                        )
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        chip3.perform(click())

        button.perform(click())
        button.check(matches(not(isEnabled())))

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val floatingActionButton2 = onView(
            allOf(
                withId(R.id.addButton),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.activity_main),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        floatingActionButton2.perform(click())
        Thread.sleep(1000)
        val button1 = onView(
            allOf(
                withId(R.id.submitReportButton), withText("Dodaj"),
                childAtPosition(
                    allOf(
                        withId(R.id.add_sheet),
                        childAtPosition(
                            withId(R.id.activity_main),
                            2
                        )
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        button1.check(matches(not(isEnabled())))
    }


    private fun childAtPosition(
        parentMatcher: Matcher<View>, position: Int
    ): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
