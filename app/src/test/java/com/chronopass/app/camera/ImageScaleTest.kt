package com.chronopass.app.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tamanho final da foto de batida — função pura, JUnit puro. */
class ImageScaleTest {

    @Test
    fun foto4000px_viraExatamente960() {
        assertEquals(960 to 720, targetSize(4000, 3000))
    }

    @Test
    fun retratoUsaOMaiorLado() {
        assertEquals(720 to 960, targetSize(3000, 4000))
    }

    @Test
    fun imagemMenorQueOTetoVoltaIntacta_nuncaAmplia() {
        assertEquals(800 to 600, targetSize(800, 600))
        assertEquals(960 to 960, targetSize(960, 960))
    }

    @Test
    fun tetoInvalidoNaoTrava() {
        assertEquals(4000 to 3000, targetSize(4000, 3000, 0))
    }

    @Test
    fun ladoMinimoNuncaViraZero() {
        val (w, h) = targetSize(10000, 1, 960)
        assertEquals(960, w)
        assertTrue("lado menor tem de ficar >= 1", h >= 1)
    }
}
