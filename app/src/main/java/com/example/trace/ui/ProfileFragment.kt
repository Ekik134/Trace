package com.example.trace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Подключаем ваш красивый макет профиля
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Здесь в будущем вы будете получать данные из SQLite и DataStore
        // val tvUserName = view.findViewById<TextView>(R.id.tvUserName)
        // val tvFileCount = view.findViewById<TextView>(R.id.tvFileCount)

        // Пока просто заглушка (данные уже прописаны в XML)
    }
}