package com.example.snowboard.Interface

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherApiServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var api: WeatherApiService

    private val body = """
        {"main":{"temp":1.5,"humidity":70,"pressure":1000,"temp_min":0.0,"temp_max":3.0,"sea_level":1000},
         "weather":[{"main":"Clouds","description":"few clouds"}],
         "wind":{"speed":2.0},
         "sys":{"sunrise":1,"sunset":2,"country":"BG"},
         "name":"Borovets"}
    """.trimIndent()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/data/2.5/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun getCurrentWeather_sendsTheExpectedQuery_andParsesTheBody() = runBlocking {
        server.enqueue(MockResponse().setBody(body))

        val response = api.getCurrentWeather(42.27, 23.62, "KEY", language = "bg")

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        val url = request.requestUrl!!
        assertEquals("/data/2.5/weather", url.encodedPath())
        assertEquals("42.27", url.queryParameter("lat"))
        assertEquals("23.62", url.queryParameter("lon"))
        assertEquals("KEY", url.queryParameter("appid"))
        assertEquals("metric", url.queryParameter("units"))
        assertEquals("bg", url.queryParameter("lang"))
        assertEquals("Borovets", response.name)
        assertEquals(1.5, response.main.temp, 0.0)
    }

    @Test
    fun getCurrentWeather_unitsCanBeOverridden() = runBlocking {
        server.enqueue(MockResponse().setBody(body))
        api.getCurrentWeather(0.0, 0.0, "K", units = "imperial", language = "en")
        assertEquals("imperial", server.takeRequest().requestUrl!!.queryParameter("units"))
    }

    @Test
    fun getCurrentWeather_httpErrorIsThrown() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"cod":401}"""))
        val error = runCatching { runBlocking { api.getCurrentWeather(0.0, 0.0, "bad", language = "en") } }
            .exceptionOrNull()
        assertTrue(error is HttpException)
        assertEquals(401, (error as HttpException).code())
    }
}
