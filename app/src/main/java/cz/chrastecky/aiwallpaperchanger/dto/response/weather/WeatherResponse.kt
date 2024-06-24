package cz.chrastecky.aiwallpaperchanger.dto.response.weather

data class WeatherResponse(
    val coord: WeatherResponseCoordinates,
    val weather: List<WeatherResponseWeatherDetail>,
)
