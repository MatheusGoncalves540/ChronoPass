package com.chronopass.app.camera

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
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
        // tag do arquivo: a tela de ponto passa o uid da batida (nome único por foto).
        tag: String = java.util.UUID.randomUUID().toString(),
        onPhoto: (File) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // Captura já perto do tamanho final (1280x960 > 960 finais, folga p/ a redução): sem JPEG cheio
    // de 3-5 MB em disco. CLOSEST_HIGHER_THEN_LOWER só desce se o sensor não tiver algo acima.
    val imageCapture = remember {
        ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setResolutionSelector(
                        ResolutionSelector.Builder()
                                .setResolutionStrategy(
                                        ResolutionStrategy(
                                                Size(1280, 960),
                                                ResolutionStrategy
                                                        .FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                                        )
                                )
                                .build()
                )
                .build()
    }
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
                            tag,
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
        tag: String,
        onPhoto: (File) -> Unit,
) {
    imageCapture.takePicture(
            executor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    scope.launch(Dispatchers.IO) {
                        val out =
                                image.use {
                                    PhotoCompressor.compress(
                                            it.toBitmap(),
                                            it.imageInfo.rotationDegrees,
                                            PhotoStore.newPhotoFile(context, tag),
                                    )
                                }
                        withContext(Dispatchers.Main) { onPhoto(out) }
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
