# Lector 📚

Aplicación Android nativa para leer libros en **EPUB**, **PDF** y **TXT**,
inspirada en lectores tipo ElevenReader.

## Características

- **Biblioteca**: importa libros desde el almacenamiento del teléfono (selector de archivos).
- **EPUB**: lectura por capítulos, con imágenes y estilos del propio libro.
- **PDF**: renderizado página a página con navegación.
- **TXT**: lectura con desplazamiento y detección automática de codificación (UTF-8 / Windows-1252).
- **Progreso guardado**: recuerda capítulo, página y porcentaje de cada libro.
- **Tamaño de letra ajustable** (A- / A+), se guarda entre sesiones.
- **Lectura en voz alta** de EPUB, TXT y PDF con texto seleccionable, usando las voces españolas de Android.
- **Selector de voz y velocidad**: lenta, normal o rápida; funciona sin conexión
  cuando la voz seleccionada está descargada.
- **Estilos de narración**: Audiolibro, Narrador, Cuento y Podcast, cada uno con ritmo y tono propios.
- **Velocidad precisa**: 20 niveles, desde muy lenta hasta muy rápida, con cambios suaves.
- **Entonación**: 10 niveles de intensidad, desde suave hasta muy expresiva.
- **Pitch visible**: barra directa de 1 (voz grave) a 10 (voz aguda).
- **Piano/Synth ambiental**: acordes originales de piano y sintetizador, sin percusión ni efectos, con volumen independiente.
- **Seguimiento de narración**: muestra y resalta la línea que se está pronunciando.
- **Marcado dentro del PDF**: resalta la palabra narrada directamente sobre la página
  y avanza automáticamente a la página correspondiente (PDF con texto seleccionable).
- **Posición de audio**: permite elegir con una barra dónde empezar a escuchar.
- **Inicio exacto en PDF**: ve a cualquier página y pulsa “Escuchar desde esta página”.
- **Historial por libro**: guarda automáticamente cuánto se escuchó y cuánto falta.
- **Referencias bíblicas naturales**: `Juan 13:1-5` se narra como
  “Libro Juan, capítulo 13, versículos del 1 al 5”.
- **Tema claro/oscuro** automático según el sistema.

## Dónde está el APK

```
app\build\outputs\apk\release\app-release.apk
```

## Instalarlo en el teléfono

1. Copia `app-release.apk` al móvil (cable USB, Google Drive, Telegram…).
2. Ábrelo desde el explorador de archivos.
3. Si Android lo pide, activa **"Instalar apps desconocidas"** para esa app
   (Ajustes → Seguridad, o el aviso que aparece en pantalla).
4. Pulsa **Instalar**.

> El APK está firmado con la clave de depuración: sirve para uso personal.
> Para publicarlo en Google Play hay que firmarlo con una clave de publicación.

## Compilar de nuevo

Requisitos ya instalados en este equipo:

| Herramienta | Ruta |
|---|---|
| JDK 17 (Temurin) | `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot` |
| Android SDK | `C:\Android\sdk` |
| Gradle 8.9 | `C:\Android\gradle-8.9` |

Comando de compilación (PowerShell):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
$env:ANDROID_HOME = 'C:\Android\sdk'
C:\Android\gradle-8.9\bin\gradle.bat -p "C:\Users\Antonio\Documents\APK Libro\Lector de Libros" assembleRelease
```

- `assembleDebug` genera una versión de depuración más rápida de compilar.
- Para instalar directo por USB con el teléfono conectado:
  `C:\Android\sdk\platform-tools\adb.exe install -r app\build\outputs\apk\release\app-release.apk`

## Estructura del proyecto

```
Lector/
├─ app/src/main/java/com/antonio/lector/
│  ├─ MainActivity.kt     # arranque, tema y navegación
│  ├─ LibraryScreen.kt    # biblioteca
│  ├─ ReaderScreen.kt     # barra del lector y progreso
│  ├─ EpubReader.kt       # visor EPUB (WebView + recursos del ZIP)
│  ├─ PdfReader.kt        # visor PDF (PdfRenderer)
│  ├─ TxtReader.kt        # visor TXT
│  ├─ Epub.kt             # parser EPUB/OPF
│  └─ BookStore.kt        # almacenamiento y metadatos (JSON)
└─ app/src/main/res/      # icono, tema y textos
```

## Limitaciones conocidas

- Formatos soportados: EPUB 2/3, PDF y TXT. **MOBI/AZW3 no** (son formatos propietarios de Amazon).
- El audio de PDF requiere texto seleccionable, salvo que se reconozca antes con **☰ → Reconocer texto (OCR)**; los PDF escaneados se reconocen en el propio teléfono, sin Internet.
- El EPUB se muestra con desplazamiento vertical por capítulo, con modo página opcional (☰ → Modo página).
- DRM (libros protegidos con Adobe/Amazon) no está soportado.
- Los PDF se renderizan a la anchura de la pantalla, con zoom de 100 % a 300 %.

## Versión 2.9: motores y voces

En el lector, abre «Voz ⚙» para elegir entre los motores instalados en Android.
Puedes escuchar una prueba sin cambiar la posición guardada del libro. El estilo
«Natural» usa velocidad y tono originales. La calidad indicada por Android es
orientativa y no garantiza que una voz suene mejor.

Para probar voces neuronales gratuitas, instala un motor Sherpa-ONNX/Piper desde
su página oficial (elige Android TTS Engine, no la aplicación de demostración):
https://k2-fsa.github.io/sherpa/onnx/tts/apk-engine.html

Voces con muestras y enlaces para Android:
- España, Davefx: https://k2-fsa.github.io/sherpa/onnx/tts/all/Spanish/vits-piper-es_ES-davefx-medium.html
- Argentina, Daniela: https://k2-fsa.github.io/sherpa/onnx/tts/all/Spanish/vits-piper-es_AR-daniela-high.html
- México, Claude: https://k2-fsa.github.io/sherpa/onnx/tts/all/Spanish/vits-piper-es_MX-claude-high.html

Después de instalar el motor, vuelve a la app, selecciónalo y prueba su voz.
Los modelos no están incluidos en el APK del lector. Los motores locales leen
sin Internet; las voces de red envían el texto a su proveedor. Cada modelo tiene
su propia licencia. El seguimiento palabra a palabra depende de que el motor
comunique los intervalos; si no lo hace, el progreso se actualiza por fragmentos.

Esta versión corrige avisos atrasados del audio y la recuperación de velocidades
extremas, guarda el progreso al pasar a segundo plano y usa escritura atómica
para proteger la biblioteca frente a escrituras interrumpidas. No recupera un
archivo de biblioteca que ya estuviera corrupto.

## Versión 3.0: diez voces españolas incluidas

El APK incluye Supertonic 3: cinco perfiles femeninos y cinco masculinos, con
síntesis fijada a español. No requiere instalar otro motor, cuentas ni conexión
para narrar. Abre un libro → Voz → Probar junto a cada perfil. Los ajustes de
entonación artificial se ocultan para conservar el tono propio de estas voces.

La primera carga del modelo puede tardar unos segundos. El progreso se guarda
por fragmentos; al pausar se puede repetir el fragmento actual porque este motor
no ofrece alineación por palabra. El resaltado exacto del PDF no está disponible
con las voces integradas. Su naturalidad debe valorarse escuchándolas.

Compilación para Android de 64 bits (arm64-v8a), probada en Pixel 7. Las diez voces
han generado WAV válidos mediante una prueba en el dispositivo. Las cinco pruebas
unitarias existentes también pasan. Licencias y procedencia: VOCES-INTEGRADAS.md.

## Versión 3.1: nueva interfaz

Biblioteca con búsqueda por título y autor, filtros EPUB/PDF/TXT y tarjeta para
continuar un libro en curso. Portadas tipográficas decorativas, colores de papel
y verde, y eliminación protegida en el menú de cada libro.

El lector deja el documento en la zona principal y coloca el reproductor abajo.
«Más» despliega detener, escuchar desde la página actual de PDF, piano y volumen.
«Voces» conserva los diez perfiles integrados y permite probar cada uno.

## Versión 3.2: elegir dónde empieza la narración

En el reproductor, «Elegir inicio» permite escribir página y renglón o tocar el
texto de la vista previa. «Leer desde aquí» omite lo anterior y continúa el libro.

- PDF: páginas reales del archivo, incluyendo páginas vacías. Los renglones son
  las líneas de texto extraídas, sin líneas vacías. Los PDF escaneados necesitan OCR.
- EPUB: páginas de texto de hasta 30 renglones, respetando los capítulos.
- TXT: páginas de texto de hasta 30 renglones.

En EPUB y TXT los renglones se ajustan a 80 caracteres por palabras. Esta numeración
es estable y no corresponde a las pantallas que resultan al cambiar la letra.
La vista previa y la narración comparten el mismo texto, incluida la expansión de
referencias bíblicas, para conservar el punto elegido.

El visor salta a la página PDF o al capítulo EPUB seleccionado. En TXT desplaza a
una posición aproximada por porcentaje; el punto de inicio del audio sí corresponde
al renglón elegido. En EPUB no se desplaza al renglón dentro del capítulo.

## Versión 3.3: seguimiento y narración más fluida

El reproductor muestra la frase que está sonando, subrayada, con página y renglón.
En PDF se resalta el renglón sobre la página y se sigue la página correspondiente.
En EPUB se sigue el capítulo; en TXT se aproxima el desplazamiento por porcentaje.
El seguimiento es por frases, no una alineación fonética palabra a palabra. Una
frase que ocupa varios renglones se identifica por su renglón de inicio. En EPUB
 y TXT se conserva la numeración de páginas de texto del selector de inicio.

Las marcas de frase usan posiciones de muestras PCM entregadas a Android; no
avanzan al terminar de generar el audio ni mediante un temporizador estimado.
Requiere Android 8 o posterior para los eventos de rango. El libro sigue siendo
legible en Android 7, con progreso por bloques.

La generación usa una cola acotada de dos frases, con generación anticipada en
un hilo distinto del que entrega PCM a Android. El controlador también mantiene
el siguiente bloque en la cola TTS. Se sustituyen los saltos de línea de maquetación
por espacios para pronunciar, se conservan los offsets originales para seguir el
texto, y se recortan silencios extremos dejando márgenes alrededor de la voz.
Se utilizan cinco pasos de generación (valor predeterminado de la interfaz del
motor), y accesos directos a 1x, 1.2x y 1.5x.

Medición en Pixel 7, misma muestra y voz F1 a 1x: primer audio con modelo cargado
antes 4276 ms, después 1176 ms. Arranque en frío antes 5874 ms, después 2718 ms.
Son mediciones de una muestra, no una garantía para todos los libros o teléfonos.
Tres marcas de frase llegaron ordenadas durante reproducción silenciada.

## Versión 3.4: carátulas reales

La biblioteca muestra las imágenes de portada de los EPUB (declaraciones EPUB 3,
metadatos EPUB 2, guía de portada HTML o imagen en el primer capítulo). En PDF se
usa la primera página del archivo. Los TXT y los libros sin imagen compatible
conservan la portada tipográfica.

Las miniaturas se extraen en segundo plano y se guardan en caché con dimensiones
limitadas. También funciona con libros ya importados; no se descargan portadas de
Internet ni se cambian los archivos originales. Las portadas SVG no rasterizadas
pueden conservar la portada decorativa si Android no puede decodificarlas.

## Versión 3.5: subrayado directamente sobre el libro

El indicador de texto duplicado inferior se retira. EPUB y TXT subrayan en rojo
el renglón de origen correspondiente al inicio de la frase que está sonando.
PDF usa la misma línea roja sobre el renglón, sin recuadro amarillo.

EPUB conserva el diseño del capítulo y superpone líneas sin modificar su texto.
La búsqueda admite espacios, estilos en línea y repeticiones. El visor desplaza
el texto para mantener visible el renglón. El JavaScript evaluado pertenece al
lector; los scripts y eventos del libro se bloquean mediante CSP. TXT dibuja la
línea roja sobre la disposición real del texto conservando el color de las letras.

El avance se produce por frases, conforme a las marcas reales del audio. No es
un subrayado palabra por palabra ni alineación acústica de cada renglón dentro
de una frase larga. Se mantienen voces, velocidades, portada y selector de inicio.

## Versión 3.6: sesión única y lectura en segundo plano

El reproductor pertenece a ReadingSession, no a la pantalla. Recrear la Activity,
volver desde el icono o abrir desde la notificación reutiliza el mismo controlador.
MainActivity usa singleTask y conserva la navegación existente.

La narración activa o pausada dispone de un servicio foreground mediaPlayback,
una sesión multimedia y notificación con pausar, continuar, detener y abrir el libro.
Se protege la generación y reproducción con un bloqueo parcial de CPU limitado
por tiempo y liberado al pausar o destruir el servicio. Se persiste el avance del
audio también cuando no hay una pantalla visible.

El último libro abierto se guarda fuera del estado temporal de la Activity. Si
Android elimina el proceso, al abrir de nuevo se recuperan libro y progreso, pero
no se reproduce audio automáticamente ni se puede conservar un proceso eliminado.
Salir explícitamente del lector a la biblioteca cierra la sesión. Deslizar la app
de recientes conserva la narración mientras el servicio siga activo. Forzar la
detención de la app desde Android siempre termina la reproducción.

Prueba en Pixel 7: recreación de Activity con el mismo controlador, avance real
de audio en segundo plano, reapertura desde el icono sin otra Activity, y pausa/
reanudación mediante las acciones de la notificación. Prueba silenciosa con un
libro temporal, sin añadirlo a la biblioteca del usuario.

## Versión 3.7: narración más natural

La lectura en voz alta agrupa frases completas en bloques de voz más largos, en
lugar de reiniciar el motor cada 900 caracteres. Los bloques empiezan y terminan
en una frase completa, nunca en medio de una palabra o de un párrafo.

Entre frase y frase se conserva una pausa breve; entre párrafos y páginas la pausa
es mayor. Ya no se recortan los silencios internos del motor: se mantiene su
prosodia natural y solo se eliminan los silencios extremos de los extremos. La
cola de generación se amplió a cuatro frases para que el audio no se corte en
teléfonos más lentos.

Al empezar desde un renglón concreto, la narración retrocede al inicio de esa
frase para no omitir el principio. El seguimiento por frase, el subrayado, el
progreso y la reanudación conservan la misma posición.

## Versión 3.8: guía de lectura amarilla

La línea roja del seguimiento se sustituye por una sola línea fina de color
amarillo suave. En EPUB y TXT se marca únicamente el primer renglón del
fragmento narrado; en PDF se marca la palabra actual. La guía es solo visual:
no interviene en la generación de voz, en la sincronización ni en los tiempos de
lectura.

## Versión 4.0: fluidez, herramientas y OCR

La biblioteca se lee una vez por proceso y se escribe en un hilo aparte, con el
progreso de audio agrupado cada 1,5 s; el desplazamiento por el libro ya no
recalcula posiciones en cada palabra.

Se añaden marcadores con notas, búsqueda dentro del libro (sin distinguir
mayúsculas ni acentos), temporizador de apagado con fundido de volumen, modo
página en EPUB y zoom en PDF.

**OCR local**: en un PDF escaneado, ☰ → «Reconocer texto (OCR)» reconoce las
páginas que no tienen texto con el modelo latino de ML Kit incluido en el APK,
sin Internet y sin Play Services. Solo se procesan las páginas vacías, el
resultado se guarda por libro (`files/ocr/<id>.json`) y alimenta a la vez la
narración, la búsqueda y el subrayado. La primera pasada tarda unos segundos por
página y se puede cancelar sin perder lo ya reconocido.
