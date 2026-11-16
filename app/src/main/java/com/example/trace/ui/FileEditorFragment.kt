package com.example.trace.ui

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil3.load
import com.example.trace.R
import com.example.trace.data.local.AppDatabase
import com.example.trace.data.local.FileStorageManager
import com.example.trace.data.local.entity.TagEntity
import com.example.trace.data.repository.FileRepository
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class FileEditorFragment : Fragment(R.layout.fragment_file_editor) {

    private lateinit var fileStorageManager: FileStorageManager
    private lateinit var fileRepository: FileRepository
    private lateinit var db: AppDatabase

    // Режим создания
    private var selectedUri: Uri? = null
    private var selectedMimeType: String? = null
    private var selectedSize: Long? = null
    private var selectedName: String? = null

    // Режим редактирования
    private var editFileId: Long = -1L

    // Теги
    private val selectedTagIds = mutableListOf<Long>()
    private var allTagsList: List<TagEntity> = emptyList()

    // Ссылки на View для обновления
    private lateinit var chipGroupTags: ChipGroup

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext())
        fileRepository = FileRepository(requireContext(), db.fileDao(), db.tagDao())
        fileStorageManager = FileStorageManager(requireContext())

        val ivPreview = view.findViewById<ImageView>(R.id.ivFilePreview)
        val etName = view.findViewById<EditText>(R.id.etName)
        val etDescription = view.findViewById<EditText>(R.id.etDescription)
        val btnSave = view.findViewById<Button>(R.id.btnSave)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        val btnDelete = view.findViewById<Button>(R.id.btnDelete)
        val tvTitle = view.findViewById<android.widget.TextView>(R.id.tvTitle)
        chipGroupTags = view.findViewById(R.id.chipGroupTags)

        editFileId = arguments?.getLong("editFileId", -1L) ?: -1L

        // 1. Загружаем все теги и настраиваем UI
        viewLifecycleOwner.lifecycleScope.launch {
            allTagsList = db.tagDao().getAllTags().first()

            if (editFileId != -1L) {
                // РЕЖИМ РЕДАКТИРОВАНИЯ
                tvTitle.text = "Редактирование"
                btnDelete.visibility = View.VISIBLE

                val fileWithTags = db.fileDao().getFileWithTags(editFileId)
                if (fileWithTags != null) {
                    val file = fileWithTags.file
                    etName.setText(file.name)
                    etDescription.setText(file.description)
                    selectedMimeType = file.mimeType

                    // Загружаем ID тегов
                    selectedTagIds.clear()
                    selectedTagIds.addAll(fileWithTags.tags.map { it.id })

                    // Превью
                    val imagePath = file.previewPath ?: file.localUri
                    if (imagePath != null && file.mimeType?.startsWith("image/") == true) {
                        ivPreview.load(File(imagePath))
                    } else {
                        ivPreview.setImageResource(R.drawable.ic_pdf_miniature)
                    }
                }
            } else {
                // РЕЖИМ СОЗДАНИЯ
                tvTitle.text = "Добавление файла"
                btnDelete.visibility = View.GONE

                arguments?.getString("fileUri")?.let { uriString ->
                    selectedUri = Uri.parse(uriString)
                    extractFileMetadata(selectedUri!!)

                    if (selectedMimeType?.startsWith("image/") == true) {
                        ivPreview.load(selectedUri)
                    } else {
                        ivPreview.setImageResource(R.drawable.ic_pdf_miniature)
                    }
                    etName.setText(selectedName)
                }
            }
            // Отрисовываем чипсы (теперь имена известны)
            updateTagsUI()
        }

        // --- ОБРАБОТЧИКИ КНОПОК ---
        btnCancel.setOnClickListener { findNavController().navigateUp() }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val description = etDescription.text.toString().trim()

            if (name.isEmpty()) {
                etName.error = "Введите название"
                return@setOnClickListener
            }

            if (editFileId != -1L) {
                updateExistingFile(editFileId, name, description, selectedTagIds)
            } else {
                if (selectedUri == null) {
                    Toast.makeText(requireContext(), "Файл не выбран", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                saveNewFile(name, description, selectedTagIds)
            }
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Удалить файл?")
                .setMessage("Вы уверены, что хотите удалить этот файл?")
                .setPositiveButton("Удалить") { _, _ -> deleteCurrentFile() }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    private fun updateTagsUI() {
        chipGroupTags.removeAllViews()

        // 1. Отрисовка выбранных тегов
        selectedTagIds.forEach { tagId ->
            val tagName = allTagsList.find { it.id == tagId }?.name ?: "ID: $tagId"

            val chip = Chip(requireContext()).apply {
                text = tagName
                isCloseIconVisible = true
                setChipBackgroundColorResource(android.R.color.white)
                setChipStrokeColorResource(R.color.green_200) // Зеленая обводка
                setChipStrokeWidth(2f)
                setTextColor(android.graphics.Color.BLACK)

                setOnCloseIconClickListener {
                    selectedTagIds.remove(tagId)
                    updateTagsUI()
                }
            }
            chipGroupTags.addView(chip)
        }

        // 2. Кнопка "+"
        val addChip = Chip(requireContext()).apply {
            text = "+"
            setChipBackgroundColorResource(android.R.color.transparent)
            setChipStrokeColorResource(R.color.green_200)
            setChipStrokeWidth(2f)
            setTextColor(android.graphics.Color.parseColor("#66BB6A"))
            textSize = 18f

            setOnClickListener { showTagSelectionDialog() }
        }
        chipGroupTags.addView(addChip)
    }

    private fun showTagSelectionDialog() {
        if (allTagsList.isEmpty()) {
            Toast.makeText(requireContext(), "Сначала создайте теги на вкладке 'Теги'", Toast.LENGTH_SHORT).show()
            return
        }

        val names = allTagsList.map { it.name }.toTypedArray()
        val checkedItems = allTagsList.map { selectedTagIds.contains(it.id) }.toBooleanArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Выберите теги")
            .setMultiChoiceItems(names, checkedItems) { _, which, isChecked ->
                val tagId = allTagsList[which].id
                if (isChecked) {
                    if (!selectedTagIds.contains(tagId)) selectedTagIds.add(tagId)
                } else {
                    selectedTagIds.remove(tagId)
                }
            }
            .setPositiveButton("ОК") { _, _ -> updateTagsUI() }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateExistingFile(id: Long, name: String, description: String, tagIds: List<Long>) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = fileRepository.updateFile(
                fileId = id,
                newName = name,
                newDescription = description,
                newTagIds = tagIds
            )
            result.onSuccess {
                Toast.makeText(requireContext(), "Файл обновлен!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }.onFailure {
                Toast.makeText(requireContext(), "Ошибка: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveNewFile(name: String, description: String, tagIds: List<Long>) {
        val uri = selectedUri ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            val result = fileRepository.addFile(
                uri = uri,
                name = name,
                description = description,
                mimeType = selectedMimeType,
                size = selectedSize,
                tagIds = tagIds
            )

            result.onSuccess {
                Toast.makeText(requireContext(), "Файл добавлен!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }.onFailure { error ->
                Toast.makeText(requireContext(), "Ошибка: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteCurrentFile() {
        if (editFileId == -1L) return
        viewLifecycleOwner.lifecycleScope.launch {
            val result = fileRepository.deleteFile(editFileId)
            result.onSuccess {
                Toast.makeText(requireContext(), "Файл удален", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack(R.id.filesFragment, false)
            }.onFailure {
                Toast.makeText(requireContext(), "Ошибка: ${it.message}", Toast.LENGTH_SHORT).show()
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