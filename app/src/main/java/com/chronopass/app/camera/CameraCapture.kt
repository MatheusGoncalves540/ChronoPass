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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CameraCapture(
        modifier: Modifier = Modifier,
        onPhoto: (File) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }
    val scope = rememberCoroutineScope()
    val providerRef = remember { arrayOfNulls<ProcessCameraProvider>(1) }

    LaunchedEffect(Unit) {
        val provider = ProcessCameraProvider.getInstance(context).await()
        providerRef[0] = provider
        val preview =
                Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        provider.unbindAll()
        // ponytail: prefer the front camera, but fall back to the back one when the
        // device (or emulator AVD) only exposes a single camera — hard-failing on
        // DEFAULT_FRONT_CAMERA crashed the app ("No available camera can be found").
        val selector =
                if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA))
                        CameraSelector.DEFAULT_FRONT_CAMERA
                else CameraSelector.DEFAULT_BACK_CAMERA
        provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
    }
    // Ao sair da composição (ex.: dialog de foto do colaborador fechado),
    // solta a câmera para não ficar ligada em segundo plano.
    DisposableEffect(Unit) { onDispose { providerRef[0]?.unbindAll() } }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AndroidView(factory = { previewView }, modifier = Modifier.weight(1f).fillMaxWidth())
        Button(
                onClick = {
                    takePhoto(
                            context,
                            imageCapture,
                            scope,
                            ContextCompat.getMainExecutor(context),
                            onPhoto
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) { Text("TIRAR FOTO") }
    }
}

private fun takePhoto(
        context: Context,
        imageCapture: ImageCapture,
        scope: CoroutineScope,
        executor: Executor,
        onPhoto: (File) -> Unit,
) {
    val raw = PhotoStore.newRawFile(context)
    val options = ImageCapture.OutputFileOptions.Builder(raw).build()
    imageCapture.takePicture(
            options,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                    scope.launch(Dispatchers.IO) {
                        val compressed = PhotoCompressor.compress(raw)
                        withContext(Dispatchers.Main) { onPhoto(compressed) }
                    }
                }
                override fun onError(exc: ImageCaptureException) {
                    /* ponytail: retry via button */
                }
            }
    )
}

private suspend fun <T> com.google.common.util.concurrent.ListenableFuture<T>.await(): T =
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            addListener({ cont.resumeWith(Result.success(get())) }, Runnable::run)
        }
