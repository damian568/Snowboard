package com.example.snowboard.Adapters

import android.content.Intent
import android.net.Uri
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.snowboard.Constants.Constants
import com.example.snowboard.Lists.SkiSlopesList
import com.example.snowboard.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView

class SkiSlopesAdapter(private val skiSlopesList: List<SkiSlopesList>) :
    RecyclerView.Adapter<SkiSlopesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.list_ski_slopes,
            parent, false
        )
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = skiSlopesList[position]

        holder.imageResort.setImageResource(currentItem.imgResort)
        holder.textTitle.text = currentItem.title
        holder.textDescription.text = currentItem.description
        holder.imageSkiSlopes.setImageResource(currentItem.imgSkiSlopes)

        // The link string is HTML, so render it and make the link open the browser
        val link = HtmlCompat.fromHtml(currentItem.link.trim(), HtmlCompat.FROM_HTML_MODE_LEGACY)
        holder.textLink.text = link
        holder.textLink.movementMethod = LinkMovementMethod.getInstance()

        // Tapping anywhere on the row opens the same link
        holder.linkRow.setOnClickListener { view ->
            firstUrl(link)?.let { url ->
                view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }

        // 1. Set initial visibility of the expanded content
        setExpandedVisibility(holder, currentItem.isExpanded)

        // 2. Set the initial rotation of the arrow (180 degrees if expanded, 0 if collapsed)
        holder.arrowButton.rotation = if (currentItem.isExpanded) 180f else 0f

        // 3. Handle the click event for the animation
        holder.cardView.setOnClickListener {
            // Toggle the boolean state
            currentItem.isExpanded = !currentItem.isExpanded

            // Animate the card expanding/collapsing
            TransitionManager.beginDelayedTransition(holder.cardView, AutoTransition())
            setExpandedVisibility(holder, currentItem.isExpanded)

            // Animate the arrow flipping!
            val targetRotation = if (currentItem.isExpanded) 180f else 0f
            holder.arrowButton.animate()
                .rotation(targetRotation)
                .setDuration(Constants.SkiRotation)
                .start()
        }
    }

    override fun getItemCount(): Int {
        return skiSlopesList.size
    }

    private fun setExpandedVisibility(holder: ViewHolder, isExpanded: Boolean) {
        val visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.textDescription.visibility = visibility
        holder.imageSkiSlopes.visibility = visibility
        holder.linkRow.visibility = visibility
    }

    private fun firstUrl(text: Spanned): String? =
        text.getSpans(0, text.length, URLSpan::class.java).firstOrNull()?.url

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageResort: ShapeableImageView = itemView.findViewById(R.id.imgResort)
        val textTitle: TextView = itemView.findViewById(R.id.titleSkiSlopes)
        val textDescription: TextView = itemView.findViewById(R.id.descriptionSkiSlopes)
        val imageSkiSlopes: ImageView = itemView.findViewById(R.id.imgSkiSlopes)
        val textLink: TextView = itemView.findViewById(R.id.linkSkiSlopes)
        val linkRow: MaterialCardView = itemView.findViewById(R.id.ski_link_row)
        val cardView: MaterialCardView = itemView.findViewById(R.id.ski_slopes_cardView)
        val arrowButton: ImageView = itemView.findViewById(R.id.arrow_button)
    }
}
