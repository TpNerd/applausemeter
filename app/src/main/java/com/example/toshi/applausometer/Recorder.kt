package com.example.toshi.applausometer

interface Recorder {
    fun start(outputPath: String): Boolean
    fun maxAmplitude(): Long
    fun stopAndRelease()
}
