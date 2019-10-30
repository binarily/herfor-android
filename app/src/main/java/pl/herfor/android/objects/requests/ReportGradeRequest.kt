package pl.herfor.android.objects.requests

import pl.herfor.android.objects.Point
import pl.herfor.android.objects.enums.Grade

data class ReportGradeRequest(
    var reportId: String,
    var location: Point,
    var grade: Grade
)