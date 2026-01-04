package com.example.toshi.applausometer

import android.Manifest
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.toshi.applausometer.databinding.ActivityMainBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.DecimalFormat
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var recorder: MediaRecorder? = null
    private var outputFile: String? = null

    private var isRecording = false
    private var progress = 0.0
    private var ticks = 0.0

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

        ViewCompat.setOnApplyWindowInsetsListener(binding.safeContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom,
            )
            insets
        }
        ViewCompat.requestApplyInsets(binding.safeContainer)

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

        ensureOutputFile()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopJobs()
        releaseRecorder()
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun ensureOutputFile() {
        if (outputFile != null) return

        val outDir = getExternalFilesDir(null) ?: filesDir
        outputFile = File(outDir, "recording.3gp").absolutePath
    }

    private fun startRecording() {
        if (isRecording) return

        ensureOutputFile()

        progress = 0.0
        ticks = 0.0
        binding.listView.visibility = View.INVISIBLE

        val localOutputFile = outputFile
        if (localOutputFile == null) {
            Toast.makeText(this, "Failed to prepare output file", Toast.LENGTH_LONG).show()
            return
        }

        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        recorder = r

        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            r.setOutputFile(localOutputFile)
            r.prepare()
            r.start()
        } catch (e: IllegalStateException) {
            Toast.makeText(this, "Recorder error", Toast.LENGTH_LONG).show()
            releaseRecorder()
            return
        } catch (e: IOException) {
            Toast.makeText(this, "Recorder error", Toast.LENGTH_LONG).show()
            releaseRecorder()
            return
        }

        isRecording = true
        Toast.makeText(this, "Sto Ascoltando...", Toast.LENGTH_LONG).show()

        startSamplingLoop()
        scheduleStop()
    }

    private fun startSamplingLoop() {
        stopJobs()

        val animationStop = AnimationUtils.loadAnimation(applicationContext, R.anim.anim_stop)

        sampleJob = lifecycleScope.launch {
            while (isRecording) {
                val amp = recorder?.maxAmplitude?.toLong() ?: 0L

                if (amp > 0) {
                    val drawable = when {
                        amp <= 200L -> R.drawable.lights0
                        amp <= 2400L -> R.drawable.lights1
                        amp <= 5600L -> R.drawable.lights2
                        amp <= 11800L -> R.drawable.lights3
                        amp <= 15000L -> R.drawable.lights4
                        amp <= 19200L -> R.drawable.lights5
                        amp <= 22400L -> R.drawable.lights6
                        amp <= 25600L -> R.drawable.lights7
                        amp <= 29800L -> R.drawable.lights8
                        amp <= 31800L -> R.drawable.lights9
                        else -> R.drawable.lights10
                    }
                    binding.meter.setImageResource(drawable)
                }

                progress += 1.0
                ticks += amp.toDouble()

                if (progress == 10.0) {
                    binding.myImageButton.startAnimation(animationStop)
                    binding.textView2.startAnimation(animationStop)
                }

                val df2 = DecimalFormat("00")
                val su = df2.format((amp.toFloat() / 330f))
                binding.textView.text = "sto ascoltando    $su"

                val remaining = max(0.0, 8.0 - ((progress - 1.0) / 10.0))
                val df1 = DecimalFormat("0")
                binding.textView2.text = " ${df1.format(remaining)} sec"

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
                binding.textView2.text = "<----   Click me!"
            }
        })

        isRecording = false
        stopJobs()

        val avg = if (progress > 0) ticks / progress else 0.0
        val score = ((avg / 660.0) + 50.0)

        val drawable = when {
            avg <= 200.0 -> R.drawable.lights0
            avg <= 2400.0 -> R.drawable.lights1
            avg <= 5600.0 -> R.drawable.lights2
            avg <= 11800.0 -> R.drawable.lights3
            avg <= 15000.0 -> R.drawable.lights4
            avg <= 19200.0 -> R.drawable.lights5
            avg <= 22400.0 -> R.drawable.lights6
            avg <= 25600.0 -> R.drawable.lights7
            avg <= 29800.0 -> R.drawable.lights8
            avg <= 31800.0 -> R.drawable.lights9
            else -> R.drawable.lights10
        }
        binding.meter.setImageResource(drawable)

        val dfScore = DecimalFormat("00.00")
        val scoreText = dfScore.format(score)

        if (num > 7 && adapter.count > 0) {
            adapter.remove(adapter.getItem(0))
        }

        binding.textView.text = "Applause!            $scoreText!"
        adapter.add("${num++}: $scoreText")

        Toast.makeText(this, "Ha totalizzato $scoreText punti!", Toast.LENGTH_LONG).show()

        progress = 0.0
        ticks = 0.0

        binding.listView.visibility = View.VISIBLE

        releaseRecorder()
    }

    private fun stopJobs() {
        sampleJob?.cancel()
        sampleJob = null

        stopJob?.cancel()
        stopJob = null
    }

    private fun releaseRecorder() {
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
