package zw.co.byrosolutions.landmarkguide

//import okhttp3.ResponseBody
import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import zw.co.byrosolutions.landmarkguide.logic.methods
import zw.co.byrosolutions.landmarkguide.models.Landmark
import zw.co.byrosolutions.landmarkguide.models.MapData
import zw.co.byrosolutions.landmarkguide.preferences.PreferenceProvider
import zw.co.byrosolutions.landmarkguide.retrofit.APIClient
import zw.co.byrosolutions.landmarkguide.retrofit.APIInterface


class MainMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var currentLocation: Location
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient // fused location provider
    private val permissionCode = 101 // permission code
    private lateinit var dialog: ProgressDialog // progress dialog
    private var places = listOf<Landmark?>() // list of landmarks
    private lateinit var prefs: PreferenceProvider // preference provider
    private lateinit var mapFragment: SupportMapFragment // support fragment
    lateinit var mMap: GoogleMap // google map instance
    private var apiKey = ""

    // Current location is set to India, this will be of no use
    var currentLocation1: LatLng = LatLng(20.5, 78.9)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_map)

        dialog = ProgressDialog(this)

        mapFragment = (supportFragmentManager.findFragmentById(
            R.id.map_fragment
        ) as? SupportMapFragment)!!
        mapFragment.getMapAsync(this)

        prefs = PreferenceProvider(this)

        // Fetching API_KEY which we wrapped
        val ai: ApplicationInfo = applicationContext.packageManager
            .getApplicationInfo(applicationContext.packageName, PackageManager.GET_META_DATA)
        val value = ai.metaData["com.google.android.geo.API_KEY"]
        apiKey = value.toString()

        // Initializing the Places API with the help of our API_KEY
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }

        // Initializing fused location client
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this@MainMapActivity)

        // get the landmarks
        getLandmarks()
    }

    private val bicycleIcon: BitmapDescriptor by lazy {
        val color = ContextCompat.getColor(this, R.color.red)
        BitmapHelper.vectorToBitmap(this, R.drawable.ic_baseline_landscape_24, color)
    }

    // add markers to map
    private fun addMarkers(googleMap: GoogleMap) {
        try {
            places.forEach { place ->
                val latLng = LatLng(place!!.latitude!!.toDouble(), place!!.longtude!!.toDouble())
                if (place.type.equals(prefs.getLandmark(), true)) {
                    val marker = googleMap.addMarker(
                        MarkerOptions()
                            .title(place.name)
                            .position(latLng)
                            .icon(bicycleIcon)
                    )
                    // Set place as the tag on the marker object so it can be referenced within
                    // MarkerInfoWindowAdapter
                    marker.tag = place
                }
            }
        } catch (e: Exception) {
            methods.alertUser("Error", e.toString(), this)
        }
    }

    // fetch user current location
    @SuppressLint("MissingPermission")
    private fun fetchLocation() {
        try {
            // check permissions
            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) ==
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION
                ) ==
                PackageManager.PERMISSION_GRANTED
            ) {

                // check if location is enabled
                if (isLocationEnabled()) {

                    val task = fusedLocationProviderClient.lastLocation
                    task.addOnSuccessListener {
                        try {

                            // if location is not equal to null then show it
                            if (it != null) {
                                currentLocation = it
                                Toast.makeText(
                                    applicationContext, currentLocation.latitude.toString() + "" +
                                            currentLocation.longitude, Toast.LENGTH_SHORT
                                ).show()

                                // map initialized here
                                // val supportMapFragment =
                                //   (supportFragmentManager.findFragmentById(R.id.map_fragment) as
                                //            SupportMapFragment?)!!
                                // supportMapFragment.getMapAsync(this@MainMapActivity)
                                val latLng =
                                    LatLng(currentLocation.latitude, currentLocation.longitude)
                                mMap.addMarker(
                                    MarkerOptions().position(latLng).title("You are here")
                                )
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10F))
                            } else {
                                // request new location
                                requestNewLocationData()
                                Toast.makeText(
                                    this@MainMapActivity,
                                    "Location is null",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                                // tell user hint to start google maps app
                            }
                        } catch (e: Exception) {
                            methods.alertUser("Error", e.toString(), this@MainMapActivity)
                        }
                    }
                } else {
                    // open intent to enable location
                    Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                }
            } else {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), permissionCode
                )
            }
        } catch (e: Exception) {
            methods.alertUser("Error", e.toString(), this)
        }
    }

    // on map ready
    override fun onMapReady(googleMap: GoogleMap?) {
        try {
            // initialise map object
            if (googleMap != null) {
                mMap = googleMap
            }

            // get current location
            fetchLocation()

            // adding on click listener to marker of google maps.
            mMap.setOnMarkerClickListener { marker ->
                // on marker click we are getting the title of our marker
                // which is clicked and displaying it in a toast message.
                val markerName = marker.title
                // var thisPlace = Landmark(0,"","","","","","")
                var thisPlace: Landmark? = null
                var markerLatLong = LatLng(0.0, 0.0)
                places.forEach {
                    if (it!!.name == markerName) {
                        thisPlace = it
                        markerLatLong = LatLng(it!!.latitude.toDouble(), it!!.longtude.toDouble())
                        return@forEach
                    }
                }

                // show a sign up bottom dialog
                val dialog = BottomSheetDialog(this, R.style.SheetDialog)
                val view = layoutInflater.inflate(R.layout.actions_info_contents, null)
                // widgets on bottom dialog
                val btnRoute = view.findViewById<ImageButton>(R.id.btnDrawRoute)
                val btnEstimates = view.findViewById<ImageButton>(R.id.btnEstTimeDistance)
                val btnDirections = view.findViewById<ImageButton>(R.id.btnDirections)
                val btnSetFav = view.findViewById<ImageButton>(R.id.btnSetFav)

                // button route
                btnRoute.setOnClickListener {
                    // draw route on map
                    drawRoute(markerLatLong.latitude, markerLatLong.longitude)
                }

                // button estimates
                btnEstimates.setOnClickListener {
                    // retrieve estimates form google api
                    // check current location
                    if (currentLocation != null) {
                        estimateTimeAndDistance(
                            currentLocation.latitude.toString(),
                            currentLocation.longitude.toString(),
                            markerLatLong.latitude.toString(),
                            markerLatLong.longitude.toString(),
                            apiKey
                        )
                    } else {
                        Toast.makeText(
                            this@MainMapActivity,
                            "Please make sure your current location is update!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                // button directions
                btnDirections.setOnClickListener {
                    getDirections(
                        currentLocation.latitude.toString(),
                        currentLocation.longitude.toString(),
                        markerLatLong.latitude.toString(),
                        markerLatLong.longitude.toString(),
                        apiKey
                    )
                }

                // button set as favorite
                btnSetFav.setOnClickListener {
                    saveAsFavorite(thisPlace?.id.toString())
                }

                // disallow cancellation from touch outside
                dialog.setCanceledOnTouchOutside(true)
                dialog.setCancelable(true)
                // dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

                //set content
                dialog.setContentView(view)
                // dialog show
                dialog.show()

                Toast.makeText(
                    this@MainMapActivity,
                    "Clicked location is $markerName",
                    Toast.LENGTH_SHORT
                )
                    .show()
                false
            }
        } catch (e: Exception) {
            methods.alertUser("Error", e.toString(), this)
        }
    }

    private fun saveAsFavorite(locationId: String) {
        try {
            val preferenceProvider = PreferenceProvider(this)

            var jsonObject = JSONObject()
            jsonObject.put("user_id", preferenceProvider.getUserId())
            jsonObject.put("location_id", locationId)

            val apiClient = APIClient().getInstance().create(
                APIInterface::class.java
            )
            var attendance: Call<ResponseBody?>? =
                apiClient.saveFavorite(jsonObject.toString())

            dialog.setTitle("Saving favorite")
            dialog.setMessage("Uploading favorite.Please wait...")
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()

            attendance?.enqueue(object : Callback<ResponseBody?> {
                override fun onResponse(
                    call: Call<ResponseBody?>,
                    response: Response<ResponseBody?>
                ) {
                    dialog.dismiss()
                    if (response.isSuccessful) {
                        val res = response.body()?.string()
                        val jsonObject = JSONObject(res)
                        when (val message = jsonObject.getString("message")) {
                            "Exists" -> {
                                Toast.makeText(
                                    this@MainMapActivity,
                                    "Already a favorite",
                                    Toast.LENGTH_LONG
                                )
                                    .show()
                            }
                            "Success" -> {
                                Toast.makeText(
                                    this@MainMapActivity,
                                    "Successfully saved favorite",
                                    Toast.LENGTH_LONG
                                )
                                    .show()
                            }
                            "Failed" -> {
                                Toast.makeText(
                                    this@MainMapActivity,
                                    "Failed to save favorite, try again!",
                                    Toast.LENGTH_LONG
                                )
                                    .show()
                            }
                            else -> {
                                Toast.makeText(
                                    this@MainMapActivity,
                                    message,
                                    Toast.LENGTH_LONG
                                )
                                    .show()
                            }
                        }
                    } else {
                        Toast.makeText(this@MainMapActivity, "Request failed", Toast.LENGTH_LONG)
                            .show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                    dialog.dismiss()
                    methods.alertUser(
                        "Response",
                        "A network error occurred. The request failed, please try again after a moment!",
                        this@MainMapActivity
                    )
                }

            })
        } catch (e: Exception) {
            dialog.dismiss()
            methods.alertUser("Error", e.toString(), this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == permissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation()
            }
        }
    }

    // create options menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.map_options_menu, menu)
        return true
    }

    // on options menu selected
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // navigate to real time recognition activity
                val realTime = Intent(this, SettingsActivity::class.java)
                startActivity(realTime)
                return true
            }

            R.id.action_my_location -> {
                // call fetch location
                fetchLocation()
                return true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    // download and save form fields
    private fun getLandmarks() {
        try {

            dialog.setTitle("Loading landmarks")
            dialog.setMessage("Please wait while loading...")

            val apiClient = APIClient().getInstance()
                .create(APIInterface::class.java)
            var tasksCall: Call<ResponseBody?>? = apiClient.getLandmarks()

            // show dialog
            dialog.show()
            tasksCall?.enqueue(object : Callback<ResponseBody?> {
                override fun onResponse(
                    call: Call<ResponseBody?>,
                    response: Response<ResponseBody?>
                ) {
                    // dismiss dialog
                    dialog.dismiss()

                    if (response.isSuccessful) {
                        //get response string
                        val res = response.body()!!.string()
                        val mainArray = JSONArray(res)
                        val hasDataObject = mainArray.getJSONObject(0)
                        val hasData = hasDataObject.getString("response")
                        if (hasData == "yes") {
                            val dataObject = mainArray.getJSONObject(1)
                            val dataArray = dataObject.getJSONArray("data")
                            var place: Landmark
                            for (i in 0 until dataArray.length()) {
                                val jsonObject = dataArray.getJSONObject(i)
                                place = Landmark(
                                    jsonObject.getLong("id"),
                                    jsonObject.getString("latitude"),
                                    jsonObject.getString("longtude"),
                                    jsonObject.getString("latlong"),
                                    jsonObject.getString("address"),
                                    jsonObject.getString("name"),
                                    jsonObject.getString("type"),
                                )

                                places += place
                            }

                            // show markers
                            mapFragment?.getMapAsync { googleMap ->
                                addMarkers(googleMap)
                                googleMap.setInfoWindowAdapter(MarkerInfoWindowAdapter(this@MainMapActivity))
                            }
                        } else {
                            Toast.makeText(
                                this@MainMapActivity,
                                "No landmark data found!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    } else {
                        Toast.makeText(
                            this@MainMapActivity,
                            "Request failed with code \" + response.code() + \"\\nAn unexpected error occurred.Please retry and if problem persists contact admin(s)!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                    dialog.dismiss()
                    methods.alertUser(
                        "Error",
                        "A network error occurred. The request failed, please try again after a moment!",
                        this@MainMapActivity
                    )
                }

            })
        } catch (e: Exception) {
            dialog.dismiss()
            methods.alertUser(
                "Error",
                "An unexpected error occurred during executing current operation.Please contact your admin(s)",
                this@MainMapActivity
            )
        }
    }

    // Get current location, if shifted
    // from previous location
    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        try {
            val mLocationRequest = LocationRequest()
            mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            mLocationRequest.interval = 0
            mLocationRequest.fastestInterval = 0
            mLocationRequest.numUpdates = 1

            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationProviderClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                Looper.myLooper()
            )
        } catch (e: Exception) {
            methods.alertUser("Error", e.toString(), this)
        }
    }

    // If current location could not be located, use last location
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            currentLocation1 = LatLng(mLastLocation.latitude, mLastLocation.longitude)
        }
    }

    // function to check if GPS is on
    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    // method to request directions
    private fun getDirectionURL(origin: LatLng, dest: LatLng, secret: String): String {
        return "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}" +
                "&destination=${dest.latitude},${dest.longitude}" +
                "&sensor=false" +
                "&mode=driving" +
                "&key=$secret"
    }

    @SuppressLint("StaticFieldLeak")
    private inner class GetDirection(val url: String) :
        AsyncTask<Void, Void, List<List<LatLng>>>() {
        override fun doInBackground(vararg params: Void?): List<List<LatLng>> {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val data = response.body!!.string()

            val result = ArrayList<List<LatLng>>()
            try {
                val respObj = Gson().fromJson(data, MapData::class.java)
                val path = ArrayList<LatLng>()
                for (i in 0 until respObj.routes[0].legs[0].steps.size) {
                    path.addAll(decodePolyline(respObj.routes[0].legs[0].steps[i].polyline.points))
                }
                result.add(path)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return result
        }

        override fun onPostExecute(result: List<List<LatLng>>) {
            val lineOption = PolylineOptions()
            for (i in result.indices) {
                lineOption.addAll(result[i])
                lineOption.width(10f)
                lineOption.color(Color.GREEN)
                lineOption.geodesic(true)
            }
            mMap.addPolyline(lineOption)
        }
    }

    // decode polyline
    fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val latLng = LatLng((lat.toDouble() / 1E5), (lng.toDouble() / 1E5))
            poly.add(latLng)
        }
        return poly
    }

    // draw route from current location to selected location
    private fun drawRoute(lat: Double, long: Double) {
        try {
            if (currentLocation != null) {
                mapFragment.getMapAsync {
                    mMap = it
                    val originLocation = LatLng(currentLocation.latitude, currentLocation.longitude)
                    mMap.addMarker(MarkerOptions().position(originLocation))
                    val destinationLocation = LatLng(lat, long)
                    mMap.addMarker(MarkerOptions().position(destinationLocation))
                    val url = getDirectionURL(originLocation, destinationLocation, apiKey)
                    GetDirection(url).execute()
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(originLocation, 14F))
                }
            } else {
                Toast.makeText(this, "Please get your current location first!", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: Exception) {
            methods.alertUser("Error", e.toString(), this)
        }
    }

    // test estimated time and distance
    private fun estimateTimeAndDistance(
        oLat: String,
        oLong: String,
        dLat: String,
        dLong: String,
        key: String
    ) {

        // try catch
        try {
            // progress dialog
            val dialog = ProgressDialog(this)
            dialog.setTitle("Processing")
            dialog.setCanceledOnTouchOutside(false)
            dialog.setMessage("Retrieving info, please please wait...")
            dialog.setCancelable(true)
            dialog.show()

            val url =
                "https://maps.googleapis.com/maps/api/distancematrix/json?origins=${oLat},${oLong}" +
                        "&destinations=${dLat},${dLong}" +
                        "&units=imperial" +
                        "&key=$key"

            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            CoroutineScope(IO).launch {
                try {
                    val response = client.newCall(request).execute()

                    // dismiss dialog
                    withContext(Dispatchers.Main) {
                        dialog.dismiss()
                    }

                    val data = response.body!!.string()

                    val body = JSONObject(data)
                    val status = body.getString("status")
                    if (status == "OK") {
                        val rows = body.getJSONArray("rows")
                        val objectAtZero = rows.getJSONObject(0)
                        val elements = objectAtZero.getJSONArray("elements")
                        val elementAtZero = elements.getJSONObject(0)
                        if (elementAtZero.getString("status") == "OK") {
                            val distance = elementAtZero.getJSONObject("distance")
                            val duration = elementAtZero.getJSONObject("duration")

                            val dis = distance.getString("text").split(" ")

                            // distance
                            var actualDistance: String = if (prefs.getMetric() == "KM") {
                                methods.convertIntoKms(dis[0].toDouble()) + "Km"
                            } else {
                                distance.getString("text")
                            }
                            // time
                            val time = duration.getString("text")

                            withContext(Dispatchers.Main) {
                                // show info pop up
                                methods.alertUser(
                                    "Estimates",
                                    "Estimated distance : $actualDistance \nEstimated time : $time",
                                    this@MainMapActivity
                                )
                            }

                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@MainMapActivity,
                                    elementAtZero.getString("status"),
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainMapActivity, status, Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        dialog.dismiss()
                        methods.alertUser("Error", e.toString(), this@MainMapActivity)
                    }
                }
            }
        } catch (e: Exception) {
            methods.alertUser("Error", e.toString(), this)
        }
    }

    // get directions
    private fun getDirections(
        oLat: String,
        oLong: String,
        dLat: String,
        dLong: String,
        key: String
    ) {
        try {
            // progress dialog
            val dialog = ProgressDialog(this)
            dialog.setTitle("Processing")
            dialog.setCanceledOnTouchOutside(false)
            dialog.setMessage("Retrieving info, please please wait...")
            dialog.setCancelable(true)
            dialog.show()

            val url =
                "https://maps.googleapis.com/maps/api/directions/json?origin=${oLat},${oLong}" +
                        "&destination=${dLat},${dLong}" +
                        "&sensor=false" +
                        "&mode=driving" +
                        "&key=$key"

            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            CoroutineScope(IO).launch {
                try {
                    val response = client.newCall(request).execute()

                    // dismiss dialog
                    withContext(Dispatchers.Main) {
                        dialog.dismiss()
                    }

                    val data = response.body!!.string()

                    Log.d("Dir", data)

//                    val body = JSONObject(data)
//                    val status = body.getString("status")
//                    if (status == "OK") {
//                        val rows = body.getJSONArray("rows")
//                        val objectAtZero = rows.getJSONObject(0)
//                        val elements = objectAtZero.getJSONArray("elements")
//                        val elementAtZero = elements.getJSONObject(0)
//                        if (elementAtZero.getString("status") == "OK") {
//                            val distance = elementAtZero.getJSONObject("distance")
//                            val duration = elementAtZero.getJSONObject("duration")
//
//                            val dis = distance.getString("text").split(" ")
//
//                            // distance
//                            var actualDistance: String = if (prefs.getMetric() == "KM") {
//                                methods.convertIntoKms(dis[0].toDouble()) + "Km"
//                            } else {
//                                distance.getString("text")
//                            }
//                            // time
//                            val time = duration.getString("text")
//
//                            withContext(Dispatchers.Main) {
//                                // show info pop up
//                                methods.alertUser(
//                                    "Estimates",
//                                    "Estimated distance : $actualDistance \nEstimated time : $time",
//                                    this@MainMapActivity
//                                )
//                            }
//
//                        } else {
//                            withContext(Dispatchers.Main) {
//                                Toast.makeText(
//                                    this@MainMapActivity,
//                                    elementAtZero.getString("status"),
//                                    Toast.LENGTH_LONG
//                                ).show()
//                            }
//
//                        }
//                    } else {
//                        withContext(Dispatchers.Main) {
//                            Toast.makeText(this@MainMapActivity, status, Toast.LENGTH_LONG).show()
//                        }
//                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        dialog.dismiss()
                        methods.alertUser("Error", e.toString(), this@MainMapActivity)
                    }
                }
            }
        } catch (e: Exception) {
            methods.alertUser("Error", e.toString(), this)
        }
    }
}