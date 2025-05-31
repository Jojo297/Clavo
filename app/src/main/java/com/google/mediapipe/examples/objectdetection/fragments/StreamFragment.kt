package com.google.mediapipe.examples.objectdetection.fragments

import android.app.AlertDialog
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.mediapipe.examples.objectdetection.BuildConfig
import com.google.mediapipe.examples.objectdetection.ObjectDetectorHelper
import com.google.mediapipe.examples.objectdetection.R
import com.google.mediapipe.examples.objectdetection.databinding.FragmentStreamBinding
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectionResult
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.jiangdg.ausbc.utils.ToastUtils.show
import com.longdo.mjpegviewer.MjpegView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.concurrent.Executors

class StreamFragment : Fragment(), ObjectDetectorHelper.DetectorListener {

    // ViewBinding to access layout views
    private var _binding: FragmentStreamBinding? = null
    private val binding get() = _binding!!

    // Object detector helper for MediaPipe detection
    private lateinit var objectDetectorHelper: ObjectDetectorHelper

    // Handler and detection interval for real-time detection loop
    private val handler = Handler(Looper.getMainLooper())
    private val detectionInterval = 300L

    // Executor background
    private var executor = Executors.newSingleThreadExecutor()

    // Inflate the layout and initialize components
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStreamBinding.inflate(inflater, container, false)

        // dialog how to use
        AlertDialog.Builder(requireContext())
            .setTitle("🔌 Hubungkan Clavo Hardware")
            .setMessage(
                "1. Nyalakan perangkat Clavo Hardware\n\n" +
                        "2. Sambungkan ke jaringan WiFi bernama \"Clavo Hardware\"\n\n" +
                        "3. Kembali ke aplikasi ini untuk melanjutkan"
            )
            .setIcon(R.drawable.ic_wifi)
            .setPositiveButton("Saya Mengerti") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .create()
            .apply {
                show()
                getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.mp_primary)
                )
            }

        setupObjectDetector()
        setupMjpegView()
        setupOverlayView()
        return binding.root
    }

    // Set up the ObjectDetectorHelper in LIVE_STREAM mode
    private fun setupObjectDetector() {
        objectDetectorHelper = ObjectDetectorHelper(
            context = requireContext(),
            runningMode = RunningMode.LIVE_STREAM,
            objectDetectorListener = this
        )
    }

    // Configure the MJPEG view and start streaming
    private fun setupMjpegView() {
        val apiKey = BuildConfig.API_KEY
        val streamUrl = "http://192.168.4.1/stream"

        binding.mjpegView.apply {
            setStreamUrl(streamUrl, apiKey)
            startStream()

            doOnLayout {
                binding.overlay.setPreviewLayout(
                    it.left, it.top, it.width, it.height
                )
            }
        }

        startRealTimeDetection()
    }


    // Configure the overlay view to use LIVE_STREAM mode
    private fun setupOverlayView() {
        binding.overlay.setRunningMode(RunningMode.LIVE_STREAM)
    }

    // Coroutine job for real-time detection loop
    private var detectionJob: Job? = null

    // Start the real-time detection loop using coroutine
    private fun startRealTimeDetection() {
        detectionJob?.cancel() // pastikan hanya satu job berjalan
        detectionJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                captureAndDetect() // Capture frame and detect
                delay(detectionInterval) // Wait before next iteration
            }
        }
    }

    // Capture a frame from the MJPEG stream and run object detection
    private fun captureAndDetect() {
        val localBinding = _binding ?: return
        val localActivity = activity ?: return
        if (!isAdded) return

        val bitmap = try {
            if (localBinding.mjpegView.width > 0 && localBinding.mjpegView.height > 0) {
                localBinding.mjpegView.getBitmap()?.let { frame ->
                    // Resize frame to 384x384 (input size for the model)
                    Bitmap.createScaledBitmap(frame, 384, 384, true)
                }
            } else null
        } catch (e: Exception) {
            Log.e("StreamFragment", "Failed to capture frame: ${e.message}")
            null
        }

        // Run detection if frame was captured successfully
        bitmap?.let {
            objectDetectorHelper.detectFromBitmap(it, getDeviceRotation())
        }
    }

    // Get device rotation to pass to the detector
    private fun getDeviceRotation(): Int {
        return when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> 90
            else -> 0
        }
    }

    // Callback triggered when detection results are available
    override fun onResults(resultBundle: ObjectDetectorHelper.ResultBundle) {
        activity?.runOnUiThread {
            binding.overlay.setResults(
                resultBundle.results.firstOrNull() ?: return@runOnUiThread,
                resultBundle.inputImageHeight,
                resultBundle.inputImageWidth,
                resultBundle.inputImageRotation
            )
        }
    }

//    private val detectionRunnable = object : Runnable {
//        override fun run() {
//            if (isAdded && view != null) {
//                captureAndDetect()
//                handler.postDelayed(this, detectionInterval)
//            }
//        }
//    }

    // Callback triggered if there is an error in detection
    override fun onError(error: String, errorCode: Int) {
        Log.e("StreamFragment", "Detection error: $error")
    }

    // Stop detection and stream when fragment is paused
    override fun onPause() {
        super.onPause()
        detectionJob?.cancel()
        binding.mjpegView.stopStream()
    }

    // Resume detection and stream when fragment becomes active again
    override fun onResume() {
        super.onResume()
        if (::objectDetectorHelper.isInitialized) {
            binding.mjpegView.startStream()
            startRealTimeDetection()
        }
    }

    // Clean up resources when fragment is destroyed
    override fun onDestroyView() {
        super.onDestroyView()
        detectionJob?.cancel()
        objectDetectorHelper.clearObjectDetector()
        _binding = null
    }
}

// Extension function to capture a bitmap from MjpegView
fun MjpegView.getBitmap(): Bitmap? {
    return try {
        val bitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        this.draw(canvas)
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
