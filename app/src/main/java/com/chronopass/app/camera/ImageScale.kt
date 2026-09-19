package com.chronopass.app.camera

import kotlin.math.roundToInt

/**
 * Cálculo do tamanho final da foto de batida.
 *
 * Fora do PhotoCompressor de propósito — arquivo 100% JVM, nenhum import android.*, então o teto de
 * dimensão é testável em JUnit puro.
 */

// ponytail: calibração — medir em aparelho real antes de mexer. 800 corta ~30% a mais.
const val MAX_PHOTO_DIM = 960

/** Maior lado vira [max], proporção mantida; imagem já menor volta intacta (nunca amplia). */
fun targetSize(width: Int, height: Int, max: Int = MAX_PHOTO_DIM): Pair<Int, Int> {
    val maior = maxOf(width, height)
    if (max <= 0 || maior <= max) return width to height
    val f = max.toDouble() / maior
    return maxOf(1, (width * f).roundToInt()) to maxOf(1, (height * f).roundToInt())
}
