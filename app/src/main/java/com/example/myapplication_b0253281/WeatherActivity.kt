package com.example.myapplication_b0253281

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private val apiKey = "CWA-50850EEF-639A-4AD8-BE65-0D533D2A28E8"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather)

        textView = findViewById(R.id.weatherTextView)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://opendata.cwa.gov.tw/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(WeatherService::class.java)

        lifecycleScope.launch {
            try {
                val response = service.getWeather(apiKey)
                if (response.isSuccessful) {
                    val stations = response.body()?.records?.Station
                    val displayText = stations?.joinToString("\n") {
                        "${it.StationName} - ${it.GeoInfo.CountyName} - ${it.WeatherElement.Weather}"
                    } ?: "無資料"
                    textView.text = displayText
                } else {
                    textView.text = "連線失敗：${response.code()}"
                }
            } catch (e: Exception) {
                Toast.makeText(this@WeatherActivity, "發生錯誤：${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
