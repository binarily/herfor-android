package pl.herfor.android

import android.app.Application
import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import pl.herfor.android.contexts.AppContext
import pl.herfor.android.interfaces.AppContract
import pl.herfor.android.interfaces.ContextRepository
import pl.herfor.android.modules.*
import pl.herfor.android.presenters.ReportViewPresenter
import pl.herfor.android.retrofits.RetrofitRepository
import pl.herfor.android.viewmodels.ReportViewModel
import pl.herfor.android.views.MapsActivity

val appModule = module {
    single { RetrofitRepository(get()) }
    scope(named<MapsActivity>()) {
        factory<ContextRepository> { (context: Context) -> AppContext(context) }
        factory { (context: ContextRepository) -> PreferencesModule(context) }
    }

    single<ContextRepository> { AppContext(androidContext()) }

    single { NotificationGeofenceModule() }
    single { SilentZoneGeofenceModule() }
    single { PreferencesModule(get()) }
    single { IntentModule() }
    single { LiveDataModule() }

    factory<AppContract.Presenter> { (view: AppContract.View, model: ReportViewModel, context: ContextRepository) ->
        ReportViewPresenter(
            view,
            model,
            context
        )
    }

    viewModel { ReportViewModel(get()) }
}

class ObsApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@ObsApplication)
            modules(appModule)
        }
    }
}