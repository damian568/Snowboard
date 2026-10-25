package com.example.snowboard

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import com.bumptech.glide.Glide
import com.example.snowboard.User.AvatarRepository
import com.example.snowboard.User.Settings.AppPreferences
import com.example.snowboard.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private data class DrawerItem(
        val rowId: Int,
        val accentId: Int,
        val iconId: Int,
        val textId: Int,
        val chevronId: Int,
        val destinationId: Int
    )

    private val drawerItems by lazy {
        listOf(
            DrawerItem(R.id.home, R.id.home_accent, R.id.home_icon, R.id.home_text, R.id.home_chevron, R.id.mainScreenFragment),
            DrawerItem(R.id.history, R.id.history_accent, R.id.history_icon, R.id.history_text, R.id.history_chevron, R.id.historyScreenFragment),
            DrawerItem(R.id.tips, R.id.tips_accent, R.id.tips_icon, R.id.tips_text, R.id.tips_chevron, R.id.tipsScreenFragment),
            DrawerItem(R.id.equipment, R.id.equipment_accent, R.id.equipment_icon, R.id.equipment_text, R.id.equipment_chevron, R.id.equipmentScreenFragment),
            DrawerItem(R.id.skiSlopes, R.id.skiSlopes_accent, R.id.skiSlopes_icon, R.id.skiSlopes_text, R.id.skiSlopes_chevron, R.id.skiSlopesScreenFragment),
            DrawerItem(R.id.videos, R.id.videos_accent, R.id.videos_icon, R.id.videos_text, R.id.videos_chevron, R.id.videosScreenFragment),
            DrawerItem(R.id.weather, R.id.weather_accent, R.id.weather_icon, R.id.weather_text, R.id.weather_chevron, R.id.weatherScreenFragment)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(
            if (AppPreferences.isDarkMode(this)) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        // Setup NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // Set Toolbar
        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController, binding.drawerLayout)

        drawerMenu()
        handleDrawerMenuClicks()
        navControllerDestinationChanged()
        loadNavHeaderProfile()
    }

    private fun drawerMenu() {
        // 1. Open Drawer when BottomAppBar menu icon is clicked
        binding.bottomAppBar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // 1b. Keep the drawer header in sync with the signed-in profile
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                loadNavHeaderProfile()
            }
        })
    }

    private fun handleDrawerMenuClicks() {
        // 2. Handle Drawer Item Clicks
        val destinationByRowId = mapOf(
            R.id.home to R.id.mainScreenFragment,
            R.id.history to R.id.historyScreenFragment,
            R.id.tips to R.id.tipsScreenFragment,
            R.id.equipment to R.id.equipmentScreenFragment,
            R.id.skiSlopes to R.id.skiSlopesScreenFragment,
            R.id.videos to R.id.videosScreenFragment,
            R.id.weather to R.id.weatherScreenFragment,
            R.id.btn_nav_header_profile to R.id.profileScreenFragment
        )

        destinationByRowId.forEach { (rowId, destinationId) ->
            binding.navDrawerContent.findViewById<View>(rowId).setOnClickListener {
                navController.navigate(destinationId)
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }
        }

        binding.navDrawerContent.findViewById<View>(R.id.logout).setOnClickListener {
            logOut()
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun loadNavHeaderProfile() {
        val nameView = binding.navDrawerContent.findViewById<TextView>(R.id.txt_nav_header_name)
        val emailView = binding.navDrawerContent.findViewById<TextView>(R.id.txt_nav_header_email)
        val avatarView = binding.navDrawerContent.findViewById<ImageView>(R.id.img_profile)

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            nameView.text = ""
            emailView.text = ""
            avatarView.setImageResource(R.mipmap.ic_icon)
            return
        }

        emailView.text = user.email.orEmpty()
        nameView.text = user.displayName.orEmpty()

        FirebaseFirestore.getInstance().collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                val fullName = document.getString("fullName")
                if (!fullName.isNullOrBlank()) {
                    nameView.text = fullName
                }
                if (isDestroyed) return@addOnSuccessListener
                val avatarBytes = AvatarRepository.avatarBytes(document)
                if (avatarBytes == null) {
                    avatarView.setImageResource(R.mipmap.ic_icon)
                } else {
                    Glide.with(this)
                        .load(avatarBytes)
                        .centerCrop()
                        .placeholder(R.mipmap.ic_icon)
                        .error(R.mipmap.ic_icon)
                        .into(avatarView)
                }
            }
    }

    private fun logOut() {
        FirebaseAuth.getInstance().signOut()
        loadNavHeaderProfile()
        navController.navigate(
            R.id.loginScreenFragment,
            null,
            androidx.navigation.NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build()
        )
    }

    private fun navControllerDestinationChanged() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            behaviorBottomAppBar(destination)
            updateSelectedDrawerItem(destination.id)
        }
    }

    private fun updateSelectedDrawerItem(destinationId: Int) {
        val selectedColor = ContextCompat.getColor(this, R.color.crimson_red)
        val defaultTextColor = ContextCompat.getColor(this, R.color.black)
        val defaultChevronColor = ContextCompat.getColor(this, R.color.grey)

        drawerItems.forEach { item ->
            val isSelected = item.destinationId == destinationId

            binding.navDrawerContent.findViewById<View>(item.rowId).background =
                if (isSelected) ContextCompat.getDrawable(this, R.drawable.bg_nav_row_selected) else null

            binding.navDrawerContent.findViewById<View>(item.accentId).setBackgroundColor(
                if (isSelected) selectedColor else Color.TRANSPARENT
            )

            binding.navDrawerContent.findViewById<ImageView>(item.iconId).imageTintList =
                ColorStateList.valueOf(if (isSelected) selectedColor else defaultTextColor)

            binding.navDrawerContent.findViewById<TextView>(item.textId)
                .setTextColor(if (isSelected) selectedColor else defaultTextColor)

            binding.navDrawerContent.findViewById<ImageView>(item.chevronId).imageTintList =
                ColorStateList.valueOf(if (isSelected) selectedColor else defaultChevronColor)
        }
    }

    private fun behaviorBottomAppBar(destination: NavDestination) {
        when (destination.id) {
            R.id.splashScreenFragment, R.id.mainScreenFragment, R.id.historyScreenFragment, R.id.tipsScreenFragment, R.id.equipmentScreenFragment, R.id.skiSlopesScreenFragment, R.id.videosScreenFragment, R.id.tipDetailFragment, R.id.equipmentDetailFragment, R.id.loginScreenFragment, R.id.registerScreenFragment, R.id.profileScreenFragment, R.id.personalInformationFragment, R.id.settingsFragment, R.id.helpSupportFragment, R.id.changePasswordScreenFragment, R.id.changeEmailScreenFragment -> {
                binding.bottomAppBar.visibility = View.GONE
                binding.fab.hide()
            }


            else -> {
                binding.bottomAppBar.visibility = View.VISIBLE
                binding.fab.hide() //Need to change it to show
                /**
                 *  When I make the user Interface need this fab to make it show
                 *  and to add it only to some of the screens
                 */
                binding.bottomAppBar.performShow()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        // This allows the drawer hamburger icon to work with the NavController
        return NavigationUI.navigateUp(
            navController,
            binding.drawerLayout
        ) || super.onSupportNavigateUp()
    }
}