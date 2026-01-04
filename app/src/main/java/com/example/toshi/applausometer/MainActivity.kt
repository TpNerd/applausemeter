package com.example.toshi.applausometer

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.activity.compose.setContent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    private enum class SliderAnimMode {
        StopDown,
        XUp,
        Stop,
        Down,
    }

    private lateinit var recorderController: AudioRecorderController
    private lateinit var outputFileProvider: OutputFileProvider

    private var isRecording by mutableStateOf(false)
    private var sampleCount by mutableStateOf(0)
    private val stats = RecordingStats()

    private var sampleJob: Job? = null
    private var stopJob: Job? = null

    private val scoreList = mutableStateListOf<String>()
    private var num = 1

    private var statusText by mutableStateOf("Idle")
    private var scoreText by mutableStateOf("")
    private var timerText by mutableStateOf("<")
    private var meterResId by mutableIntStateOf(R.drawable.lights10)
    private var listVisible by mutableStateOf(true)
    private var sliderAnimMode by mutableStateOf(SliderAnimMode.StopDown)

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            startRecording()
        } else {
            Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_LONG).show()
            sliderAnimMode = SliderAnimMode.StopDown
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        recorderController = AudioRecorderController(this)
        outputFileProvider = OutputFileProvider(this)

        setContent {
            MaterialTheme {
                ApplauseScreen(
                    statusText = statusText,
                    scoreText = scoreText,
                    timerText = timerText,
                    meterResId = meterResId,
                    listVisible = listVisible,
                    scores = scoreList,
                    sliderAnimMode = sliderAnimMode,
                    onSliderPressed = {
                        if (!isRecording) {
                            sliderAnimMode = SliderAnimMode.XUp
                            if (hasRecordAudioPermission()) {
                                startRecording()
                            } else {
                                requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                )
            }
        }

        applySystemBarsStyle(R.color.colorSystemBars, window.decorView)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopJobs()
        recorderController.stopAndRelease()
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun startRecording() {
        if (isRecording) return

        sampleCount = 0
        stats.reset()
        scoreText = ""
        listVisible = false
        timerText = "<"
        sliderAnimMode = SliderAnimMode.XUp

        val ok = recorderController.start(outputFileProvider.recordingFile())
        if (!ok) {
            Toast.makeText(this, "Recorder error", Toast.LENGTH_LONG).show()
            recorderController.stopAndRelease()
            return
        }

        isRecording = true
        startSamplingLoop()
        scheduleStop()
    }

    private fun startSamplingLoop() {
        stopJobs()

        sampleJob = lifecycleScope.launch {
            while (isRecording) {
                val amp = recorderController.maxAmplitude()

                if (amp > 0) {
                    meterResId = MeterLogic.drawableForAmplitude(amp)
                }

                sampleCount += 1
                stats.addSample(amp)

                if (sampleCount == 10) {
                    sliderAnimMode = SliderAnimMode.Stop
                }

                val df2 = DecimalFormat("00")
                val su = df2.format((amp.toFloat() / 330f))
                statusText = "Listening..."
                scoreText = su

                val remaining = max(0.0, 8.0 - ((sampleCount - 1.0) / 10.0))
                val df1 = DecimalFormat("0")
                timerText = " ${df1.format(remaining)}"

                delay(100)
            }
        }
    }

    private fun scheduleStop() {
        stopJob = lifecycleScope.launch {
            delay(7000)
            finishRecordingAndShowScore()
        }
    }

    private fun finishRecordingAndShowScore() {
        if (!isRecording) return

        isRecording = false
        stopJobs()

        val avg = stats.average()
        val score = MeterLogic.scoreForAverage(avg)
        meterResId = MeterLogic.drawableForAmplitude(avg.toLong())

        val dfScore = DecimalFormat("00.00")
        val scoreText = dfScore.format(score)

        while (num > 7 && scoreList.isNotEmpty()) {
            scoreList.removeAt(0)
        }

        statusText = "Idle"
        this.scoreText = scoreText
        scoreList.add("${num++}: $scoreText")

        Toast.makeText(this, ": $scoreText punti!", Toast.LENGTH_LONG).show()

        sampleCount = 0
        stats.reset()

        listVisible = true
        timerText = "<3"
        sliderAnimMode = SliderAnimMode.Down

        lifecycleScope.launch {
            delay(400)
            sliderAnimMode = SliderAnimMode.StopDown
        }

        recorderController.stopAndRelease()
    }

    private fun stopJobs() {
        sampleJob?.cancel()
        sampleJob = null

        stopJob?.cancel()
        stopJob = null
    }

    @Composable
    private fun ApplauseScreen(
        statusText: String,
        scoreText: String,
        timerText: String,
        meterResId: Int,
        listVisible: Boolean,
        scores: List<String>,
        sliderAnimMode: SliderAnimMode,
        onSliderPressed: () -> Unit,
    ) {
        val background = colorResource(R.color.colorSystemBars)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            Image(
                painter = painterResource(R.drawable.sfondo2),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(3f),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(7f)
                            .fillMaxSize(),
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.TopStart),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(8f)
                            .fillMaxSize(),
                    ) {
                        Text(
                            text = scoreText,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.CenterEnd),
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(18f),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(7f)
                            .fillMaxSize(),
                    ) {
                        if (listVisible) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                reverseLayout = true,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    bottom = 110.dp,
                                ),
                            ) {
                                items(scores.asReversed()) { item ->
                                    Text(
                                        text = item,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                    )
                                }
                            }
                        }

                        SliderAndTimer(
                            timerText = timerText,
                            sliderAnimMode = sliderAnimMode,
                            onSliderPressed = onSliderPressed,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 20.dp),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(8f)
                            .fillMaxSize(),
                    ) {
                        Image(
                            painter = painterResource(meterResId),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    }

    @Composable
    private fun SliderAndTimer(
        timerText: String,
        sliderAnimMode: SliderAnimMode,
        onSliderPressed: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val parentHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
            val translateFrac = rememberSliderTranslateFraction(sliderAnimMode)

            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .graphicsLayer {
                        translationY = parentHeightPx * translateFrac
                    },
            ) {
                Text(
                    text = timerText,
                    color = Color(0xFFF72EF0),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(top = 36.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Image(
                    painter = painterResource(R.drawable.slider),
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 64.dp, height = 98.dp)
                        .clickable(onClick = onSliderPressed),
                )
            }
        }
    }

    @Composable
    private fun rememberSliderTranslateFraction(sliderAnimMode: SliderAnimMode): Float {
        val anim = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(-0.01f) }

        androidx.compose.runtime.LaunchedEffect(sliderAnimMode) {
            when (sliderAnimMode) {
                SliderAnimMode.StopDown -> {
                    anim.snapTo(-0.01f)
                    while (isActive) {
                        anim.animateTo(
                            targetValue = -0.04f,
                            animationSpec = androidx.compose.animation.core.tween(
                                durationMillis = 1000,
                                easing = androidx.compose.animation.core.LinearEasing,
                            ),
                        )
                        anim.animateTo(
                            targetValue = -0.01f,
                            animationSpec = androidx.compose.animation.core.tween(
                                durationMillis = 1000,
                                easing = androidx.compose.animation.core.LinearEasing,
                            ),
                        )
                    }
                }

                SliderAnimMode.XUp -> {
                    anim.snapTo(-0.10f)
                    anim.animateTo(
                        targetValue = -0.65f,
                        animationSpec = androidx.compose.animation.core.tween(
                            durationMillis = 1000,
                            easing = androidx.compose.animation.core.LinearEasing,
                        ),
                    )
                }

                SliderAnimMode.Stop -> {
                    anim.snapTo(-0.65f)
                    while (isActive) {
                        anim.animateTo(
                            targetValue = -0.60f,
                            animationSpec = androidx.compose.animation.core.tween(
                                durationMillis = 500,
                                easing = androidx.compose.animation.core.LinearEasing,
                            ),
                        )
                        anim.animateTo(
                            targetValue = -0.65f,
                            animationSpec = androidx.compose.animation.core.tween(
                                durationMillis = 500,
                                easing = androidx.compose.animation.core.LinearEasing,
                            ),
                        )
                    }
                }

                SliderAnimMode.Down -> {
                    anim.snapTo(-0.55f)
                    anim.animateTo(
                        targetValue = -0.10f,
                        animationSpec = androidx.compose.animation.core.tween(
                            durationMillis = 400,
                            easing = androidx.compose.animation.core.LinearEasing,
                        ),
                    )
                }
            }
        }

        return anim.value
    }
}
