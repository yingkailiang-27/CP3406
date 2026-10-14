package au.edu.jcu.assessment.utilityapp

import au.edu.jcu.assessment.utilityapp.data.*
import au.edu.jcu.assessment.utilityapp.model.TemperatureUnit
import au.edu.jcu.assessment.utilityapp.model.WeatherCity
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: RetrofitWeatherRepository
    @Before fun setUp() {
        server = MockWebServer()
        server.start()
        repository = RetrofitWeatherRepository(Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create()).build().create(WeatherApi::class.java))
    }
    @After fun tearDown() { server.shutdown() }

    @Test fun requestUsesChosenCityAndTemperatureUnit() = runBlocking {
        server.enqueue(MockResponse().setBody(
            """{"timezone":"Australia/Brisbane","current":{"time":"2026-10-08T15:00",
            "temperature_2m":81.5,"apparent_temperature":84.0,"weather_code":2}}"""
        ))
        val weather = repository.fetch(WeatherCity.TOWNSVILLE, TemperatureUnit.FAHRENHEIT)
        val request = server.takeRequest().requestUrl!!
        assertEquals("/v1/forecast", request.encodedPath)
        assertEquals("-19.259", request.queryParameter("latitude"))
        assertEquals("fahrenheit", request.queryParameter("temperature_unit"))
        assertEquals("auto", request.queryParameter("timezone"))
        assertEquals(81.5, weather.temperature, 0.001)
        assertEquals("Partly cloudy", weather.description)
    }
    @Test fun serverFailurePropagatesForViewModelHandling() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))
        try {
            repository.fetch(WeatherCity.CAIRNS, TemperatureUnit.CELSIUS)
            fail("Expected HTTP failure")
        } catch (expected: HttpException) { assertEquals(503, expected.code()) }
    }
    @Test fun missingTemperatureDoesNotBecomeFakeZeroDegrees() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"current":{"weather_code":0}}"""))
        try {
            repository.fetch(WeatherCity.CAIRNS, TemperatureUnit.CELSIUS)
            fail("Expected missing data failure")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message!!.contains("temperature"))
        }
    }
}
