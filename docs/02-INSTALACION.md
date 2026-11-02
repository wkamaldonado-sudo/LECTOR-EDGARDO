# Instalar en el teléfono

## Requisitos

- Android 7.0 (API 24) o superior.
- Teléfono **arm64-v8a** (prácticamente todos los móviles desde 2016).
- ~400 MB libres (instalación + modelos + caché de OCR).

## Opción 1 — desde el propio teléfono

1. Copia `apk/Lector-de-Libros-4.0-f7.apk` al móvil (cable, Drive, Telegram…).
2. Ábrelo desde el explorador de archivos.
3. Si Android lo pide, permite **«Instalar apps desconocidas»** para esa aplicación.
4. Pulsa **Instalar** y luego **Abrir**.

## Opción 2 — por USB con adb (recomendada para actualizar)

Con **Depuración USB** activada (Ajustes → Opciones de desarrollador) y el cable puesto:

```powershell
adb devices
adb install -r "C:\Users\Antonio\Desktop\LECTOR EDGARDO\apk\Lector-de-Libros-4.0-f7.apk"
```

- `adb devices` debe mostrar tu móvil como `device`.
- `-r` reinstala conservando biblioteca, progreso, marcadores y ajustes.
- Si sale `unauthorized`, desbloquea el teléfono y acepta «¿Permitir depuración USB?».

Comprobar la versión instalada:

```powershell
adb shell dumpsys package com.antonio.lector | findstr versionName
```

Debe responder `versionName=4.0-f7`.

Abrir la app:

```powershell
adb shell monkey -p com.antonio.lector -c android.intent.category.LAUNCHER 1
```

## Actualizar desde 3.8 o anterior

No hay que desinstalar nada: el `applicationId` y la clave de firma no cambian, así que
Android la trata como una actualización. Si tuvieras instalada una compilación firmada
con otra clave, Android rechazará la instalación con `INSTALL_FAILED_UPDATE_INCOMPATIBLE`;
en ese caso hay que desinstalar (se pierde la biblioteca) o exportar antes los archivos.

## Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| `adb: no devices/emulators found` | Cable solo de carga, puerto USB defectuoso o depuración USB desactivada. |
| `unauthorized` | Falta aceptar el diálogo en la pantalla del teléfono (desbloquéalo). |
| `INSTALL_FAILED_INSUFFICIENT_STORAGE` | Libera espacio: el APK son ~179 MB más los datos. |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Otra clave de firma instalada; desinstala la versión anterior. |
| La app se cierra al abrir | Comprueba que el teléfono es `arm64-v8a` y que la versión de Android es 7.0+. |
| No suena la voz natural | Abre un libro → **Voz** → elige un perfil F1–M5; la primera carga del modelo tarda unos segundos. |
| El PDF escaneado no se lee | **☰ → Reconocer texto (OCR)**; si no detecta las páginas, marca «Forzar OCR en todas las páginas». |

## Desinstalar

```powershell
adb uninstall com.antonio.lector
```

Se borran biblioteca, progreso, marcadores y la caché de OCR. Los archivos originales de
los libros no se tocan: la app solo guarda referencias al documento importado.
