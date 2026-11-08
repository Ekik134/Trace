package com.example.trace.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.trace.R
import com.example.trace.data.preferences.UserPreferences
import kotlinx.coroutines.launch

class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {

    private lateinit var userPreferences: UserPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userPreferences = UserPreferences(requireContext())

        val etName = view.findViewById<EditText>(R.id.etName)
        val btnOkay = view.findViewById<Button>(R.id.btnOkay)

        etName.doAfterTextChanged {
            etName.error = null
        }

        btnOkay.setOnClickListener {
            val name = etName.text.toString().trim()

            if (name.isEmpty()) {
                etName.error = "Пожалуйста, введите имя"
                return@setOnClickListener
            }

            btnOkay.isEnabled = false
            btnOkay.alpha = 0.5f

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    Log.d("OnboardingFragment", "Saving name: $name")
                    userPreferences.saveUserName(name)

                    Log.d("OnboardingFragment", "Navigating to Main Graph")
                    // Важно: если у вас в nav_graph нет action_onboardingFragment_to_mainGraph,
                    // используйте R.id.mainGraph напрямую
                    findNavController().navigate(R.id.action_onboardingFragment_to_mainGraph)

                } catch (e: Exception) {
                    Log.e("OnboardingFragment", "Crash on save/navigate", e)
                    Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                    btnOkay.isEnabled = true
                    btnOkay.alpha = 1f
                }
            }
        }
    }
}