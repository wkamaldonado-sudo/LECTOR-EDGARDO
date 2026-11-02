# Lector de Libros

Lector de **EPUB, PDF y TXT** para Android con narración neuronal **sin Internet**.
Kotlin + Jetpack Compose, sin cuentas, sin telemetría y sin permiso de red.

![Versión](https://img.shields.io/badge/versi%C3%B3n-4.0--f7-brightgreen)
![Android](https://img.shields.io/badge/Android-7.0%2B%20(API%2024)-blue)
![ABI](https://img.shields.io/badge/ABI-arm64--v8a-informational)

[**Descargar el APK (4.0-f7)**](https://github.com/wkamaldonado-sudo/LECTOR-EDGARDO/releases/download/v4.0-f7/Lector-de-Libros-4.0-f7.apk) ·
178,9 MB · Android 7.0+ · arm64-v8a

---

## Qué es

Una app de lectura pensada para **escuchar libros**, no solo para mirarlos. El motor
de voz (Supertonic 3 vía sherpa-onnx) va dentro del APK: diez voces en español que
funcionan en modo avión, sin descargas ni servicios en la nube.

| | |
|---|---|
| **Paquete** | `com.antonio.lector` |
| **Versión** | `4.0-f7` (versionCode 36) |
| **Android mínimo** | 7.0 (API 24) · objetivo API 35 |
| **Arquitectura** | `arm64-v8a` |
| **Tamaño del APK** | ~179 MB (los modelos de voz son la mayor parte) |
| **Firma** | clave de depuración (uso personal, no Play Store) |

## Funciones

**Lectura**

- EPUB 2/3 con estilos e imágenes propias del libro, y **modo página** opcional (columnas).
- PDF página a página con **zoom 100 %–300 %** y navegación horizontal al ampliar.
- TXT con detección automática de codificación (UTF-8 / Windows-1252).
- Biblioteca con búsqueda, filtros por formato, portadas reales y «continuar leyendo».
- Progreso guardado por libro (capítulo, página, porcentaje).
- Tema claro/oscuro según el sistema, tamaño de letra ajustable.

**Narración**

- 10 voces neuronales en español (F1–F5, M1–M5) **offline**, dentro del APK.
- También puedes usar cualquier motor TTS instalado en Android.
- Estilos (Audiolibro, Narrador, Cuento, Podcast), 20 velocidades y 10 niveles de entonación.
- Piano/sintetizador ambiental con volumen independiente.
- **Seguimiento visual** con guía amarilla sobre el renglón o la palabra que suena.
- Cola de frases con generación anticipada: el audio no se corta entre frases.
- **Lectura en segundo plano** con servicio en primer plano, notificación y pantalla de
  bloqueo con la **carátula del libro**.

**Herramientas (4.0)**

- **Marcadores con notas** por libro, con salto a la posición.
- **Búsqueda dentro del libro** (sin distinguir mayúsculas ni acentos).
- **Temporizador de apagado** 15/30/45/60/90 min con fundido de volumen, activo con la
  pantalla apagada.
- **OCR local** para PDF escaneados o con la capa de texto dañada.
- Elegir dónde empieza la narración (página y renglón) y «Escuchar desde esta página».

## Instalar el APK

**Opción 1 — desde el móvil:** abre
<https://github.com/wkamaldonado-sudo/LECTOR-EDGARDO/releases/latest> y descarga el APK del
Release. Es la vía recomendada, porque el archivo no vive en el árbol de Git.

**Opción 2 — desde este paquete:** el APK listo para usar está en `apk/`: 

```
apk/Lector-de-Libros-4.0-f7.apk
```

> Este archivo pesa **178,9 MB**, por encima del límite de 100 MB por archivo de GitHub.
> En el repositorio está ignorado a propósito: al publicar, súbelo como **asset de una
> Release** (instrucciones en [docs/07-PUBLICAR-EN-GITHUB.md](docs/07-PUBLICAR-EN-GITHUB.md)).
> Verificado: `versionCode 36` / `versionName 4.0-f7`,
> SHA-256 `8B7D3F66ABC3ECBB0F4F8C2F5A1F6D8239B3EF7815905273E42E25C23EC4A7A8`.

**Desde el teléfono:** copia el APK al móvil, ábrelo con el explorador de archivos y acepta
«Instalar apps desconocidas» si Android lo pide.

**Por USB (conserva biblioteca y progreso):**

```powershell
adb install -r "apk\Lector-de-Libros-4.0-f7.apk"
```

Guía detallada, requisitos y solución de problemas: [docs/02-INSTALACION.md](docs/02-INSTALACION.md).

## Compilar

Requisitos: **JDK 17**, **Android SDK 35** (platform + build-tools) y conexión a Maven
la primera vez. Gradle 8.9 viene en el wrapper.

```powershell
.\gradlew.bat :app:assembleDebug
```

Instrucciones completas (variables de entorno, tests, firma): [docs/03-COMPILAR.md](docs/03-COMPILAR.md).

## Estructura del proyecto

```
Lector de Libros/
├─ app/src/main/java/com/antonio/lector/
│  ├─ MainActivity.kt              arranque, tema y navegación
│  ├─ LibraryScreen.kt             biblioteca, filtros y portadas
│  ├─ ReaderScreen.kt              barra del lector, herramientas y diálogos
│  ├─ ReadingSession.kt            sesión que sobrevive a la Activity
│  ├─ BookSpeechController.kt      cola de frases, anticipación y seguimiento
│  ├─ NarrationService.kt          servicio en primer plano y notificación
│  ├─ NaturalVoiceService.kt       motor TTS propio (Supertonic 3)
│  ├─ EpubReader.kt / PdfReader.kt / TxtReader.kt
│  ├─ BookStore.kt                 biblioteca en JSON, caché y escritura diferida
│  ├─ Bookmarks.kt / BookSearch.kt / SleepTimer.kt
│  ├─ PdfOcr.kt / PdfTextQuality.kt / BookTextLoader.kt
│  └─ BookCovers.kt / EpubUnderline.kt / TextUnderline.kt / ReadingGuide.kt
├─ app/src/main/assets/supertonic/     modelos ONNX int8 + voice.bin
├─ app/src/main/jniLibs/              sherpa-onnx + onnxruntime (arm64-v8a)
└─ docs/                              documentación de este paquete
```

Arquitectura y flujo de la narración: [docs/01-ARQUITECTURA.md](docs/01-ARQUITECTURA.md).

## Documentación

| Documento | Contenido |
|---|---|
| [docs/01-ARQUITECTURA.md](docs/01-ARQUITECTURA.md) | Módulos, flujo de datos y pipeline de narración |
| [docs/02-INSTALACION.md](docs/02-INSTALACION.md) | Instalar en el teléfono, actualizar, problemas |
| [docs/03-COMPILAR.md](docs/03-COMPILAR.md) | Compilar desde cero, tests, firma |
| [docs/04-OCR.md](docs/04-OCR.md) | OCR local, detección de texto ilegible y caché |
| [docs/05-VOCES-Y-LICENCIAS.md](docs/05-VOCES-Y-LICENCIAS.md) | Modelos de voz y licencias de terceros |
| [docs/06-PRIVACIDAD.md](docs/06-PRIVACIDAD.md) | Qué datos toca la app (ninguno sale del teléfono) |
| [docs/07-PUBLICAR-EN-GITHUB.md](docs/07-PUBLICAR-EN-GITHUB.md) | Subir el proyecto y publicar el APK como Release |
| [CHANGELOG.md](CHANGELOG.md) | Historial completo, de 1.0 a 4.0-f7 |

## Privacidad

La app **no declara el permiso `INTERNET`**. No hay cuentas, analítica ni publicidad:
todo (texto, OCR, voz) se procesa en el dispositivo. Detalle en
[docs/06-PRIVACIDAD.md](docs/06-PRIVACIDAD.md).

## Limitaciones conocidas

- Sin MOBI/AZW3 (formatos propietarios de Amazon) y sin DRM.
- Solo `arm64-v8a`: no arranca en teléfonos de 32 bits.
- El OCR es solo para PDF; las imágenes dentro de un EPUB no se reconocen.
- La primera pasada de OCR tarda segundos por página; después queda en caché.
- El EPUB usa scroll por capítulo (o modo página); no hay paginación tipográfica perfecta.
- El fundido del temporizador baja por frase, no muestra a muestra.
- APK firmado con clave de depuración: no válido para Google Play.

## Licencia

El código de la app no tiene licencia publicada todavía: si vas a distribuirla, añade
una (MIT o Apache-2.0 son las habituales). Los componentes de terceros —sherpa-onnx,
modelo Supertonic 3, PDFBox, ML Kit, Compose— conservan sus propias licencias, detalladas
en [docs/05-VOCES-Y-LICENCIAS.md](docs/05-VOCES-Y-LICENCIAS.md).
