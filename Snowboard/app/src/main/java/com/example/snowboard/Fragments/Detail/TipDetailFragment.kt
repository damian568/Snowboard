package com.example.snowboard.Fragments.Detail

import android.graphics.Color
import android.graphics.Matrix
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
import com.example.snowboard.R
import com.example.snowboard.databinding.FragmentTipDetailBinding
import com.google.android.material.transition.MaterialContainerTransform

class TipDetailFragment : Fragment() {
    private lateinit var binding: FragmentTipDetailBinding

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
        binding = FragmentTipDetailBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideTheToolbar()
        cropHeaderArtToTop()
        // 1. Get the transition name passed from the list and apply it to the root view
        val transitionName = arguments?.getString("TRANSITION_NAME")
        view.findViewById<View>(R.id.tip_detail_root).transitionName = transitionName

        // 2. Set the rest of your data...
        val title = arguments?.getString("TIP_TITLE")
        val desc = arguments?.getString("TIP_DESC").orEmpty()
        val imageRes = arguments?.getInt("TIP_IMAGE") ?: Constants.Detail_IMG
        val iconRes = arguments?.getInt("TIP_ICON") ?: Constants.Detail_IMG

        val quote = arguments?.getString("TIP_QUOTE")
        binding.tipDetailTitle.text = title
        binding.tipQuote.text = quote
        // The quote is optional on tips added by users
        binding.tipQuoteCard.visibility = if (quote.isNullOrBlank()) View.GONE else View.VISIBLE
        if (imageRes != Constants.Detail_IMG) {
            binding.tipDetailImg.setImageResource(imageRes)
        }

        // First paragraph is the intro in the hero card, the rest become sections
        val paragraphs = desc.split(Regex("\\s*\\n\\s*\\n\\s*")).map { it.trim() }.filter { it.isNotEmpty() }
        binding.tipDetailDescription.text = paragraphs.firstOrNull()
        showSections(paragraphs.drop(1), iconRes)

        binding.tipBtnClose.setOnClickListener {
            // This tells the NavController to go back to the list.
            // It will automatically play the reverse "shrinking" animation!
            findNavController().navigateUp()
        }
    }

    private fun showSections(sections: List<String>, iconRes: Int) {
        val container = binding.tipDetailSections
        container.visibility = if (sections.isEmpty()) View.GONE else View.VISIBLE

        // The tip's own icon leads, the extra sections cycle through mountain themed ones
        val icons = listOf(
            if (iconRes != Constants.Detail_IMG) iconRes else R.drawable.ic_snowboarding,
            R.drawable.ic_mountain,
            R.drawable.ic_flag_hill
        )

        sections.forEachIndexed { index, text ->
            val row = layoutInflater.inflate(R.layout.list_tip_section, container, false)
            row.findViewById<View>(R.id.section_separator).visibility =
                if (index == 0) View.GONE else View.VISIBLE
            row.findViewById<ImageView>(R.id.section_icon).setImageResource(icons[index % icons.size])
            row.findViewById<TextView>(R.id.section_body).text = text
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
