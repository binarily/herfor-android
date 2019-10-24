package pl.herfor.android.objects

import pl.herfor.android.objects.enums.Grade

data class MarkerGradeRequest(
    var markerId: String,
    var location: Point,
    var grade: Grade
)