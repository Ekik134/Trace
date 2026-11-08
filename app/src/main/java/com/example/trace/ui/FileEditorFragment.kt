package com.example.trace.ui

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class FileEditorFragment : Fragment() {
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        return TextView(requireContext()).apply {
            text = "Редактор файлов (в разработке)"
            gravity = Gravity.CENTER
            textSize = 24f
        }
    }
}