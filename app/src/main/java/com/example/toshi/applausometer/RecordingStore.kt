package com.example.toshi.applausometer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round
import kotlin.math.roundToInt

class RecordingStore(
    private val scope: CoroutineScope,
    private val recorder: Recorder,
    private val outputPathProvider: () -> String,
) {

    data class State(
        val isRecording: Boolean = false,
        val statusText: String = "Idle",
        val liveScoreText: String = "",
        val timerText: String = "<",
        val meterLevel: Int = 10,
        val listVisible: Boolean = true,
        val sliderAnimMode: SliderAnimMode = SliderAnimMode.StopDown,
        val scores: List<String> = emptyList(),
    )

    enum class SliderAnimMode {
        StopDown,
        XUp,
        Stop,
        Down,
    }

    sealed interface Effect {
        data object RequestMicrophonePermission : Effect
        data class Toast(val message: String) : Effect
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<Effect>(extraBufferCapacity = 8)
    val effects: SharedFlow<Effect> = _effects.asSharedFlow()

    private val stats = RecordingStats()
    private var sampleCount = 0
    private var num = 1

    private var sampleJob: Job? = null
    private var stopJob: Job? = null

    private fun formatTwoDigits(value: Double): String {
        val v = value.roundToInt().coerceAtLeast(0)
        return v.toString().padStart(2, '0')
    }

    private fun formatScore(value: Double): String {
        val scaled = round(value * 100.0).toLong()
        val whole = scaled / 100
        val frac = abs(scaled % 100)
        return "${whole.toString().padStart(2, '0')}.${frac.toString().padStart(2, '0')}"
    }

    fun onSliderPressed(hasPermission: Boolean) {
        if (_state.value.isRecording) return

        _state.value = _state.value.copy(sliderAnimMode = SliderAnimMode.XUp)

        if (!hasPermission) {
            _effects.tryEmit(Effect.RequestMicrophonePermission)
            return
        }

        startRecording()
    }

    fun onMicrophonePermissionResult(granted: Boolean) {
        if (granted) {
            startRecording()
        } else {
            _state.value = _state.value.copy(sliderAnimMode = SliderAnimMode.StopDown)
            _effects.tryEmit(Effect.Toast("Microphone permission denied"))
        }
    }

    fun stop() {
        stopJobs()
        recorder.stopAndRelease()
        _state.value = _state.value.copy(isRecording = false)
    }

    private fun startRecording() {
        if (_state.value.isRecording) return

        sampleCount = 0
        stats.reset()

        _state.value = _state.value.copy(
            isRecording = false,
            statusText = "Idle",
            liveScoreText = "",
            listVisible = false,
            timerText = "<",
            sliderAnimMode = SliderAnimMode.XUp,
            meterLevel = 10,
        )

        val ok = recorder.start(outputPathProvider())
        if (!ok) {
            recorder.stopAndRelease()
            _state.value = _state.value.copy(sliderAnimMode = SliderAnimMode.StopDown)
            _effects.tryEmit(Effect.Toast("Recorder error"))
            return
        }

        _state.value = _state.value.copy(isRecording = true)
        startSamplingLoop()
        scheduleStop()
    }

    private fun startSamplingLoop() {
        stopJobs()

        sampleJob = scope.launch {
            while (_state.value.isRecording) {
                val amp = recorder.maxAmplitude()

                sampleCount += 1
                stats.addSample(amp)

                val level = MeterLogic.levelForAmplitude(amp)
                val su = formatTwoDigits((amp.toDouble() / 330.0))
                val remaining = max(0.0, 8.0 - ((sampleCount - 1.0) / 10.0))
                val remainingSeconds = remaining.roundToInt().coerceAtLeast(0)

                _state.value = _state.value.copy(
                    statusText = "Listening...",
                    liveScoreText = su,
                    timerText = " $remainingSeconds",
                    meterLevel = level,
                    sliderAnimMode = if (sampleCount == 10) SliderAnimMode.Stop else _state.value.sliderAnimMode,
                )

                delay(100)
            }
        }
    }

    private fun scheduleStop() {
        stopJob = scope.launch {
            delay(7000)
            finishRecordingAndShowScore()
        }
    }

    private fun finishRecordingAndShowScore() {
        if (!_state.value.isRecording) return

        _state.value = _state.value.copy(isRecording = false)
        stopJobs()

        val avg = stats.average()
        val score = MeterLogic.scoreForAverage(avg)
        val scoreText = formatScore(score)

        val updatedScores = _state.value.scores.toMutableList().apply {
            add("${num++}: $scoreText")
            while (size > 7) {
                removeAt(0)
            }
        }

        _state.value = _state.value.copy(
            statusText = "Idle",
            liveScoreText = scoreText,
            timerText = "<3",
            listVisible = true,
            sliderAnimMode = SliderAnimMode.Down,
            meterLevel = MeterLogic.levelForAmplitude(avg.toLong()),
            scores = updatedScores,
        )

        _effects.tryEmit(Effect.Toast(": $scoreText punti!"))

        sampleCount = 0
        stats.reset()

        scope.launch {
            delay(400)
            _state.value = _state.value.copy(sliderAnimMode = SliderAnimMode.StopDown)
        }

        recorder.stopAndRelease()
    }

    private fun stopJobs() {
        sampleJob?.cancel()
        sampleJob = null

        stopJob?.cancel()
        stopJob = null
    }
}
