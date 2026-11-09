package com.example.trace.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.trace.R

class HomeFragment : Fragment() {

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                // ВАЖНО: Запрашиваем постоянное разрешение на чтение этого URI
                requireContext().contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                // Передаем URI на экран редактора
                val bundle = bundleOf("fileUri" to it.toString())
                findNavController().navigate(R.id.action_homeFragment_to_fileEditorFragment, bundle)

            } catch (e: SecurityException) {
                // Некоторые провайдеры (например, Google Drive) не поддерживают постоянные разрешения
                // В таком случае мы просто передаем URI, и работаем с ним сразу
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

        view.findViewById<View>(R.id.btnAddFile).setOnClickListener {
            // Фильтр: картинки и PDF
            pickFileLauncher.launch(arrayOf("image/png", "image/jpeg", "image/gif", "application/pdf"))
        }

        view.findViewById<CardView>(R.id.cardTree).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_treeFragment)
        }
    }
}