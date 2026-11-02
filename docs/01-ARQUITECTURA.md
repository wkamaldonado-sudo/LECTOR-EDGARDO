# Arquitectura

## Capas

```
┌──────────────────────────────────────────────────────────────┐
│ UI (Compose)                                                 │
│   MainActivity · LibraryScreen · ReaderScreen                │
│   diálogos: inicio de lectura, marcadores, búsqueda, OCR     │
├──────────────────────────────────────────────────────────────┤
│ Sesión y control                                            │
│   ReadingSession  ·  BookSpeechController  ·  SleepTimer     │
│   Bookmarks  ·  BookSearch                                   │
├──────────────────────────────────────────────────────────────┤
│ Contenido                                                   │
│   EpubReader/Epub · PdfReader/PdfLineExtractor · TxtReader   │
│   BookTextLoader · PdfOcr · PdfTextQuality · BookCovers      │
├──────────────────────────────────────────────────────────────┤
│ Persistencia y sistema                                      │
│   BookStore (biblioteca JSON) · NarrationService (foreground)│
│   NaturalVoiceService (motor TTS propio)                     │
└──────────────────────────────────────────────────────────────┘
```

## Módulos

| Archivo | Responsabilidad |
|---|---|
| `MainActivity.kt` | Punto de entrada (`singleTask`), tema y navegación biblioteca ↔ lector. |
| `LibraryScreen.kt` | Biblioteca: búsqueda, filtros, tarjetas, portadas, menú por libro. |
| `ReaderScreen.kt` | Barra del lector, controles de audio, menú ☰ y todos los diálogos (inicio, marcadores, búsqueda, temporizador, OCR, tamaño de letra). |
| `ReadingSession.kt` | Dueña del controlador de voz; sobrevive a la recreación de la Activity. Agrupa el guardado de progreso y hace `flush()` al cerrar. |
| `BookSpeechController.kt` | Trocea el libro en frases, mantiene la cola de generación, pide audio al motor, recibe las marcas de rango y publica el estado de narración. |
| `NarrationService.kt` | Servicio en primer plano `mediaPlayback`: notificación con controles, sesión multimedia, carátula y wake lock acotado. |
| `NaturalVoiceService.kt` | Motor TTS propio (`:natural_voice`) que expone las diez voces Supertonic como un motor más de Android. |
| `BookStore.kt` | `library.json`: caché en memoria por proceso, hilo escritor único, escritura atómica, `flush()`. |
| `Bookmarks.kt` | `bookmarks.json` por libro con el mismo patrón de persistencia. |
| `BookSearch.kt` | Búsqueda normalizada (sin mayúsculas ni acentos) sobre el texto ya extraído. |
| `SleepTimer.kt` | Temporizador de proceso, fundido de volumen y pausa final. |
| `EpubReader.kt` / `Epub.kt` | Visor WebView con recursos del ZIP, subrayado superpuesto y modo página por columnas. |
| `PdfReader.kt` / `PdfLineExtractor.kt` | Render con `PdfRenderer`, extracción de renglones con PDFBox, zoom y subrayado. |
| `TxtReader.kt` | Texto plano con detección de codificación y guía de lectura. |
| `BookTextLoader.kt` | Punto único de extracción de texto: EPUB, PDF, TXT y OCR. |
| `PdfOcr.kt` | Render por página + ML Kit + caché `files/ocr/<id>.json`. |
| `PdfTextQuality.kt` | Heurística de «texto ilegible» para decidir cuándo hace falta OCR. |
| `BookCovers.kt` | Portada real (EPUB/PDF) o portada dibujada para TXT y libros sin imagen. |
| `ReadingText.kt` / `SpeechSegments.kt` | Offsets, `positionAt()`, segmentado por frases. |
| `ReadingGuide.kt` / `EpubUnderline.kt` / `TextUnderline.kt` | Dibujo de la guía amarilla en cada formato. |

## Flujo de la narración

1. **Extracción.** `BookTextLoader` obtiene el texto del libro (EPUB: capítulos del OPF;
   PDF: renglones de PDFBox o del OCR; TXT: archivo completo). Se hace **una vez** por
   proceso y se reutiliza, incluso al cambiar de página.
2. **Segmentado.** `SpeechSegments` corta en frases completas y `BookSpeechController`
   agrupa frases en bloques de voz, calculando los offsets de cada bloque por adelantado
   (`partOffsets`).
3. **Generación.** La cola mantiene hasta cuatro frases preparadas; la generación ocurre
   en un hilo distinto del que entrega PCM a Android, para que el audio no se corte.
4. **Marcas.** El motor informa posiciones de muestras reales (`onRangeStart`). Con ellas
   se calcula la frase en curso y el renglón de origen mediante búsqueda binaria
   (`ReadingText.positionAt`), nunca recorriendo el libro.
5. **Presentación.** La guía amarilla se dibuja sobre el renglón (EPUB/TXT) o la palabra
   (PDF) sin modificar el documento. En PDF, si el renglón viene del OCR, se usa su
   recuadro escalado a la página renderizada.
6. **Persistencia.** El progreso se agrupa 1,5 s y se escribe en un hilo aparte; al
   pausar, cerrar o detener, se guarda de inmediato.

## Decisiones que conviene no deshacer

- **La sesión no vive en la pantalla.** Recrear la Activity (rotación, volver desde el
  icono, abrir desde la notificación) reutiliza el mismo controlador.
- **El texto del libro se extrae una vez.** Reabrir el PDF por cada consulta era el mayor
  coste de la app; ahora solo se invalida tras un OCR.
- **La guía es solo visual.** No interviene en la generación ni en los tiempos.
- **El OCR no sustituye al texto bueno.** Solo se aplica a páginas vacías o ilegibles,
  salvo que el usuario fuerce el OCR del libro.
- **La app no pide `INTERNET`.** Cualquier función nueva debe respetarlo: si algo
  necesitara red, tiene que ser opcional y explícito.

## Pruebas

| Tipo | Dónde | Qué cubre |
|---|---|---|
| Unitarias (JVM) | `app/src/test/java/com/antonio/lector/` | offsets y posiciones, segmentado, búsqueda, calidad de texto, OCR (orden y guiones) |
| Instrumentación | `app/src/androidTest/java/com/antonio/lector/` | subrayado, narración, sesión en segundo plano, comprobación de OCR en dispositivo |

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Las de instrumentación necesitan dispositivo conectado; se lanzan por suite con
`am instrument -e suite <nombre>` (por ejemplo `ocr`).
