package com.example.snowboard.Lists

data class EquipmentList(
    val equipmentImage: Int,
    val equipmentTitle: String,
    val equipmentDescription: String,
    val equipmentIcon: Int,
    val equipmentQuote: String,
    // Stable key (the title is translated) that user-added equipment is filed under
    val category: String
)
