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
import androidx.core.widget.doAfterTextChanged // Импорт для отслеживания ввода
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.trace.R

class HomeFragment : Fragment() {

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        // ... (ваш существующий код обработки файла)
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

        // 1. Кнопка "Добавить файл"
        view.findViewById<View>(R.id.btnAddFile).setOnClickListener {
            pickFileLauncher.launch(arrayOf("image/png", "image/jpeg", "image/gif", "application/pdf"))
        }

        // 2. Карточка "Дерево файлов"
        view.findViewById<CardView>(R.id.cardTree).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_treeFragment)
        }

        // 3. НОВОЕ: Поиск на Главной
        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        etSearch.doAfterTextChanged { text ->
            val query = text?.toString()?.trim() ?: ""
            if (query.isNotEmpty()) {
                // Переходим на экран файлов и передаем запрос
                val bundle = bundleOf("searchQuery" to query)
                findNavController().navigate(R.id.action_homeFragment_to_filesFragment, bundle)

                // Очищаем поле на Главной, чтобы при возврате оно было пустым
                etSearch.text?.clear()
            }
        }
    }
}