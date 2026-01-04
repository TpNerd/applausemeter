package com.example.toshi.applausometer

import android.Manifest
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.toshi.applausometer.databinding.ActivityMainBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var recorderController: AudioRecorderController
    private lateinit var outputFileProvider: OutputFileProvider

    private var isRecording = false
    private var progress = 0
    private val stats = RecordingStats()

    private var sampleJob: Job? = null
    private var stopJob: Job? = null

    private val scoreList = ArrayList<String>()
    private lateinit var adapter: ArrayAdapter<String>
    private var num = 1

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            startRecording()
        } else {
            Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recorderController = AudioRecorderController(this)
        outputFileProvider = OutputFileProvider(this)

        applySystemBarsStyle(R.color.colorSystemBars, binding.root)
        binding.safeContainer.applySystemBarsPadding()

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, scoreList)
        binding.listView.adapter = adapter

        val animationX = AnimationUtils.loadAnimation(this, R.anim.anim_x)
        val animationStopDown = AnimationUtils.loadAnimation(applicationContext, R.anim.anim_stop_down)

        binding.textView2.startAnimation(animationX)
        binding.myImageButton.startAnimation(animationStopDown)
        binding.textView2.startAnimation(animationStopDown)

        binding.myImageButton.setOnTouchListener { v, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN && !isRecording) {
                if (hasRecordAudioPermission()) {
                    startRecording()
                } else {
                    requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
                }
                v.startAnimation(animationX)
                binding.textView2.startAnimation(animationX)
                return@setOnTouchListener true
            }
            true
        }

        binding.textView.text = "Idle"
        binding.textViewScore.text = ""
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

        progress = 0
        stats.reset()
        binding.textViewScore.text = ""
        binding.listView.visibility = View.INVISIBLE

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

        val animationStop = AnimationUtils.loadAnimation(applicationContext, R.anim.anim_stop)

        sampleJob = lifecycleScope.launch {
            while (isRecording) {
                val amp = recorderController.maxAmplitude()

                if (amp > 0) {
                    binding.meter.setImageResource(MeterLogic.drawableForAmplitude(amp))
                }

                progress += 1
                stats.addSample(amp)

                if (progress == 10) {
                    binding.myImageButton.startAnimation(animationStop)
                    binding.textView2.startAnimation(animationStop)
                }

                val df2 = DecimalFormat("00")
                val su = df2.format((amp.toFloat() / 330f))
                binding.textView.text = "Listening..."
                binding.textViewScore.text = su

                val remaining = max(0.0, 8.0 - ((progress - 1.0) / 10.0))
                val df1 = DecimalFormat("0")
                binding.textView2.text = " ${df1.format(remaining)}"

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

        val animationDown = AnimationUtils.loadAnimation(applicationContext, R.anim.anim_down)
        binding.myImageButton.startAnimation(animationDown)
        binding.textView2.startAnimation(animationDown)

        animationDown.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) = Unit
            override fun onAnimationRepeat(animation: Animation?) = Unit

            override fun onAnimationEnd(animation: Animation?) {
                val animationStopDown =
                    AnimationUtils.loadAnimation(applicationContext, R.anim.anim_stop_down)
                binding.myImageButton.startAnimation(animationStopDown)
                binding.textView2.startAnimation(animationStopDown)
                binding.textView2.text = "<3"
            }
        })

        isRecording = false
        stopJobs()

        val avg = stats.average()
        val score = MeterLogic.scoreForAverage(avg)
        binding.meter.setImageResource(MeterLogic.drawableForAmplitude(avg.toLong()))

        val dfScore = DecimalFormat("00.00")
        val scoreText = dfScore.format(score)

        if (num > 7 && adapter.count > 0) {
            adapter.remove(adapter.getItem(0))
        }

        binding.textView.text = "Idle"
        binding.textViewScore.text = scoreText
        adapter.add("${num++}: $scoreText")

        Toast.makeText(this, ": $scoreText punti!", Toast.LENGTH_LONG).show()

        progress = 0
        stats.reset()

        binding.listView.visibility = View.VISIBLE

        recorderController.stopAndRelease()
    }

    private fun stopJobs() {
        sampleJob?.cancel()
        sampleJob = null

        stopJob?.cancel()
        stopJob = null
    }

}
