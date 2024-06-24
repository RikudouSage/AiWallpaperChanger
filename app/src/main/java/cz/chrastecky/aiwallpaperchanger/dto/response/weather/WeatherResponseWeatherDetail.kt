package cz.chrastecky.aiwallpaperchanger.dto.response.weather

data class WeatherResponseWeatherDetail(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String,
)
