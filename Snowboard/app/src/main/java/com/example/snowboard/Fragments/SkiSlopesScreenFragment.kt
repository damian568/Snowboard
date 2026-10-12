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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.snowboard.Adapters.SkiSlopesAdapter
import com.example.snowboard.Lists.SkiSlopesList
import com.example.snowboard.R
import com.example.snowboard.databinding.FragmentSkiSlopesScreenBinding

class SkiSlopesScreenFragment : Fragment() {
    private lateinit var binding: FragmentSkiSlopesScreenBinding
    private lateinit var skiSlopesArrayList: ArrayList<SkiSlopesList>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSkiSlopesScreenBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnMenu.setOnClickListener { openDrawer() }
        cropHeaderArtToTop()
        dataInitialize()

        val layoutManager = LinearLayoutManager(context)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.setHasFixedSize(true)

        val skiSlopesAdapter = SkiSlopesAdapter(skiSlopesArrayList)
        binding.recyclerView.adapter = skiSlopesAdapter
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

    private fun dataInitialize() {
        skiSlopesArrayList = arrayListOf()

        skiSlopesArrayList.add(
            SkiSlopesList(
                R.drawable.bansko_resort,
                getString(R.string.title_ski_slopes_1),
                getString(R.string.description_ski_slopes_1),
                R.drawable.bansko,
                getString(R.string.link_ski_slopes_1)
            )
        )
        skiSlopesArrayList.add(
            SkiSlopesList(
                R.drawable.borovets_resort,
                getString(R.string.title_ski_slopes_2),
                getString(R.string.description_ski_slopes_2),
                R.drawable.borovets,
                getString(R.string.link_ski_slopes_2)
            )
        )
        skiSlopesArrayList.add(
            SkiSlopesList(
                R.drawable.pamporovo_resort,
                getString(R.string.title_ski_slopes_3),
                getString(R.string.description_ski_slopes_3),
                R.drawable.pamporovo,
                getString(R.string.link_ski_slopes_3)
            )
        )
        skiSlopesArrayList.add(
            SkiSlopesList(
                R.drawable.vitosha_resort,
                getString(R.string.title_ski_slopes_4),
                getString(R.string.description_ski_slopes_4),
                R.drawable.vitosha,
                getString(R.string.link_ski_slopes_4)
            )
        )
        skiSlopesArrayList.add(
            SkiSlopesList(
                R.drawable.chepelare_resort,
                getString(R.string.title_ski_slopes_5),
                getString(R.string.description_ski_slopes_5),
                R.drawable.chepelare,
                getString(R.string.link_ski_slopes_5)
            )
        )
        skiSlopesArrayList.add(
            SkiSlopesList(
                R.drawable.dobrinishte_resort,
                getString(R.string.title_ski_slopes_6),
                getString(R.string.description_ski_slopes_6),
                R.drawable.dobrinishte,
                getString(R.string.link_ski_slopes_6)
            )
        )
        skiSlopesArrayList.add(
            SkiSlopesList(
                R.drawable.malyovitsa_resort,
                getString(R.string.title_ski_slopes_7),
                getString(R.string.description_ski_slopes_7),
                R.drawable.malyovitsa,
                getString(R.string.link_ski_slopes_7)
            )
        )
        skiSlopesArrayList.add(
            SkiSlopesList(
                R.drawable.uzana_resort,
                getString(R.string.title_ski_slopes_8),
                getString(R.string.description_ski_slopes_8),
                R.drawable.ic_snowboard_logo,
                getString(R.string.link_ski_slopes_8)
            )
        )
        skiSlopesArrayList.add(
            SkiSlopesList(
                R.drawable.semkovo_resort,
                getString(R.string.title_ski_slopes_9),
                getString(R.string.description_ski_slopes_9),
                R.drawable.semkovo,
                getString(R.string.link_ski_slopes_9)
            )
        )
        skiSlopesArrayList.add(
            SkiSlopesList(
                R.drawable.kartala_resort,
                getString(R.string.title_ski_slopes_10),
                getString(R.string.description_ski_slopes_10),
                R.drawable.kartala,
                getString(R.string.link_ski_slopes_10)
            )
        )
    }
}