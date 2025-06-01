package com.google.mediapipe.examples.objectdetection

import android.content.Context
import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

fun decryptModelToTempFile(context: Context, encryptedAssetName: String): File? {
    return try {
        val ModelKey = BuildConfig.MODEL_KEY
        val key = ModelKey.toByteArray() //
        val inputStream = context.assets.open(encryptedAssetName)

        val iv = ByteArray(16)
        inputStream.read(iv)
        val ivSpec = IvParameterSpec(iv)

        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)

        val encryptedBytes = inputStream.readBytes()
        val decryptedBytes = cipher.doFinal(encryptedBytes)

        val tempFile = File.createTempFile("decrypted_model", ".tflite", context.cacheDir)
        tempFile.writeBytes(decryptedBytes)
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
