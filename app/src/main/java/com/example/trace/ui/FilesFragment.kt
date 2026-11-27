package com.example.trace.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trace.R
import com.example.trace.data.local.AppDatabase
import com.example.trace.data.local.entity.TagEntity
import com.example.trace.ui.adapters.FileAdapter
import com.example.trace.ui.viewmodel.FilesViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FilesFragment : Fragment(R.layout.fragment_files) {

    private val viewModel: FilesViewModel by viewModels()
    private lateinit var fileAdapter: FileAdapter
    private lateinit var chipGroupFilters: ChipGroup
    private var allTags: List<TagEntity> = emptyList()

    // Обработчик выбора файла
    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) { /* Игнорируем */ }

            val bundle = bundleOf("fileUri" to it.toString())
            findNavController().navigate(R.id.fileEditorFragment, bundle)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvFiles = view.findViewById<RecyclerView>(R.id.rvFiles)
        val etSearch = view.findViewById<AppCompatEditText>(R.id.etSearch)
        val btnSearchAction = view.findViewById<ImageButton>(R.id.btnSearchAction)
        val btnAddFile = view.findViewById<View>(R.id.btnAddFile)
        val btnAddTagFilter = view.findViewById<View>(R.id.btnAddTagFilter)
        chipGroupFilters = view.findViewById(R.id.chipGroupFilters)

        // 1. Загрузка всех тегов для отображения имен
        viewLifecycleOwner.lifecycleScope.launch {
            allTags = AppDatabase.getDatabase(requireContext()).tagDao().getAllTags().first()
            updateFilterChips(viewModel.getSelectedTagIds())
        }

        // 2. Настройка адаптера
        fileAdapter = FileAdapter(emptyList()) { file ->
            val bundle = Bundle().apply { putLong("fileId", file.id) }
            findNavController().navigate(R.id.action_filesFragment_to_fileDetailFragment, bundle)
        }

        rvFiles.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = fileAdapter
        }

        // 3. Обработка аргументов (откуда пришли)
        arguments?.let { args ->
            args.getString("searchQuery")?.let { query ->
                etSearch.setText(query)
                viewModel.resetFilters()
                viewModel.onSearchQueryChanged(query)
                updateFilterChips(emptySet())
            }

            val filterTagId = args.getLong("filterTagId", -1L)
            if (filterTagId != -1L) {
                val currentSelected = viewModel.getSelectedTagIds().toMutableSet()
                currentSelected.add(filterTagId)
                viewModel.onTagsSelected(currentSelected)
                updateFilterChips(currentSelected)
            }
        }

        // 4. Кнопка "Добавить файл"
        btnAddFile.setOnClickListener {
            pickFileLauncher.launch(arrayOf("image/png", "image/jpeg", "image/gif", "application/pdf"))
        }

        // 5. Кнопка "Фильтр тегов"
        btnAddTagFilter.setOnClickListener {
            showFilterSelectionDialog()
        }

        // 6. Логика поиска (кнопка и клавиатура)
        val performSearch = {
            val query = etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                // Скрываем клавиатуру
                val imm = requireContext().getSystemService(InputMethodManager::class.java)
                imm?.hideSoftInputFromWindow(etSearch.windowToken, 0)

                // Обновляем ViewModel
                viewModel.onSearchQueryChanged(query)
            }
        }

        btnSearchAction.setOnClickListener { performSearch() }

        etSearch.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                performSearch()
                true
            } else {
                false
            }
        }

        // 7. Обработка ручного ввода (реактивно)
        etSearch.doAfterTextChanged { text ->
            viewModel.onSearchQueryChanged(text?.toString() ?: "")
        }

        // 8. Подписка на файлы
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.files.collect { files ->
                    fileAdapter.updateData(files)
                }
            }
        }
    }

    private fun showFilterSelectionDialog() {
        if (allTags.isEmpty()) {
            Toast.makeText(requireContext(), "Сначала создайте теги", Toast.LENGTH_SHORT).show()
            return
        }

        val names = allTags.map { it.name }.toTypedArray()
        val currentSelectedIds = viewModel.getSelectedTagIds()
        val checkedItems = allTags.map { currentSelectedIds.contains(it.id) }.toBooleanArray()

        val tempSelectedIds = currentSelectedIds.toMutableSet()

        AlertDialog.Builder(requireContext())
            .setTitle("Фильтр по тегам")
            .setMultiChoiceItems(names, checkedItems) { _, which, isChecked ->
                val tagId = allTags[which].id
                if (isChecked) {
                    tempSelectedIds.add(tagId)
                } else {
                    tempSelectedIds.remove(tagId)
                }
            }
            .setPositiveButton("Применить") { _, _ ->
                viewModel.onTagsSelected(tempSelectedIds)
                updateFilterChips(tempSelectedIds)
            }
            .setNegativeButton("Сбросить") { _, _ ->
                viewModel.onTagsSelected(emptySet())
                updateFilterChips(emptySet())
            }
            .show()
    }

    private fun updateFilterChips(selectedIds: Set<Long>) {
        chipGroupFilters.removeAllViews()

        selectedIds.forEach { tagId ->
            val tagName = allTags.find { it.id == tagId }?.name ?: ""
            val chip = Chip(requireContext()).apply {
                text = tagName
                isCloseIconVisible = true
                setChipBackgroundColorResource(android.R.color.white)
                setChipStrokeColorResource(R.color.green_200)
                setChipStrokeWidth(2f)

                setOnCloseIconClickListener {
                    val newSet = selectedIds.toMutableSet()
                    newSet.remove(tagId)
                    viewModel.onTagsSelected(newSet)
                    updateFilterChips(newSet)
                }
            }
            chipGroupFilters.addView(chip)
        }
    }
}