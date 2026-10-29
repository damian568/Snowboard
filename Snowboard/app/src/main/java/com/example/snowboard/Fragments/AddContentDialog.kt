package com.example.snowboard.Fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.snowboard.Content.UserContentRepository
import com.example.snowboard.Lists.ContentType
import com.example.snowboard.Lists.UserContent
import com.example.snowboard.R
import com.example.snowboard.databinding.DialogAddContentBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// Opened by the FAB to add a tip, equipment, ski slope or video, and from a card's menu to edit one
class AddContentDialog : DialogFragment() {

    private lateinit var binding: DialogAddContentBinding
    private lateinit var type: ContentType
    private var editing: UserContent? = null

    private var pickedImage: Uri? = null
    private var removeImage = false

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            pickedImage = it
            removeImage = false
            showPhoto()
        }
    }

    private val hasPhotoField get() = type == ContentType.EQUIPMENT || type == ContentType.SKI_SLOPE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogAddContentBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    @Suppress("DEPRECATION")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Keep the Save button above the keyboard
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        type = ContentType.valueOf(requireArguments().getString(ARG_TYPE)!!)
        editing = requireArguments().getParcelable(ARG_CONTENT)

        // Downsampled to the dialog's size; the drawable itself is far larger than needed
        Glide.with(this).load(R.drawable.snow_back).centerCrop().into(binding.imgBackground)

        setUpFields()
        editing?.let { fillFields(it) }
        showPhoto()

        binding.imgClose.setOnClickListener { dismiss() }
        binding.photoBox.setOnClickListener { pickImage.launch("image/*") }
        binding.btnRemovePhoto.setOnClickListener {
            pickedImage = null
            removeImage = true
            showPhoto()
        }
        binding.btnSave.setOnClickListener { save() }
    }

    override fun onStart() {
        super.onStart()
        // Centered card, 90% of the screen width; the transparent window lets the rounded corners show
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout((resources.displayMetrics.widthPixels * DIALOG_WIDTH_RATIO).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.CENTER)
        }
    }

    private fun setUpFields() {
        binding.txtDialogTitle.setText(if (editing == null) type.addTitle else type.editTitle)
        binding.imgSaveIcon.setImageResource(if (editing == null) R.drawable.ic_add_circle else R.drawable.ic_check_circle)
        setSaving(false)
        binding.imgTitleIcon.setImageResource(type.titleIcon)
        binding.editTitle.setHint(type.titleHint)

        binding.fieldLink.isVisible = type == ContentType.VIDEO || type == ContentType.SKI_SLOPE
        binding.editLink.setHint(
            if (type == ContentType.VIDEO) R.string.hint_youtube_link else R.string.hint_website_optional
        )
        binding.editDescription.setHint(
            if (type == ContentType.VIDEO) R.string.hint_description_optional else R.string.hint_description
        )
        binding.fieldQuote.isVisible = type == ContentType.TIP
        binding.photoBox.isVisible = hasPhotoField
    }

    private fun fillFields(content: UserContent) {
        binding.editTitle.setText(content.title)
        binding.editLink.setText(content.link)
        binding.editDescription.setText(content.description)
        binding.editQuote.setText(content.quote)
    }

    private fun showPhoto() {
        if (!hasPhotoField) return
        val savedImage = editing?.image?.takeUnless { removeImage }
        val photo: Any? = pickedImage ?: savedImage
        binding.imgPhoto.isVisible = photo != null
        binding.btnRemovePhoto.isVisible = photo != null
        binding.photoPlaceholder.isVisible = photo == null
        if (photo != null) {
            Glide.with(this).load(photo).centerCrop().into(binding.imgPhoto)
        }
    }

    private fun save() {
        val title = binding.editTitle.text.toString().trim()
        val description = binding.editDescription.text.toString().trim()
        val quote = binding.editQuote.text.toString().trim()
        var link = binding.editLink.text.toString().trim()

        if (title.isEmpty()) {
            showError(binding.editTitle, R.string.error_field_required)
            return
        }
        when (type) {
            ContentType.VIDEO -> if (UserContent.youTubeId(link) == null) {
                showError(binding.editLink, R.string.error_invalid_youtube_link)
                return
            }
            ContentType.SKI_SLOPE -> if (link.isNotEmpty()) {
                if (!link.contains("://")) link = "https://$link"
                if (!Patterns.WEB_URL.matcher(link).matches()) {
                    showError(binding.editLink, R.string.error_invalid_website)
                    return
                }
            }
            else -> Unit
        }
        if (type != ContentType.VIDEO && description.isEmpty()) {
            showError(binding.editDescription, R.string.error_field_required)
            return
        }

        val draft = UserContent(
            id = editing?.id.orEmpty(),
            type = type,
            ownerId = editing?.ownerId.orEmpty(),
            title = title,
            description = description,
            quote = if (binding.fieldQuote.isVisible) quote else "",
            link = if (binding.fieldLink.isVisible) link else "",
            category = editing?.category ?: requireArguments().getString(ARG_CATEGORY).orEmpty()
        )

        setSaving(true)
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                UserContentRepository.save(appContext, draft, pickedImage, removeImage)
                val message = if (editing == null) R.string.msg_content_added else R.string.msg_content_updated
                Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
                dismiss()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't save ${type.collection}", e)
                Toast.makeText(appContext, R.string.msg_content_save_failed, Toast.LENGTH_LONG).show()
                setSaving(false)
            }
        }
    }

    private fun showError(field: EditText, messageId: Int) {
        field.error = getString(messageId)
        field.requestFocus()
    }

    private fun setSaving(saving: Boolean) {
        // Don't let the dialog be closed halfway through a save
        isCancelable = !saving
        binding.imgClose.isEnabled = !saving
        binding.btnSave.isEnabled = !saving
        binding.btnSave.alpha = if (saving) 0.6f else 1f
        binding.txtSave.setText(
            when {
                saving -> R.string.btn_saving
                editing == null -> R.string.btn_add
                else -> R.string.btn_save_changes
            }
        )
    }

    companion object {
        const val TAG = "AddContentDialog"
        private const val DIALOG_WIDTH_RATIO = 0.9f
        private const val ARG_TYPE = "type"
        private const val ARG_CONTENT = "content"
        private const val ARG_CATEGORY = "category"

        /**
         * Pass [content] to edit it, leave it out to add a new one.
         * New equipment needs the [category] (EquipmentList.category) it is added under.
         */
        fun newInstance(type: ContentType, category: String? = null, content: UserContent? = null) =
            AddContentDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TYPE, type.name)
                    putString(ARG_CATEGORY, category)
                    putParcelable(ARG_CONTENT, content)
                }
            }
    }
}
