# Privacidad

## Resumen

La app **no pide permiso de red**. Su `AndroidManifest.xml` declara únicamente:

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

Sin `android.permission.INTERNET`, el sistema operativo impide cualquier conexión de red
aunque el código lo intentara. En la práctica: **no hay cuentas, analítica, publicidad ni
telemetría**, y ningún libro, texto o audio sale del teléfono.

## Qué se guarda y dónde

| Dato | Ubicación | Se borra al desinstalar |
|---|---|---|
| Biblioteca (títulos, rutas, progreso) | `files/library.json` (escritura atómica) | Sí |
| Marcadores y notas | `files/bookmarks.json` | Sí |
| Caché de OCR | `files/ocr/<id>.json` | Sí |
| Portadas cacheadas | caché interna de la app | Sí |
| Preferencias (letra, modo página, voz) | `SharedPreferences` | Sí |

Los **archivos originales** de los libros no se copian ni se modifican: la app guarda la
referencia al documento que elegiste y lo lee con permiso del sistema de archivos.

## Procesamiento

- **Texto y renglones**: PDFBox y parsers propios, en el dispositivo.
- **OCR**: ML Kit con el modelo latino **incluido en el APK**; no requiere Play Services
  ni descarga de modelos.
- **Voz**: sherpa-onnx + Supertonic 3 dentro del APK; síntesis local.
- **Búsqueda y subrayado**: locales, sobre el texto ya extraído.

## Permisos que sí usa y para qué

| Permiso | Motivo |
|---|---|
| `FOREGROUND_SERVICE` / `..._MEDIA_PLAYBACK` | seguir narrando con la pantalla apagada o la app en segundo plano |
| `WAKE_LOCK` | bloqueo parcial de CPU, acotado en el tiempo y liberado al pausar o cerrar |

El acceso a los libros se hace por el selector de archivos del sistema (SAF), sin pedir
permisos amplios de almacenamiento.

## Copias de seguridad

`android:allowBackup="true"`: si el teléfono tiene copia de seguridad de Android activada,
la biblioteca y los marcadores pueden incluirse en ella. Si prefieres que no, cambia ese
atributo a `false` en `AndroidManifest.xml`.
