package com.example.snowboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.ContextThemeWrapper
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.FutureTarget
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic

/** The app context wearing the real app theme, so Material widgets inflate. */
fun themedContext(): Context =
    ContextThemeWrapper(ApplicationProvider.getApplicationContext<Context>(), R.style.Theme_SnowBoard)

/**
 * Replaces Glide's bitmap loading with a fake that "decodes" every Uri to [bitmap], so the
 * photo compression code runs without a real image. Call `unmockkStatic(Glide::class)` afterwards.
 */
fun fakeGlideBitmapLoading(bitmap: Bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)) {
    mockkStatic(Glide::class)
    val manager = mockk<RequestManager>()
    val builder = mockk<RequestBuilder<Bitmap>>()
    val target = mockk<FutureTarget<Bitmap>>()
    every { Glide.with(any<Context>()) } returns manager
    every { manager.asBitmap() } returns builder
    every { builder.load(any<Uri>()) } returns builder
    every { builder.centerCrop() } returns builder
    every { builder.fitCenter() } returns builder
    every { builder.submit(any(), any()) } returns target
    every { target.get() } returns bitmap
}

class GlideViewFake(val manager: RequestManager, val builder: RequestBuilder<Drawable>)

/**
 * Replaces Glide's view loading with a fake whose every `load(...).placeholder().error().into()`
 * chain ends at relaxed mocks, so tests can verify what was loaded without decoding anything.
 */
fun fakeGlideViewLoading(): GlideViewFake {
    mockkStatic(Glide::class)
    val manager = mockk<RequestManager>(relaxed = true)
    val builder = mockk<RequestBuilder<Drawable>>(relaxed = true)
    every { builder.placeholder(any<Int>()) } returns builder
    every { builder.error(any<Int>()) } returns builder
    every { builder.centerCrop() } returns builder
    every { builder.fitCenter() } returns builder
    every { manager.load(any<ByteArray>()) } returns builder
    every { manager.load(any<Uri>()) } returns builder
    every { manager.load(any<Int>()) } returns builder
    every { manager.load(any<Any>()) } returns builder
    every { Glide.with(any<View>()) } returns manager
    every { Glide.with(any<androidx.fragment.app.Fragment>()) } returns manager
    every { Glide.with(any<androidx.fragment.app.FragmentActivity>()) } returns manager
    every { Glide.with(any<Context>()) } returns manager
    return GlideViewFake(manager, builder)
}

/**
 * Launches [F] in a container with the app theme, and hands it [nav] as its NavController before
 * its first onViewCreated, so `findNavController()` works and navigation can be verified.
 */
inline fun <reified F : androidx.fragment.app.Fragment> launchWithNav(
    nav: androidx.navigation.NavController,
    args: android.os.Bundle? = null
): androidx.fragment.app.testing.FragmentScenario<F> {
    val factory = object : androidx.fragment.app.FragmentFactory() {
        override fun instantiate(classLoader: ClassLoader, className: String): androidx.fragment.app.Fragment {
            val fragment = super.instantiate(classLoader, className)
            fragment.viewLifecycleOwnerLiveData.observeForever { owner ->
                if (owner != null) androidx.navigation.Navigation.setViewNavController(fragment.requireView(), nav)
            }
            return fragment
        }
    }
    return androidx.fragment.app.testing.FragmentScenario.launchInContainer(F::class.java, args, R.style.Theme_SnowBoard, factory)
}

/** A fragment hosted in a real AppCompatActivity that also has a drawer, like MainActivity does. */
class HostedFragment<F : androidx.fragment.app.Fragment>(
    val activity: androidx.appcompat.app.AppCompatActivity,
    val fragment: F,
    val drawer: androidx.drawerlayout.widget.DrawerLayout
)

/**
 * For fragments that cast their activity to AppCompatActivity or look for the `drawerLayout`
 * (FragmentScenario's own host is a plain FragmentActivity without either).
 */
fun <F : androidx.fragment.app.Fragment> hostInAppCompat(fragment: F, nav: androidx.navigation.NavController): HostedFragment<F> {
    val activity = org.robolectric.Robolectric.buildActivity(androidx.appcompat.app.AppCompatActivity::class.java).setup().get()
    val drawer = androidx.drawerlayout.widget.DrawerLayout(activity).apply { id = R.id.drawerLayout }
    val content = android.widget.FrameLayout(activity).apply { id = View.generateViewId() }
    drawer.addView(content, androidx.drawerlayout.widget.DrawerLayout.LayoutParams(-1, -1))
    drawer.addView(
        android.widget.FrameLayout(activity),
        androidx.drawerlayout.widget.DrawerLayout.LayoutParams(300, -1, android.view.Gravity.START)
    )
    activity.setContentView(drawer)
    fragment.viewLifecycleOwnerLiveData.observeForever { owner ->
        if (owner != null) androidx.navigation.Navigation.setViewNavController(fragment.requireView(), nav)
    }
    activity.supportFragmentManager.beginTransaction().add(content.id, fragment).commitNow()
    return HostedFragment(activity, fragment, drawer)
}

/** Gives the fragment's lazily built Retrofit weather service a fake, so no request leaves the test. */
fun replaceWeatherService(fragment: androidx.fragment.app.Fragment, api: com.example.snowboard.Interface.WeatherApiService) {
    val field = fragment.javaClass.getDeclaredField("weatherService\$delegate")
    field.isAccessible = true
    field.set(fragment, lazyOf(api))
}

/** A current-location result for the fused location client; null means "no fix", so the last location is used. */
class FakeLocation(val client: com.google.android.gms.location.FusedLocationProviderClient)

/**
 * Makes `LocationServices.getFusedLocationProviderClient(...)` return a fake client that reports
 * [current] (and [last] when there is no current fix).
 */
fun fakeFusedLocation(current: android.location.Location?, last: android.location.Location? = null): FakeLocation {
    mockkStatic(com.google.android.gms.location.LocationServices::class)
    val client = mockk<com.google.android.gms.location.FusedLocationProviderClient>()
    every { com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(any<android.app.Activity>()) } returns client
    every {
        client.getCurrentLocation(
            eq(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY),
            any<com.google.android.gms.tasks.CancellationToken>()
        )
    } returns com.google.android.gms.tasks.Tasks.forResult(current)
    every { client.getCurrentLocation(any<Int>(), any()) } returns com.google.android.gms.tasks.Tasks.forResult(current)
    every { client.lastLocation } returns com.google.android.gms.tasks.Tasks.forResult(last)
    return FakeLocation(client)
}

fun location(lat: Double, lon: Double) = android.location.Location("test").apply {
    latitude = lat
    longitude = lon
}

fun grantLocationPermission() {
    org.robolectric.Shadows.shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>())
        .grantPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION)
}

fun weatherResponse(
    condition: String = "Snow",
    description: String = "light snow",
    temp: Double = -3.5,
    name: String = "Bansko"
) = com.example.snowboard.Response.WeatherResponse(
    main = com.example.snowboard.Response.Main(temp, 80, 1012, -5.0, -1.0, 1012),
    weather = listOf(com.example.snowboard.Response.Weather(condition, description)),
    wind = com.example.snowboard.Response.Wind(4.2),
    sys = com.example.snowboard.Response.Sys(1_700_000_000L, 1_700_030_000L, "BG"),
    name = name
)
