package com.example.snowboard.Fragments

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.snowboard.BuildConfig
import com.example.snowboard.Interface.WeatherApiService
import com.example.snowboard.R
import com.example.snowboard.Response.WeatherResponse
import com.example.snowboard.User.Settings.AppPreferences
import com.example.snowboard.databinding.FragmentWeatherScreenBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherScreenFragment : Fragment() {
    private lateinit var binding: FragmentWeatherScreenBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val currentLang = Locale.getDefault().language
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val weatherService by lazy { retrofit.create(WeatherApiService::class.java) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWeatherScreenBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 100)
        } else {
            getLocationAndFetchWeather()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationAndFetchWeather()
            } else {
                Log.e("Weather", getString(R.string.weather_error))
            }
        }
    }

    private fun getLocationAndFetchWeather() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            checkLocationPermission()
            return
        }

        fusedLocationClient.getCurrentLocation(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location ->
            if (location != null) {
                fetchWeatherData(location.latitude, location.longitude)
            } else {
                // Fallback to lastLocation if getCurrentLocation fails
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                    lastLoc?.let { fetchWeatherData(it.latitude, it.longitude) }
                }
            }
        }
    }

    private fun fetchWeatherData(lat: Double, lon: Double) {
        val useMetric = AppPreferences.isMetricUnits(requireContext())
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val response = weatherService.getCurrentWeather(
                    lat,
                    lon,
                    BuildConfig.WEATHER_API_KEY,
                    if (useMetric) "metric" else "imperial",
                    currentLang
                )
                updateUI(response, useMetric)
            } catch (e: Exception) {
                Log.e("Weather", "Error: ${e.message}")
            }
        }
    }

    private fun updateUI(data: WeatherResponse, useMetric: Boolean) {
        val minTepValue = data.main.temp_min.toInt()
        val maxTepValue = data.main.temp_max.toInt()
        val tempUnit = if (useMetric) "C" else "F"
        val speedUnit = if (useMetric) "m/s" else "mph"
        binding.apply {
            date.text = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())
            myLocation.text = "${data.name}"
            temp.text = "${data.main.temp.toInt()}°$tempUnit"
            minTemp.text = getString(R.string.weather_min, minTepValue, tempUnit)
            maxTemp.text = getString(R.string.weather_max, maxTepValue, tempUnit)
            humidity.text = "${data.main.humidity}%"
            windSpeed.text = "${data.wind.speed} $speedUnit"
            conditions.text = data.weather[0].main
            sunrise.text =
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(data.sys.sunrise * 1000))
            sunset.text =
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(data.sys.sunset * 1000))
            sea.text = "${data.main.sea_level} hPa"
            updateTheBackUI(conditions.text.toString())
        }
    }

    private fun updateTheBackUI(conditions: String) {
        val language = Locale.getDefault().language
        if (language == "bg") {
            updateBulgarianUI(conditions)
        } else {
            updateEnglishUI(conditions)
        }
    }

    private fun updateBulgarianUI(conditions: String) {
        binding.apply {
            when (conditions) {
                "Clouds" -> {
                    weather.text = getString(R.string.weather_condition_1)
                    weatherBack.setBackgroundResource(R.drawable.mist_back)
                    weatherAnimation.setAnimation(R.raw.clouds)
                }

                "Windy" -> {
                    weather.text = getString(R.string.weather_condition_2)
                    weatherBack.setBackgroundResource(R.drawable.mist_back)
                    weatherAnimation.setAnimation(R.raw.windy)
                }

                "Rain" -> {
                    weather.text = getString(R.string.weather_condition_3)
                    weatherBack.setBackgroundResource(R.drawable.rain_back)
                    weatherAnimation.setAnimation(R.raw.cloudy_rain)
                }

                "Snow" -> {
                    weather.text = getString(R.string.weather_condition_4)
                    weatherBack.setBackgroundResource(R.drawable.snow_back)
                    weatherAnimation.setAnimation(R.raw.snow)
                }

                "Clear" -> {
                    weather.text = getString(R.string.weather_condition_5)
                    weatherBack.setBackgroundResource(R.drawable.sunny_back)
                    weatherAnimation.setAnimation(R.raw.sunny)
                }

                "Sunny" -> {
                    weather.text = getString(R.string.weather_condition_6)
                    weatherBack.setBackgroundResource(R.drawable.sunny_back)
                    weatherAnimation.setAnimation(R.raw.sunny)
                }

                "Haze" -> {
                    weather.text = getString(R.string.weather_condition_7)
                    weatherBack.setBackgroundResource(R.drawable.mist_back)
                    weatherAnimation.setAnimation(R.raw.mist)
                }

                "Squall" -> {
                    weather.text = getString(R.string.weather_condition_8)
                    weatherBack.setBackgroundResource(R.drawable.snow_back)
                    weatherAnimation.setAnimation(R.raw.snow)
                }

                "Drizzle" -> {
                    weather.text = getString(R.string.weather_condition_9)
                    weatherBack.setBackgroundResource(R.drawable.rain_back)
                    weatherAnimation.setAnimation(R.raw.cloudy_rain)
                }

                "Thunderstorm" -> {
                    weather.text = getString(R.string.weather_condition_10)
                    weatherBack.setBackgroundResource(R.drawable.storm_back)
                    weatherAnimation.setAnimation(R.raw.storm)
                }

                else -> {
                    weather.text = getString(R.string.weather_condition_5)
                    weatherBack.setBackgroundResource(R.drawable.sunny_back)
                    weatherAnimation.setAnimation(R.raw.sunny)
                }
            }
            weatherAnimation.playAnimation()
        }
    }

    private fun updateEnglishUI(conditions: String) {
        binding.apply {
            when (conditions) {
                "Clouds" -> {
                    weather.text = getString(R.string.weather_condition_1)
                    weatherBack.setBackgroundResource(R.drawable.mist_back)
                    weatherAnimation.setAnimation(R.raw.clouds)
                }

                "Windy" -> {
                    weather.text = getString(R.string.weather_condition_2)
                    weatherBack.setBackgroundResource(R.drawable.mist_back)
                    weatherAnimation.setAnimation(R.raw.windy)
                }

                "Rain" -> {
                    weather.text = getString(R.string.weather_condition_3)
                    weatherBack.setBackgroundResource(R.drawable.rain_back)
                    weatherAnimation.setAnimation(R.raw.cloudy_rain)
                }

                "Snow" -> {
                    weather.text = getString(R.string.weather_condition_4)
                    weatherBack.setBackgroundResource(R.drawable.snow_back)
                    weatherAnimation.setAnimation(R.raw.snow)
                }

                "Clear" -> {
                    weather.text = getString(R.string.weather_condition_5)
                    weatherBack.setBackgroundResource(R.drawable.sunny_back)
                    weatherAnimation.setAnimation(R.raw.sunny)
                }

                "Sunny" -> {
                    weather.text = getString(R.string.weather_condition_6)
                    weatherBack.setBackgroundResource(R.drawable.sunny_back)
                    weatherAnimation.setAnimation(R.raw.sunny)
                }

                "Haze" -> {
                    weather.text = getString(R.string.weather_condition_7)
                    weatherBack.setBackgroundResource(R.drawable.mist_back)
                    weatherAnimation.setAnimation(R.raw.mist)
                }

                "Squall" -> {
                    weather.text = getString(R.string.weather_condition_8)
                    weatherBack.setBackgroundResource(R.drawable.snow_back)
                    weatherAnimation.setAnimation(R.raw.snow)
                }

                "Drizzle" -> {
                    weather.text = getString(R.string.weather_condition_9)
                    weatherBack.setBackgroundResource(R.drawable.rain_back)
                    weatherAnimation.setAnimation(R.raw.cloudy_rain)
                }

                "Thunderstorm" -> {
                    weather.text = getString(R.string.weather_condition_10)
                    weatherBack.setBackgroundResource(R.drawable.storm_back)
                    weatherAnimation.setAnimation(R.raw.storm)
                }

                else -> {
                    weather.text = getString(R.string.weather_condition_5)
                    weatherBack.setBackgroundResource(R.drawable.sunny_back)
                    weatherAnimation.setAnimation(R.raw.sunny)
                }
            }
            weatherAnimation.playAnimation()
        }
    }

    override fun onResume() {
        super.onResume()
        // To HIDE the toolbar
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    override fun onStop() {
        super.onStop()
        // To SHOW the toolbar when leaving this fragment
        (activity as? AppCompatActivity)?.supportActionBar?.show()
    }
}