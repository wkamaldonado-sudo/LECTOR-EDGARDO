# Voces integradas y licencias

## Motor de voz incluido en el APK

El APK incluye **Supertonic 3** distribuido por sherpa-onnx, con diez perfiles en
español: cinco femeninos (F1–F5) y cinco masculinos (M1–M5).

- **sherpa-onnx**: v2.0.1, interfaces Kotlin y bibliotecas Android oficiales.
  <https://github.com/k2-fsa/sherpa-onnx/releases/tag/v2.0.1>
- **Modelo**: `sherpa-onnx-supertonic-3-tts-int8-2026-05-11`.
  <https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models>
- **Origen del modelo**: Supertonic 3, Supertone. Su distribución conserva la licencia
  **OpenRAIL-M**, incluida en `assets/licenses/SUPERTONIC-MODEL-LICENSE.txt`.
- El código de ejemplo y el modelo tienen licencias distintas; se incluyen ambas.
- Idioma de síntesis fijado a español (`es`). Solo se presentan los diez perfiles
  F1–F5 y M1–M5. El orden de `voice.bin` corresponde al orden alfabético que usa
  `scripts/supertonic/generate_voices_bin.py` en sherpa-onnx v2.0.1.
- Las bibliotecas nativas incluidas son para Android **arm64-v8a** (Pixel 7 y otros
  teléfonos de 64 bits). El servicio TTS es privado de esta aplicación.
- El modelo se lee de los assets del APK: no usa claves, servicios remotos ni descargas
  durante el uso. Los avisos de licencia se consultan dentro de la app en **Voz**.
- La compilación Kotlin usa `-Xlambdas=class` porque JNI busca el método tipado
  `invoke(FloatArray)` del callback de audio; las lambdas `invokedynamic` fallan.
- Este motor **no ofrece alineación por palabra**: el progreso avanza por fragmentos y
  no se simula un resaltado que el motor no puede proporcionar.

## Archivos del modelo

```
app/src/main/assets/supertonic/
├─ text_encoder.int8.onnx
├─ duration_predictor.int8.onnx
├─ vector_estimator.int8.onnx
├─ vocoder.int8.onnx
├─ voice.bin                 diez perfiles F1–F5, M1–M5
├─ unicode_indexer.bin
└─ tts.json
```

Bibliotecas nativas: `app/src/main/jniLibs/arm64-v8a/` (`libsherpa-onnx-*.so`,
`libonnxruntime.so`).

## Voces del sistema (alternativa)

También puedes usar cualquier motor TTS instalado en Android: abre un libro → **Voz**.
Para voces neuronales gratuitas, instala un motor Sherpa-ONNX/Piper desde la página
oficial (elige **Android TTS Engine**, no la app de demostración):
<https://k2-fsa.github.io/sherpa/onnx/tts/apk-engine.html>

Voces con muestras para Android:

- España, Davefx: <https://k2-fsa.github.io/sherpa/onnx/tts/all/Spanish/vits-piper-es_ES-davefx-medium.html>
- Argentina, Daniela: <https://k2-fsa.github.io/sherpa/onnx/tts/all/Spanish/vits-piper-es_AR-daniela-high.html>
- México, Claude: <https://k2-fsa.github.io/sherpa/onnx/tts/all/Spanish/vits-piper-es_MX-claude-high.html>

Los modelos externos no están incluidos en este APK. Los motores locales leen sin
Internet; las voces de red envían el texto a su proveedor. Cada modelo tiene su licencia.

## Dependencias y licencias

| Componente | Uso | Licencia |
|---|---|---|
| sherpa-onnx 2.0.1 | motor TTS nativo | Apache-2.0 |
| Modelo Supertonic 3 (int8) | voces F1–F5 / M1–M5 | OpenRAIL-M (Supertone) |
| onnxruntime | inferencia | MIT |
| Apache PDFBox (`pdfbox-android` 2.0.27.0) | texto y renglones de PDF | Apache-2.0 |
| ML Kit Text Recognition 16.0.1 | OCR latino | Términos de Google ML Kit (gratuito, offline) |
| Jetpack Compose / AndroidX | interfaz | Apache-2.0 |
| Kotlin | lenguaje | Apache-2.0 |

Si publicas el proyecto, conserva los archivos de licencia de `assets/licenses/` y este
documento. Antes de redistribuir comercialmente el modelo Supertonic, revisa las
restricciones de uso de OpenRAIL-M.
