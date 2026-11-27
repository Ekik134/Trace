package com.example.trace.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil3.load
import com.example.trace.R
import com.example.trace.data.local.AppDatabase
import com.example.trace.data.local.FileStorageManager
import com.example.trace.data.model.TagModel
import com.example.trace.ui.viewmodel.TagEditorViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class TagEditorFragment : Fragment(R.layout.fragment_tag_editor) {

    private val viewModel: TagEditorViewModel by viewModels()
    private var editTagId: Long? = null
    private var selectedIconUri: String? = null // Путь к выбранной иконке
    private lateinit var fileStorageManager: FileStorageManager

    // Обработчик выбора картинки из галереи
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // Сохраняем картинку в приватную папку приложения
            val (localPath, _) = fileStorageManager.saveFileToInternalStorage(it, "image/*")
            if (localPath != null) {
                selectedIconUri = localPath
                // Отображаем выбранную иконку на экране
                view?.findViewById<ImageView>(R.id.ivTagIcon)?.load(File(localPath))
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fileStorageManager = FileStorageManager(requireContext())

        val etName = view.findViewById<EditText>(R.id.etName)
        val btnSave = view.findViewById<Button>(R.id.btnSave)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        val btnClose = view.findViewById<ImageButton>(R.id.btnClose)
        val btnDelete = view.findViewById<Button>(R.id.btnDelete)
        val tvTitle = view.findViewById<TextView>(R.id.tvEditorTitle)
        val btnAddParent = view.findViewById<ImageButton>(R.id.btnAddParent)
        val btnAddChild = view.findViewById<ImageButton>(R.id.btnAddChild)
        val chipGroupParents = view.findViewById<ChipGroup>(R.id.chipGroupParents)
        val chipGroupChildren = view.findViewById<ChipGroup>(R.id.chipGroupChildren)
        val ivTagIcon = view.findViewById<ImageView>(R.id.ivTagIcon) // Иконка в редакторе

        // Клик по иконке -> открываем проводник/галерею
        ivTagIcon.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        editTagId = arguments?.getLong("editTagId", -1L)?.takeIf { it != -1L }

        if (editTagId != null) {
            tvTitle.text = "Редактирование тега"
            btnDelete.visibility = View.VISIBLE
            viewModel.loadTag(editTagId!!)
        } else {
            tvTitle.text = "Новый тег"
            btnDelete.visibility = View.GONE
        }

        // Подписка на загрузку имени и иконки текущего тега
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentTag.collect { tag ->
                    if (tag != null) {
                        etName.setText(tag.name)

                        // Загрузка иконки (если есть)
                        if (!tag.iconUri.isNullOrBlank()) {
                            selectedIconUri = tag.iconUri
                            val ivIcon = view.findViewById<ImageView>(R.id.ivTagIcon)
                            ivIcon.load(File(tag.iconUri))
                            ivIcon.clearColorFilter() // <-- Важно
                        }
                    }
                }
            }
        }

        // Подписка на Родителей
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.parents.collect { parents ->
                    chipGroupParents.removeAllViews()
                    parents.forEach { parent ->
                        val chip = createTagChip(parent) {
                            viewModel.removeRelation(parent.id, editTagId!!, editTagId!!)
                        }
                        chipGroupParents.addView(chip)
                    }
                }
            }
        }

        // Подписка на Детей
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.children.collect { children ->
                    chipGroupChildren.removeAllViews()
                    children.forEach { child ->
                        val chip = createTagChip(child) {
                            viewModel.removeRelation(editTagId!!, child.id, editTagId!!)
                        }
                        chipGroupChildren.addView(chip)
                    }
                }
            }
        }

        // Обработчики кнопок "+"
        btnAddParent.setOnClickListener {
            if (editTagId == null) {
                Toast.makeText(requireContext(), "Сначала сохраните тег", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showTagSelectionDialog(isForParent = true)
        }

        btnAddChild.setOnClickListener {
            if (editTagId == null) {
                Toast.makeText(requireContext(), "Сначала сохраните тег", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showTagSelectionDialog(isForParent = false)
        }

        // Сохранение тега
        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                etName.error = "Введите название тега"
                return@setOnClickListener
            }
            // Передаем имя и путь к иконке
            viewModel.saveTag(editTagId, name, selectedIconUri)
        }

        // ... (остальные подписки на saveSuccess, errorEvent, deleteSuccess)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.saveSuccess.collect {
                    Toast.makeText(requireContext(), "Тег сохранен", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorEvent.collect { msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.deleteSuccess.collect {
                    Toast.makeText(requireContext(), "Тег удален", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Удалить тег?")
                .setMessage("Вы уверены? Все связи с этим тегом также будут удалены.")
                .setPositiveButton("Удалить") { _, _ ->
                    editTagId?.let { id -> deleteTagAndClose(id) }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        btnCancel.setOnClickListener { findNavController().navigateUp() }
        btnClose.setOnClickListener { findNavController().navigateUp() }
    }

    private fun deleteTagAndClose(tagId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.deleteTag(tagId)
        }
    }

    private fun createTagChip(tag: TagModel, onRemove: () -> Unit): Chip {
        return Chip(requireContext()).apply {
            text = tag.name
            isCloseIconVisible = true
            setChipBackgroundColorResource(android.R.color.white)
            setChipStrokeColorResource(R.color.green_200)
            setChipStrokeWidth(2f)
            setOnCloseIconClickListener { onRemove() }
        }
    }

    private fun showTagSelectionDialog(isForParent: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val allTags = db.tagDao().getAllTags().first()

            val currentParents = viewModel.parents.value.map { it.id }
            val currentChildren = viewModel.children.value.map { it.id }
            val allRelatedIds = (currentParents + currentChildren).toSet()

            val availableTags = allTags.filter { tag ->
                tag.id != editTagId && tag.id !in allRelatedIds
            }

            if (availableTags.isEmpty()) {
                Toast.makeText(requireContext(), "Нет доступных тегов для добавления", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val names = availableTags.map { it.name }.toTypedArray()

            AlertDialog.Builder(requireContext())
                .setTitle("Выберите тег")
                .setItems(names) { _, which ->
                    val selectedTag = availableTags[which]
                    if (isForParent) {
                        viewModel.addParentRelation(parentId = selectedTag.id, childId = editTagId!!)
                    } else {
                        viewModel.addChildRelation(parentId = editTagId!!, childId = selectedTag.id)
                    }
                }
                .show()
        }
    }
}