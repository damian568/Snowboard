package com.example.snowboard

import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.test.core.app.ApplicationProvider
import com.example.snowboard.Content.UserContentRepository
import com.example.snowboard.Fragments.AddContentDialog
import com.example.snowboard.Lists.ContentType
import com.example.snowboard.User.Settings.AppPreferences
import com.google.android.gms.tasks.Tasks
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowToast
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    private val auth = mockk<FirebaseAuth>(relaxed = true)
    private val userDoc = mockk<DocumentReference>()
    private var signedIn: FirebaseUser? = null

    @Before
    fun setUp() {
        mockkStatic(FirebaseAuth::class, FirebaseFirestore::class)
        every { FirebaseAuth.getInstance() } returns auth
        every { auth.currentUser } answers { signedIn }

        val users = mockk<CollectionReference>()
        val firestore = mockk<FirebaseFirestore>()
        every { FirebaseFirestore.getInstance() } returns firestore
        every { firestore.collection("users") } returns users
        every { users.document(any()) } returns userDoc
        profile(name = null, avatar = null)

        mockkObject(UserContentRepository)
        every { UserContentRepository.listen(any(), any(), any(), any()) } returns mockk<ListenerRegistration>(relaxed = true)
        fakeGlideViewLoading()
    }

    @After
    fun tearDown() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        unmockkAll()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)

    private fun signIn(email: String? = "rider@example.com", displayName: String? = "Rider") {
        signedIn = mockk {
            every { uid } returns "uid-1"
            every { this@mockk.email } returns email
            every { this@mockk.displayName } returns displayName
        }
    }

    private fun profile(name: String?, avatar: Blob?) {
        val doc = mockk<DocumentSnapshot> {
            every { getString("fullName") } returns name
            every { getBlob("avatar") } returns avatar
        }
        every { userDoc.get() } returns Tasks.forResult(doc)
    }

    private fun start(): MainActivity {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        idle()
        return activity
    }

    private fun MainActivity.nav(): NavController = findNavController(R.id.navHostFragment)
    private fun MainActivity.drawer() = findViewById<DrawerLayout>(R.id.drawerLayout)

    /** True when [action] makes the drawer open, or start sliding open (it animates once it has been laid out). */
    private fun DrawerLayout.opensWhen(action: () -> Unit): Boolean {
        var started = false
        addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerStateChanged(newState: Int) {
                if (newState == DrawerLayout.STATE_SETTLING) started = true
            }

            override fun onDrawerOpened(drawerView: View) {
                started = true
            }
        })
        action()
        idle()
        return started || isDrawerOpen(GravityCompat.START) || isDrawerVisible(GravityCompat.START)
    }
    private fun MainActivity.text(id: Int) = findViewById<TextView>(id).text.toString()

    private fun MainActivity.go(destination: Int, args: Bundle? = null) {
        nav().navigate(destination, args)
        idle()
    }

    // ---- start-up ----

    @Test
    fun startsOnTheSplashScreen() {
        val activity = start()

        assertEquals(R.id.splashScreenFragment, activity.nav().currentDestination?.id)
    }

    @Test
    fun savedDarkMode_isAppliedAtStartUp() {
        AppPreferences.setDarkMode(ApplicationProvider.getApplicationContext(), true)

        start()

        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())
    }

    @Test
    fun lightMode_isTheDefault() {
        start()

        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.getDefaultNightMode())
    }

    // ---- the drawer header ----

    @Test
    fun drawerHeader_isEmptyWhenSignedOut() {
        val activity = start()

        assertEquals("", activity.text(R.id.txt_nav_header_name))
        assertEquals("", activity.text(R.id.txt_nav_header_email))
    }

    @Test
    fun drawerHeader_showsTheSignedInUser() {
        signIn(email = "rider@example.com", displayName = "Google Rider")
        val activity = start()

        assertEquals("rider@example.com", activity.text(R.id.txt_nav_header_email))
        assertEquals("Google Rider", activity.text(R.id.txt_nav_header_name))
    }

    @Test
    fun drawerHeader_prefersTheProfileName() {
        signIn(displayName = "Google Rider")
        profile(name = "Profile Rider", avatar = null)
        val activity = start()

        assertEquals("Profile Rider", activity.text(R.id.txt_nav_header_name))
    }

    @Test
    fun drawerHeader_keepsTheDisplayNameWhenTheProfileNameIsBlank() {
        signIn(displayName = "Google Rider")
        profile(name = "  ", avatar = null)
        val activity = start()

        assertEquals("Google Rider", activity.text(R.id.txt_nav_header_name))
    }

    @Test
    fun drawerHeader_withoutAnAvatar_showsTheAppIcon() {
        signIn()
        val activity = start()

        val avatar = activity.findViewById<ImageView>(R.id.img_profile)
        assertEquals(R.mipmap.ic_icon, shadowOf(avatar.drawable).createdFromResId)
    }

    @Test
    fun drawerHeader_withAnAvatar_loadsItThroughGlide() {
        val glide = fakeGlideViewLoading()
        signIn()
        profile(name = "Rider", avatar = Blob.fromBytes(byteArrayOf(4, 5, 6)))
        start()

        verify { glide.manager.load(byteArrayOf(4, 5, 6)) }
    }

    @Test
    fun openingTheDrawer_refreshesTheHeader() {
        val activity = start()
        assertEquals("", activity.text(R.id.txt_nav_header_email))

        signIn(email = "new@example.com")
        activity.drawer().openDrawer(GravityCompat.START, false)
        idle()

        assertEquals("new@example.com", activity.text(R.id.txt_nav_header_email))
    }

    // ---- drawer menu ----

    @Test
    fun menuIconInTheBottomBar_opensTheDrawer() {
        val activity = start()
        val bar = activity.findViewById<BottomAppBar>(R.id.bottomAppBar)
        val navIcon = (0 until bar.childCount).map { bar.getChildAt(it) }.filterIsInstance<ImageButton>().first()

        assertTrue(activity.drawer().opensWhen { navIcon.performClick() })
    }

    @Test
    fun drawerRows_navigateToTheirScreens_andCloseTheDrawer() {
        val activity = start()
        val rows = mapOf(
            R.id.history to R.id.historyScreenFragment,
            R.id.tips to R.id.tipsScreenFragment,
            R.id.equipment to R.id.equipmentScreenFragment,
            R.id.skiSlopes to R.id.skiSlopesScreenFragment,
            R.id.videos to R.id.videosScreenFragment
        )

        rows.forEach { (row, destination) ->
            activity.drawer().openDrawer(GravityCompat.START)
            idle()

            activity.findViewById<View>(row).performClick()
            idle()

            assertEquals(destination, activity.nav().currentDestination?.id)
            assertFalse(activity.drawer().isDrawerOpen(GravityCompat.START))
        }
    }

    @Test
    fun theCurrentScreensRow_isHighlighted() {
        val activity = start()

        activity.go(R.id.historyScreenFragment)

        val selected = ContextCompat.getColor(activity, R.color.crimson_red)
        val normal = ContextCompat.getColor(activity, R.color.black)
        assertEquals(selected, activity.findViewById<TextView>(R.id.history_text).currentTextColor)
        assertEquals(normal, activity.findViewById<TextView>(R.id.tips_text).currentTextColor)
        assertNotNull(activity.findViewById<View>(R.id.history).background)
        assertEquals(null, activity.findViewById<View>(R.id.tips).background)

        activity.go(R.id.tipsScreenFragment)
        assertEquals(selected, activity.findViewById<TextView>(R.id.tips_text).currentTextColor)
        assertEquals(normal, activity.findViewById<TextView>(R.id.history_text).currentTextColor)
    }

    @Test
    fun logOut_signsOut_clearsTheHeader_andReturnsToLogin() {
        signIn()
        val activity = start()
        activity.go(R.id.tipsScreenFragment)
        every { auth.signOut() } answers { signedIn = null }

        activity.findViewById<View>(R.id.logout).performClick()
        idle()

        verify { auth.signOut() }
        assertEquals(R.id.loginScreenFragment, activity.nav().currentDestination?.id)
        assertEquals("", activity.text(R.id.txt_nav_header_email))
        // Nothing to go back to after logging out
        assertFalse(activity.nav().popBackStack())
    }

    // ---- bottom bar ----

    @Test
    fun bottomBar_isHiddenOnTheScreensThatHaveTheirOwnHeader() {
        val activity = start()

        activity.go(R.id.historyScreenFragment)

        assertEquals(View.GONE, activity.findViewById<BottomAppBar>(R.id.bottomAppBar).visibility)
    }

    @Test
    fun bottomBar_isShownOnTheWeatherScreen() {
        fakeFusedLocation(current = null)
        val activity = start()

        activity.go(R.id.weatherScreenFragment)

        assertEquals(View.VISIBLE, activity.findViewById<BottomAppBar>(R.id.bottomAppBar).visibility)
    }

    // ---- the add button ----

    private fun MainActivity.fab() = findViewById<FloatingActionButton>(R.id.fab)

    @Test
    fun fab_isHiddenOnScreensWithNothingToAdd() {
        val activity = start()

        activity.go(R.id.historyScreenFragment)

        assertTrue(activity.fab().isOrWillBeHidden)
    }

    @Test
    fun fab_addsTheContentTypeOfTheCurrentScreen() {
        val activity = start()
        val screens = mapOf(
            R.id.tipsScreenFragment to ContentType.TIP,
            R.id.skiSlopesScreenFragment to ContentType.SKI_SLOPE,
            R.id.videosScreenFragment to ContentType.VIDEO
        )

        screens.forEach { (destination, type) ->
            activity.go(destination)

            assertTrue("fab on $type", activity.fab().isOrWillBeShown)
            assertEquals(activity.getString(type.addTitle), activity.fab().contentDescription)
        }
    }

    @Test
    fun fab_onEquipmentDetails_needsACategory() {
        val activity = start()

        activity.go(R.id.equipmentDetailFragment, bundleOf("EQUIPMENT_TITLE" to "Boots"))
        assertTrue(activity.fab().isOrWillBeHidden)

        activity.go(R.id.equipmentDetailFragment, bundleOf("EQUIPMENT_TITLE" to "Boots", "EQUIPMENT_CATEGORY" to "boots"))
        assertTrue(activity.fab().isOrWillBeShown)
        assertEquals(activity.getString(ContentType.EQUIPMENT.addTitle), activity.fab().contentDescription)
    }

    @Test
    fun fab_whenSignedOut_asksTheUserToLogIn() {
        val activity = start()
        activity.go(R.id.tipsScreenFragment)

        activity.fab().performClick()

        assertEquals(activity.getString(R.string.msg_sign_in_to_add), ShadowToast.getTextOfLatestToast())
        assertEquals(null, activity.supportFragmentManager.findFragmentByTag(AddContentDialog.TAG))
    }

    @Test
    fun fab_whenSignedIn_opensTheAddDialog() {
        signIn()
        val activity = start()
        activity.go(R.id.videosScreenFragment)

        activity.fab().performClick()
        activity.supportFragmentManager.executePendingTransactions()

        assertTrue(activity.supportFragmentManager.findFragmentByTag(AddContentDialog.TAG) is AddContentDialog)
    }

    @Test
    fun fab_onEquipmentDetails_addsUnderTheOpenCategory() {
        signIn()
        val activity = start()
        activity.go(R.id.equipmentDetailFragment, bundleOf("EQUIPMENT_TITLE" to "Boots", "EQUIPMENT_CATEGORY" to "boots"))

        activity.fab().performClick()
        activity.supportFragmentManager.executePendingTransactions()

        val dialog = activity.supportFragmentManager.findFragmentByTag(AddContentDialog.TAG)!!
        assertEquals("boots", dialog.requireArguments().getString("category"))
        assertEquals(ContentType.EQUIPMENT.name, dialog.requireArguments().getString("type"))
    }

    // ---- navigating up ----

    @Test
    fun navigatingUp_fromAStartScreen_opensTheDrawerInsteadOfLeaving() {
        val activity = start()

        var handled = false
        assertTrue(activity.drawer().opensWhen { handled = activity.onSupportNavigateUp() })

        assertTrue(handled)
        assertEquals(R.id.splashScreenFragment, activity.nav().currentDestination?.id)
    }
}
