package com.example.snowboard.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.snowboard.Lists.TipsList
import com.example.snowboard.R
import com.google.android.material.card.MaterialCardView

class TipsAdapter(
    private val tipsList: ArrayList<TipsList>,
    private val onItemClick: (TipsList, View) -> Unit
) :
    RecyclerView.Adapter<TipsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.list_tips,
            parent, false
        )

        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val component = tipsList[position]

        holder.imageView.setImageResource(component.tipIcon)
        holder.textTitle.text = component.tipTitle
        holder.textDescription.text = component.tipDescription

        // 1. Give this specific card a unique transition name based on its title
        holder.cardView.transitionName = "card_transform_${component.tipTitle}"

        holder.cardView.setOnClickListener {
            // 2. Pass BOTH the data and the clicked card view
            onItemClick(component, holder.cardView)
        }
    }

    override fun getItemCount(): Int {
        return tipsList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.icon_info)
        val textTitle: TextView = itemView.findViewById(R.id.title)
        val textDescription: TextView = itemView.findViewById(R.id.description)
        val cardView: MaterialCardView = itemView.findViewById(R.id.tip_cardView)
    }
}