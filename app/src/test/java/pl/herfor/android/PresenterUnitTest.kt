package pl.herfor.android

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import org.junit.After
import org.junit.Before
import pl.herfor.android.contexts.MarkerContext
import pl.herfor.android.interfaces.MarkerContract
import pl.herfor.android.presenters.MarkerViewPresenter
import pl.herfor.android.viewmodels.MarkerViewModel

class PresenterUnitTest {

    private val model = MarkerViewModel()
    private val viewMock = spy<MarkerContract.View>()
    private val contextMock = mock<MarkerContext>()

    private val presenter = MarkerViewPresenter(model, viewMock, contextMock)

    @Before
    fun startUp() {
        presenter.start()
    }

    @After
    fun cleanUp() {
        presenter.stop()
    }


}