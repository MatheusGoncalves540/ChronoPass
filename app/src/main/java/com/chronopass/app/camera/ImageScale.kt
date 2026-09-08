package com.chronopass.app.camera

/**
 * Fase 4 (imagens): cálculo do inSampleSize do BitmapFactory.
 *
 * Fora do PhotoCompressor de propósito — arquivo 100% JVM, nenhum import android.*, então o teto de
 * dimensão é testável em JUnit puro.
 */

/** Maior lado da foto depois do decode; acima disso ~2 MB por foto viram ~60 KB. */
const val MAX_PHOTO_DIM = 1280

/**
 * Menor potência de 2 que faz o maior lado caber em [max] (o decoder só aceita potência de 2 e
 * arredonda para baixo, então devolver 3 seria decodificar em 2). 4000px com teto 1280 -> 4.
 */
fun inSampleSize(width: Int, height: Int, max: Int = MAX_PHOTO_DIM): Int {
    if (max <= 0) return 1 // teto inválido: não subamostra (e não entra em loop)
    var sample = 1
    val maior = maxOf(width, height)
    while (maior / sample > max) sample *= 2
    return sample
}
