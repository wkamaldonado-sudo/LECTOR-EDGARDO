# Historial de versiones

Todas las versiones usan el mismo `applicationId` (`com.antonio.lector`) y la misma clave
de firma, así que se instalan **encima** conservando biblioteca, progreso y ajustes
(`adb install -r`).

---

## 4.0 — serie de mejora (f1 → f7)

La serie 4.0 partió de la 3.8 publicada y se entregó en pasos verificados, cada uno
compilado, probado e instalado en el dispositivo antes de pasar al siguiente.

### 4.0-f7 — versionCode 36

Todo el trabajo de esta versión está en la voz integrada (Supertonic), que sonaba
metálica y con chasquidos al empalmar frases.

- **Pasos de síntesis**: `numSteps` pasa de 5 a 8. Con 5 (el mínimo del motor) el latente
  no terminaba de limpiarse y quedaba un zumbido metálico pegado a la voz.
- **Conversión a PCM**: se redondea con `roundToInt()` en vez de truncar hacia cero, lo
  que añadía distorsión de cuantización en las sílabas.
- **Uniones sin chasquido**: antes de recortar se mide y se quita la continua de la señal,
  y las dos puntas se rampan con *smoothstep* (20 ms de subida, 30 ms de bajada) hasta
  cero exacto. Antes la rampa era lineal y una continua de pocas milésimas producía un
  escalón audible.
- **Limitador de picos**: desde 0,9 se comprime con `tanh` en lugar de recortar en duro
  con `coerceIn(-1f, 1f)`, que hacía crujir las sílabas fuertes.
- **AudioTrack con margen**: la reproducción arranca con una frase de buffer (máximo
  700 ms de espera) y la cola sube de 4 a 6 frases, para que el motor no deje el track
  sin datos entre frase y frase.
- **Ritmo de lectura**: pausas más largas — 300 ms fin de frase, 340/520 ms fin de línea,
  720 ms fin de párrafo — manteniendo el incremento por longitud de la frase.
- **Tasa nativa**: si no llega ningún paquete de audio se declara la tasa real del modelo
  (44100 Hz) en vez de 24000, y se registra en el log (`PCM 44100 Hz · mono · 16 bits`).

Verificación: 49 tests unitarios, 0 fallos; instrumentación con 3 marcas de reproducción
sincronizadas con el audio; WAV analizado muestra silencios de 355/326 ms y ningún salto
en las uniones.

### 4.0-f6 — versionCode 35

- **Cambiar de página mientras narra**: pulsar «Siguiente ›» en PDF (o pasar página en
  EPUB, en modo página o al cambiar de capítulo) hace que la narración **empiece en la
  página nueva** en lugar de volver a la anterior. En pausa solo recoloca la vista.
- El controlador guarda el texto ya extraído del libro, así que el salto es inmediato;
  antes cada cambio de página reabría el PDF completo.
- **Carátula en segundo plano**: la notificación y la pantalla de bloqueo muestran la
  portada y el autor en EPUB, PDF y TXT. Los libros sin imagen reciben una portada
  dibujada (lomo, título, autor y formato).
- Corregido un error de compilación de la f5 en `BookTextLoader` (tipos de línea de PDF).

### 4.0-f5 — versionCode 34

- **Detección de capa de texto dañada**: PDFs cuya tabla `ToUnicode` está rota producían
  texto ilegible (ideogramas, kana, zona privada de Unicode) en lugar de texto vacío, y
  el OCR no se lanzaba porque «había texto».
- `PdfTextQuality` evalúa cada página por proporción de caracteres fuera del alfabeto
  esperado, zona privada, controles C1 y `U+FFFD`. El español normal pasa limpio.
- Narración, búsqueda, OCR y subrayado usan el mismo criterio.
- Opción **«Forzar OCR en todas las páginas»**, persistente por libro.
- Mensajes que distinguen «PDF escaneado» de «capa de texto dañada».

### 4.0-f4 — versionCode 33

- **OCR local con ML Kit** (`text-recognition:16.0.1`, modelo latino incluido, sin
  Internet y sin Play Services).
- Solo se procesan las páginas sin texto legible; el resultado se guarda por libro en
  `files/ocr/<id>.json` (cada 8 páginas) y alimenta narración, búsqueda y subrayado.
- Progreso por página y cancelación sin perder lo reconocido.
- El APK pasa de ~173 MB a ~179 MB.

### 4.0-f3 — versionCode 32

- **Zoom en PDF** de 100 % a 300 % en pasos del 25 %, con render hasta 2400 px y
  desplazamiento horizontal de la página ampliada. El subrayado se reescala.
- **Modo página en EPUB**: el capítulo se reparte en columnas del ancho de pantalla,
  con indicador `capítulo/página`, botones ‹ ›, deslizamiento y preferencia guardada.
  Desactivado por defecto.

### 4.0-f2 — versionCode 31

- **Marcadores con notas** por libro (`bookmarks.json`, caché en memoria + hilo escritor),
  con lista, porcentaje, texto del punto y salto a la posición.
- **Búsqueda dentro del libro** en EPUB, PDF y TXT, sin distinguir mayúsculas ni acentos,
  con debounce de 350 ms, contexto y salto al resultado. El texto se extrae una vez por
  proceso.
- **Temporizador de apagado** (15/30/45/60/90 min) con fundido de volumen en los últimos
  45 s y pausa al llegar a cero; vive en el proceso, así que sigue contando con la
  pantalla apagada. Se refleja en la notificación.

### 4.0-f1 — versionCode 30

- **Biblioteca en memoria**: `library.json` se lee y parsea una vez por proceso; las
  escrituras van a un único hilo (`lector-library-writer`) y nunca al hilo principal.
- **Guardado del progreso agrupado** cada 1,5 s, inmediato al pausar o cerrar, con
  `flush()` en importar y al salir del lector.
- **Posiciones sin coste cuadrático**: `positionAt()` por búsqueda binaria sobre offsets
  precalculados, `offsetOf()` O(1), offsets de bloque precalculados y un índice de
  repeticiones de renglón construido una vez por libro.
- Dos tests de regresión nuevos (ida y vuelta offset→posición y renglones repetidos).

---

## 3.8 — guía de lectura amarilla

La línea roja del seguimiento pasa a una línea fina amarilla. En EPUB y TXT se marca el
primer renglón del fragmento narrado; en PDF, la palabra actual. Es solo visual: no
interviene en la generación, la sincronización ni los tiempos.

## 3.7 — narración más natural

Los bloques de voz agrupan frases completas en lugar de reiniciar el motor cada 900
caracteres. Pausa breve entre frases, mayor entre párrafos y páginas. Ya no se recortan
los silencios internos del motor. La cola de generación sube a cuatro frases. Al empezar
desde un renglón, la narración retrocede al inicio de esa frase.

## 3.6 — sesión única y lectura en segundo plano

El reproductor pertenece a `ReadingSession` y no a la pantalla. `MainActivity` usa
`singleTask`. Servicio en primer plano `mediaPlayback` con sesión multimedia y
notificación (pausar, continuar, detener, abrir). Bloqueo parcial de CPU limitado por
tiempo. El avance se guarda sin pantalla visible y se recupera el último libro abierto.

## 3.5 — subrayado sobre el libro

Se retira el panel de texto duplicado. EPUB y TXT subrayan el renglón de origen de la
frase en curso; PDF dibuja la línea sobre el renglón. EPUB conserva el diseño del
capítulo y superpone líneas sin tocar el texto, con CSP que bloquea los scripts del libro.

## 3.4 — carátulas reales

Portadas de EPUB (EPUB 3, metadatos EPUB 2, guía HTML o primera imagen del capítulo) y
primera página en PDF. Miniaturas extraídas en segundo plano y cacheadas. Sin descargas
de Internet y sin modificar los archivos originales.

## 3.3 — seguimiento y narración más fluida

Frase actual subrayada con página y renglón, en PDF sobre la página. Marcas de frase
basadas en posiciones PCM reales, no en temporizadores. Cola acotada de dos frases con
generación anticipada en otro hilo. Medición en Pixel 7 con F1 a 1x: primer audio de
4276 → 1176 ms con el modelo ya cargado; arranque en frío de 5874 → 2718 ms.

## 3.2 — elegir dónde empieza la narración

«Elegir inicio» permite escribir página y renglón o tocar el texto de la vista previa.
En PDF se usan páginas reales del archivo; en EPUB y TXT, páginas de texto de hasta 30
renglones de 80 caracteres. La vista previa y la narración comparten el mismo texto,
incluida la expansión de referencias bíblicas.

## 3.1 — nueva interfaz

Biblioteca con búsqueda por título y autor, filtros EPUB/PDF/TXT, tarjeta para continuar
un libro y portadas tipográficas. El documento ocupa la zona principal y el reproductor
va abajo; «Más» despliega detener, escuchar desde la página actual, piano y volumen.

## 3.0 — diez voces españolas incluidas

Supertonic 3 integrado: cinco perfiles femeninos y cinco masculinos con síntesis fijada
a español, sin instalar motores, cuentas ni conexión. Primera carga del modelo de unos
segundos; el progreso avanza por fragmentos porque el motor no ofrece alineación por
palabra. Compilación `arm64-v8a`, probada en Pixel 7.

## 2.9 — motores y voces

Selector de motor TTS instalado en Android, prueba de voz sin mover la posición guardada
y estilo «Natural» con velocidad y tono originales. Corrige avisos atrasados del audio,
la recuperación de velocidades extremas y usa escritura atómica para la biblioteca.

## Serie 1.0 → 2.8 — línea inicial

Base del lector: biblioteca con importación desde el almacenamiento, EPUB por capítulos,
PDF página a página, TXT con detección de codificación, progreso por libro, tamaño de
letra, lectura en voz alta con las voces del sistema, estilos de narración, 20
velocidades, entonación, pitch, piano ambiental, seguimiento de la narración, posición
de audio, referencias bíblicas naturales y tema claro/oscuro.
