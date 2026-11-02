# Prepara el paquete LECTOR EDGARDO para GitHub.
# Copia el APK 4.0-f6 (y opcionalmente el codigo fuente) y deja el repositorio listo.
# Uso: doble clic en COMPLETAR-PAQUETE.cmd

param([switch]$ConCodigo)

$ErrorActionPreference = 'Stop'
$destino  = Split-Path -Parent $MyInvocation.MyCommand.Path
$proyecto = 'C:\Users\Antonio\Documents\APK Libro\Lector de Libros'
$apkDir   = Join-Path $proyecto 'app\build\outputs\apk\debug'
$apkSrc   = Join-Path $apkDir 'app-debug.apk'
$metaSrc  = Join-Path $apkDir 'output-metadata.json'
$apkDst   = Join-Path $destino 'apk\Lector-de-Libros-4.0-f6.apk'

function Linea($texto) { Write-Host $texto }

Linea ''
Linea '=== LECTOR EDGARDO · preparar paquete ==='
Linea ''

# --- 1. Comprobar el APK -------------------------------------------------
if (-not (Test-Path $apkSrc)) {
    Linea "ERROR: no existe el APK compilado:"
    Linea "  $apkSrc"
    Linea 'Compilalo antes con:  .\gradlew.bat :app:assembleDebug'
    exit 1
}

if (Test-Path $metaSrc) {
    $meta = Get-Content $metaSrc -Raw
    if ($meta -notmatch '4\.0-f6') {
        Linea 'AVISO: el output-metadata.json no dice 4.0-f6. Se copia igual, revisa la version.'
    }
}

New-Item -ItemType Directory -Force -Path (Split-Path -Parent $apkDst) | Out-Null
Copy-Item -Path $apkSrc -Destination $apkDst -Force
$mb = [math]::Round((Get-Item $apkDst).Length / 1MB, 1)
Linea "OK  APK copiado -> apk\Lector-de-Libros-4.0-f6.apk ($mb MB)"

# --- 2. Documentacion original del proyecto ------------------------------
$docsOrig = Join-Path $destino 'docs\original'
New-Item -ItemType Directory -Force -Path $docsOrig | Out-Null
foreach ($f in @('README.md', 'VOCES-INTEGRADAS.md')) {
    $src = Join-Path $proyecto $f
    if (Test-Path $src) {
        Copy-Item $src (Join-Path $docsOrig $f) -Force
        Linea "OK  copiado docs\original\$f"
    }
}

# --- 3. Codigo fuente (opcional) -----------------------------------------
if ($ConCodigo) {
    Linea ''
    Linea 'Copiando codigo fuente (puede tardar: incluye los modelos de voz)...'
    $codigo = Join-Path $destino 'codigo'
    New-Item -ItemType Directory -Force -Path $codigo | Out-Null

    $excluirDir = @('build', '.gradle', '.idea', 'vendor-downloads', '.git', 'captures', '.cxx')
    $excluirFil = @('*.apk', '*.aab', '*.zip', '*.iml', 'local.properties')

    $args = @(
        $proyecto, $codigo, '/E', '/NFL', '/NDL', '/NJH', '/NJS', '/NP',
        '/XD') + $excluirDir + @('/XF') + $excluirFil
    & robocopy @args | Out-Null
    if ($LASTEXITCODE -ge 8) {
        Linea "ERROR: robocopy fallo (codigo $LASTEXITCODE)."
        exit 1
    }
    Linea 'OK  codigo copiado en codigo\'
    Linea '    Recuerda: los modelos grandes van en codigo\app\src\main\assets\supertonic'
    Linea '    (si superan 100 MB, dejalos fuera y publicalos como Release o descarga aparte).'
}
else {
    Linea ''
    Linea 'Codigo fuente NO copiado (solo documentacion + APK).'
    Linea 'Si lo quieres:  .\COMPLETAR-PAQUETE.cmd -ConCodigo   (o edita el .cmd)'
}

# --- 4. Repositorio git --------------------------------------------------
Linea ''
$git = Get-Command git -ErrorAction SilentlyContinue
if (-not $git) {
    Linea 'git no esta instalado: instala Git for Windows y repite este paso.'
    Linea 'El contenido del paquete ya esta listo igualmente.'
}
else {
    Push-Location $destino
    try {
        if (-not (Test-Path (Join-Path $destino '.git'))) {
            $resp = Read-Host '¿Crear el repositorio git e hacer el primer commit? (S/N)'
            if ($resp -match '^[sSyY]') {
                git init -b main | Out-Null
                $nombre = git config user.name
                if (-not $nombre) { git config user.name  'Antonio' }
                $correo = git config user.email
                if (-not $correo) { git config user.email 'antonio@localhost' }
                git add -A
                git commit -m 'Lector de Libros 4.0-f6: documentacion y APK' | Out-Null
                Linea 'OK  repositorio git creado con el primer commit.'
                Linea 'Siguiente paso:  gh repo create lector-de-libros --public --source=. --remote=origin --push'
            }
            else {
                Linea 'Repositorio git no creado. Puedes hacerlo luego con: git init -b main'
            }
        }
        else {
            Linea 'Ya existe .git en esta carpeta: no se toca.'
        }
    }
    finally { Pop-Location }
}

Linea ''
Linea '=== LISTO ==='
Linea "Carpeta: $destino"
Linea ''
Linea 'Para subir a GitHub (ver docs\07-PUBLICAR-EN-GITHUB.md):'
Linea '  git remote add origin https://github.com/TU-USUARIO/lector-de-libros.git'
Linea '  git push -u origin main'
Linea '  gh release create v4.0-f6 "apk\Lector-de-Libros-4.0-f6.apk" --title "4.0-f6"'
Linea ''
