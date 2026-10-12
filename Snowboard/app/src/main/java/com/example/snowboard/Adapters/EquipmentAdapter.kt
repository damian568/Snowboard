package com.example.snowboard.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.snowboard.Lists.EquipmentList
import com.example.snowboard.R
import com.google.android.material.card.MaterialCardView

class EquipmentAdapter(
    private val equipmentList: List<EquipmentList>,
    private val onItemClick: (EquipmentList, View) -> Unit
) :
    RecyclerView.Adapter<EquipmentAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.list_equipment,
            parent, false
        )
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = equipmentList[position]

        holder.imageView.setImageResource(currentItem.equipmentImage)
        holder.iconView.setImageResource(currentItem.equipmentIcon)
        holder.textTitle.text = currentItem.equipmentTitle
        holder.textInstruction.text = currentItem.equipmentDescription

        // 1. Give this specific card a unique transition name based on its title
        holder.cardView.transitionName = "card_transform_${currentItem.equipmentTitle}"

        holder.cardView.setOnClickListener {
            // 2. Pass BOTH the data and the clicked card view
            onItemClick(currentItem, holder.cardView)
        }
    }

    override fun getItemCount(): Int {
        return equipmentList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.equipment_image)
        val iconView: ImageView = itemView.findViewById(R.id.equipment_icon)
        val textTitle: TextView = itemView.findViewById(R.id.equipment_title)
        val textInstruction: TextView = itemView.findViewById(R.id.equipment_instruction)
        val cardView: MaterialCardView = itemView.findViewById(R.id.equipment_cardView)
    }
}