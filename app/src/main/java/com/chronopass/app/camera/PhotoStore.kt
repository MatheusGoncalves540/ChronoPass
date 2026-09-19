package com.chronopass.app.camera

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ponytail: photos live in app-private files/punches, never the public gallery.
object PhotoStore {
    fun dir(context: Context): File = File(context.filesDir, "punches").apply { mkdirs() }

    // tag = uid do ponto: o carimbo de tempo tem resolução de SEGUNDOS e duas batidas no mesmo
    // segundo se sobrescreviam. O arquivo já nasce final (WebP comprimido).
    fun newPhotoFile(context: Context, tag: String): File {
        val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        return File(dir(context), "${stamp}_$tag.webp")
    }

    fun employeeDir(context: Context): File = File(context.filesDir, "employees").apply { mkdirs() }

    // Moves a captured photo (already compressed) into the employee photo area. Mantém o nome
    // capturado (carimbo + tag única) — dois cadastros no mesmo segundo não colidem.
    fun saveEmployeePhoto(context: Context, captured: File): File {
        val out = File(employeeDir(context), "emp_${captured.nameWithoutExtension}.webp")
        captured.copyTo(out, overwrite = true)
        captured.delete()
        return out
    }
}
