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

        // Получаем ID, если это режим редактирования
        editTagId = arguments?.getLong("editTagId", -1L)?.takeIf { it != -1L }

        if (editTagId != null) {
            tvTitle.text = "Редактирование тега"
            btnDelete.visibility = View.VISIBLE
            viewModel.loadTag(editTagId!!)
        } else {
            tvTitle.text = "Новый тег"
            btnDelete.visibility = View.GONE
        }

        // Подписка на загрузку имени
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentTag.collect { tag ->
                    if (tag != null) etName.setText(tag.name)
                }
            }
        }

        // Подписка на Родителей (Входит в)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.parents.collect { parents ->
                    chipGroupParents.removeAllViews()
                    parents.forEach { parent ->
                        val chip = createTagChip(parent) {
                            viewModel.removeRelation(parent.id, editTagId!!)
                        }
                        chipGroupParents.addView(chip)
                    }
                }
            }
        }

        // Подписка на Детей (Включает в себя)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.children.collect { children ->
                    chipGroupChildren.removeAllViews()
                    children.forEach { child ->
                        val chip = createTagChip(child) {
                            viewModel.removeRelation(editTagId!!, child.id)
                        }
                        chipGroupChildren.addView(chip)
                    }
                }
            }
        }

        // Обработка кнопок "+"
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

        // Сохранение
        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                etName.error = "Введите название тега"
                return@setOnClickListener
            }
            viewModel.saveTag(editTagId, name)
        }

        // Подписка на успешное сохранение
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.saveSuccess.collect {
                    Toast.makeText(requireContext(), "Тег сохранен", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        }

        // Подписка на ошибки (циклы)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorEvent.collect { msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                }
            }
        }

        btnCancel.setOnClickListener { findNavController().navigateUp() }
        btnClose.setOnClickListener { findNavController().navigateUp() }
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

            // ИСПРАВЛЕНО: Правильное получение первого элемента из Flow
            val allTags = db.tagDao().getAllTags().first()

            // Фильтруем: нельзя добавить себя
            val availableTags = allTags.filter { it.id != editTagId }
            val names = availableTags.map { it.name }.toTypedArray()

            if (names.isEmpty()) {
                Toast.makeText(requireContext(), "Нет доступных тегов", Toast.LENGTH_SHORT).show()
                return@launch
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Выберите тег")
                .setItems(names) { _, which ->
                    val selectedTag = availableTags[which]
                    if (isForParent) {
                        // Текущий тег входит в selectedTag
                        viewModel.addParentRelation(parentId = selectedTag.id, childId = editTagId!!)
                    } else {
                        // Текущий тег включает selectedTag
                        viewModel.addChildRelation(parentId = editTagId!!, childId = selectedTag.id)
                    }
                }
                .show()
        }
    }
}