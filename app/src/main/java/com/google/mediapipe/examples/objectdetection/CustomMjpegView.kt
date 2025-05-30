package com.google.mediapipe.examples.objectdetection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.net.HttpURLConnection
import java.net.URL

class CustomMjpegView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    private var drawThread: Thread? = null
    private var isStreaming = false
    private var apiKey: String = ""
    private var streamUrl: String = ""
    private var lastFrame: Bitmap? = null

    fun setStreamUrl(url: String, apiKey: String) {
        this.streamUrl = url
        this.apiKey = apiKey
    }

    fun startStream() {
        isStreaming = true
        holder.addCallback(this)
    }

    fun stopStream() {
        isStreaming = false
        drawThread?.interrupt()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        drawThread = Thread {
            try {
                val connection = URL(streamUrl).openConnection() as HttpURLConnection
                connection.setRequestProperty("X-API-Key", apiKey)
                connection.doInput = true
                connection.connect()

                val mjpegInputStream = MjpegInputStream(connection.inputStream)
                while (isStreaming) {
                    val frame = mjpegInputStream.readMjpegFrame() ?: continue

                    // Simpan frame terakhir untuk digunakan di getBitmap()
                    lastFrame = frame.copy(Bitmap.Config.ARGB_8888, false)

                    val canvas = holder.lockCanvas() ?: continue
                    canvas.drawBitmap(frame, null, Rect(0, 0, width, height), null)
                    holder.unlockCanvasAndPost(canvas)

                }

            } catch (e: Exception) {
                Log.e("MJPEG", "Streaming error: ${e.message}")
            }
        }
        drawThread?.start()
    }


    fun getBitmap(): Bitmap? {
        return lastFrame
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopStream()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
}
