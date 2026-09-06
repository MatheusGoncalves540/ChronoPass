package com.chronopass.app.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fase 4 (imagens): teto de dimensão do decode — função pura, JUnit puro. */
class ImageScaleTest {

    @Test
    fun foto4000px_respeitaOTetoDe1280() {
        val fator = inSampleSize(4000, 3000, 1280)
        assertEquals(4, fator) // 4000/2 = 2000 ainda estoura; 4000/4 = 1000 cabe
        assertTrue("maior lado tem de caber no teto", 4000 / fator <= 1280)
    }

    @Test
    fun retratoUsaOMaiorLado() {
        assertEquals(4, inSampleSize(3000, 4000, 1280))
    }

    @Test
    fun imagemMenorQueOTetoNaoSubamostra() {
        assertEquals(1, inSampleSize(1024, 768, 1280))
        assertEquals(1, inSampleSize(1280, 1280, 1280))
    }

    @Test
    fun tetoInvalidoNaoTrava() {
        assertEquals(1, inSampleSize(4000, 3000, 0))
    }
}
