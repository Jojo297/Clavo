package com.google.mediapipe.examples.objectdetection.fragments

import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.mediapipe.examples.objectdetection.ObjectDetectorHelper

class WebcamFragment : Fragment(), ObjectDetectorHelper.DetectorListener {

    override fun onError(error: String, errorCode: Int) {
        Log.e("WebcamFragment", "Detection error: $error (code: $errorCode)")
        Toast.makeText(requireContext(), "Error: $error", Toast.LENGTH_SHORT).show()
    }

    override fun onResults(
        resultBundle: ObjectDetectorHelper.ResultBundle
    ) {

    }

}
