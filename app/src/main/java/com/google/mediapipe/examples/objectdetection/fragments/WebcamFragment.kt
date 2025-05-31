package com.google.mediapipe.examples.objectdetection.fragments

import android.app.AlertDialog
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.provider.MediaStore.Images.Media.getBitmap
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.mediapipe.examples.objectdetection.ObjectDetectorHelper
import com.google.mediapipe.examples.objectdetection.OverlayView
import com.google.mediapipe.examples.objectdetection.R
import com.google.mediapipe.examples.objectdetection.databinding.FragmentWebcamBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.IDeviceConnectCallBack
import com.jiangdg.ausbc.utils.ToastUtils.show
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.widget.IAspectRatio
import com.jiangdg.usb.USBMonitor
import com.jiangdg.uvc.UVCCamera
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


class WebcamFragment : Fragment(), ObjectDetectorHelper.DetectorListener {
    private lateinit var mUVCCameraView: AspectRatioTextureView
    private lateinit var mUSBMonitor: USBMonitor
    private var mUVCCamera: UVCCamera? = null

    private lateinit var overlay: OverlayView
    private lateinit var objectDetectorHelper: ObjectDetectorHelper

    private val DEFAULT_PREVIEW_WIDTH = 640
    private val DEFAULT_PREVIEW_HEIGHT = 480

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_webcam, container, false)

        // dialog how to use webcam
        AlertDialog.Builder(requireContext())
            .setTitle("Gunakan Webcam")
            .setMessage(
                "1. Pastikan webcam sudah tersambung\n\n" +
                        "2. Pastikan memilih transfer file\n\n" +
                        "3. Jika muncul dialog izin, tekan oke"
            )
            .setIcon(R.drawable.webcam)
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

        overlay = view.findViewById(R.id.overlay)
        mUVCCameraView = view.findViewById(R.id.textureView)

        objectDetectorHelper = ObjectDetectorHelper(
            context = requireContext(),
            runningMode = RunningMode.LIVE_STREAM,
            objectDetectorListener = this
        )
        overlay.setRunningMode(RunningMode.LIVE_STREAM)

        return view
    }

    private val detectionInterval = 150L
    private var detectionJob: Job? = null

    private fun startRealTimeDetection() {
//        Toast.makeText(context, "startRealTimeDetection dipanggil", Toast.LENGTH_SHORT).show()

        detectionJob?.cancel()
        detectionJob = lifecycleScope.launch {
            while (isActive) {
                detectFromWebcam()
                delay(detectionInterval)
            }
        }
    }

    private fun detectFromWebcam() {
//        Toast.makeText(context, "detectFromWebcam dipanggil", Toast.LENGTH_SHORT).show()

        val bitmap = try {
            mUVCCameraView.getBitmap()?.let { frame ->
                Bitmap.createScaledBitmap(frame, 384, 384, true)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error di detectFromWebcam: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }

        bitmap?.let {
            objectDetectorHelper.detectFromBitmap(it, getDeviceRotation())
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        Toast.makeText(context, "onViewCreated dipanggil", Toast.LENGTH_SHORT).show()

        mUSBMonitor = USBMonitor(requireContext(), object : USBMonitor.OnDeviceConnectListener {

            override fun onAttach(device: UsbDevice) {
                Toast.makeText(context, "Device attached: ${device.deviceName}", Toast.LENGTH_SHORT).show()
                mUSBMonitor.requestPermission(device)
            }

            override fun onConnect(
                device: UsbDevice?,
                controlBlock: USBMonitor.UsbControlBlock?,
                createNew: Boolean
            ) {
                activity?.runOnUiThread {
                    try {
//                        Toast.makeText(context, "onConnect: mencoba membuka kamera", Toast.LENGTH_SHORT).show()

                        mUVCCamera = UVCCamera().apply {
                            open(controlBlock)
                            setPreviewSize(DEFAULT_PREVIEW_WIDTH, DEFAULT_PREVIEW_HEIGHT, UVCCamera.FRAME_FORMAT_MJPEG)
                            setPreviewTexture(mUVCCameraView.surfaceTexture)
                            startPreview()
                        }

//                        Toast.makeText(context, "Preview dimulai", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Gagal membuka kamera: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                Toast.makeText(context, "Camera disconnected", Toast.LENGTH_SHORT).show()
                mUVCCamera?.stopPreview()
                mUVCCamera?.destroy()
                mUVCCamera = null
            }

            override fun onDetach(device: UsbDevice?) {
                Toast.makeText(context, "Webcam dicabut", Toast.LENGTH_SHORT).show()
            }

            override fun onCancel(device: UsbDevice?) {
                Toast.makeText(context, "Izin USB ditolak", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Get device rotation to pass to the detector
    private fun getDeviceRotation(): Int {
        return when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> 90
            else -> 0
        }
    }

    override fun onResults(resultBundle: ObjectDetectorHelper.ResultBundle) {
        activity?.runOnUiThread {
            try {
                val result = resultBundle.results.firstOrNull() ?: return@runOnUiThread

                overlay.setResults(
                    result,
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth,
                    resultBundle.inputImageRotation
                )
//                Toast.makeText(requireContext(), "Terdeteksi", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                // Tangkap kesalahan, tampilkan toast
                Toast.makeText(requireContext(), "Error saat proses hasil deteksi: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace() // Optional, untuk debugging jika Logcat aktif
            }
        }
    }

    override fun onError(error: String, errorCode: Int) {
        Toast.makeText(context, "Terjadi kesalahan: $error ($errorCode)", Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()
//        Toast.makeText(context, "onStart dipanggil", Toast.LENGTH_SHORT).show()
        mUSBMonitor.register()
        startRealTimeDetection()
    }

    override fun onStop() {
        super.onStop()
//        Toast.makeText(context, "onStop dipanggil", Toast.LENGTH_SHORT).show()
        mUSBMonitor.unregister()
        mUVCCamera?.stopPreview()
        mUVCCamera?.destroy()
        detectionJob?.cancel()
        mUVCCamera = null
    }
}



