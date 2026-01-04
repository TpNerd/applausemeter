package com.example.toshi.applausometer

import android.content.Context
import java.io.File

class OutputFileProvider(private val context: Context) {
    fun recordingFile(): File {
        val outDir = context.getExternalFilesDir(null) ?: context.filesDir
        return File(outDir, "recording.3gp")
    }
}
