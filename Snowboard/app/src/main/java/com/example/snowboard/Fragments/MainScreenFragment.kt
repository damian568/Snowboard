package com.example.snowboard.Fragments

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.snowboard.BuildConfig
import com.example.snowboard.Interface.WeatherApiService
import com.example.snowboard.Lists.NearbySlopeData
import com.example.snowboard.Lists.NearbySlopeList
import com.example.snowboard.Lists.QuickAction
import com.example.snowboard.R
import com.example.snowboard.Response.WeatherResponse
import com.example.snowboard.User.Settings.AppPreferences
import com.example.snowboard.databinding.FragmentMainScreenBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class MainScreenFragment : Fragment() {

    private lateinit var binding: FragmentMainScreenBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
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
        binding = FragmentMainScreenBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnMenu.setOnClickListener { openDrawer() }
        binding.txtQuickActionsViewAll.setOnClickListener { openDrawer() }
        binding.txtNearbySlopesViewAll.setOnClickListener { goToSkiSlopes() }
        binding.btnEditQuickActions.setOnClickListener {
            EditQuickActionsBottomSheet().show(childFragmentManager, "EditQuickActions")
        }
        childFragmentManager.setFragmentResultListener(
            EditQuickActionsBottomSheet.REQUEST_KEY, viewLifecycleOwner
        ) { _, _ -> renderQuickActions() }
        renderQuickActions()
        loadUserName()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        checkLocationPermission()
    }

    private fun renderQuickActions() {
        val selected = AppPreferences.getQuickActions(requireContext()).mapNotNull { QuickAction.fromId(it) }
        val slots = listOf(
            Triple(binding.btnQuickSlot1, binding.imgQuickSlot1, binding.txtQuickSlot1),
            Triple(binding.btnQuickSlot2, binding.imgQuickSlot2, binding.txtQuickSlot2),
            Triple(binding.btnQuickSlot3, binding.imgQuickSlot3, binding.txtQuickSlot3),
            Triple(binding.btnQuickSlot4, binding.imgQuickSlot4, binding.txtQuickSlot4)
        )
        slots.forEachIndexed { index, (container, icon, label) ->
            val action = selected.getOrNull(index)
            if (action == null) {
                container.visibility = View.GONE
            } else {
                container.visibility = View.VISIBLE
                icon.setImageResource(action.icon)
                label.setText(action.label)
                container.setOnClickListener { findNavController().navigate(action.destination) }
            }
        }
    }

    private fun openDrawer() {
        requireActivity().findViewById<DrawerLayout>(R.id.drawerLayout)
            ?.openDrawer(GravityCompat.START)
    }

    private fun goToSkiSlopes() {
        findNavController().navigate(R.id.skiSlopesScreenFragment)
    }

    private fun loadUserName() {
        val user = auth.currentUser ?: return

        firestore.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (!isAdded) return@addOnSuccessListener
                val fullName = document.getString("fullName")
                if (!fullName.isNullOrBlank()) {
                    binding.txtUserName.text = getString(R.string.home_user_name_format, fullName)
                }
            }
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
                Log.e("Home", getString(R.string.weather_error))
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
                updateNearbySlopes(location.latitude, location.longitude)
            } else {
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                    lastLoc?.let {
                        fetchWeatherData(it.latitude, it.longitude)
                        updateNearbySlopes(it.latitude, it.longitude)
                    }
                }
            }
        }
    }

    private fun updateNearbySlopes(lat: Double, lon: Double) {
        if (!isAdded) return

        val nearest = NearbySlopeData.all
            .map { it to distanceKm(lat, lon, it.latitude, it.longitude) }
            .sortedBy { it.second }
            .take(3)

        binding.apply {
            bindNearbySlopeCard(
                nearest[0].first, nearest[0].second,
                imgSlope1, txtSlopeTitle1, txtSlopeSubtitle1, txtSlopeDistance1, txtSlopeDifficulty1, txtSlopeRating1
            )
            bindNearbySlopeCard(
                nearest[1].first, nearest[1].second,
                imgSlope2, txtSlopeTitle2, txtSlopeSubtitle2, txtSlopeDistance2, txtSlopeDifficulty2, txtSlopeRating2
            )
            bindNearbySlopeCard(
                nearest[2].first, nearest[2].second,
                imgSlope3, txtSlopeTitle3, txtSlopeSubtitle3, txtSlopeDistance3, txtSlopeDifficulty3, txtSlopeRating3
            )
        }
    }

    private fun bindNearbySlopeCard(
        slope: NearbySlopeList,
        distance: Double,
        image: ImageView,
        title: TextView,
        subtitle: TextView,
        distanceText: TextView,
        difficultyText: TextView,
        ratingText: TextView
    ) {
        image.setImageResource(slope.imgResort)
        title.text = getString(slope.nameRes)
        subtitle.text = getString(slope.locationRes)
        distanceText.text = getString(R.string.distance_km_format, distance)
        difficultyText.text = getString(slope.difficultyRes)
        difficultyText.backgroundTintList = ContextCompat.getColorStateList(
            requireContext(), colorForDifficulty(slope.difficultyRes)
        )
        ratingText.text = String.format(Locale.getDefault(), "%.1f", slope.rating)
        fetchLiveRating(slope, ratingText)
    }

    private fun fetchLiveRating(slope: NearbySlopeList, ratingText: TextView) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val rating = withContext(Dispatchers.IO) {
                    val doc = Jsoup
                        .connect("https://www.skiresort.com/en/ski-resort/${slope.skiresortSlug}/")
                        .userAgent("Mozilla/5.0")
                        .timeout(10_000)
                        .get()
                    doc.selectFirst("div.js-star-ranking.stars-big")
                        ?.attr("data-rank")
                        ?.toDoubleOrNull()
                }
                if (isAdded && rating != null) {
                    ratingText.text = String.format(Locale.getDefault(), "%.1f", rating)
                }
            } catch (e: Exception) {
                // Falls back to the static rating already shown - scraping is best-effort.
                Log.e("Home", "Error fetching live rating: ${e.message}")
            }
        }
    }

    private fun colorForDifficulty(difficultyRes: Int): Int = when (difficultyRes) {
        R.string.difficulty_advanced -> R.color.shadow_black
        R.string.difficulty_intermediate -> R.color.crimson_red
        else -> R.color.bordeaux_velvet
    }

    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
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
                if (isAdded) updateWeatherUI(response, useMetric)
            } catch (e: Exception) {
                Log.e("Home", "Error: ${e.message}")
            }
        }
    }

    private fun updateWeatherUI(data: WeatherResponse, useMetric: Boolean) {
        val tempUnit = if (useMetric) "C" else "F"
        val condition = data.weather[0].main
        binding.apply {
            txtTemp.text = "${data.main.temp.toInt()}°$tempUnit"
            txtCondition.text = data.weather[0].description.replaceFirstChar { it.uppercase() }
            txtLocationHome.text = data.name
            imgWeatherIcon.setImageResource(iconForCondition(condition))
        }
    }

    private fun iconForCondition(condition: String): Int {
        return when (condition) {
            "Clear", "Sunny" -> R.drawable.ic_weather_sun
            "Rain", "Drizzle" -> R.drawable.ic_weather_rain
            "Snow", "Squall" -> R.drawable.ic_weather_snow
            "Thunderstorm" -> R.drawable.ic_weather_storm
            else -> R.drawable.ic_cloud
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    override fun onStop() {
        super.onStop()
        (activity as? AppCompatActivity)?.supportActionBar?.show()
    }
}
