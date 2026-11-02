package com.antonio.lector

import android.app.Instrumentation
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Device smoke test: binds the bundled engine and renders every voice without playing audio. */
class NaturalVoiceInstrumentation : Instrumentation() {
    private var args: Bundle? = null
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        args = arguments
        start()
    }

    override fun onStart() {
        val result = Bundle()
        var engine: TextToSpeech? = null
        try {
            val initialized = CountDownLatch(1)
            var initStatus = TextToSpeech.ERROR
            runOnMainSync {
                engine = TextToSpeech(targetContext, {
                    initStatus = it
                    initialized.countDown()
                }, targetContext.packageName)
            }
            check(initialized.await(30, TimeUnit.SECONDS)) { "El motor no se inició" }
            check(initStatus == TextToSpeech.SUCCESS) { "Error de inicio: $initStatus" }
            val tts = checkNotNull(engine)
            val voices = tts.voices
            check(voices.size == 10) { "Se esperaban 10 voces, hay ${voices.size}" }
            check(voices.all { it.locale.language == "es" && !it.isNetworkConnectionRequired })
            val directory = File(targetContext.getExternalFilesDir(null), "voice-tests").apply { mkdirs() }
            // Permite medir una sola voz: -e voices F1
            val requested = args?.getString("voices")
                ?.split(',')?.map { it.trim().uppercase() }?.filter { it.isNotEmpty() }
            val selected = if (requested.isNullOrEmpty()) NaturalVoices.all
            else NaturalVoices.all.filter { it.id in requested }
            check(selected.isNotEmpty()) { "Ninguna voz coincide con $requested" }
            for (voice in selected) {
                val done = CountDownLatch(1)
                var error = false
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {}
                    override fun onDone(id: String?) { done.countDown() }
                    @Deprecated("Deprecated in Java")
                    override fun onError(id: String?) { error = true; done.countDown() }
                })
                check(tts.setVoice(voices.first { it.name == voice.id }) == TextToSpeech.SUCCESS)
                val audio = File(directory, "${voice.id}.wav")
                val started = System.nanoTime()
                check(tts.synthesizeToFile(
                    "Al caer la tarde, abrió su libro favorito y comenzó a leer. Cada palabra le invitaba a imaginar un mundo diferente.",
                    Bundle(), audio, voice.id) == TextToSpeech.SUCCESS)
                check(done.await(90, TimeUnit.SECONDS)) { "Tiempo agotado: ${voice.id}" }
                val elapsed = (System.nanoTime() - started) / 1_000_000
                check(!error) { "Error de síntesis: ${voice.id}" }
                val bytes = audio.readBytes()
                check(bytes.size > 1000 && String(bytes, 0, 4) == "RIFF") { "Audio inválido: ${voice.id}" }
                check(bytes.drop(44).any { it != 0.toByte() }) { "Audio vacío: ${voice.id}" }
                sendStatus(0, Bundle().apply {
                    putString("stream", "${voice.label}: OK (${bytes.size} bytes, ${elapsed} ms)\n")
                })
            }
            result.putString("stream", "OK: 10 voces españolas locales generan audio WAV.\n")
            finish(-1, result)
        } catch (e: Throwable) {
            result.putString("stream", "FALLO: ${e.message}\n")
            finish(0, result)
        } finally {
            engine?.shutdown()
        }
    }
}
