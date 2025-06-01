package com.example.myapplication_b0253281

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication_b0253281.databinding.ActivityMapsBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.*
import com.google.android.libraries.places.api.net.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.view.View

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapsBinding
    private lateinit var mMap: GoogleMap
    private lateinit var weatherTextView: TextView
    private lateinit var searchEditText: EditText
    private lateinit var placesClient: PlacesClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    private val stationCoordinates = mapOf(
        "466940" to LatLng(25.135, 121.739),
        "466900" to LatLng(25.180, 121.432),
        "466881" to LatLng(25.010, 121.460),
        "466930" to LatLng(25.166, 121.544),
        "466910" to LatLng(25.070, 121.640)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 狀態列背景設為白色
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.white)

        // 啟用「亮色狀態列圖示模式」→ 黑色圖示（適合白底）
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        weatherTextView = binding.tvWeatherInfo
        searchEditText = binding.etSearch

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, BuildConfig.PLACES_API_KEY)
        }
        placesClient = Places.createClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        searchEditText.setOnClickListener {
            searchEditText.requestFocus()
            showKeyboard(searchEditText)
        }

        searchEditText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = v.text.toString().trim()
                if (query.isNotEmpty()) searchPlace(query)
                true
            } else false
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        checkLocationPermission()

        mMap.setOnMapClickListener {
            hideKeyboard()
            searchEditText.clearFocus()
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else enableUserLocation()
    }

    private fun enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
        }

        LocationServices.getFusedLocationProviderClient(this).lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    val userLatLng = LatLng(it.latitude, it.longitude)
                    mMap.addMarker(MarkerOptions().position(userLatLng).title("你的位置"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                    fetchWeather(it.latitude, it.longitude)
                }
            }
    }

    private fun fetchWeather(lat: Double, lng: Double) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://opendata.cwa.gov.tw/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(WeatherService::class.java)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = service.getWeather(BuildConfig.WEATHER_API_KEY, "JSON", 100)
                if (response.isSuccessful) {
                    val stations = response.body()?.records?.Station ?: return@launch
                    val nearest = stations.minByOrNull {
                        val coord = stationCoordinates[it.StationId] ?: return@minByOrNull Double.MAX_VALUE
                        distanceBetween(lat, lng, coord.latitude, coord.longitude)
                    }
                    nearest?.let {
                        val icon = when {
                            it.WeatherElement.Weather.contains("晴") -> "☀️"
                            it.WeatherElement.Weather.contains("雨") -> "🌧️"
                            it.WeatherElement.Weather.contains("陰") -> "☁️"
                            else -> "🌤"
                        }
                        val temp = it.WeatherElement.AirTemperature ?: "--"
                        weatherTextView.text = "$icon ${it.GeoInfo.CountyName}（${it.StationName}）：${it.WeatherElement.Weather} $temp°C"
                    }
                }
            } catch (e: Exception) {
                weatherTextView.text = "天氣錯誤：${e.message}"
            }
        }
    }

    private fun distanceBetween(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val result = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, result)
        return result[0].toDouble()
    }

    private fun searchPlace(query: String) {
        if (!::mMap.isInitialized) return
        val bounds = mMap.projection.visibleRegion.latLngBounds
        val token = AutocompleteSessionToken.newInstance()
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setLocationRestriction(RectangularBounds.newInstance(bounds))
            .setSessionToken(token)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { result ->
                mMap.clear()
                val topResults = result.autocompletePredictions.take(3)
                if (topResults.isEmpty()) {
                    Toast.makeText(this, "找不到地點", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                topResults.forEachIndexed { index, prediction ->
                    getPlaceLocation(prediction, index == 0)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "搜尋失敗：${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getPlaceLocation(prediction: AutocompletePrediction, moveCamera: Boolean) {
        val request = FetchPlaceRequest.builder(prediction.placeId, listOf(Place.Field.LAT_LNG, Place.Field.NAME)).build()
        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                response.place.latLng?.let { latLng ->
                    mMap.addMarker(
                        MarkerOptions().position(latLng).title(response.place.name)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    )
                    if (moveCamera) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    }
                }
            }
    }

    private fun showKeyboard(editText: EditText) {
        editText.requestFocus()
        val imm = ContextCompat.getSystemService(this, InputMethodManager::class.java)
        imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = ContextCompat.getSystemService(this, InputMethodManager::class.java)
        currentFocus?.let { imm?.hideSoftInputFromWindow(it.windowToken, 0) }
    }
}
