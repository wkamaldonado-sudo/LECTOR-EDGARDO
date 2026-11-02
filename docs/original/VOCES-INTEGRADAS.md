# Componentes de voces integradas

- sherpa-onnx: v2.0.1, interfaces Kotlin y bibliotecas Android oficiales.
  https://github.com/k2-fsa/sherpa-onnx/releases/tag/v2.0.1
- Modelo: sherpa-onnx-supertonic-3-tts-int8-2026-05-11.
  https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models
- Origen del modelo: Supertonic 3, Supertone. Su distribución conserva la licencia
  OpenRAIL-M (incluida en assets/licenses/SUPERTONIC-MODEL-LICENSE.txt).
- El código de ejemplo y el modelo tienen licencias distintas. Se incluyen ambas.
- Idioma de síntesis fijado a español (es). Solo se presentan los diez perfiles
  F1-F5 y M1-M5. El orden del archivo voice.bin corresponde al orden alfabético
  usado por scripts/supertonic/generate_voices_bin.py en sherpa-onnx v2.0.1.
- Las bibliotecas nativas incluidas son para Android arm64-v8a (Pixel 7 y otros
  teléfonos Android de 64 bits). El servicio es privado para esta aplicación.
- El modelo se lee de los assets del APK. No utiliza claves, servicios remotos ni
  descargas durante el uso. Los avisos de licencia se consultan en «Voz».
- La compilación Kotlin usa -Xlambdas=class porque JNI busca el método tipado
  invoke(FloatArray) del callback de audio; las lambdas invokedynamic fallan.
- No hay alineación por palabra en este motor. El progreso avanza por fragmentos;
  no se simula un resaltado de palabras que el motor no pueda proporcionar.
