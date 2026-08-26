package com.chronopass.app.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import java.io.File
import java.io.FileOutputStream

// ponytail: Android's Bitmap.CompressFormat has no AVIF encoder (checked SDK 35/36 stubs —
// it only ever shipped JPEG/PNG/WEBP). WEBP_LOSSY is the smallest format the platform can
// encode natively; real AVIF would need a new native codec dependency (e.g. libavif via JNI).
object PhotoCompressor {
    private const val QUALITY = 80

    fun compress(raw: File): File {
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            Bitmap.CompressFormat.WEBP_LOSSY
        else
            @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
        val bitmap = BitmapFactory.decodeFile(raw.path)
        val out = File(raw.parentFile, raw.nameWithoutExtension + ".webp")
        FileOutputStream(out).use { bitmap.compress(format, QUALITY, it) }
        bitmap.recycle()
        raw.delete()
        return out
    }
}
