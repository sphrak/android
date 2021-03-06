package fi.kroon.vadret.presentation.weatherforecast.model

data class WeatherForecastItemModel(
    val time: String,
    val temperature: Double,
    val feelsLikeTemperature: String? = null,
    val precipitationType: Int,
    val windSpeed: Double,
    val weatherIcon: Int,
    val weatherDescription: Int
) : BaseWeatherForecastModel