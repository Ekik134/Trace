package com.example.trace.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.trace.R

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Кнопка "Добавить файл"
        val btnAddFile = view.findViewById<View>(R.id.btnAddFile)
        btnAddFile.setOnClickListener {
            // Переход на экран редактора (создание)
            findNavController().navigate(R.id.action_homeFragment_to_fileEditorFragment)
        }

        // 2. Карточка "Дерево файлов"
        val cardTree = view.findViewById<CardView>(R.id.cardTree)
        cardTree.setOnClickListener {
            // Переход на вкладку "Древо" (или отдельный фрагмент)
            findNavController().navigate(R.id.action_homeFragment_to_treeFragment)
        }

        // 3. Поиск
        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        // Обработку ввода (TextWatcher) лучше вынести в ViewModel (LiveData),
        // но для начала можно добавить слушатель:
        /*
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // viewModel.search(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        */

        // 4. Загрузка "Недавно добавленных" (В будущем)
        // Пока карточки захардкожены в XML (через include).
        // В идеале здесь должен быть RecyclerView, который вы будете
        // заполнять данными из ViewModel (Room DAO: getRecentFiles()).
    }
}