package au.edu.jcu.assessment.utilityapp.data

import com.google.gson.annotations.SerializedName
import au.edu.jcu.assessment.utilityapp.model.TemperatureUnit
import au.edu.jcu.assessment.utilityapp.model.WeatherCity
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("v1/forecast")
    suspend fun currentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("temperature_unit") temperatureUnit: String,
        @Query("current") current: String = "temperature_2m,apparent_temperature,weather_code",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 1
    ): WeatherResponse
}

data class WeatherResponse(val current: CurrentWeatherDto?, val timezone: String?)

data class CurrentWeatherDto(
    val time: String?,
    @SerializedName("temperature_2m") val temperature: Double?,
    @SerializedName("apparent_temperature") val apparentTemperature: Double?,
    @SerializedName("weather_code") val weatherCode: Int?
)

data class WeatherInfo(
    val city: WeatherCity,
    val unit: TemperatureUnit,
    val temperature: Double,
    val apparentTemperature: Double?,
    val description: String,
    val observedAt: String,
    val timezone: String
)

interface WeatherRepository {
    suspend fun fetch(city: WeatherCity, unit: TemperatureUnit): WeatherInfo
}

class RetrofitWeatherRepository(private val api: WeatherApi) : WeatherRepository {
    override suspend fun fetch(city: WeatherCity, unit: TemperatureUnit): WeatherInfo {
        val response = api.currentWeather(city.latitude, city.longitude, unit.apiValue)
        val current = response.current ?: error("Weather service returned no current conditions.")
        val temperature = current.temperature
            ?: error("Weather service returned no temperature.")
        check(temperature.isFinite()) { "Weather service returned an invalid temperature." }
        return WeatherInfo(
            city, unit, temperature, current.apparentTemperature,
            weatherDescription(current.weatherCode),
            current.time ?: "Unknown time", response.timezone ?: "Local time"
        )
    }
}

fun weatherDescription(code: Int?): String = when (code) {
    0 -> "Clear sky"
    1 -> "Mainly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45, 48 -> "Fog"
    51, 53, 55 -> "Drizzle"
    56, 57 -> "Freezing drizzle"
    61, 63, 65 -> "Rain"
    66, 67 -> "Freezing rain"
    71, 73, 75, 77 -> "Snow"
    80, 81, 82 -> "Rain showers"
    85, 86 -> "Snow showers"
    95, 96, 99 -> "Thunderstorm"
    else -> "Conditions unavailable"
}
