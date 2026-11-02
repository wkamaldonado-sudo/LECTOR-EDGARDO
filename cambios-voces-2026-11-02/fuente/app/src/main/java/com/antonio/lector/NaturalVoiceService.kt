package com.antonio.lector

import android.os.Build
import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import android.util.Log
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsSupertonicModelConfig
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import java.util.ArrayDeque
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/** Bundled Spanish synthesis. No account, downloads or network requests at runtime. */
class NaturalVoiceService : TextToSpeechService() {
    private val stopGeneration = AtomicLong()
    private val generator = Executors.newSingleThreadExecutor()
    @Volatile private var activeJob: Future<*>? = null
    private var model: OfflineTts? = null

    private companion object {
        /**
         * Supertonic es un modelo de flow matching: con los cinco pasos por defecto
         * el ruido residual del latente sigue presente y se oye como un timbre
         * metálico. Ocho es el valor del ejemplo oficial de sherpa-onnx y el punto
         * donde el timbre ya está limpio sin que la generación deje de ir por delante
         * de la reproducción. Catorce pasos suenan algo mejor, pero en el Pixel 7 la
         * síntesis pasa de 0,65× a 1,3× el tiempo real y el AudioTrack se queda sin
         * datos entre frases (que es justo el corte que hay que evitar).
         */
        const val STEPS = 8

        /**
         * Dos hilos. Medido en el Pixel 7: con cuatro el motor va un 40 % más lento,
         * porque el planificador reparte el trabajo entre núcleos grandes y pequeños.
         */
        val THREADS: Int = 2

        /**
         * El modelo declara 44100 Hz en `supertonic/tts.json` (`ae.sample_rate`) y
         * sherpa-onnx devuelve ese mismo valor en `audio.sampleRate`. Se declara al
         * framework tal cual: el AudioTrack se abre a la tasa nativa del modelo, así
         * que no hay remuestreo por interpolación en medio. Solo se usa como valor
         * de respaldo cuando no llegó ningún paquete de audio.
         */
        const val MODEL_RATE = 44_100
    }

    override fun onGetLanguage(): Array<String> = arrayOf("spa", "ESP", "")

    override fun onIsLanguageAvailable(lang: String, country: String, variant: String): Int =
        if (lang == "es" || lang == "spa") TextToSpeech.LANG_COUNTRY_AVAILABLE
        else TextToSpeech.LANG_NOT_SUPPORTED

    override fun onLoadLanguage(lang: String, country: String, variant: String): Int =
        onIsLanguageAvailable(lang, country, variant)

    override fun onGetVoices(): List<Voice> = NaturalVoices.all.map {
        Voice(it.id, Locale("es", "ES"), Voice.QUALITY_HIGH, Voice.LATENCY_NORMAL, false, emptySet())
    }

    override fun onGetDefaultVoiceNameFor(lang: String, country: String, variant: String): String? =
        if (onIsLanguageAvailable(lang, country, variant) >= 0) NaturalVoices.all.first().id else null

    override fun onIsValidVoiceName(voiceName: String): Int =
        if (NaturalVoices.all.any { it.id == voiceName }) TextToSpeech.SUCCESS else TextToSpeech.ERROR

    override fun onLoadVoice(voiceName: String): Int = onIsValidVoiceName(voiceName)

    override fun onStop() { stopGeneration.incrementAndGet(); activeJob?.cancel(true) }

    private fun loadModel(): OfflineTts = model ?: OfflineTts(
        assetManager = assets,
        config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                supertonic = OfflineTtsSupertonicModelConfig(
                    durationPredictor = "supertonic/duration_predictor.int8.onnx",
                    textEncoder = "supertonic/text_encoder.int8.onnx",
                    vectorEstimator = "supertonic/vector_estimator.int8.onnx",
                    vocoder = "supertonic/vocoder.int8.onnx",
                    ttsJson = "supertonic/tts.json",
                    unicodeIndexer = "supertonic/unicode_indexer.bin",
                    voiceStyle = "supertonic/voice.bin",
                ),
                numThreads = THREADS,
            ),
            maxNumSentences = 1,
        ),
    ).also { model = it }

    private sealed interface Packet {
        data class Audio(val span: SpeechSpan, val samples: FloatArray, val rate: Int) : Packet
        data class Failure(val cause: Throwable) : Packet
        data object End : Packet
    }

    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        val generation = stopGeneration.get()
        val text = request.charSequenceText.toString()
        val voice = NaturalVoices.all.firstOrNull { it.id == request.voiceName } ?: NaturalVoices.all.first()
        val speechRate = (request.speechRate / 100f).coerceIn(0.55f, 1.8f)
        // Seis frases de colchón: si el motor se retrasa un instante, el
        // AudioTrack sigue teniendo datos y no aparece un corte entre frases.
        val queue = ArrayBlockingQueue<Packet>(6)
        fun publish(packet: Packet) {
            while (generation == stopGeneration.get() && !Thread.currentThread().isInterrupted) {
                if (queue.offer(packet, 100, TimeUnit.MILLISECONDS)) return
            }
        }
        activeJob = generator.submit {
            try {
                val tts = loadModel()
                for (span in SpeechSegments.splitForSynthesis(text)) {
                    if (generation != stopGeneration.get() || Thread.currentThread().isInterrupted) break
                    val startedAt = System.nanoTime()
                    val audio = tts.generateWithConfigAndCallback(
                        text = text.substring(span.start, span.end).replace(Regex("\\s+"), " "),
                        config = GenerationConfig(
                            sid = voice.speaker,
                            speed = speechRate,
                            numSteps = STEPS,
                            // 1.0 respeta las pausas que el propio modelo marca en
                            // las comas: comprimirlas aplana el fraseo y suena leído.
                            silenceScale = 1.0f,
                            extra = mapOf("lang" to "es"),
                        ),
                    ) { if (generation == stopGeneration.get()) 1 else 0 }
                    if (audio.samples.isEmpty()) continue
                    val elapsed = (System.nanoTime() - startedAt) / 1_000_000
                    val audioMs = audio.samples.size * 1000L / audio.sampleRate.coerceAtLeast(1)
                    Log.d("NaturalVoice", "frase ${span.end - span.start} car · $audioMs ms audio · $elapsed ms · rtf " +
                        String.format(Locale.US, "%.2f", elapsed.toFloat() / audioMs.coerceAtLeast(1)))
                    val trimmed = SpeechSegments.matchLoudness(
                        SpeechSegments.trimSilence(audio.samples, audio.sampleRate),
                    )
                    val pause = (span.pauseAfterMs / speechRate).toInt().coerceIn(0, 1_200)
                    val samples = SpeechSegments.appendPause(trimmed, audio.sampleRate, pause)
                    publish(Packet.Audio(span, samples, audio.sampleRate))
                }
                publish(Packet.End)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (e: Throwable) {
                if (generation == stopGeneration.get()) {
                    Log.e("NaturalVoice", "Error al generar la voz", e)
                    publish(Packet.Failure(e))
                }
            }
        }
        // Arrancar con una frase de margen: si el primer paquete se escribía en
        // cuanto llegaba, el AudioTrack se quedaba sin datos antes de la segunda
        // frase y al reanudarse sonaba un chasquido con corte. El margen máximo
        // de espera es corto para no retrasar la primera palabra.
        val buffered = ArrayDeque<Packet>()
        val warmupDeadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(700)
        while (buffered.size < 2 && generation == stopGeneration.get()) {
            val remaining = (warmupDeadline - System.nanoTime()) / 1_000_000L
            if (remaining <= 0L) break
            val packet = queue.poll(minOf(remaining, 100L), TimeUnit.MILLISECONDS) ?: continue
            buffered.addLast(packet)
            if (packet !is Packet.Audio) break
        }
        var frames = 0
        try {
            synthesis@ while (generation == stopGeneration.get()) {
                val packet = if (buffered.isNotEmpty()) buffered.removeFirst()
                    else queue.poll(150, TimeUnit.MILLISECONDS) ?: continue
                when (packet) {
                    is Packet.Audio -> {
                        if (!callback.hasStarted()) {
                            Log.d("NaturalVoice", "PCM ${packet.rate} Hz · mono · 16 bits")
                            if (callback.start(packet.rate, AudioFormat.ENCODING_PCM_16BIT, 1) != TextToSpeech.SUCCESS) break
                        }
                        // Mark the actual frame where this phrase starts, never the generation time.
                        if (Build.VERSION.SDK_INT >= 26) callback.rangeStart(frames, packet.span.start, packet.span.end)
                        val buffer = ByteArray(callback.maxBufferSize.coerceAtLeast(2).let { it - it % 2 })
                        var position = 0
                        while (position < packet.samples.size) {
                            if (generation != stopGeneration.get()) break@synthesis
                            val count = minOf(buffer.size / 2, packet.samples.size - position)
                            for (index in 0 until count) {
                                // Redondeo en vez de truncar: truncar hacia cero deja
                                // distorsión de cuantización audible en las sílabas.
                                val pcm = (packet.samples[position + index].coerceIn(-1f, 1f) * 32767f)
                                    .roundToInt()
                                buffer[index * 2] = pcm.toByte()
                                buffer[index * 2 + 1] = (pcm shr 8).toByte()
                            }
                            if (callback.audioAvailable(buffer, 0, count * 2) != TextToSpeech.SUCCESS) break@synthesis
                            position += count
                        }
                        frames += packet.samples.size
                    }
                    is Packet.Failure -> { callback.error(TextToSpeech.ERROR_SYNTHESIS); break@synthesis }
                    Packet.End -> break@synthesis
                }
            }
        } catch (e: Exception) {
            if (generation == stopGeneration.get()) callback.error(TextToSpeech.ERROR_SYNTHESIS)
        } finally {
            activeJob?.cancel(true)
            if (!callback.hasStarted() && generation == stopGeneration.get()) callback.start(MODEL_RATE, AudioFormat.ENCODING_PCM_16BIT, 1)
            callback.done()
        }
    }

    override fun onDestroy() {
        onStop()
        generator.execute { model?.release(); model = null }
        generator.shutdown()
        super.onDestroy()
    }
}
data class NaturalVoice(val id: String, val label: String, val speaker: Int)

object NaturalVoices {
    // Speaker order follows the official generate_voices_bin.py conversion.
    val all: List<NaturalVoice> = (1..5).map { NaturalVoice("F$it", "Femenina $it", it - 1) } +
        (1..5).map { NaturalVoice("M$it", "Masculina $it", it + 4) }
}

