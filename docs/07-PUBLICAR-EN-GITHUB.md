# Publicar en GitHub

## Antes de nada: el APK no va en el repositorio

GitHub **rechaza archivos de más de 100 MB** y este APK pesa ~179 MB. Por eso el paquete
viene preparado así:

| Qué | Dónde va |
|---|---|
| Código, documentación y configuración | repositorio Git |
| `apk/Lector-de-Libros-4.0-f6.apk` | **Release** de GitHub (límite 2 GB por archivo) |

El `.gitignore` ya excluye `apk/`, `build/`, `.gradle/`, `local.properties` y las carpetas
pesadas de modelos descargados. Así el repositorio queda en unos pocos MB y se puede
clonar rápido.

## 1 · Crear el repositorio

Con [GitHub CLI](https://cli.github.com/) (recomendado):

```powershell
cd "C:\Users\Antonio\Desktop\LECTOR EDGARDO"
git init -b main
git add .
git commit -m "Lector de Libros 4.0-f6: documentacion y codigo fuente"
gh repo create lector-de-libros --public --source=. --remote=origin --push
```

Sin `gh`, créalo a mano en <https://github.com/new> y luego:

```powershell
git init -b main
git add .
git commit -m "Lector de Libros 4.0-f6"
git remote add origin https://github.com/TU-USUARIO/lector-de-libros.git
git push -u origin main
```

Si `git commit` se queja de identidad:

```powershell
git config user.name "Tu Nombre"
git config user.email "tu-correo@ejemplo.com"
```

## 2 · Publicar el APK como Release

```powershell
gh release create v4.0-f6 `
  "apk\Lector-de-Libros-4.0-f6.apk" `
  --title "Lector de Libros 4.0-f6" `
  --notes "Version estable de la serie 4.0: fluidez, marcadores, busqueda, temporizador, modo pagina, zoom, OCR local, salto de pagina narrando y caratula en segundo plano."
```

El enlace de descarga directa queda así:

```
https://github.com/TU-USUARIO/lector-de-libros/releases/download/v4.0-f6/Lector-de-Libros-4.0-f6.apk
```

Ese es el enlace que conviene poner en el README para que la gente lo instale desde el
móvil. Si prefieres no tocar `gh`, puedes subirlo a mano: *Releases → Draft a new release →
Attach binaries*.

## 3 · Qué incluye el repositorio

```
README.md                 presentacion y guia rapida
CHANGELOG.md              1.0 -> 4.0-f6
docs/                     arquitectura, instalacion, compilacion, OCR, licencias, privacidad
app/                      codigo fuente Kotlin + recursos + assets del modelo
gradle/ , gradlew , gradlew.bat
build.gradle.kts , settings.gradle.kts , gradle.properties
.gitignore
```

**No** se sube: `apk/` (va en Releases), `build/`, `.gradle/`, `local.properties`,
`vendor-downloads/` y los APK antiguos de la carpeta de trabajo.

## 4 · Si quieres el historial de versiones en el repo

Puedes crear una rama o etiqueta por versión:

```powershell
git tag -a v4.0-f6 -m "Version 4.0-f6 (versionCode 35)"
git push origin v4.0-f6
```

Los APK antiguos (1.0 → 3.8) conviene subirlos como assets de sus propias Releases, nunca
dentro del árbol de Git.

## 5 · Alternativa con Git LFS

Solo si de verdad necesitas el APK versionado junto al código:

```powershell
git lfs install
git lfs track "*.apk"
git add .gitattributes
```

Aun así, cada versión del APK añade ~179 MB al historial: es mejor Releases.

## 6 · Checklist antes de publicar

- [ ] `versionCode` / `versionName` correctos en `app/build.gradle.kts` (35 / 4.0-f6).
- [ ] `.\gradlew.bat :app:testDebugUnitTest` en verde.
- [ ] README con el enlace de descarga del Release.
- [ ] `docs/05-VOCES-Y-LICENCIAS.md` incluido (modelo Supertonic es OpenRAIL-M).
- [ ] Decidir la licencia del código (MIT o Apache-2.0) y añadir `LICENSE`.
- [ ] Revisar que no se suben libros, claves ni `local.properties`.
