package com.google.mediapipe.examples.objectdetection.fragments

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
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.mediapipe.examples.objectdetection.ObjectDetectorHelper
import com.google.mediapipe.examples.objectdetection.databinding.FragmentStreamBinding
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectionResult
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
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
    private var _binding: FragmentStreamBinding? = null
    private val binding get() = _binding!!
    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private val handler = Handler(Looper.getMainLooper())
    private val detectionInterval = 300L

    private var executor = Executors.newSingleThreadExecutor()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStreamBinding.inflate(inflater, container, false)
        setupObjectDetector()
        setupMjpegView()
        setupOverlayView()
        return binding.root
    }

    private fun setupObjectDetector() {
        objectDetectorHelper = ObjectDetectorHelper(
            context = requireContext(),
            runningMode = RunningMode.LIVE_STREAM,
            objectDetectorListener = this
        )
    }

    private fun setupMjpegView() {
        binding.mjpegView.apply {
            setMode(MjpegView.MODE_FIT_WIDTH)
            setAdjustHeight(true)
            setSupportPinchZoomAndPan(true)
            setUrl("http://192.168.4.1/stream")
            startStream()
            binding.mjpegView.doOnLayout {
                binding.overlay.setPreviewLayout(
                    it.left, it.top, it.width, it.height
                )
            }

        }
        startRealTimeDetection()
    }

    private fun setupOverlayView() {
        binding.overlay.setRunningMode(RunningMode.LIVE_STREAM)
    }

    private var detectionJob: Job? = null

    private fun startRealTimeDetection() {
        detectionJob?.cancel() // pastikan hanya satu job berjalan
        detectionJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                captureAndDetect()
                delay(detectionInterval)
            }
        }
    }




    private fun captureAndDetect() {
        val localBinding = _binding ?: return
        val localActivity = activity ?: return
        if (!isAdded) return

        val bitmap = try {
            if (localBinding.mjpegView.width > 0 && localBinding.mjpegView.height > 0) {
                localBinding.mjpegView.getBitmap()?.let { frame ->
                    Bitmap.createScaledBitmap(frame, 384, 384, true)
                }
            } else null
        } catch (e: Exception) {
            Log.e("StreamFragment", "Failed to capture frame: ${e.message}")
            null
        }

        bitmap?.let {
            objectDetectorHelper.detectFromBitmap(it, getDeviceRotation())
        }
    }

    private fun getDeviceRotation(): Int {
        return when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> 90
            else -> 0
        }
    }

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

    override fun onError(error: String, errorCode: Int) {
        Log.e("StreamFragment", "Detection error: $error")
    }

    override fun onPause() {
        super.onPause()
        detectionJob?.cancel()
        binding.mjpegView.stopStream()
    }

    private val detectionRunnable = object : Runnable {
        override fun run() {
            if (isAdded && view != null) {
                captureAndDetect()
                handler.postDelayed(this, detectionInterval)
            }
        }
    }


    override fun onResume() {
        super.onResume()
        if (::objectDetectorHelper.isInitialized) {
            binding.mjpegView.startStream()
            startRealTimeDetection()
        }
    }





    override fun onDestroyView() {
        super.onDestroyView()
        detectionJob?.cancel()
        objectDetectorHelper.clearObjectDetector()
        _binding = null
    }
}

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


//class StreamFragment : Fragment() {
//
//    private var _binding: FragmentStreamBinding? = null
//    private val binding get() = _binding!!
//
//    private lateinit var interpreter: Interpreter
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentStreamBinding.inflate(inflater, container, false)
//
//        setupWebView()
//        loadModel()
//
//        binding.captureButton.setOnClickListener {
//            val bitmap = captureWebView(binding.webView)
//            bitmap?.let {
//                runModelOnBitmap(it)
//            }
//        }
//
//        return binding.root
//    }
//
//    private fun setupWebView() {
//        binding.webView.settings.javaScriptEnabled = true
//        binding.webView.webViewClient = WebViewClient()
//        binding.webView.loadUrl("http://192.168.4.1/stream") // IP streaming
//    }
//
//    private fun captureWebView(webView: WebView): Bitmap? {
//        return try {
//            val bitmap = Bitmap.createBitmap(
//                webView.width,
//                webView.height,
//                Bitmap.Config.ARGB_8888
//            )
//
//            val canvas = Canvas(bitmap)
//            webView.draw(canvas)
//            bitmap
//
//        } catch (e: Exception) {
//            Log.e("StreamFragment", "Capture failed: ${e.message}")
//            null
//        }
//    }
//
//    private fun loadModel() {
//        val fileDescriptor = requireContext().assets.openFd("efficientdet_lite2_cengkeh_V2.tflite")
//        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
//        val fileChannel = inputStream.channel
//        val startOffset = fileDescriptor.startOffset
//        val declaredLength = fileDescriptor.declaredLength
//        val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
//
//        val options = Interpreter.Options()
//        interpreter = Interpreter(modelBuffer, options)
//    }
//
//
//    private fun runModelOnBitmap(bitmap: Bitmap) {
//        val inputSize = 384 // default EfficientDet input
//        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
//        val inputBuffer = convertBitmapToByteBuffer(resizedBitmap, inputSize)
//
//        // Buat buffer output untuk semua output dari model EfficientDet-Lite2
//        val outputMap = HashMap<Int, Any>()
//        val locations = Array(1) { Array(100) { FloatArray(4) } } // bounding boxes
//        val classes = Array(1) { FloatArray(100) }                // class indexes
//        val scores = Array(1) { FloatArray(100) }                 // confidence scores
//        val detections = FloatArray(1)                           // number of detections
//
//        outputMap[0] = locations
//        outputMap[1] = classes
//        outputMap[2] = scores
//        outputMap[3] = detections
//
//        interpreter.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputMap)
//
//        Log.d("Detection", "Jumlah Deteksi: ${detections[0]}")
//        for (i in 0 until detections[0].toInt()) {
//            val box = locations[0][i]
//            val score = scores[0][i]
//            val classIndex = classes[0][i].toInt()
//            if (score > 0.5) {
//                Log.d("Detection", "Class: $classIndex, Score: $score, Box: ${box.joinToString()}")
//                // TODO: Gambar bounding box dari box (ymin, xmin, ymax, xmax)
//            }
//        }
//    }
//
//    private fun convertBitmapToByteBuffer(bitmap: Bitmap, inputSize: Int): ByteBuffer {
//        // Sesuaikan dengan kebutuhan model EfficientDet-Lite2
//        val byteBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4) // 384x384x3x4 bytes
//        byteBuffer.order(ByteOrder.nativeOrder())
//
//        val intValues = IntArray(inputSize * inputSize)
//        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
//
//        var pixel = 0
//        for (i in 0 until inputSize) {
//            for (j in 0 until inputSize) {
//                val value = intValues[pixel++]
//
//                // Normalisasi ke [-1,1] seperti di pipeline MediaPipe
//                byteBuffer.putFloat(((value shr 16 and 0xFF) / 255.0f) * 2 - 1) // R
//                byteBuffer.putFloat(((value shr 8 and 0xFF) / 255.0f) * 2 - 1)  // G
//                byteBuffer.putFloat(((value and 0xFF) / 255.0f) * 2 - 1)        // B
//            }
//        }
//        return byteBuffer
//    }
//
//
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//        interpreter.close()
//    }
//}
