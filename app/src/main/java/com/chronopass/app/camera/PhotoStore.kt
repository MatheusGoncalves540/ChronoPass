package com.chronopass.app.camera

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ponytail: photos live in app-private files/punches, never the public gallery.
object PhotoStore {
    fun dir(context: Context): File =
        File(context.filesDir, "punches").apply { mkdirs() }

    // ponytail: CameraX only captures straight to JPEG; PhotoCompressor re-encodes this and deletes it.
    fun newRawFile(context: Context): File {
        val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        return File(dir(context), "$stamp.jpg")
    }
}
