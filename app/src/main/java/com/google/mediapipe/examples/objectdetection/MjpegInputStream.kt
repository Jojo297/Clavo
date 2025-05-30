package com.google.mediapipe.examples.objectdetection

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.InputStream

class MjpegInputStream(inputStream: InputStream) : DataInputStream(BufferedInputStream(inputStream)) {
    fun readMjpegFrame(): Bitmap? {
        mark(4096)
        val header = ByteArray(4096)
        val headerLen = read(header)

        val boundaryIndex = header.indexOfSequence("\r\n\r\n".toByteArray())
        if (boundaryIndex == -1) return null

        reset()
        skipBytes(boundaryIndex + 4)

        val contentLength = header.decodeToString()
            .lines().firstOrNull { it.startsWith("Content-Length") }
            ?.substringAfter(":")?.trim()?.toIntOrNull() ?: return null

        val frameData = ByteArray(contentLength)
        readFully(frameData)

        return BitmapFactory.decodeByteArray(frameData, 0, contentLength)
    }

    private fun ByteArray.indexOfSequence(sequence: ByteArray): Int {
        outer@ for (i in 0 until size - sequence.size) {
            for (j in sequence.indices) {
                if (this[i + j] != sequence[j]) continue@outer
            }
            return i
        }
        return -1
    }
}

