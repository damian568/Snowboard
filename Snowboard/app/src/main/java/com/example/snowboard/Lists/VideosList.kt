package com.example.snowboard.Lists

data class VideosList(
    val webVideos: String,
    val title: String,
    val description: String,
    // Set when a user added this video rather than it being built in
    val userContent: UserContent? = null
)
