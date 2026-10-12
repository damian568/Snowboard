package com.example.snowboard.Fragments

import android.graphics.Matrix
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.doOnLayout
import androidx.drawerlayout.widget.DrawerLayout
import com.example.snowboard.R
import com.example.snowboard.databinding.FragmentHistoryScreenBinding

class HistoryScreenFragment : Fragment() {
    private lateinit var binding: FragmentHistoryScreenBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryScreenBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnMenu.setOnClickListener { openDrawer() }
        cropHeaderArtToTop()
    }

    private fun openDrawer() {
        requireActivity().findViewById<DrawerLayout>(R.id.drawerLayout)
            ?.openDrawer(GravityCompat.START)
    }

    // Scale the art to fill the header and keep its dark sky behind the title
    private fun cropHeaderArtToTop() {
        binding.headerArt.doOnLayout { view ->
            val art = binding.headerArt.drawable ?: return@doOnLayout
            val scale = maxOf(
                view.width.toFloat() / art.intrinsicWidth,
                view.height.toFloat() / art.intrinsicHeight
            )
            binding.headerArt.imageMatrix = Matrix().apply {
                setScale(scale, scale)
                postTranslate((view.width - art.intrinsicWidth * scale) / 2f, 0f)
            }
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
