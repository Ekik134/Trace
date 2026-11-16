package com.example.trace.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trace.R
import com.example.trace.ui.adapters.TreeAdapter
import com.example.trace.ui.viewmodel.TreeViewModel
import kotlinx.coroutines.launch

class TreeFragment : Fragment(R.layout.fragment_tree) {

    private val viewModel: TreeViewModel by viewModels()
    private lateinit var treeAdapter: TreeAdapter

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

        val rvTree = view.findViewById<RecyclerView>(R.id.rvTree)

        // 1. Инициализация адаптера
        treeAdapter = TreeAdapter(emptyList()) { node ->
            // КЛИК ПО УЗЛУ: Переходим на экран файлов с фильтром по этому тегу
            val bundle = bundleOf("filterTagId" to node.id)
            findNavController().navigate(R.id.filesFragment, bundle) // Прямой переход
        }

        rvTree.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = treeAdapter
        }

        // 2. Кнопка "Добавить файл"
        view.findViewById<View>(R.id.btnAddFile).setOnClickListener {
            pickFileLauncher.launch(arrayOf("image/png", "image/jpeg", "image/gif", "application/pdf"))
        }

        // 3. Подписка на данные из ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.treeNodes.collect { nodes ->
                    treeAdapter.updateData(nodes)
                }
            }
        }
    }
}