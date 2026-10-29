package com.example.snowboard.Content

import android.app.Dialog
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.snowboard.Fragments.AddContentDialog
import com.example.snowboard.Lists.ContentType
import com.example.snowboard.Lists.UserContent
import com.example.snowboard.R
import com.example.snowboard.databinding.DialogDeleteContentBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val TAG = "UserContent"
internal const val DIALOG_WIDTH_RATIO = 0.8f
// Where the mountains start in bg_profile_header_art, below the empty sky
internal const val HEADER_ART_TOP = 0.17f

/** Shows the user's photo when there is one, otherwise the bundled [fallbackRes]. Safe to call from recycled rows. */
fun ImageView.showContentImage(bytes: ByteArray?, fallbackRes: Int) {
    if (bytes == null) {
        Glide.with(this).clear(this)
        setImageResource(fallbackRes)
    } else {
        Glide.with(this).load(bytes).placeholder(fallbackRes).error(fallbackRes).into(this)
    }
}

/** Calls [onChange] with every user's content of [type] (only [category] when given) for as long as this fragment's view exists. */
fun Fragment.observeUserContent(
    type: ContentType,
    category: String? = null,
    onChange: (List<UserContent>) -> Unit
) {
    val registration = UserContentRepository.listen(
        type,
        category,
        // Only logged: the built-in items still show, so there is nothing for the user to act on
        onError = { error -> Log.w(TAG, "Couldn't load ${type.collection}", error) },
        onChange = onChange
    )
    viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) registration.remove()
    })
}

/**
 * Shows the Edit / Delete buttons in [ownerActions] only when the signed-in user added [content]
 * (built-in items pass null), and wires them up.
 */
fun Fragment.bindOwnerActions(ownerActions: View, content: UserContent?) {
    val ownContent = content?.takeIf { it.isMine }
    ownerActions.visibility = if (ownContent != null) View.VISIBLE else View.GONE
    if (ownContent == null) return

    ownerActions.findViewById<View>(R.id.btn_edit).setOnClickListener {
        AddContentDialog.newInstance(ownContent.type, content = ownContent)
            .show(childFragmentManager, AddContentDialog.TAG)
    }
    ownerActions.findViewById<View>(R.id.btn_delete).setOnClickListener { confirmDelete(ownContent) }
}

private fun Fragment.confirmDelete(content: UserContent) {
    val binding = DialogDeleteContentBinding.inflate(layoutInflater)
    val dialog = Dialog(requireContext()).apply {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
    }

    binding.txtTitle.text = getString(R.string.delete_content_title, content.title)
    binding.imgHeader.doOnLayout { (it as ImageView).cropToBand(HEADER_ART_TOP) }
    binding.btnCancel.setOnClickListener { dialog.dismiss() }
    binding.btnDelete.setOnClickListener {
        dialog.dismiss()
        delete(content)
    }

    dialog.show()
    // Centered card, 80% of the screen width; the transparent window lets the rounded corners show
    dialog.window?.apply {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setLayout((resources.displayMetrics.widthPixels * DIALOG_WIDTH_RATIO).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
    }
}

/** Fills the width with the image and shows it from [topFraction] of its height down, so a wide band of a tall picture fits. */
internal fun ImageView.cropToBand(topFraction: Float) {
    val art = drawable ?: return
    val scale = width.toFloat() / art.intrinsicWidth
    imageMatrix = Matrix().apply {
        setScale(scale, scale)
        postTranslate(0f, -art.intrinsicHeight * topFraction * scale)
    }
}

private fun Fragment.delete(content: UserContent) {
    val appContext = requireContext().applicationContext
    viewLifecycleOwner.lifecycleScope.launch {
        val message = try {
            UserContentRepository.delete(content)
            R.string.msg_content_deleted
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't delete ${content.id}", e)
            R.string.msg_content_delete_failed
        }
        Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
    }
}
