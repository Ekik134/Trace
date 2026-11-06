package com.example.trace

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Безопасный поиск панели навигации
        val bottomNavContainer = findViewById<View>(R.id.bottomNavInclude)
        val bottomNav = bottomNavContainer?.findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // Применяем отступы (чтобы панель не уезжала под системные кнопки)
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavContainer) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        // 2. Настраиваем слушатель только если панель найдена
        bottomNav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { loadFragment(HomeFragment()); true }
                R.id.nav_tree -> { loadFragment(TreeFragment()); true }
                R.id.nav_tags -> { loadFragment(TagsFragment()); true }
                R.id.nav_profile -> { loadFragment(ProfileFragment()); true }
                else -> false
            }
        }

        // 3. Открываем Главную при первом запуске
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}