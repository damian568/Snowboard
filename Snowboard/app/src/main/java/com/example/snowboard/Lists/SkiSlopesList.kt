package com.example.snowboard.Lists

data class SkiSlopesList(
    val imgResort: Int,
    val title: String,
    val description: String,
    val imgSkiSlopes: Int,
    val link: String,
    var isExpanded: Boolean = false,
    // Set when a user added this ski slope rather than it being built in
    val userContent: UserContent? = null
)
