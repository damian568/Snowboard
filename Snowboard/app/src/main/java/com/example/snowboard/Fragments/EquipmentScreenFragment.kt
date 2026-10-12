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
import androidx.recyclerview.widget.GridLayoutManager
import com.example.snowboard.Adapters.EquipmentAdapter
import com.example.snowboard.Lists.EquipmentList
import com.example.snowboard.R
import com.example.snowboard.databinding.FragmentEquipmentScreenBinding

class EquipmentScreenFragment : Fragment() {
    private lateinit var binding: FragmentEquipmentScreenBinding
    private lateinit var equipmentArrayList: ArrayList<EquipmentList>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEquipmentScreenBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnMenu.setOnClickListener { openDrawer() }
        cropHeaderArtToTop()
        dataInitialize()

        // 1. Setup Grid Layout
        val layoutManager = GridLayoutManager(context, 2)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.setHasFixedSize(true)

        // 2. Initialize Adapter with the click listener logic
        val adapter = EquipmentAdapter(equipmentArrayList) { selectedItem, clickedView ->

            // This matches the logic we used for the Tips screen
            val transitionName = clickedView.transitionName

            val bundle = Bundle().apply {
                putInt("EQUIPMENT_IMAGE", selectedItem.equipmentImage)
                putString("EQUIPMENT_TITLE", selectedItem.equipmentTitle)
                putString("TRANSITION_NAME", transitionName)
            }

            val extras = FragmentNavigatorExtras(
                clickedView to transitionName
            )
            findNavController().navigate(R.id.equipmentDetailFragment, bundle, null, extras)
        }

        binding.recyclerView.adapter = adapter
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
        equipmentArrayList = arrayListOf()

        equipmentArrayList.add(
            EquipmentList(
                R.drawable.snowboard_pic_equipment,
                getString(R.string.equipment_title_1),
                getString(R.string.equipment_instruction),
                R.drawable.ic_gear_snowboard
            )
        )
        equipmentArrayList.add(
            EquipmentList(
                R.drawable.bindings_pic_equipment,
                getString(R.string.equipment_title_2),
                getString(R.string.equipment_instruction),
                R.drawable.ic_gear_bindings
            )
        )
        equipmentArrayList.add(
            EquipmentList(
                R.drawable.boots_pic_equipment,
                getString(R.string.equipment_title_3),
                getString(R.string.equipment_instruction),
                R.drawable.ic_gear_boots
            )
        )
        equipmentArrayList.add(
            EquipmentList(
                R.drawable.helmet_pic_equipment,
                getString(R.string.equipment_title_4),
                getString(R.string.equipment_instruction),
                R.drawable.ic_gear_helmet
            )
        )
        equipmentArrayList.add(
            EquipmentList(
                R.drawable.goggles_pic_equipment,
                getString(R.string.equipment_title_5),
                getString(R.string.equipment_instruction),
                R.drawable.ic_gear_goggles
            )
        )
        equipmentArrayList.add(
            EquipmentList(
                R.drawable.jacket_pic_equipment,
                getString(R.string.equipment_title_6),
                getString(R.string.equipment_instruction),
                R.drawable.ic_gear_jacket
            )
        )
        equipmentArrayList.add(
            EquipmentList(
                R.drawable.pants_pic_equipment,
                getString(R.string.equipment_title_7),
                getString(R.string.equipment_instruction),
                R.drawable.ic_gear_pants
            )
        )
        equipmentArrayList.add(
            EquipmentList(
                R.drawable.gloves_pic_equipment,
                getString(R.string.equipment_title_8),
                getString(R.string.equipment_instruction),
                R.drawable.ic_gear_gloves
            )
        )
        equipmentArrayList.add(
            EquipmentList(
                R.drawable.base_layers_pic_equipment,
                getString(R.string.equipment_title_9),
                getString(R.string.equipment_instruction),
                R.drawable.ic_gear_base_layers
            )
        )
        equipmentArrayList.add(
            EquipmentList(
                R.drawable.socks_pic_equipment,
                getString(R.string.equipment_title_10),
                getString(R.string.equipment_instruction),
                R.drawable.ic_gear_socks
            )
        )
    }
}