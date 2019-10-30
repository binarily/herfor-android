package pl.herfor.android.objects.requests

import pl.herfor.android.objects.Point
import pl.herfor.android.objects.ReportProperties

data class ReportAddRequest(
    var location: Point,
    var properties: ReportProperties
)