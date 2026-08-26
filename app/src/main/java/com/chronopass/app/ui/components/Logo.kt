package com.chronopass.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

// Logo configurada em tempo de build: coloque a imagem em app/src/main/assets/logo.png
// e recompile. Opcional — se o arquivo não existir, o app e o PDF simplesmente não
// mostram logo (sem erro). Ver assets/LEIA-ME.txt.
object LogoAsset {
    private const val NAME = "logo.png"

    fun bitmap(context: Context): Bitmap? = runCatching {
        context.assets.open(NAME).use { BitmapFactory.decodeStream(it) }
    }.getOrNull()
}

@Composable
fun rememberLogo(context: Context): ImageBitmap? =
    remember { LogoAsset.bitmap(context)?.asImageBitmap() }
