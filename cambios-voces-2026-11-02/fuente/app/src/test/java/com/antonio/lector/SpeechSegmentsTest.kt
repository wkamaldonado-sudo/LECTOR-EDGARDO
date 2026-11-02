package com.antonio.lector

import org.junit.Assert.*
import org.junit.Test

class SpeechSegmentsTest {
    @Test fun phrasesKeepEveryWordAndOriginalOffsets() {
        val text = "Primera frase, con una coma.\nSegunda frase: ¿dónde estamos? Tercera frase."
        val spans = SpeechSegments.split(text)
        assertTrue(spans.size >= 3)
        assertEquals(text.replace(Regex("\\s+"), " ").trim(),
            spans.joinToString(" ") { text.substring(it.start, it.end) }.replace(Regex("\\s+"), " "))
        assertTrue(spans.zipWithNext().all { (a, b) -> a.end <= b.start })
    }

    @Test fun longSentencesNeverLoseOrCutWords() {
        val text = "Este es un párrafo muy largo sin punto intermedio ".repeat(20)
        val spans = SpeechSegments.split(text, 80)
        assertTrue(spans.size > 5)
        assertTrue(spans.all { it.start == 0 || text[it.start - 1].isWhitespace() })
        assertTrue(spans.all { it.end == text.length || text[it.end].isWhitespace() })
        assertEquals(text.trim(), spans.joinToString(" ") { text.substring(it.start, it.end) })
    }

    @Test fun trimsLongSilencesButKeepsBreathingRoomAroundSpeech() {
        val input = FloatArray(3000)
        for (index in 1000 until 2000) input[index] = 0.2f
        val output = SpeechSegments.trimSilence(input, 1000)
        assertEquals(1_270, output.size)
        assertEquals(90, output.indexOfFirst { it > 0.1f })
        assertEquals(1000, output.count { it > 0.1f })
    }

    @Test fun phraseJoinsReachExactSilenceWithoutAStep() {
        val rate = 24_000
        val tone = FloatArray(rate / 2) { index ->
            0.4f * kotlin.math.sin(2.0 * Math.PI * 220.0 * index / rate).toFloat()
        }
        val first = SpeechSegments.appendPause(SpeechSegments.trimSilence(tone, rate), rate, 230)
        val second = SpeechSegments.trimSilence(tone, rate)

        assertEquals(0f, first.first(), 0f)
        assertEquals(0f, first.last(), 0f)
        assertEquals(0f, second.first(), 0f)

        val joined = first + second
        val maxStep = joined.toList().zipWithNext().maxOf { (a, b) -> kotlin.math.abs(b - a) }
        assertTrue("Salto audible en la unión de frases: $maxStep", maxStep < 0.05f)
    }

    @Test fun smallDcOffsetDoesNotLeaveAThumpBetweenPhrases() {
        val rate = 8_000
        val input = FloatArray(rate / 2) { 0.02f }
        for (index in 200 until 600) {
            input[index] = 0.02f + 0.3f * kotlin.math.sin(2.0 * Math.PI * 200.0 * index / rate).toFloat()
        }
        val output = SpeechSegments.trimSilence(input, rate)
        val tail = output.copyOfRange(
            output.size - rate * 100 / 1000,
            output.size - rate * 40 / 1000,
        )
        val residual = tail.average()
        assertTrue("Continua residual en el silencio: $residual", kotlin.math.abs(residual) < 0.002)
    }

    @Test fun sentencesGetANaturalPauseAndParagraphsGetALongerOne() {
        val text = "Primera frase. Segunda frase.\n\nTercera frase."
        val spans = SpeechSegments.split(text)
        assertEquals(3, spans.size)
        assertTrue("Pausa de frase demasiado corta: ${spans[0].pauseAfterMs}", spans[0].pauseAfterMs >= 200)
        assertTrue("Pausa de párrafo demasiado corta: ${spans[1].pauseAfterMs}", spans[1].pauseAfterMs >= 500)
        assertEquals(0, spans[2].pauseAfterMs)
    }

    @Test fun veryShortNeighbouringSentencesShareOneSynthesisRequest() {
        val text = "—Sí. —No. —Tal vez."
        val separate = SpeechSegments.split(text)
        val together = SpeechSegments.splitForSynthesis(text)
        assertEquals(3, separate.size)
        assertTrue("Las frases cortas siguen separadas: ${together.size}", together.size < separate.size)
        assertEquals(text, together.joinToString(" ") { text.substring(it.start, it.end) })
    }

    @Test fun normalSentencesKeepTheirOwnProsody() {
        val text = "La tarde caía sobre el valle y el río brillaba con la última luz. " +
            "Nadie recordaba una tarde tan quieta como aquella."
        val spans = SpeechSegments.splitForSynthesis(text)
        assertEquals(2, spans.size)
    }

    @Test fun pausesGrowFromSentenceToLineToParagraph() {
        val text = "Una frase cualquiera que termina en punto. Otra frase distinta.\n" +
            "Otra línea del mismo párrafo.\n\nOtro párrafo empieza aquí."
        val spans = SpeechSegments.split(text)
        val sentence = spans[0].pauseAfterMs
        val line = spans[1].pauseAfterMs
        val paragraph = spans[2].pauseAfterMs
        assertTrue("Pausa de frase ($sentence) no es menor que la de línea ($line)", sentence < line)
        assertTrue("Pausa de línea ($line) no es menor que la de párrafo ($paragraph)", line < paragraph)
        assertTrue("La pausa de párrafo se queda corta: $paragraph", paragraph >= 700)
    }

    @Test fun blocksCoverTheWholeTextWithoutGapsOrOverlaps() {
        val text = ("Primera frase del párrafo. Segunda frase, algo más larga, con una coma. " +
            "Tercera y última.\n\n").repeat(3)
        val spans = SpeechSegments.splitForSynthesis(text)
        assertTrue(spans.zipWithNext().all { (a, b) -> a.end <= b.start })
        assertEquals(text.replace(Regex("\\s+"), " ").trim(),
            spans.joinToString(" ") { text.substring(it.start, it.end) }.replace(Regex("\\s+"), " "))
    }

    @Test fun paragraphsAreNeverJoinedAcrossTheBreak() {
        val text = "Sí.\n\nNo."
        val spans = SpeechSegments.splitForSynthesis(text)
        assertEquals(2, spans.size)
    }

    @Test fun quietAndLoudPhrasesEndAtTheSameLevel() {
        fun speech(amplitude: Float) = FloatArray(4_000) { index ->
            if (index < 500) 0f
            else amplitude * kotlin.math.sin(2.0 * Math.PI * 180.0 * index / 8_000).toFloat()
        }
        fun rms(samples: FloatArray): Double {
            val voiced = samples.filter { kotlin.math.abs(it) > 0.005f }
            return kotlin.math.sqrt(voiced.sumOf { it.toDouble() * it } / voiced.size)
        }
        val quiet = SpeechSegments.matchLoudness(speech(0.03f))
        val loud = SpeechSegments.matchLoudness(speech(0.5f))
        assertTrue("La frase floja no subió: ${rms(quiet)}", rms(quiet) > rms(speech(0.03f)))
        assertTrue("La frase fuerte no bajó: ${rms(loud)}", rms(loud) < rms(speech(0.5f)))
        val before = kotlin.math.abs(rms(speech(0.5f)) - rms(speech(0.03f)))
        val after = kotlin.math.abs(rms(quiet) - rms(loud))
        assertTrue("La diferencia de nivel no se redujo: $before → $after", after < before * 0.75)
        assertTrue("Pico saturado: ${loud.maxOf { kotlin.math.abs(it) }}", loud.maxOf { kotlin.math.abs(it) } <= 0.97f)
    }

    @Test fun titlesDoNotSplitASentence() {
        val text = "El Sr. García llegó tarde. Después se fue."
        val spans = SpeechSegments.split(text)
        assertEquals(2, spans.size)
        assertEquals("El Sr. García llegó tarde.", text.substring(spans[0].start, spans[0].end))
    }

    @Test fun startSnapsBackToTheBeginningOfTheSentence() {
        val text = "Primera frase. Segunda frase más larga. Tercera frase."
        val insideSecond = text.indexOf("frase más")
        assertEquals(text.indexOf("Segunda"), SpeechSegments.sentenceStart(text, insideSecond))
    }

    @Test fun utteranceBlocksCoverTheWholeTextWithoutBreakingSentences() {
        val text = "Primera frase corta. " + "Segunda frase con varias palabras. ".repeat(80) +
            "Última frase."
        val blocks = SpeechSegments.splitIntoUtterances(text, 300)
        assertEquals(text, blocks.joinToString(""))
        assertTrue(blocks.all { it.length <= 300 })
    }

    @Test fun normalizedPositionsKeepPdfPageAndLine() {
        val book = ReadingText(listOf(ReadingPage(listOf("Juan 13:1-5", "Otra línea"), 4),
            ReadingPage(listOf("Página siguiente"), 5)), true)
        assertEquals(ReadingPosition(0, 1, 4), book.positionAt(book.offsetOf(0, 1)))
        assertEquals(ReadingPosition(1, 0, 5), book.positionAt(book.offsetOf(1, 0)))
    }
}
