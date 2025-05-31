package com.example.myapplication_b0253281

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication_b0253281.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var weatherTextView: TextView

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val apiKey = "CWA-50850EEF-639A-4AD8-BE65-0D533D2A28E8"

    private val stationCoordinates = mapOf(
        "466940" to LatLng(25.135, 121.739), // 基隆
        "466900" to LatLng(25.180, 121.432), // 淡水
        "466881" to LatLng(25.010, 121.460), // 新北
        "466930" to LatLng(25.166, 121.544), // 陽明山
        "466910" to LatLng(25.070, 121.640)  // 鞍部
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        weatherTextView = findViewById(R.id.tvWeatherInfo)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            enableUserLocation()
        }
    }

    private fun enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val lat = location.latitude
                    val lng = location.longitude
                    val userLatLng = LatLng(lat, lng)

                    Log.d("LocationDebug", "目前位置：$lat, $lng")
                    Toast.makeText(this, "目前位置：$lat, $lng", Toast.LENGTH_SHORT).show()

                    mMap.addMarker(MarkerOptions().position(userLatLng).title("你的位置"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))

                    fetchWeatherWithLocation(lat, lng)
                } else {
                    Toast.makeText(this, "無法取得目前位置", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun fetchWeatherWithLocation(userLat: Double, userLng: Double) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://opendata.cwa.gov.tw/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(WeatherService::class.java)

        lifecycleScope.launch {
            try {
                val response = service.getWeather(apiKey, "JSON", 100)
                if (response.isSuccessful) {
                    val stations = response.body()?.records?.Station ?: emptyList()

                    val nearest = stations.minByOrNull { station ->
                        val latLng = stationCoordinates[station.StationId]
                            ?: return@minByOrNull Double.MAX_VALUE
                        distanceBetween(userLat, userLng, latLng.latitude, latLng.longitude)
                    }

                    if (nearest != null) {
                        val city = nearest.GeoInfo.CountyName
                        val name = nearest.StationName
                        val weather = nearest.WeatherElement.Weather
                        val temp = nearest.WeatherElement.AirTemperature ?: "--"

                        val icon = when {
                            weather.contains("晴") -> "☀️"
                            weather.contains("雨") -> "🌧️"
                            weather.contains("陰") -> "☁️"
                            else -> "🌤"
                        }

                        weatherTextView.text = "$icon $city（$name）：$weather $temp°C"
                    } else {
                        weatherTextView.text = "找不到鄰近氣象站"
                    }
                } else {
                    weatherTextView.text = "天氣錯誤：${response.code()}"
                }
            } catch (e: Exception) {
                weatherTextView.text = "連線失敗：${e.message}"
            }
        }
    }

    private fun distanceBetween(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val result = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, result)
        return result[0].toDouble()
    }
}
