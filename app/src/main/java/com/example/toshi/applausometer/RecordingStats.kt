package com.example.toshi.applausometer

data class RecordingStats(
    var samples: Int = 0,
    var ticks: Double = 0.0,
) {
    fun reset() {
        samples = 0
        ticks = 0.0
    }

    fun addSample(amplitude: Long) {
        samples += 1
        ticks += amplitude.toDouble()
    }

    fun average(): Double {
        return if (samples > 0) ticks / samples else 0.0
    }
}
