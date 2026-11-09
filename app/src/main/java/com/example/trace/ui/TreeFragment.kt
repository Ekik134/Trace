package com.example.trace.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trace.R
import com.example.trace.ui.adapters.TreeAdapter

class TreeFragment : Fragment(R.layout.fragment_tree) {


    private lateinit var treeAdapter: TreeAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvTree = view.findViewById<RecyclerView>(R.id.rvTree)

        // 1. Инициализация адаптера
        treeAdapter = TreeAdapter(emptyList()) { tag ->
            // Переход в редактор тега при клике на карточку
            val bundle = Bundle().apply { putLong("tagId", tag.id) }
            findNavController().navigate(R.id.action_treeFragment_to_tagEditorFragment, bundle)
        }

        // 2. Настройка RecyclerView
        rvTree.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = treeAdapter
            // Добавляем декоратор для отрисовки линий (если он у вас есть)
            // addItemDecoration(TreeItemDecoration())
        }

        val btnAddFile = view.findViewById<View>(R.id.btnAddFile)
        btnAddFile.setOnClickListener {
            findNavController().navigate(R.id.fileEditorFragment) // Прямой переход к экрану редактора
        }

        // 4. Подписка на данные из ViewModel (Room)
        /*
        viewModel.treeNodes.observe(viewLifecycleOwner) { nodes ->
            treeAdapter.updateData(nodes)
        }
        */

        // ВРЕМЕННАЯ ЗАГЛУШКА ДЛЯ ПРОВЕРКИ (пока нет ViewModel):
        // Создаем фейковые данные, чтобы проверить, что экран работает
        val fakeData = listOf(
            TreeNodeModel(1, "природа", 15, 0),
            TreeNodeModel(2, "осень", 7, 1),
            TreeNodeModel(3, "лето", 8, 1)
        )
        treeAdapter.updateData(fakeData)
    }
}

// Временная модель для отображения
data class TreeNodeModel(
    val id: Long,
    val name: String,
    val fileCount: Int,
    val level: Int // 0 - корень, 1 - ребенок и т.д.
)