package com.example.toshi.applausometer

import android.app.Activity
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat

fun Activity.applySystemBarsStyle(@ColorRes colorRes: Int, rootView: View) {
    val color = ContextCompat.getColor(this, colorRes)
    window.statusBarColor = color
    window.navigationBarColor = color

    WindowCompat.getInsetsController(window, rootView).apply {
        isAppearanceLightStatusBars = false
        isAppearanceLightNavigationBars = false
    }
}
