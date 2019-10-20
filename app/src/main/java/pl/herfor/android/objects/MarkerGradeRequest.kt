package pl.herfor.android.objects

data class MarkerGradeRequest(
    var markerId: String,
    var location: Point,
    var grade: MarkerGrade
)