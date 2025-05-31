package com.example.myapplication_b0253281

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {
    @GET("v1/rest/datastore/O-A0003-001")
    suspend fun getWeather(
        @Query("Authorization") key: String,
        @Query("format") format: String = "JSON",
        @Query("limit") limit: Int = 5
    ): Response<WeatherResponse>
}
