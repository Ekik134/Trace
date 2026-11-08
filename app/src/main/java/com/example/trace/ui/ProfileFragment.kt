package com.example.trace.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.trace.R
import com.example.trace.data.preferences.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private lateinit var userPreferences: UserPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userPreferences = UserPreferences(requireContext())

        val tvUserName = view.findViewById<TextView>(R.id.tvUserName)
        val tvFileCount = view.findViewById<TextView>(R.id.tvFileCount)

        // 1. Загрузка имени из DataStore
        viewLifecycleOwner.lifecycleScope.launch {
            val name = userPreferences.userNameFlow.first()
            tvUserName.text = if (name.isNotBlank()) name else "Пользователь"
        }

        // 2. Загрузка количества файлов из Room (SQLite)
        // Вам нужен экземпляр БД (например, через ViewModel).
        // Примерная логика (псевдокод):
        /*
        viewModel.fileCount.observe(viewLifecycleOwner) { count ->
            tvFileCount.text = count.toString()
        }
        */

        // Пока заглушка, как в XML:
        tvFileCount.text = "0"
    }
}