package com.google.mediapipe.examples.objectdetection.fragments

import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.mediapipe.examples.objectdetection.R
import com.google.mediapipe.examples.objectdetection.databinding.FragmentWebcamBinding
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.IDeviceConnectCallBack
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.widget.IAspectRatio
import com.jiangdg.usb.USBMonitor
import com.jiangdg.uvc.UVCCamera


class WebcamFragment : Fragment() {
    private lateinit var mUVCCameraView: AspectRatioTextureView
    private lateinit var mUSBMonitor: USBMonitor
    private var mUVCCamera: UVCCamera? = null

    // resulotion default webcam
    private val DEFAULT_PREVIEW_WIDTH = 640
    private val DEFAULT_PREVIEW_HEIGHT = 480

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_webcam, container, false)
        mUVCCameraView = view.findViewById(R.id.textureView)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mUSBMonitor = USBMonitor(requireContext(), object : USBMonitor.OnDeviceConnectListener {

            override fun onAttach(device: UsbDevice) {
                Log.d("Webcam", "Device attached: ${device.deviceName}")
                mUSBMonitor.requestPermission(device)
            }

            override fun onConnect(
                device: UsbDevice?,
                controlBlock: USBMonitor.UsbControlBlock?,
                createNew: Boolean
            ) {
                activity?.runOnUiThread {
                    try {
                        mUVCCamera = UVCCamera().apply {
                            open(controlBlock)
                                setPreviewSize(DEFAULT_PREVIEW_WIDTH, DEFAULT_PREVIEW_HEIGHT,
                                    UVCCamera.FRAME_FORMAT_MJPEG)
                                setPreviewTexture(mUVCCameraView.surfaceTexture)
                                startPreview()
                                Log.d("Webcam", "Preview started with MJPEG")
                        }
                    } catch (e: Exception) {
                        Log.e("Webcam", "Failed to start camera", e)
                        Toast.makeText(context, "Failed to start camera: ${e.message}",
                            Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                mUVCCamera?.stopPreview()
                mUVCCamera?.destroy()
                mUVCCamera = null
                Log.d("Webcam", "Camera disconnected")
            }

            override fun onDetach(device: UsbDevice?) {
                Log.d("Webcam", "Device detached")
                Toast.makeText(context, "Webcam dicabut", Toast.LENGTH_SHORT).show()
            }

            override fun onCancel(device: UsbDevice?) {
                Log.d("Webcam", "Permission cancelled")
                Toast.makeText(context, "Izin USB ditolak", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        mUSBMonitor.register()
        Log.d("Webcam", "USBMonitor registered")
    }

    override fun onStop() {
        super.onStop()
        mUSBMonitor.unregister()
        mUVCCamera?.stopPreview()
        mUVCCamera?.destroy()
        mUVCCamera = null
        Log.d("Webcam", "USBMonitor unregistered")
    }
}


