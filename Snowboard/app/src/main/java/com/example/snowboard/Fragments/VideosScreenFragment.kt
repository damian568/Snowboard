package com.example.snowboard.Fragments

import android.graphics.Matrix
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.doOnLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.snowboard.Adapters.VideosAdapter
import com.example.snowboard.Content.observeUserContent
import com.example.snowboard.Content.bindOwnerActions
import com.example.snowboard.Lists.ContentType
import com.example.snowboard.Lists.UserContent
import com.example.snowboard.Lists.VideosList
import com.example.snowboard.R
import com.example.snowboard.databinding.FragmentVideosScreenBinding

class VideosScreenFragment : Fragment() {
    private lateinit var binding: FragmentVideosScreenBinding
    private lateinit var videosArrayList: ArrayList<VideosList>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVideosScreenBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnMenu.setOnClickListener { openDrawer() }
        cropHeaderArtToTop()
        dataInitialize()

        val layoutManager = LinearLayoutManager(context)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.setHasFixedSize(true)

        val videosAdapter = VideosAdapter(videosArrayList, viewLifecycleOwner.lifecycle) { actions, content ->
            bindOwnerActions(actions, content)
        }
        binding.recyclerView.adapter = videosAdapter

        // Videos added by users go above the built-in ones and update live
        observeUserContent(ContentType.VIDEO) { userVideos -> showUserVideos(userVideos) }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            // This should now resolve correctly!
            if (videosAdapter.isAnyVideoFullscreen) {
                videosAdapter.exitCurrentFullscreen()
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun showUserVideos(userVideos: List<UserContent>) {
        videosArrayList.removeAll { it.userContent != null }
        videosArrayList.addAll(0, userVideos.mapNotNull { content ->
            val videoId = UserContent.youTubeId(content.link) ?: return@mapNotNull null
            VideosList(videoId, content.title, content.description, userContent = content)
        })
        binding.recyclerView.adapter?.notifyDataSetChanged()
    }

    private fun openDrawer() {
        requireActivity().findViewById<DrawerLayout>(R.id.drawerLayout)
            ?.openDrawer(GravityCompat.START)
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

    override fun onResume() {
        super.onResume()
        // To HIDE the toolbar
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    override fun onStop() {
        super.onStop()
        // To SHOW the toolbar when leaving this fragment
        (activity as? AppCompatActivity)?.supportActionBar?.show()
    }

    private fun dataInitialize() {
        videosArrayList = arrayListOf()

        videosArrayList.add(
            VideosList(
                getString(R.string.youtube_key_1),
                getString(R.string.title_videos_1),
                getString(R.string.description_videos_1)
            )
        )
        videosArrayList.add(
            VideosList(
                getString(R.string.youtube_key_2),
                getString(R.string.title_videos_2),
                getString(R.string.description_videos_2)
            )
        )
        videosArrayList.add(
            VideosList(
                getString(R.string.youtube_key_3),
                getString(R.string.title_videos_3),
                getString(R.string.description_videos_3)
            )
        )
        videosArrayList.add(
            VideosList(
                getString(R.string.youtube_key_4),
                getString(R.string.title_videos_4),
                getString(R.string.description_videos_4)
            )
        )
        videosArrayList.add(
            VideosList(
                getString(R.string.youtube_key_5),
                getString(R.string.title_videos_5),
                getString(R.string.description_videos_5)
            )
        )
        videosArrayList.add(
            VideosList(
                getString(R.string.youtube_key_6),
                getString(R.string.title_videos_6),
                getString(R.string.description_videos_6)
            )
        )
        videosArrayList.add(
            VideosList(
                getString(R.string.youtube_key_7),
                getString(R.string.title_videos_7),
                getString(R.string.description_videos_7)
            )
        )
        videosArrayList.add(
            VideosList(
                getString(R.string.youtube_key_8),
                getString(R.string.title_videos_8),
                getString(R.string.description_videos_8)
            )
        )
        videosArrayList.add(
            VideosList(
                getString(R.string.youtube_key_9),
                getString(R.string.title_videos_9),
                getString(R.string.description_videos_9)
            )
        )
        videosArrayList.add(
            VideosList(
                getString(R.string.youtube_key_10),
                getString(R.string.title_videos_10),
                getString(R.string.description_videos_10)
            )
        )
    }
}