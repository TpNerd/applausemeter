package com.example.toshi.applausometer

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

class AudioRecorderController(private val context: Context) : Recorder {

    private var recorder: MediaRecorder? = null

    override fun start(outputPath: String): Boolean {
        if (recorder != null) return false

        val outputFile = File(outputPath)

        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            r.setOutputFile(outputFile.absolutePath)
            r.prepare()
            r.start()
        } catch (_: IllegalStateException) {
            try {
                r.release()
            } catch (_: Throwable) {
            }
            return false
        } catch (_: IOException) {
            try {
                r.release()
            } catch (_: Throwable) {
            }
            return false
        }

        recorder = r
        return true
    }

    override fun maxAmplitude(): Long {
        return recorder?.maxAmplitude?.toLong() ?: 0L
    }

    override fun stopAndRelease() {
        val r = recorder
        recorder = null

        if (r != null) {
            try {
                r.stop()
            } catch (_: Throwable) {
            }
            try {
                r.release()
            } catch (_: Throwable) {
            }
        }
    }
}
