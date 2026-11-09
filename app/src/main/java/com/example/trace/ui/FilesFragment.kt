package com.example.trace.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.trace.ui.adapters.FileAdapter
import com.example.trace.ui.viewmodel.FilesViewModel
import kotlinx.coroutines.launch

class FilesFragment : Fragment(R.layout.fragment_files) {

    private val viewModel: FilesViewModel by viewModels()
    private lateinit var fileAdapter: FileAdapter

    // Обработчик выбора файла
    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) { /* Игнорируем */ }

            // Переход на экран редактора
            val bundle = bundleOf("fileUri" to it.toString())
            findNavController().navigate(R.id.fileEditorFragment, bundle)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvFiles = view.findViewById<RecyclerView>(R.id.rvFiles)
        val etSearch = view.findViewById<AppCompatEditText>(R.id.etSearch)
        val btnAddFile = view.findViewById<View>(R.id.btnAddFile) // Наша кнопка

        // 1. Настройка адаптера
        fileAdapter = FileAdapter(emptyList()) { file ->
            val bundle = Bundle().apply { putLong("fileId", file.id) }
            findNavController().navigate(R.id.action_filesFragment_to_fileDetailFragment, bundle)
        }

        rvFiles.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = fileAdapter
        }

        // 2. КЛИК ПО КНОПКЕ "ДОБАВИТЬ ФАЙЛ"
        btnAddFile.setOnClickListener {
            pickFileLauncher.launch(arrayOf("image/png", "image/jpeg", "image/gif", "application/pdf"))
        }

        // 3. Проверяем, пришел ли поисковый запрос с Главной
        arguments?.getString("searchQuery")?.let { query ->
            etSearch.setText(query)
            viewModel.onSearchQueryChanged(query)
        }

        // 4. Обработка ручного ввода поиска
        etSearch.doAfterTextChanged { text ->
            viewModel.onSearchQueryChanged(text?.toString() ?: "")
        }

        // 5. Подписка на данные
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.files.collect { files ->
                    fileAdapter.updateData(files)
                }
            }
        }
    }
}