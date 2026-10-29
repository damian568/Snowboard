package com.example.snowboard.Lists

data class TipsList(
    val tipTitle: String,
    val tipDescription: String,
    val tipImage: Int,
    val tipIcon: Int,
    val tipQuote: String,
    // Set when a user added this tip rather than it being built in
    val userContent: UserContent? = null
)
