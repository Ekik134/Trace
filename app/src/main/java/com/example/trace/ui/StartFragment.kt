package com.example.trace.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.trace.R
import com.example.trace.data.preferences.UserPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class StartFragment : Fragment(R.layout.fragment_start) {

    private lateinit var userPreferences: UserPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userPreferences = UserPreferences(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            delay(1500) // Задержка для визуала

            try {
                // Безопасное чтение из DataStore
                val userName = userPreferences.userNameFlow
                    .catch { e ->
                        Log.e("StartFragment", "Error reading DataStore", e)
                        emit("") // Если ошибка, считаем, что имени нет
                    }
                    .first()

                Log.d("StartFragment", "Read name: '$userName'")

                if (userName.isBlank()) {
                    Log.d("StartFragment", "Navigating to Onboarding")
                    findNavController().navigate(R.id.action_startFragment_to_onboardingFragment)
                } else {
                    Log.d("StartFragment", "Navigating to Main Graph")
                    findNavController().navigate(R.id.action_startFragment_to_mainGraph)
                }
            } catch (e: Exception) {
                Log.e("StartFragment", "Navigation crashed", e)
                // Если навигация упала, показываем ошибку (или ничего не делаем)
            }
        }
    }
}