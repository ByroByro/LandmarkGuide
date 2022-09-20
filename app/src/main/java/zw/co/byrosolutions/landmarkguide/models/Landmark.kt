package zw.co.byrosolutions.landmarkguide.models

data class Landmark(
    var id: Long,
    var latitude: String,
    var longtude: String,
    var latlong: String,
    var address: String,
    var name: String,
    var type: String
)
