package com.example.trace.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trace.R
import com.example.trace.ui.adapters.FileAdapter
import com.example.trace.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var recentFilesAdapter: FileAdapter

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                requireContext().contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val bundle = bundleOf("fileUri" to it.toString())
                findNavController().navigate(R.id.action_homeFragment_to_fileEditorFragment, bundle)
            } catch (e: SecurityException) {
                val bundle = bundleOf("fileUri" to it.toString())
                findNavController().navigate(R.id.action_homeFragment_to_fileEditorFragment, bundle)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Настройка адаптера для "Недавних"
        val rvRecent = view.findViewById<RecyclerView>(R.id.recentFilesContainer)

        recentFilesAdapter = FileAdapter(emptyList()) { file ->
            // Переход на экран деталей при клике на карточку
            val bundle = Bundle().apply { putLong("fileId", file.id) }
            findNavController().navigate(R.id.action_homeFragment_to_fileDetailFragment, bundle)
        }

        rvRecent.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = recentFilesAdapter
        }

        // Подписка на данные из ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recentFiles.collect { files ->
                    recentFilesAdapter.updateData(files)
                }
            }
        }

        // 2. Кнопка "Добавить файл"
        view.findViewById<View>(R.id.btnAddFile).setOnClickListener {
            pickFileLauncher.launch(arrayOf("image/png", "image/jpeg", "image/gif", "application/pdf"))
        }

        // 3. Карточка "Дерево файлов"
        view.findViewById<CardView>(R.id.cardTree).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_treeFragment)
        }

        // 4. Поиск на Главной
        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        etSearch.doAfterTextChanged { text ->
            val query = text?.toString()?.trim() ?: ""
            if (query.isNotEmpty()) {
                val bundle = bundleOf("searchQuery" to query)
                findNavController().navigate(R.id.action_homeFragment_to_filesFragment, bundle)
                etSearch.text?.clear()
            }
        }
    }
}