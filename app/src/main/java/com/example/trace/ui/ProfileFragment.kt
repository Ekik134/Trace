package com.example.trace.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.trace.R
import com.example.trace.ui.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvUserName = view.findViewById<TextView>(R.id.tvUserName)
        val tvFileCount = view.findViewById<TextView>(R.id.tvFileCount)
        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        val btnSearchAction = view.findViewById<ImageButton>(R.id.btnSearchAction)

        // Подписка на ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.userName.collect { name -> tvUserName.text = name }
                }
                launch {
                    viewModel.fileCount.collect { count -> tvFileCount.text = count.toString() }
                }
            }
        }

        // Функция для поиска
        val performSearch = {
            val query = etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                val imm = requireContext().getSystemService(InputMethodManager::class.java)
                imm?.hideSoftInputFromWindow(etSearch.windowToken, 0)

                val bundle = bundleOf("searchQuery" to query)
                findNavController().navigate(R.id.filesFragment, bundle)
                etSearch.text?.clear()
            }
        }

        btnSearchAction.setOnClickListener { performSearch() }

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }
    }
}