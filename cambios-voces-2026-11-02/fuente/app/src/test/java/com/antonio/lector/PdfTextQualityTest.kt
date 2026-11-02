package com.antonio.lector

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfTextQualityTest {

    /** Envuelve texto plano en líneas con una caja geométrica irrelevante aquí. */
    private fun page(vararg lines: String): List<PdfTextLine> = lines.map { text ->
        PdfTextLine(text, PdfWordBox(text, 0, 0f, 0f, 0f, 0f, 600f, 800f))
    }

    @Test
    fun realSpanishTextIsReadable() {
        val bookPage = page(
            "El faro de la costa seguía encendido cuando llegamos al puerto.",
            "—¿Otra vez tarde? —preguntó ella, sin levantar la vista del libro.",
        )
        assertFalse(PdfTextQuality.needsOcr(bookPage))
        assertFalse(PdfTextQuality.needsOcr("Página 12 — Capítulo IV: «La travesía» (1947)."))
    }

    @Test
    fun brokenTextLayerIsDetected() {
        // Salida real de un PDF con la tabla ToUnicode dañada.
        val mojibake = "O杰. わルでト。 Kelloトでイエパケで。 えうのわかるペタこの。 " +
            "ノレイアロケトアスエチルダペタルラエリケキラトラアト。"
        assertTrue(PdfTextQuality.needsOcr(mojibake))
    }

    @Test
    fun privateUseAndReplacementCharactersAreAlwaysJunk() {
        assertTrue(PdfTextQuality.needsOcr("\uE001\uE002"))
        assertTrue(PdfTextQuality.needsOcr("abc\uFFFD"))
    }

    @Test
    fun emptyPageNeedsOcr() {
        assertTrue(PdfTextQuality.needsOcr(page()))
        assertTrue(PdfTextQuality.needsOcr("   \n "))
    }

    @Test
    fun shortPageWithOnlyForeignLettersNeedsOcr() {
        assertTrue(PdfTextQuality.needsOcr("わルでト"))
    }

    @Test
    fun numbersAndPunctuationAloneAreFine() {
        assertFalse(PdfTextQuality.needsOcr("12 34 56 — 78,9 % (pág. 3)"))
    }
}
