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
import java.io.File

class FileEditorFragment : Fragment(R.layout.fragment_file_editor) {

    private lateinit var fileStorageManager: FileStorageManager
    private lateinit var fileRepository: FileRepository

    // Переменные для режима создания (новый файл)
    private var selectedUri: Uri? = null
    private var selectedMimeType: String? = null
    private var selectedSize: Long? = null
    private var selectedName: String? = null

    // Переменные для режима редактирования (существующий файл)
    private var editFileId: Long = -1L
    private var existingLocalUri: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getDatabase(requireContext())
        fileRepository = FileRepository(requireContext(), db.fileDao(), db.tagDao())
        fileStorageManager = FileStorageManager(requireContext())

        val ivPreview = view.findViewById<ImageView>(R.id.ivFilePreview)
        val etName = view.findViewById<EditText>(R.id.etName)
        val etDescription = view.findViewById<EditText>(R.id.etDescription)
        val btnSave = view.findViewById<Button>(R.id.btnSave)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        val btnDelete = view.findViewById<Button>(R.id.btnDelete)
        val tvTitle = view.findViewById<android.widget.TextView>(R.id.tvTitle)

        // --- РЕЖИМ 1: РЕДАКТИРОВАНИЕ (Пришел editFileId) ---
        arguments?.getLong("editFileId", -1L)?.let { id ->
            if (id != -1L) {
                editFileId = id
                tvTitle.text = "Редактирование"
                btnDelete.visibility = View.VISIBLE // Показываем кнопку удаления

                // Загружаем данные из БД
                viewLifecycleOwner.lifecycleScope.launch {
                    val file = db.fileDao().getFileById(id)
                    if (file != null) {
                        etName.setText(file.name)
                        etDescription.setText(file.description)
                        existingLocalUri = file.localUri
                        selectedMimeType = file.mimeType

                        // Загружаем превью
                        val imagePath = file.previewPath ?: file.localUri
                        if (imagePath != null && file.mimeType?.startsWith("image/") == true) {
                            ivPreview.load(File(imagePath))
                        } else {
                            ivPreview.setImageResource(R.drawable.ic_pdf_miniature)
                        }
                    }
                }
            }
        }

        // --- РЕЖИМ 2: СОЗДАНИЕ (Пришел fileUri) ---
        arguments?.getString("fileUri")?.let { uriString ->
            selectedUri = Uri.parse(uriString)
            tvTitle.text = "Добавление файла"
            btnDelete.visibility = View.GONE // Скрываем кнопку удаления

            extractFileMetadata(selectedUri!!)

            if (selectedMimeType?.startsWith("image/") == true) {
                try {
                    ivPreview.load(selectedUri)
                } catch (e: Exception) {
                    ivPreview.setImageResource(R.drawable.ic_pdf_miniature)
                }
            } else {
                ivPreview.setImageResource(R.drawable.ic_pdf_miniature)
            }
            etName.setText(selectedName)
        }

        // --- ОБРАБОТЧИКИ КНОПОК ---

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

            if (editFileId != -1L) {
                // Обновляем существующий файл
                updateExistingFile(editFileId, name, description)
            } else {
                // Создаем новый файл
                if (selectedUri == null) {
                    Toast.makeText(requireContext(), "Файл не выбран", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                saveNewFile(name, description)
            }
        }

        btnDelete.setOnClickListener {
            // Показываем диалог подтверждения (чтобы случайно не удалить файл)
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Удалить файл?")
                .setMessage("Вы уверены, что хотите удалить этот файл? Действие нельзя отменить.")
                .setPositiveButton("Удалить") { _, _ ->
                    deleteCurrentFile()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    private fun updateExistingFile(id: Long, name: String, description: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = fileRepository.updateFile(
                fileId = id,
                newName = name,
                newDescription = description,
                newTagIds = emptyList() // Пока без тегов
            )
            result.onSuccess {
                Toast.makeText(requireContext(), "Файл обновлен!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp() // Возвращаемся на экран деталей
            }.onFailure {
                Toast.makeText(requireContext(), "Ошибка: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveNewFile(name: String, description: String) {
        val uri = selectedUri ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            val result = fileRepository.addFile(
                uri = uri,
                name = name,
                description = description,
                mimeType = selectedMimeType,
                size = selectedSize,
                tagIds = emptyList()
            )

            result.onSuccess {
                Toast.makeText(requireContext(), "Файл добавлен!", Toast.LENGTH_SHORT).show()
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

    private fun deleteCurrentFile() {
        if (editFileId == -1L) return // Нечего удалять, если файл не сохранен

        viewLifecycleOwner.lifecycleScope.launch {
            val result = fileRepository.deleteFile(editFileId)

            result.onSuccess {
                Toast.makeText(requireContext(), "Файл удален", Toast.LENGTH_SHORT).show()

                // ВАЖНО: Возвращаемся не на экран деталей (которого больше нет),
                // а на экран списка файлов.
                // Используем popBackStack с ID экрана fileDetailFragment, чтобы удалить его из стека
                findNavController().popBackStack(R.id.filesFragment, false)

                // Если вы хотите вернуться на Главную, используйте:
                // findNavController().popBackStack(R.id.homeFragment, false)

            }.onFailure { error ->
                Toast.makeText(requireContext(), "Ошибка: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}