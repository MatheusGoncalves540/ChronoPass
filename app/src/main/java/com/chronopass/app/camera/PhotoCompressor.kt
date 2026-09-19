package com.chronopass.app.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import java.io.File
import java.io.FileOutputStream

// ponytail: Android's Bitmap.CompressFormat has no AVIF encoder (checked SDK 35/36 stubs —
// it only ever shipped JPEG/PNG/WEBP). WEBP_LOSSY is the smallest format the platform can
// encode natively; real AVIF would need a new native codec dependency (e.g. libavif via JNI).
object PhotoCompressor {
    // ponytail: calibração — q70 é onde o WebP para de ganhar bytes visivelmente.
    private const val QUALITY = 70

    /**
     * Escala (maior lado = MAX_PHOTO_DIM) e aplica [rotationDegrees] numa matriz só — uma alocação.
     * `filter = true` é bilinear: basta porque a câmera já entrega ~1280px (redução ~1,33×);
     * se a captura voltar a ser cheia do sensor, trocar por escala em passos.
     */
    fun compress(src: Bitmap, rotationDegrees: Int, out: File): File {
        val format =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY
                else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
        val (w, h) = targetSize(src.width, src.height)
        val m =
                Matrix().apply {
                    postScale(w / src.width.toFloat(), h / src.height.toFloat())
                    postRotate(rotationDegrees.toFloat())
                }
        val img = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
        FileOutputStream(out).use { img.compress(format, QUALITY, it) }
        if (img !== src) img.recycle()
        src.recycle()
        return out
    }
}
