package com.example.snowboard.Adapters

import android.content.pm.ActivityInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.snowboard.Lists.VideosList
import com.example.snowboard.R
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

class VideosAdapter(
    private val videosList: List<VideosList>,
    private val lifecycle: Lifecycle
) :
    RecyclerView.Adapter<VideosAdapter.ViewHolder>() {

    var isAnyVideoFullscreen = false
    var exitCurrentFullscreen: () -> Unit = {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.list_videos,
            parent, false
        )
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = videosList[position]

        holder.titleVideos.text = currentItem.title
        holder.descriptionVideos.text = currentItem.description

        // 1. Add the custom lifecycle to the player view
        lifecycle.addObserver(holder.webVideos)

        val options = IFramePlayerOptions.Builder(holder.itemView.context)
            .controls(1)
            .fullscreen(1)
            .build()

        // 2. THE FIX: Only initialize if it hasn't been done yet
        // This prevents the 'onMeasure' crash caused by re-initialization
        if (!holder.isInitialized) {
            // ADD THIS: Register the listener before initializing
            holder.webVideos.addFullscreenListener(object :
                com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener {
                override fun onEnterFullscreen(fullscreenView: View, exitFullscreen: () -> Unit) {
                    // 1. Hide the original list item UI
                    holder.itemView.visibility = View.GONE
                    isAnyVideoFullscreen = true
                    exitCurrentFullscreen = exitFullscreen

                    // 2. Get the root layout of the Activity (usually a FrameLayout)
                    val activity = holder.itemView.context as AppCompatActivity
                    val fullScreenContainer = activity.findViewById<ViewGroup>(android.R.id.content)
                    fullScreenContainer.addView(fullscreenView)
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
                }

                override fun onExitFullscreen() {
                    isAnyVideoFullscreen = false
                    exitCurrentFullscreen = {}
                    val activity = holder.itemView.context as AppCompatActivity
                    val fullScreenContainer = activity.findViewById<ViewGroup>(android.R.id.content)
                    if (fullScreenContainer.childCount > 0) {
                        fullScreenContainer.removeViewAt(fullScreenContainer.childCount - 1)
                    }
                    holder.itemView.visibility = View.VISIBLE
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }
            })
            holder.webVideos.initialize(object : AbstractYouTubePlayerListener() {
                override fun onReady(youTubePlayer: YouTubePlayer) {
                    holder.isInitialized = true
                    holder.youtubePlayer = youTubePlayer
                    youTubePlayer.cueVideo(currentItem.webVideos, 0f)
                }
            }, options)
        } else {
            // 3. If already initialized, just swap the video ID
            holder.youtubePlayer?.cueVideo(currentItem.webVideos, 0f)
        }
    }

    override fun getItemCount(): Int {
        return videosList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val webVideos: YouTubePlayerView = itemView.findViewById(R.id.webVideos)
        val titleVideos: TextView = itemView.findViewById(R.id.titleVideos)
        val descriptionVideos: TextView = itemView.findViewById(R.id.descriptionVideos)
        var isInitialized = false
        var youtubePlayer: YouTubePlayer? = null
    }
}