package com.antonio.lector

import java.text.BreakIterator
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.tanh

data class SpeechSpan(
    val start: Int,
    val end: Int,
    /** Silence to keep after this phrase, before the next one starts. */
    val pauseAfterMs: Int = 0,
)

object SpeechSegments {
    /** The native Supertonic chunker works best with fragments of up to 300 characters. */
    const val DEFAULT_MAXIMUM = 300

    /** A partir de esta pausa se considera que empieza otro párrafo. */
    private const val PARAGRAPH_PAUSE = 700
    /** Frases por debajo de esta longitud se pueden sintetizar junto a su vecina. */
    private const val SHORT_SENTENCE = 45
    private const val SHORT_MERGE_LIMIT = 170

    private val spanish = Locale("es", "ES")

    /** Titles that BreakIterator mistakes for the end of a sentence ("El Sr. García"). */
    private val abbreviations = setOf(
        "sr", "sra", "srta", "dr", "dra", "prof", "profa",
        "ud", "uds", "vd", "vds", "d", "dña",
    )

    private val closingMarks = setOf('"', '\u00bb', '\u201d', '\u2019', '\'', ')', ']')

    /**
     * Splits the text into complete phrases. Each phrase keeps its original offsets and
     * carries the pause that should follow it. Long sentences are cut only at a clause
     * mark or a word boundary, never in the middle of a word.
     */
    fun split(text: String, maximum: Int = DEFAULT_MAXIMUM): List<SpeechSpan> {
        require(maximum > 0)
        if (text.isBlank()) return emptyList()

        val limits = boundaries(text)
        val result = ArrayList<SpeechSpan>()
        for (index in 0 until limits.size - 1) {
            var start = limits[index]
            val sentenceEnd = limits[index + 1]
            while (start < sentenceEnd && text[start].isWhitespace()) start++
            var end = sentenceEnd
            while (end > start && text[end - 1].isWhitespace()) end--
            if (start >= end) continue

            val chunks = splitLongSentence(text, start, end, maximum)
            val sentencePause = pauseAfter(text, start, end)
            chunks.forEachIndexed { chunkIndex, chunk ->
                var chunkStart = chunk.first
                var chunkEnd = chunk.second
                while (chunkStart < chunkEnd && text[chunkStart].isWhitespace()) chunkStart++
                while (chunkEnd > chunkStart && text[chunkEnd - 1].isWhitespace()) chunkEnd--
                if (chunkStart >= chunkEnd) return@forEachIndexed
                val pause = if (chunkIndex == chunks.lastIndex) sentencePause
                else clausePause(text, chunkEnd)
                result.add(SpeechSpan(chunkStart, chunkEnd, pause))
            }
        }
        return result
    }

    /**
     * Une frases muy cortas del mismo párrafo antes de sintetizarlas. El motor
     * cierra la entonación en cada petición, así que dos frases de cuatro palabras
     * generadas por separado suenan a dos locuciones pegadas: tono final en cada
     * una y una pausa fija en medio. Al sintetizarlas juntas el modelo decide la
     * pausa y la melodía de la segunda a partir de la primera, que es lo que hace
     * un lector real.
     *
     * No se unen frases normales aunque quepan en un bloque: el lector subraya la
     * frase que suena a partir de la marca de cada petición, y un bloque de dos
     * frases dejaría el subrayado quieto durante ocho segundos.
     */
    fun splitForSynthesis(text: String, maximum: Int = DEFAULT_MAXIMUM): List<SpeechSpan> {
        val spans = split(text, maximum)
        if (spans.size < 2) return spans
        val merged = ArrayList<SpeechSpan>(spans.size)
        var current = spans.first()
        for (index in 1 until spans.size) {
            val next = spans[index]
            val sameParagraph = current.pauseAfterMs in 1 until PARAGRAPH_PAUSE
            val shortEnough = current.end - current.start <= SHORT_SENTENCE &&
                next.end - next.start <= SHORT_SENTENCE
            val fits = next.end - current.start <= SHORT_MERGE_LIMIT
            if (sameParagraph && shortEnough && fits) {
                current = SpeechSpan(current.start, next.end, next.pauseAfterMs)
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        return merged
    }

    /**
     * Sentence starts, with obvious abbreviations joined to the following word.
     * Always begins at 0 and ends at text.length.
     */
    fun boundaries(text: String): IntArray {
        if (text.isEmpty()) return intArrayOf(0)
        val iterator = BreakIterator.getSentenceInstance(spanish).apply { setText(text) }
        val raw = ArrayList<Int>()
        var boundary = iterator.first()
        while (boundary != BreakIterator.DONE) {
            if (raw.isEmpty() || raw.last() != boundary) raw.add(boundary)
            boundary = iterator.next()
        }
        if (raw.isEmpty()) raw.add(0)
        if (raw.first() != 0) raw.add(0, 0)
        if (raw.last() != text.length) raw.add(text.length)
        if (raw.size <= 2) return raw.toIntArray()

        val merged = ArrayList<Int>(raw.size)
        merged.add(raw.first())
        for (index in 1 until raw.lastIndex) {
            val fragment = text.substring(raw[index - 1], raw[index]).trimEnd()
            val next = text.substring(raw[index], raw[index + 1]).trimStart()
            val lastWord = fragment.takeLastWhile { !it.isWhitespace() }
                .trim { it == '.' || it == ':' || it == ';' || it == ',' ||
                    it == '"' || it == '\u00bb' || it == '\u201d' || it == ')' || it == ']' }
                .lowercase(spanish)
            val nextLetter = next.firstOrNull { it.isLetter() }
            val merge = lastWord in abbreviations || nextLetter?.isLowerCase() == true
            if (!merge) merged.add(raw[index])
        }
        merged.add(raw.last())
        return merged.toIntArray()
    }

    /** Moves [offset] back to the start of its sentence so the first phrase is not cut. */
    fun sentenceStart(text: String, offset: Int, maxBacktrack: Int = 1_200): Int {
        val target = offset.coerceIn(0, text.length)
        if (target == 0 || text.isEmpty()) return 0
        var start = 0
        for (boundary in boundaries(text)) {
            if (boundary > target) break
            start = boundary
        }
        return if (target - start <= maxBacktrack) start else target
    }

    /**
     * Groups sentences into large TTS blocks. Blocks are contiguous substrings of
     * [text], so progress and phrase offsets stay exact. A block never starts or ends
     * in the middle of a sentence unless that single sentence is longer than the limit.
     */
    fun splitIntoUtterances(text: String, maximum: Int): List<String> {
        require(maximum > 0)
        if (text.isBlank()) return emptyList()

        val limits = boundaries(text)
        val result = ArrayList<String>()
        var partStart = 0
        var index = 0
        while (index < limits.size - 1) {
            val sentenceStart = limits[index]
            val sentenceEnd = limits[index + 1]
            when {
                sentenceEnd - partStart <= maximum -> index++
                partStart < sentenceStart -> {
                    result.add(text.substring(partStart, sentenceStart))
                    partStart = sentenceStart
                }
                else -> {
                    for (piece in splitLongSentence(text, sentenceStart, sentenceEnd, maximum)) {
                        result.add(text.substring(piece.first, piece.second))
                    }
                    partStart = sentenceEnd
                    index++
                }
            }
        }
        if (partStart < text.length) result.add(text.substring(partStart))
        return result
    }

    /** Contiguous ranges covering [start, end), cut at clauses or words. */
    private fun splitLongSentence(
        text: String,
        start: Int,
        end: Int,
        maximum: Int,
    ): List<Pair<Int, Int>> {
        if (end - start <= maximum) return listOf(start to end)
        val pieces = ArrayList<Pair<Int, Int>>()
        var cursor = start
        while (cursor < end) {
            val limit = minOf(cursor + maximum, end)
            if (limit >= end) {
                pieces.add(cursor to end)
                break
            }
            val minimum = cursor + maximum / 3
            var cut = -1
            for (i in limit - 1 downTo minimum) {
                if (text[i] == ',' || text[i] == ';' || text[i] == ':' ||
                    text[i] == '\u2014' || text[i] == '\u2013') {
                    cut = i + 1
                    break
                }
            }
            if (cut < 0) {
                for (i in limit downTo minimum) {
                    if (text[i].isWhitespace()) {
                        cut = i + 1
                        break
                    }
                }
            }
            if (cut <= cursor) {
                cut = (limit until end).firstOrNull { text[it].isWhitespace() }?.plus(1) ?: limit
            }
            if (cut <= cursor) cut = minOf(cursor + maximum, end)
            pieces.add(cursor to cut)
            cursor = cut
        }
        return pieces
    }

    /**
     * Natural pause after a sentence, taking page and paragraph breaks into
     * account. La duración crece con la frase: nadie respira igual después de
     * cuatro palabras que después de dos líneas, y una pausa fija para todo es
     * una de las cosas que más delata a una voz sintética.
     */
    private fun pauseAfter(text: String, start: Int, end: Int): Int {
        if (end >= text.length) return 0
        var next = end
        var newlines = 0
        while (next < text.length && text[next].isWhitespace()) {
            if (text[next] == '\n') newlines++
            next++
        }
        if (next >= text.length) return 0

        var pause = when {
            newlines >= 2 -> 720
            newlines == 1 -> {
                val lineStart = text.lastIndexOf('\n', end - 1).let { if (it < 0) 0 else it + 1 }
                if (end - lineStart <= 60) 520 else 340
            }
            else -> 300
        }
        pause += ((end - start) / 28).coerceIn(0, 90)

        var punctuation = end - 1
        while (punctuation > 0 && text[punctuation] in closingMarks) punctuation--
        when (text[punctuation]) {
            '?', '!' -> pause += 40
            '\u2026' -> pause += 90
            '.' -> if (punctuation >= 2 && text[punctuation - 1] == '.' &&
                text[punctuation - 2] == '.') pause += 90
        }
        return pause.coerceIn(0, 1_000)
    }

    /** Shorter pause when a long sentence has to be cut at a comma or a clause. */
    private fun clausePause(text: String, end: Int): Int {
        var punctuation = end - 1
        while (punctuation > 0 && text[punctuation].isWhitespace()) punctuation--
        return when (text[punctuation]) {
            ',', ';', ':' -> 120
            '\u2014', '\u2013' -> 160
            else -> 80
        }
    }

    /**
     * Removes only extreme silence, keeping the attack and the natural tail of the
     * voice. Both ends are ramped to exact zero with a raised-cosine curve: a hard
     * cut, or a cut over a signal that still has a small DC offset, is heard as a
     * click every time two phrases are joined.
     */
    fun trimSilence(
        samples: FloatArray,
        rate: Int,
        // 90 ms de aire antes del ataque y 180 ms de cola: el motor recorta la
        // respiración natural de la frase y sin ese margen cada una empieza y
        // acaba en seco, como pegada a la anterior.
        leadMs: Int = 90,
        tailMs: Int = 180,
        fadeInMs: Int = 20,
        fadeOutMs: Int = 30,
    ): FloatArray {
        if (samples.isEmpty()) return samples
        // El desplazamiento de continua se mide antes de buscar el silencio: si
        // no, una continua de 0,01 hace que ningún sample parezca silencio y la
        // frase acaba cortada en seco por el final real del audio.
        val offset = smallOffset(samples)

        val first = samples.indexOfFirst { abs(it - offset) > 0.003f }
        if (first < 0) {
            return FloatArray(minOf(samples.size, rate * 80 / 1000).coerceAtLeast(0))
        }
        val last = samples.indexOfLast { abs(it - offset) > 0.003f }
        val start = (first - rate * leadMs / 1000).coerceAtLeast(0)
        val end = (last + 1 + rate * tailMs / 1000).coerceAtMost(samples.size)
        val output = samples.copyOfRange(start, end)
        if (offset != 0f) {
            for (index in output.indices) output[index] -= offset
        }

        softLimit(output)
        rampEdges(output, rate, fadeInMs, fadeOutMs)
        return output
    }

    /**
     * Un vocoder neural deja a veces una continua de pocas milésimas. Al pasar al
     * silencio insertado entre frases esa continua se convierte en un escalón y
     * suena como un chasquido. Solo se corrige cuando es pequeña: una media
     * grande es contenido real de la voz, no un desplazamiento de continua.
     */
    private fun smallOffset(samples: FloatArray): Float {
        if (samples.isEmpty()) return 0f
        var total = 0.0
        for (sample in samples) total += sample
        val mean = (total / samples.size).toFloat()
        return if (abs(mean) > 0.0005f && abs(mean) < 0.05f) mean else 0f
    }

    /**
     * Limita los picos con una rodilla suave en vez de recortarlos. El recorte
     * duro de las sílabas fuertes se oye como un crujido que se repite en cada
     * unión de frases.
     */
    private fun softLimit(samples: FloatArray, knee: Float = 0.9f) {
        for (index in samples.indices) {
            val value = samples[index]
            val magnitude = abs(value)
            if (magnitude <= knee) continue
            val over = (magnitude - knee) / (1f - knee)
            val limited = knee + (1f - knee) * tanh(over)
            samples[index] = if (value < 0f) -limited else limited
        }
    }

    /**
     * Iguala el nivel de cada frase. El motor sintetiza cada fragmento por
     * separado y el nivel de salida oscila unas décimas de decibelio entre uno y
     * otro; en una lectura larga eso se oye como un volumen que sube y baja en
     * cada frase. Se corrige hacia un nivel de referencia con un margen máximo de
     * ±4 dB y sin dejar que ningún pico llegue a saturar.
     */
    fun matchLoudness(
        samples: FloatArray,
        targetRms: Float = 0.10f,
        maxGainDb: Float = 4f,
        ceiling: Float = 0.97f,
    ): FloatArray {
        if (samples.isEmpty()) return samples
        var energy = 0.0
        var voiced = 0
        var peak = 0f
        for (sample in samples) {
            val magnitude = abs(sample)
            if (magnitude > peak) peak = magnitude
            if (magnitude > 0.005f) {
                energy += sample.toDouble() * sample
                voiced++
            }
        }
        // Menos del 5 % de muestras con voz: es una pausa, no una frase.
        if (voiced < (samples.size / 20).coerceAtLeast(1) || peak <= 1e-4f) return samples
        val rms = sqrt(energy / voiced).toFloat()
        if (rms <= 1e-4f) return samples
        val limit = 10f.pow(maxGainDb / 20f)
        var gain = (targetRms / rms).coerceIn(1f / limit, limit)
        if (peak * gain > ceiling) gain = ceiling / peak
        if (abs(gain - 1f) < 0.02f) return samples
        for (index in samples.indices) samples[index] *= gain
        return samples
    }

    /**
     * Rampa de entrada y salida hasta cero exacto (smoothstep, sin esquinas).
     * Veinte milisegundos de subida caben dentro del margen que se conserva
     * antes de la primera sílaba, así que no tocan el ataque de la voz.
     */
    private fun rampEdges(samples: FloatArray, rate: Int, fadeInMs: Int, fadeOutMs: Int) {
        if (samples.size < 2 || rate <= 0) return
        val half = samples.size / 2
        val fadeIn = (rate.toLong() * fadeInMs / 1000L).toInt().coerceIn(1, half)
        val fadeOut = (rate.toLong() * fadeOutMs / 1000L).toInt().coerceIn(1, half)
        for (index in 0 until fadeIn) {
            val t = index.toFloat() / fadeIn
            samples[index] *= t * t * (3f - 2f * t)
        }
        for (index in 0 until fadeOut) {
            val t = index.toFloat() / fadeOut
            samples[samples.size - 1 - index] *= t * t * (3f - 2f * t)
        }
    }

    /** Appends real silence so the next phrase does not start on top of this one. */
    fun appendPause(samples: FloatArray, rate: Int, pauseMs: Int): FloatArray {
        if (samples.isEmpty() || pauseMs <= 0 || rate <= 0) return samples
        val count = (rate.toLong() * pauseMs / 1000L).toInt()
        if (count <= 0) return samples
        return samples.copyOf(samples.size + count)
    }
}
