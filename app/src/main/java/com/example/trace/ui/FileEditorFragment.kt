package com.example.trace.ui

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil3.load
import com.example.trace.R
import com.example.trace.data.local.AppDatabase
import com.example.trace.data.local.FileStorageManager
import com.example.trace.data.repository.FileRepository
import kotlinx.coroutines.launch

class FileEditorFragment : Fragment(R.layout.fragment_file_editor) {

    private lateinit var fileStorageManager: FileStorageManager
    private lateinit var fileRepository: FileRepository

    private var selectedUri: Uri? = null
    private var selectedMimeType: String? = null
    private var selectedSize: Long? = null
    private var selectedName: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getDatabase(requireContext())
        fileRepository = FileRepository(requireContext(), db.fileDao(), db.tagDao())

        fileStorageManager = FileStorageManager(requireContext())

        // 1. Получаем URI из аргументов
        arguments?.getString("fileUri")?.let { uriString ->
            selectedUri = Uri.parse(uriString)
            extractFileMetadata(selectedUri!!)

            val ivPreview = view.findViewById<ImageView>(R.id.ivFilePreview)

            if (selectedMimeType?.startsWith("image/") == true) {
                try {
                    // Coil v3 (если у вас старая версия, используйте import coil.load)
                    ivPreview.load(selectedUri)
                } catch (e: Exception) {
                    // Если не удалось загрузить картинку, ставим заглушку
                    ivPreview.setImageResource(R.drawable.ic_pdf_miniature)
                }
            } else {
                ivPreview.setImageResource(R.drawable.ic_pdf_miniature)
            }

            // Автозаполнение названия
            view.findViewById<EditText>(R.id.etName).setText(selectedName)
        }

        val btnSave = view.findViewById<Button>(R.id.btnSave)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        val btnDelete = view.findViewById<Button>(R.id.btnDelete)
        val etName = view.findViewById<EditText>(R.id.etName)
        val etDescription = view.findViewById<EditText>(R.id.etDescription)

        // Скрываем кнопку "Удалить" при создании нового файла
        btnDelete.visibility = View.GONE

        btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val description = etDescription.text.toString().trim()

            if (name.isEmpty()) {
                etName.error = "Введите название"
                return@setOnClickListener
            }

            if (selectedUri == null) {
                Toast.makeText(requireContext(), "Файл не выбран", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveFile(name, description)
        }
    }

    private fun saveFile(name: String, description: String) {
        val uri = selectedUri ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            // Repository.addFile сам скопирует файл в приватную папку
            val result = fileRepository.addFile(
                uri = uri,
                name = name,
                description = description,
                mimeType = selectedMimeType,
                size = selectedSize,
                tagIds = emptyList()
            )

            result.onSuccess {
                Toast.makeText(requireContext(), "Файл успешно добавлен!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }.onFailure { error ->
                Toast.makeText(requireContext(), "Ошибка: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun extractFileMetadata(uri: Uri) {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)

                if (nameIndex != -1) selectedName = it.getString(nameIndex)
                if (sizeIndex != -1) selectedSize = it.getLong(sizeIndex)
            }
        }
        selectedMimeType = requireContext().contentResolver.getType(uri)
    }
}