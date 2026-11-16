package com.example.trace.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil3.load
import com.example.trace.R
import com.example.trace.ui.viewmodel.FileDetailViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import java.io.File

class FileDetailFragment : Fragment(R.layout.fragment_file_detail) {

    private val viewModel: FileDetailViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fileId = arguments?.getLong("fileId") ?: return
        viewModel.loadFile(fileId)

        val chipGroupTags = view.findViewById<ChipGroup>(R.id.chipGroupTags)
        val tvNoTags = view.findViewById<TextView>(R.id.tvNoTags)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.fileWithTags.collect { data ->
                    if (data != null) {
                        val file = data.file

                        view.findViewById<TextView>(R.id.tvFileName)?.text = file.name

                        val sizeKb = (file.size ?: 0) / 1024
                        view.findViewById<TextView>(R.id.tvFileMeta)?.text =
                            "Тип: ${file.mimeType ?: "неизвестно"}\nРазмер: $sizeKb Кб"

                        view.findViewById<TextView>(R.id.tvDescriptionContent)?.text =
                            file.description?.takeIf { it.isNotBlank() } ?: "Описания нет"

                        // Превью
                        val ivPreview = view.findViewById<ImageView>(R.id.ivFilePreview)
                        val imagePath = file.previewPath ?: file.localUri
                        if (imagePath != null && file.mimeType?.startsWith("image/") == true) {
                            ivPreview?.load(File(imagePath))
                        } else {
                            ivPreview?.setImageResource(R.drawable.ic_pdf_miniature)
                        }

                        // ОТРИСОВКА ТЕГОВ
                        chipGroupTags.removeAllViews()
                        if (data.tags.isEmpty()) {
                            tvNoTags.visibility = View.VISIBLE
                            chipGroupTags.visibility = View.GONE
                        } else {
                            tvNoTags.visibility = View.GONE
                            chipGroupTags.visibility = View.VISIBLE

                            data.tags.forEach { tag ->
                                val chip = Chip(requireContext()).apply {
                                    text = tag.name
                                    isClickable = false
                                    setChipBackgroundColorResource(android.R.color.white)
                                    setChipStrokeColorResource(R.color.green_200)
                                    setChipStrokeWidth(2f)
                                }
                                chipGroupTags.addView(chip)
                            }
                        }
                    }
                }
            }
        }

        view.findViewById<View>(R.id.btnClose)?.setOnClickListener {
            findNavController().navigateUp()
        }

        view.findViewById<View>(R.id.btnEdit)?.setOnClickListener {
            val bundle = Bundle().apply { putLong("editFileId", fileId) }
            findNavController().navigate(R.id.action_fileDetailFragment_to_fileEditorFragment, bundle)
        }
    }
}