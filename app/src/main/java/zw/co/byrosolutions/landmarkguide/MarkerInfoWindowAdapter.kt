package zw.co.byrosolutions.landmarkguide

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import zw.co.byrosolutions.landmarkguide.models.Landmark

class MarkerInfoWindowAdapter(
    private val context: Context
) : GoogleMap.InfoWindowAdapter {
    override fun getInfoContents(marker: Marker?): View? {
        // 1. Get tag
        val place = marker?.tag as? Landmark ?: return null

        // 2. Inflate view and set title, address, and rating
        val view = LayoutInflater.from(context).inflate(
            R.layout.marker_info_contents, null
        )
        view.findViewById<TextView>(
            R.id.text_view_title
        ).text = place.name
        view.findViewById<TextView>(
            R.id.text_view_address
        ).text = place.address
        view.findViewById<TextView>(
            R.id.text_view_rating
        ).text = "Type : " + place.type

        return view
    }

    override fun getInfoWindow(marker: Marker?): View? {
        // Return null to indicate that the
        // default window (white bubble) should be used
        return null
    }
}