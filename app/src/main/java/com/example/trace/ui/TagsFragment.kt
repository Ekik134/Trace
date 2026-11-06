package com.example.trace

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class TagsFragment : Fragment() {
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        return TextView(requireContext()).apply {
            text = "Теги (в разработке)"
            gravity = Gravity.CENTER
            textSize = 24f
        }
    }
}