package pl.herfor.android.modules

import org.koin.core.KoinComponent
import pl.herfor.android.database.AppDatabase
import pl.herfor.android.database.daos.ReportDao
import pl.herfor.android.database.daos.ReportGradeDao
import pl.herfor.android.interfaces.ContextRepository
import pl.herfor.android.objects.Report
import pl.herfor.android.objects.ReportGrade
import kotlin.concurrent.thread

class DatabaseModule(context: ContextRepository) : KoinComponent {
    private val database = AppDatabase.getDatabase(context.getContext())

    fun getReportDao(): ReportDao {
        return database.reportDao()
    }

    fun getGradeDao(): ReportGradeDao {
        return database.gradeDao()
    }

    fun threadSafeInsert(report: Report) {
        thread {
            getReportDao().insert(report)
        }
    }

    fun threadSafeDelete(report: Report) {
        thread {
            getReportDao().delete(report)
        }
    }

    fun threadSafeInsert(reportGrade: ReportGrade) {
        thread {
            getGradeDao().insert(reportGrade)
        }
    }
}