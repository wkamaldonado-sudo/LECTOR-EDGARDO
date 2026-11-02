# Cambios de voz — 2 de noviembre de 2026

Copia de seguridad de los arreglos de voz aplicados hoy al proyecto
`C:\Users\Antonio\Documents\APK Libro\Lector de Libros`, junto con el APK ya compilado.

Estado: **código compilado, 49 tests en verde e instalado en el Pixel 7**. Lo único que
queda pendiente es la valoración auditiva (escucharlo) y, si hiciera falta, el cambio a
pesos `fp32`.

---

## 1. Contenido de esta carpeta

```
cambios-voces-2026-11-02\
├─ CAMBIOS.md                                  este documento
├─ Lector-de-Libros-4.0-f6-voces.apk           APK compilado con los arreglos (188 MB)
├─ cambios-voces-2026-11-02.zip                los 5 fuentes + este documento
└─ fuente\                                     copia de los archivos modificados
   └─ app\src\
      ├─ main\java\com\antonio\lector\NaturalVoiceService.kt
      ├─ main\java\com\antonio\lector\SpeechSegments.kt
      ├─ test\java\com\antonio\lector\SpeechSegmentsTest.kt
      ├─ test\java\com\antonio\lector\PdfTextQualityTest.kt
      └─ androidTest\java\com\antonio\lector\NaturalVoiceInstrumentation.kt
```

Las rutas dentro de `fuente\` son las mismas del proyecto: se pueden copiar encima sin
buscar nada. El APK **no** lleva incrementado el número de versión (sigue siendo
`4.0-f6`, `versionCode 35`): el nombre `...-voces.apk` solo distingue esta compilación.

## 2. Qué se arregló y por qué

### Timbre metálico

`NaturalVoiceService.STEPS` estaba en 5, el mínimo del motor. Supertonic es un modelo de
*flow matching*: con cinco pasos queda ruido residual en el latente y se oye como zumbido
pegado a la voz. Ahora son **8 pasos** (el valor del ejemplo oficial de sherpa-onnx).
Con 14 el timbre mejora algo más, pero en el Pixel 7 la síntesis pasa de 0,65× a 1,3× el
tiempo real y el `AudioTrack` se queda sin datos entre frases.

La conversión a PCM de 16 bits también truncaba hacia cero (`* 32767` → `.toInt()`), lo
que añade distorsión de cuantización en las sílabas. Ahora redondea (`roundToInt()`).

### Clics y cortes entre frases

1. **La unión no terminaba en cero.** La rampa de 8 ms era lineal y se aplicaba sobre una
   señal con una pequeña continua; al pasar al silencio insertado, esa continua se
   convertía en un escalón → chasquido. Ahora se mide y se resta la continua antes de
   recortar, y las dos puntas se rampan con `smoothstep` (20 ms de subida, 30 ms de
   bajada) hasta cero exacto.
2. **Recorte duro de picos.** `coerceIn(-1f, 1f)` aplastaba las sílabas fuertes y sonaba a
   crujido. Sustituido por un limitador de rodilla suave (`tanh`) desde 0,9.
3. **El `AudioTrack` se quedaba sin datos.** Arranca con una frase de margen (espera
   máxima 700 ms) y la cola de paquetes pasa de 4 a 6, para que la reproducción no se
   vacíe cuando el motor tarda más que la duración de la frase.

### Ritmo de lectura

Las pausas eran cortas y hacían la lectura atropellada. Ahora crecen con la frase:

| Pausa | Antes | Ahora |
|---|---|---|
| Fin de frase | 260 ms | **300 ms** (+40 con `?`/`!`, +90 con `…` o `...`) |
| Fin de línea corta | 300 ms | **340 ms** |
| Fin de línea larga (≤60 car.) | 480 ms | **520 ms** |
| Fin de párrafo | 620 ms | **720 ms** |
| Corte por coma / guion / cláusula | — | **120 / 160 / 80 ms** |

Todas suman hasta 90 ms más según la longitud de la frase, con techo de 1 s.

### Frecuencia de muestreo

No había error: `supertonic/tts.json` declara 44100 Hz y el servicio ya pasaba a
`callback.start()` la tasa que devuelve el motor. Solo se corrigió el valor de respaldo
(24000 → 44100) para el caso de que no llegue ningún paquete de audio.

## 3. Constantes tal como están ahora en el código

```kotlin
const val STEPS = 8          // NaturalVoiceService.kt, antes 5
val THREADS = 2              // medido: con 4 el motor va 40 % más lento
const val MODEL_RATE = 44_100
```

## 4. Verificación hecha

| Comprobación | Resultado |
|---|---|
| Tests unitarios | **49, 0 fallos** (2 nuevos: unión sin escalón y continua) |
| Compilación | `BUILD SUCCESSFUL`, APK 188 MB |
| Instalación | `adb install -r` → Success en Pixel 7 |
| Arranque del motor | `Connected successfully to TTS engine`, sin `FATAL` ni errores ONNX |
| Marcas de subrayado | 3, ordenadas y sincronizadas con el audio |
| Audio real exportado | PCM 44100 Hz mono 16 bits, rtf 0,36–0,43, primer audio 1,86 s |
| Silencios medidos | 355 ms y 326 ms entre frases, sin escalones en las uniones |

También se arregló un test que ya venía roto de antes, ajeno a las voces:
`PdfTextQualityTest` pasaba `List<String>` a `needsOcr(List<PdfTextLine>)`. Ahora un
helper `page(...)` construye líneas reales con `PdfWordBox`.

## 5. Lo que queda pendiente

- **Escuchar la app.** El juicio de "suena natural" no se puede automatizar.
- Si tras esto aún se nota sintética, el único cambio que altera el timbre (no el ritmo)
  es sustituir los `.onnx` `int8` por los `fp32` del mismo modelo: mismo código, APK más
  grande y algo más de RAM.
- Decidir número de versión antes de publicar: los arreglos están sobre `4.0-f6`
  (`versionCode 35`); lo lógico es publicarlos como `4.0-f7` / `36` con su entrada en el
  `CHANGELOG.md` del paquete.

## 6. Publicado

Decidido y publicado: **`4.0-f7` / `versionCode 36`**, tag `v4.0-f7`, APK en
`apk/Lector-de-Libros-4.0-f7.apk` y entrada propia en el `CHANGELOG.md` del paquete.
Esta carpeta se conserva como registro del momento en que se hicieron los arreglos.
