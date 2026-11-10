package com.example.trace.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.trace.R
import com.example.trace.data.local.AppDatabase
import com.example.trace.data.model.TagModel
import com.example.trace.ui.viewmodel.TagEditorViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TagEditorFragment : Fragment(R.layout.fragment_tag_editor) {

    private val viewModel: TagEditorViewModel by viewModels()
    private var editTagId: Long? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        editTagId = arguments?.getLong("editTagId", -1L)?.takeIf { it != -1L }

        if (editTagId != null) {
            tvTitle.text = "Редактирование тега"
            btnDelete.visibility = View.VISIBLE
            viewModel.loadTag(editTagId!!)
        } else {
            tvTitle.text = "Новый тег"
            btnDelete.visibility = View.GONE
        }

        // Подписка на имя
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentTag.collect { tag ->
                    if (tag != null) etName.setText(tag.name)
                }
            }
        }

        // Подписка на Родителей
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.parents.collect { parents ->
                    chipGroupParents.removeAllViews() // Жесткая очистка
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
                    chipGroupChildren.removeAllViews() // Жесткая очистка
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

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                etName.error = "Введите название тега"
                return@setOnClickListener
            }
            viewModel.saveTag(editTagId, name)
        }

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
            // Показываем диалог подтверждения, чтобы не удалить случайно
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Удалить тег?")
                .setMessage("Вы уверены? Все связи с этим тегом также будут удалены.")
                .setPositiveButton("Удалить") { _, _ ->
                    editTagId?.let { id ->
                        deleteTagAndClose(id)
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        btnCancel.setOnClickListener { findNavController().navigateUp() }
        btnClose.setOnClickListener { findNavController().navigateUp() }
    }


    private fun deleteTagAndClose(tagId: Long) {
        // 1. Подписываемся на событие успешного удаления
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.deleteSuccess.collect {
                    Toast.makeText(requireContext(), "Тег удален", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        }
        // 2. Вызываем удаление во ViewModel
        viewModel.deleteTag(tagId)
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

            // Получаем ID тегов, которые уже есть в связях (и в родителях, и в детях)
            val currentParents = viewModel.parents.value.map { it.id }
            val currentChildren = viewModel.children.value.map { it.id }

            // Объединяем их в один список, чтобы исключить из выбора
            val allRelatedIds = (currentParents + currentChildren).toSet()

            // Фильтруем:
            // 1. Исключаем себя (editTagId)
            // 2. Исключаем теги, которые уже есть в ЛЮБОЙ из связей (чтобы не было дублей в разных категориях)
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
                        // Если добавляем в Родители (Входит в)
                        viewModel.addParentRelation(parentId = selectedTag.id, childId = editTagId!!)
                    } else {
                        // Если добавляем в Дети (Включает в себя)
                        viewModel.addChildRelation(parentId = editTagId!!, childId = selectedTag.id)
                    }
                }
                .show()
        }
    }
}