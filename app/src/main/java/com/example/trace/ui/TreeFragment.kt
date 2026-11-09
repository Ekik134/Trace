package com.example.trace.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trace.R
import com.example.trace.ui.adapters.TreeAdapter

class TreeFragment : Fragment(R.layout.fragment_tree) {

    private lateinit var treeAdapter: TreeAdapter

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) { /* Игнорируем */ }

            // ИСПРАВЛЕНО: Прямой переход к редактору, а не через экшен Главной
            val bundle = bundleOf("fileUri" to it.toString())
            findNavController().navigate(R.id.fileEditorFragment, bundle)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvTree = view.findViewById<RecyclerView>(R.id.rvTree)

        treeAdapter = TreeAdapter(emptyList()) { tag ->
            val bundle = Bundle().apply { putLong("tagId", tag.id) }
            findNavController().navigate(R.id.action_treeFragment_to_tagEditorFragment, bundle)
        }

        rvTree.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = treeAdapter
        }

        val btnAddFile = view.findViewById<View>(R.id.btnAddFile)
        btnAddFile.setOnClickListener {
            pickFileLauncher.launch(arrayOf("image/png", "image/jpeg", "image/gif", "application/pdf"))
        }

        // Временная заглушка
        val fakeData = listOf(
            TreeNodeModel(1, "природа", 15, 0),
            TreeNodeModel(2, "осень", 7, 1),
            TreeNodeModel(3, "лето", 8, 1)
        )
        treeAdapter.updateData(fakeData)
    }
}

data class TreeNodeModel(
    val id: Long,
    val name: String,
    val fileCount: Int,
    val level: Int
)