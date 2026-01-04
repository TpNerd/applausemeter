package com.example.toshi.applausometer

object MeterLogic {
    fun levelForAmplitude(amplitude: Long): Int {
        return when {
            amplitude <= 200L -> 0
            amplitude <= 2400L -> 1
            amplitude <= 5600L -> 2
            amplitude <= 11800L -> 3
            amplitude <= 15000L -> 4
            amplitude <= 19200L -> 5
            amplitude <= 22400L -> 6
            amplitude <= 25600L -> 7
            amplitude <= 29800L -> 8
            amplitude <= 31800L -> 9
            else -> 10
        }
    }

    fun drawableForLevel(level: Int): Int {
        return when (level) {
            0 -> R.drawable.lights0
            1 -> R.drawable.lights1
            2 -> R.drawable.lights2
            3 -> R.drawable.lights3
            4 -> R.drawable.lights4
            5 -> R.drawable.lights5
            6 -> R.drawable.lights6
            7 -> R.drawable.lights7
            8 -> R.drawable.lights8
            9 -> R.drawable.lights9
            else -> R.drawable.lights10
        }
    }

    fun drawableForAmplitude(amplitude: Long): Int {
        return drawableForLevel(levelForAmplitude(amplitude))
    }

    fun scoreForAverage(averageAmplitude: Double): Double {
        return averageAmplitude / 330.0
    }
}
