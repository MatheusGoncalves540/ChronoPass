package com.chronopass.app.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.util.concurrent.Executor

@Composable
fun CameraCapture(
    modifier: Modifier = Modifier,
    onPhoto: (File) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(Unit) {
        val provider = ProcessCameraProvider.getInstance(context).await()
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
        provider.unbindAll()
        provider.bindToLifecycle(
            lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageCapture
        )
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AndroidView(factory = { previewView }, modifier = Modifier.weight(1f).fillMaxWidth())
        Button(
            onClick = { takePhoto(context, imageCapture, ContextCompat.getMainExecutor(context), onPhoto) },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) { Text("TIRAR FOTO") }
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    executor: Executor,
    onPhoto: (File) -> Unit,
) {
    val file = PhotoStore.newFile(context)
    val options = ImageCapture.OutputFileOptions.Builder(file).build()
    imageCapture.takePicture(options, executor, object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(results: ImageCapture.OutputFileResults) { onPhoto(file) }
        override fun onError(exc: ImageCaptureException) { /* ponytail: retry via button */ }
    })
}

private suspend fun <T> com.google.common.util.concurrent.ListenableFuture<T>.await(): T =
    kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        addListener({ cont.resumeWith(Result.success(get())) }, Runnable::run)
    }
