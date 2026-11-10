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
import kotlinx.coroutines.launch
import java.io.File

class FileDetailFragment : Fragment(R.layout.fragment_file_detail) {

    private val viewModel: FileDetailViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Получаем ID файла
        val fileId = arguments?.getLong("fileId") ?: return

        // 2. Загружаем данные
        viewModel.loadFile(fileId)

        // 3. Подписка на StateFlow (через lifecycleScope)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.fileWithTags.collect { data ->
                    if (data != null) {
                        val file = data.file

                        // Заполняем UI (используем ?. для безопасности)
                        view.findViewById<TextView>(R.id.tvFileName)?.text = file.name

                        // Конвертируем размер в Кб/Мб для красоты
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
                    }
                }
            }
        }

        // Кнопка закрытия
        view.findViewById<View>(R.id.btnClose)?.setOnClickListener {
            findNavController().navigateUp()
        }

        view.findViewById<View>(R.id.btnEdit)?.setOnClickListener {
            // Передаем ID файла на экран редактора
            val bundle = Bundle().apply { putLong("editFileId", fileId) }
            findNavController().navigate(R.id.action_fileDetailFragment_to_fileEditorFragment, bundle)
        }
    }
}