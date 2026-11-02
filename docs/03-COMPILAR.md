# Compilar

## Requisitos

| Herramienta | Versión | Nota |
|---|---|---|
| JDK | 17 | Probado con Eclipse Temurin `jdk-17.0.20.101-hotspot` |
| Android SDK | Platform 35 + Build-Tools | `C:\Android\sdk` en este equipo |
| Gradle | 8.9 | Se usa el wrapper incluido (`gradlew.bat`) |
| Conexión | Solo la primera vez | Descarga dependencias de Maven |

`local.properties` (no se sube al repositorio) debe apuntar al SDK:

```properties
sdk.dir=C\:\\Android\\sdk
```

## Compilar

```powershell
cd "C:\Users\Antonio\Documents\APK Libro\Lector de Libros"

$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
$env:ANDROID_HOME = 'C:\Android\sdk'

.\gradlew.bat :app:assembleDebug
```

Salida: `app\build\outputs\apk\debug\app-debug.apk`.

Variantes:

| Comando | Resultado |
|---|---|
| `:app:assembleDebug` | APK de depuración, el que se instala en el teléfono |
| `:app:assembleRelease` | APK «release», también firmado con la clave de depuración |
| `:app:compileDebugKotlin` | Solo comprobación rápida de compilación |
| `:app:testDebugUnitTest` | Tests JVM (offsets, búsqueda, OCR, calidad de texto) |

## Instalar lo compilado

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Firma

`release` reutiliza `signingConfigs.getByName("debug")` para que el APK se pueda instalar
sin configurar claves. Consecuencias:

- Sirve para uso personal y para publicarlo como descarga directa.
- **No** sirve para Google Play: hay que crear un keystore propio y un `signingConfig`.
- Si algún día cambias de clave, la actualización sobre la app instalada fallará con
  `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.

## Estructura del build

```
settings.gradle.kts        módulos y repositorios
build.gradle.kts           plugins (AGP + Kotlin + Compose)
app/build.gradle.kts       versionCode/versionName, SDK, dependencias, ABI
gradle/wrapper/            Gradle 8.9
app/src/main/assets/supertonic/   modelos ONNX int8, voice.bin, tts.json
app/src/main/jniLibs/arm64-v8a/   sherpa-onnx y onnxruntime
```

## Avisos conocidos

- Tres avisos de deprecación en `NarrationService.kt` por constructores antiguos de
  `Notification.Builder`. No afectan al funcionamiento (API 24+).
- `-Xlambdas=class` es obligatorio: el callback JNI del motor busca el método tipado
  `invoke(FloatArray)`; con lambdas `invokedynamic` no lo encuentra y no suena.

## Si Gradle falla

1. **`JAVA_HOME` mal o ausente** → define la ruta del JDK 17 antes de compilar.
2. **Caché corrupta** (metadatos ausentes en `~/.gradle/caches/8.9`) → `.\gradlew.bat --stop`
   y borra esa carpeta; Gradle la reconstruye.
3. **SDK no encontrado** → revisa `local.properties` o `ANDROID_HOME`.
4. **Sin espacio** → la compilación necesita varios GB en `app/build` y en la caché.
