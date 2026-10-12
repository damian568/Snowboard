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
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.snowboard.Adapters.TipsAdapter
import com.example.snowboard.Lists.TipsList
import com.example.snowboard.R
import com.example.snowboard.databinding.FragmentTipsScreenBinding

class TipsScreenFragment : Fragment() {
    private lateinit var binding: FragmentTipsScreenBinding
    private lateinit var tipsArrayList: ArrayList<TipsList>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTipsScreenBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnMenu.setOnClickListener { openDrawer() }
        cropHeaderArtToTop()
        dataInitialize()

        // 1. Setup the RecyclerView
        val layoutManager = LinearLayoutManager(context)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.setHasFixedSize(true)

        // 2. Attach the adapter
        val tipsAdapter = TipsAdapter(tipsArrayList) { selectedTip, clickedCard ->

            val transitionName = clickedCard.transitionName

            val bundle = Bundle().apply {
                putString("TIP_TITLE", selectedTip.tipTitle)
                putString("TIP_DESC", selectedTip.tipDescription)
                putInt("TIP_IMAGE", selectedTip.tipImage)
                putString("TRANSITION_NAME", transitionName)
            }

            // Tell Navigation Component to link this specific card to the next screen
            val extras = FragmentNavigatorExtras(
                clickedCard to transitionName
            )

            findNavController().navigate(R.id.tipDetailFragment, bundle, null, extras)
        }

        binding.recyclerView.adapter = tipsAdapter
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
        tipsArrayList = arrayListOf()

        tipsArrayList.add(
            TipsList(
                tipTitle = getString(R.string.title_detail_tip_1),
                tipDescription = getString(R.string.description_detail_tip_1),
                tipImage = R.drawable.ic_snowboard_logo,
                tipIcon = R.drawable.ic_snowboarding
            )
        )

        tipsArrayList.add(
            TipsList(
                tipTitle = getString(R.string.title_detail_tip_2),
                tipDescription = getString(R.string.description_detail_tip_2),
                tipImage = R.drawable.ic_snowboard_logo,
                tipIcon = R.drawable.ic_equipment
            )
        )

        tipsArrayList.add(
            TipsList(
                tipTitle = getString(R.string.title_detail_tip_3),
                tipDescription = getString(R.string.description_detail_tip_3),
                tipImage = R.drawable.ic_snowboard_logo,
                tipIcon = R.drawable.ic_school
            )
        )

        tipsArrayList.add(
            TipsList(
                tipTitle = getString(R.string.title_detail_tip_4),
                tipDescription = getString(R.string.description_detail_tip_4),
                tipImage = R.drawable.ic_snowboard_logo,
                tipIcon = R.drawable.ic_eye
            )
        )

        tipsArrayList.add(
            TipsList(
                tipTitle = getString(R.string.title_detail_tip_5),
                tipDescription = getString(R.string.description_detail_tip_5),
                tipImage = R.drawable.ic_snowboard_logo,
                tipIcon = R.drawable.ic_stance
            )
        )

        tipsArrayList.add(
            TipsList(
                tipTitle = getString(R.string.title_detail_tip_6),
                tipDescription = getString(R.string.description_detail_tip_6),
                tipImage = R.drawable.ic_snowboard_logo,
                tipIcon = R.drawable.ic_mountain
            )
        )

        tipsArrayList.add(
            TipsList(
                tipTitle = getString(R.string.title_detail_tip_7),
                tipDescription = getString(R.string.description_detail_tip_7),
                tipImage = R.drawable.ic_snowboard_logo,
                tipIcon = R.drawable.ic_flag_hill
            )
        )

        tipsArrayList.add(
            TipsList(
                tipTitle = getString(R.string.title_detail_tip_8),
                tipDescription = getString(R.string.description_detail_tip_8),
                tipImage = R.drawable.ic_snowboard_logo,
                tipIcon = R.drawable.ic_shield
            )
        )

        tipsArrayList.add(
            TipsList(
                tipTitle = getString(R.string.title_detail_tip_9),
                tipDescription = getString(R.string.description_detail_tip_9),
                tipImage = R.drawable.ic_snowboard_logo,
                tipIcon = R.drawable.ic_hourglass
            )
        )

        tipsArrayList.add(
            TipsList(
                tipTitle = getString(R.string.title_detail_tip_10),
                tipDescription = getString(R.string.description_detail_tip_10),
                tipImage = R.drawable.ic_snowboard_logo,
                tipIcon = R.drawable.ic_heart_outline
            )
        )
    }
}