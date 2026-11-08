package com.example.snowboard.Response

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeatherResponseTest {

    private val json = """
        {
          "main": {"temp": -3.5, "humidity": 80, "pressure": 1012, "temp_min": -5.0, "temp_max": -1.0, "sea_level": 1012},
          "weather": [{"main": "Snow", "description": "light snow", "icon": "13d"}],
          "wind": {"speed": 4.2, "deg": 200},
          "sys": {"sunrise": 1700000000, "sunset": 1700030000, "country": "BG"},
          "name": "Bansko",
          "cod": 200
        }
    """.trimIndent()

    @Test
    fun parsesAnOpenWeatherResponse_ignoringUnknownFields() {
        val r = Gson().fromJson(json, WeatherResponse::class.java)
        assertEquals("Bansko", r.name)
        assertEquals(-3.5, r.main.temp, 0.0)
        assertEquals(80, r.main.humidity)
        assertEquals(1012, r.main.pressure)
        assertEquals(-5.0, r.main.temp_min, 0.0)
        assertEquals(-1.0, r.main.temp_max, 0.0)
        assertEquals(1012, r.main.sea_level)
        assertEquals(1, r.weather.size)
        assertEquals("Snow", r.weather[0].main)
        assertEquals("light snow", r.weather[0].description)
        assertEquals(4.2, r.wind.speed, 0.0)
        assertEquals(1700000000L, r.sys.sunrise)
        assertEquals(1700030000L, r.sys.sunset)
        assertEquals("BG", r.sys.country)
    }

    @Test
    fun country_canBeMissing() {
        val noCountry = json.replace(", \"country\": \"BG\"", "")
        assertNull(Gson().fromJson(noCountry, WeatherResponse::class.java).sys.country)
    }

    @Test
    fun dataClasses_compareByValue() {
        assertEquals(Weather("Snow", "x"), Weather("Snow", "x"))
        assertEquals(Wind(1.0), Wind(1.0))
        assertEquals(Sys(1, 2, null), Sys(1, 2, null))
    }
}
