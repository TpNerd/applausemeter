package com.example.toshi.applausometer

object MeterLogic {
    fun drawableForAmplitude(amplitude: Long): Int {
        return when {
            amplitude <= 200L -> R.drawable.lights0
            amplitude <= 2400L -> R.drawable.lights1
            amplitude <= 5600L -> R.drawable.lights2
            amplitude <= 11800L -> R.drawable.lights3
            amplitude <= 15000L -> R.drawable.lights4
            amplitude <= 19200L -> R.drawable.lights5
            amplitude <= 22400L -> R.drawable.lights6
            amplitude <= 25600L -> R.drawable.lights7
            amplitude <= 29800L -> R.drawable.lights8
            amplitude <= 31800L -> R.drawable.lights9
            else -> R.drawable.lights10
        }
    }

    fun scoreForAverage(averageAmplitude: Double): Double {
        return averageAmplitude / 330.0
    }
}
