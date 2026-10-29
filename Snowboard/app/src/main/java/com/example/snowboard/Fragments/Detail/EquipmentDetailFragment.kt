package com.example.snowboard.Fragments.Detail

import android.content.Intent
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.snowboard.Constants.Constants
import com.example.snowboard.Content.observeUserContent
import com.example.snowboard.Content.showContentImage
import com.example.snowboard.Content.bindOwnerActions
import com.example.snowboard.Lists.ContentType
import com.example.snowboard.Lists.UserContent
import com.example.snowboard.R
import com.example.snowboard.databinding.FragmentEquipmentDetailBinding
import com.google.android.material.transition.MaterialContainerTransform

class EquipmentDetailFragment : Fragment() {
    private lateinit var binding: FragmentEquipmentDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup the exact animation from the video
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId =
                R.id.navHostFragment // The NavHost container from your activity_main.xml
            duration = Constants.DetailDoration
            scrimColor =
                Color.TRANSPARENT // Keeps the background from turning dark during the animation
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEquipmentDetailBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideTheToolbar()
        cropHeaderArtToTop()
        // 1. Get the transition name passed from the list and apply it to the root view
        val transitionName = arguments?.getString("TRANSITION_NAME")
        view.findViewById<View>(R.id.equipment_detail_root).transitionName = transitionName

        // 2. Set the rest of your data...
        val imageRes = arguments?.getInt("EQUIPMENT_IMAGE") ?: Constants.Detail_IMG
        val title = arguments?.getString("EQUIPMENT_TITLE") ?: ""
        val iconRes = arguments?.getInt("EQUIPMENT_ICON") ?: Constants.Detail_IMG

        binding.equipmentDetailTitle.text = title
        binding.equipmentQuote.text = arguments?.getString("EQUIPMENT_QUOTE")
        if (imageRes != Constants.Detail_IMG) {
            binding.equipmentDetailImg.setImageResource(imageRes)
        }
        if (iconRes != Constants.Detail_IMG) {
            binding.equipmentDetailIcon.setImageResource(iconRes)
        }
        changeDescDetail(title)
        brandsHTML(title)

        // 3. Equipment users added to this category with the FAB, updated live
        arguments?.getString("EQUIPMENT_CATEGORY")?.let { category ->
            observeUserContent(ContentType.EQUIPMENT, category) { items -> showUserEquipment(items) }
        }

        binding.equipmentBtnClose.setOnClickListener {
            // This tells the NavController to go back to the list.
            // It will automatically play the reverse "shrinking" animation!
            findNavController().navigateUp()
        }
    }

    private fun showUserEquipment(items: List<UserContent>) {
        val container = binding.equipmentUserItems
        container.removeAllViews()
        binding.equipmentUserCard.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE

        items.forEach { content ->
            val row = layoutInflater.inflate(R.layout.list_user_equipment, container, false)
            row.findViewById<TextView>(R.id.user_equipment_name).text = content.title
            row.findViewById<TextView>(R.id.user_equipment_description).text = content.description

            val photo = row.findViewById<ImageView>(R.id.user_equipment_photo)
            photo.visibility = if (content.image != null) View.VISIBLE else View.GONE
            content.image?.let { photo.showContentImage(it, R.drawable.ic_equipment) }

            // Only the user who added it gets Edit / Delete
            bindOwnerActions(row.findViewById(R.id.owner_actions), content)

            container.addView(row)
        }
    }

    private fun changeDescDetail(title: String): String {
        val changeDesc = when (title) {
            //ENG
            "Snowboard" -> getString(R.string.equipment_desc_detail_1)
            "Bindings" -> getString(R.string.equipment_desc_detail_2)
            "Boots" -> getString(R.string.equipment_desc_detail_3)
            "Helmet" -> getString(R.string.equipment_desc_detail_4)
            "Goggles" -> getString(R.string.equipment_desc_detail_5)
            "Jacket" -> getString(R.string.equipment_desc_detail_6)
            "Pants" -> getString(R.string.equipment_desc_detail_7)
            "Gloves" -> getString(R.string.equipment_desc_detail_8)
            "Base Layers" -> getString(R.string.equipment_desc_detail_9)
            "Socks" -> getString(R.string.equipment_desc_detail_10)

            //BG
            "Сноуборд" -> getString(R.string.equipment_desc_detail_1)
            "Автомати" -> getString(R.string.equipment_desc_detail_2)
            "Обувки" -> getString(R.string.equipment_desc_detail_3)
            "Каска" -> getString(R.string.equipment_desc_detail_4)
            "Очила" -> getString(R.string.equipment_desc_detail_5)
            "Яке" -> getString(R.string.equipment_desc_detail_6)
            "Панталон" -> getString(R.string.equipment_desc_detail_7)
            "Ръкавици" -> getString(R.string.equipment_desc_detail_8)
            "Термо Бельо" -> getString(R.string.equipment_desc_detail_9)
            "Чорапи" -> getString(R.string.equipment_desc_detail_10)
            else -> getString(R.string.equipment_instruction_google)
        }
        // The title is already shown above, so drop the "Boots – " prefix
        return changeDesc.also {
            binding.equipmentDetailDescription.text = it.substringAfter(" – ")
        }
    }

    // This logic maps the clicked item to the correct list of 10+ brands
    private fun brandsHTML(title: String) {
        val brandsHtml = when (title) {
            //ENG
            "Snowboard" -> getString(R.string.brands_snowboards)
            "Bindings" -> getString(R.string.brands_bindings)
            "Boots" -> getString(R.string.brands_boots)
            "Helmet" -> getString(R.string.brands_helmets)
            "Goggles" -> getString(R.string.brands_goggles)
            "Jacket" -> getString(R.string.brands_jackets)
            "Pants" -> getString(R.string.brands_pants)
            "Gloves" -> getString(R.string.brands_gloves)
            "Base Layers" -> getString(R.string.brands_base_layers)
            "Socks" -> getString(R.string.brands_socks)

            //BG
            "Сноуборд" -> getString(R.string.brands_snowboards)
            "Автомати" -> getString(R.string.brands_bindings)
            "Обувки" -> getString(R.string.brands_boots)
            "Каска" -> getString(R.string.brands_helmets)
            "Очила" -> getString(R.string.brands_goggles)
            "Яке" -> getString(R.string.brands_jackets)
            "Панталон" -> getString(R.string.brands_pants)
            "Ръкавици" -> getString(R.string.brands_gloves)
            "Термо Бельо" -> getString(R.string.brands_base_layers)
            "Чорапи" -> getString(R.string.brands_socks)
            else -> getString(R.string.equipment_instruction_google)
        }

        showBrands(brandsHtml)
    }

    // Each "Brand: <a href="...">Sourcing Link</a> <br>" line becomes a row that opens the shop.
    // Brands without a link don't sell this item, so they're left out.
    private fun showBrands(brandsHtml: String) {
        val container = binding.equipmentDetailBrands
        val brands = brandsHtml.split("<br>").mapNotNull { line ->
            val name = line.substringBefore(":", "").trim()
            // aapt strips the unescaped quotes, so the href usually arrives bare
            val url = Regex("href=[\"']?([^\"'\\s>]+)").find(line)?.groupValues?.get(1)
            if (name.isEmpty() || '<' in name || url == null) null else name to url
        }
        binding.equipmentBrandsCard.visibility = if (brands.isEmpty()) View.GONE else View.VISIBLE

        brands.forEach { (name, url) ->
            val row = layoutInflater.inflate(R.layout.list_equipment_brand, container, false)
            row.findViewById<TextView>(R.id.brand_name).text = name
            row.findViewById<View>(R.id.brand_row).setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
            container.addView(row)
        }
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

    private fun hideTheToolbar() {
        (activity as AppCompatActivity).supportActionBar?.hide()
    }
}