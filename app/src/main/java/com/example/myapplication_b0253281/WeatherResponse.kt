package com.example.myapplication_b0253281

data class WeatherResponse(
    val success: String,
    val result: Result,
    val records: Records
)

data class Result(
    val resource_id: String,
    val fields: List<Field>
)

data class Field(
    val id: String,
    val type: String
)

data class Records(
    val Station: List<Station>
)

data class Station(
    val StationName: String,
    val StationId: String,
    val ObsTime: ObsTime,
    val GeoInfo: GeoInfo,
    val WeatherElement: WeatherElement,
)


data class ObsTime(
    val DateTime: String
)

data class GeoInfo(
    val CountyName: String,
    val StationLatitude: String?,
    val StationLongitude: String?
)


data class WeatherElement(
    val Weather: String,
    val AirTemperature: String? = null
)
