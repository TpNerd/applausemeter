package com.example.toshi.applausometer

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.setContent
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.collect

class MainActivity : AppCompatActivity() {

    private lateinit var recorderController: AudioRecorderController
    private lateinit var outputFileProvider: OutputFileProvider

    private lateinit var recordingStore: RecordingStore

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        recordingStore.onMicrophonePermissionResult(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        recorderController = AudioRecorderController(this)
        outputFileProvider = OutputFileProvider(this)

        recordingStore = RecordingStore(
            scope = lifecycleScope,
            recorder = recorderController,
            outputPathProvider = { outputFileProvider.recordingFile().absolutePath },
        )

        setContent {
            MaterialTheme {
                val uiState by recordingStore.state.collectAsStateWithLifecycle()
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    recordingStore.effects.collect { effect ->
                        when (effect) {
                            RecordingStore.Effect.RequestMicrophonePermission -> {
                                requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
                            }

                            is RecordingStore.Effect.Toast -> {
                                Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                ApplauseScreen(
                    statusText = uiState.statusText,
                    scoreText = uiState.liveScoreText,
                    timerText = uiState.timerText,
                    meterResId = MeterLogic.drawableForLevel(uiState.meterLevel),
                    listVisible = uiState.listVisible,
                    scores = uiState.scores,
                    sliderAnimMode = uiState.sliderAnimMode,
                    onSliderPressed = {
                        recordingStore.onSliderPressed(hasPermission = hasRecordAudioPermission())
                    },
                )
            }
        }

        applySystemBarsStyle(R.color.colorSystemBars, window.decorView)
    }

    override fun onDestroy() {
        super.onDestroy()
        recordingStore.stop()
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    @Composable
    private fun ApplauseScreen(
        statusText: String,
        scoreText: String,
        timerText: String,
        meterResId: Int,
        listVisible: Boolean,
        scores: List<String>,
        sliderAnimMode: RecordingStore.SliderAnimMode,
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
        sliderAnimMode: RecordingStore.SliderAnimMode,
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
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    onSliderPressed()
                                    tryAwaitRelease()
                                },
                            )
                        },
                )
            }
        }
    }

    @Composable
    private fun rememberSliderTranslateFraction(sliderAnimMode: RecordingStore.SliderAnimMode): Float {
        val anim = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(-0.01f) }

        androidx.compose.runtime.LaunchedEffect(sliderAnimMode) {
            when (sliderAnimMode) {
                RecordingStore.SliderAnimMode.StopDown -> {
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

                RecordingStore.SliderAnimMode.XUp -> {
                    anim.snapTo(-0.10f)
                    anim.animateTo(
                        targetValue = -0.65f,
                        animationSpec = androidx.compose.animation.core.tween(
                            durationMillis = 1000,
                            easing = androidx.compose.animation.core.LinearEasing,
                        ),
                    )
                }

                RecordingStore.SliderAnimMode.Stop -> {
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

                RecordingStore.SliderAnimMode.Down -> {
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
