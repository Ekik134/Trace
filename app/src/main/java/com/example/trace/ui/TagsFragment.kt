package com.example.trace.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatEditText
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
import com.example.trace.ui.adapters.TagAdapter
import com.example.trace.ui.viewmodel.TagsViewModel
import kotlinx.coroutines.launch

class TagsFragment : Fragment(R.layout.fragment_tags) {

    private val viewModel: TagsViewModel by viewModels()
    private lateinit var tagAdapter: TagAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvTags = view.findViewById<RecyclerView>(R.id.rvTags)
        val etSearch = view.findViewById<AppCompatEditText>(R.id.etSearch)
        val btnAddTag = view.findViewById<View>(R.id.btnAddTag)
        val tvEmptyState = view.findViewById<View>(R.id.tvEmptyState)

        // 1. Настройка адаптера
        tagAdapter = TagAdapter(emptyList()) { tag ->
            // Переход в редактор с передачей ID (для редактирования)
            val bundle = Bundle().apply { putLong("editTagId", tag.id) }
            findNavController().navigate(R.id.action_tagsFragment_to_tagEditorFragment, bundle)
        }

        rvTags.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = tagAdapter
        }

        // 2. Клик по кнопке "Добавить тег" (переход на создание)
        btnAddTag.setOnClickListener {
            findNavController().navigate(R.id.action_tagsFragment_to_tagEditorFragment)
        }

        // 3. Поиск
        etSearch.doAfterTextChanged { text ->
            viewModel.onSearchQueryChanged(text?.toString() ?: "")
        }

        // 4. Подписка на данные
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.tags.collect { tags ->
                    tagAdapter.updateData(tags)

                    // Показ заглушки, если тегов нет
                    if (tags.isEmpty()) {
                        tvEmptyState.visibility = View.VISIBLE
                        rvTags.visibility = View.GONE
                    } else {
                        tvEmptyState.visibility = View.GONE
                        rvTags.visibility = View.VISIBLE
                    }
                }
            }
        }
    }
}