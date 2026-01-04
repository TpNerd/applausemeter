package com.example.toshi.applausometer

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun View.applySystemBarsPadding() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(
            systemBars.left,
            systemBars.top,
            systemBars.right,
            systemBars.bottom,
        )
        insets
    }
    ViewCompat.requestApplyInsets(this)
}
