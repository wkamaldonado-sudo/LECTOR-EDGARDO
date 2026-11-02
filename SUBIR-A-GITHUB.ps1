# Sube este paquete al repositorio de GitHub y publica el APK como Release.
# Uso:  doble clic en SUBIR-A-GITHUB.cmd   (o este .ps1 desde PowerShell)

[CmdletBinding()]
param(
    [string]$RepoUrl = 'https://github.com/wkamaldonado-sudo/LECTOR-EDGARDO.git',
    [string]$Tag     = 'v4.0-f7',
    [string]$Titulo  = 'Lector de Libros 4.0-f7'
)

$ErrorActionPreference = 'Stop'

function Linea([string]$t = '') { Write-Host $t }
function Fallo([string]$t) {
    Write-Host ''
    Write-Host "ERROR: $t" -ForegroundColor Red
    Write-Host ''
    Read-Host 'Pulsa Enter para cerrar'
    exit 1
}

$raiz = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $raiz
Linea "Carpeta: $raiz"
Linea "Repositorio: $RepoUrl"
Linea ''

# ---------------------------------------------------------------- requisitos
if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    Fallo 'git no esta instalado o no esta en el PATH. Instala Git para Windows y vuelve a intentarlo.'
}

$apk = Join-Path $raiz 'apk\Lector-de-Libros-4.0-f7.apk'
if (-not (Test-Path $apk)) {
    Fallo "No encuentro el APK en apk\Lector-de-Libros-4.0-f7.apk. Ejecuta antes COMPLETAR-PAQUETE.cmd."
}
$mb = [math]::Round((Get-Item $apk).Length / 1MB, 2)
Linea "APK: $mb MB"
if ($mb -lt 100) {
    Linea 'AVISO: ese APK parece incompleto (menos de 100 MB). Revisalo antes de publicar.'
}

# ---------------------------------------------------------------- repositorio
if (-not (Test-Path (Join-Path $raiz '.git'))) {
    Linea 'No hay repositorio local: creandolo.'
    git init -b main 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) { Fallo 'git init fallo.' }
}

if (-not (git config user.name)) { git config user.name 'Antonio' }
if (-not (git config user.email)) { git config user.email 'antonio@localhost' }

# La rama debe llamarse main antes de subir.
$rama = git rev-parse --abbrev-ref HEAD 2>$null
if ($rama -and $rama -ne 'main') {
    Linea "La rama local era '$rama': renombrandola a main."
    git branch -M main
}

if (-not (Test-Path (Join-Path $raiz '.gitignore'))) {
    Fallo 'Falta .gitignore. Sin el, el APK de 179 MB entraria en el commit y el push seria rechazado.'
}

# El APK nunca debe estar versionado (GitHub rechaza archivos de mas de 100 MB).
$rastreado = git ls-files 'apk'
if ($rastreado) {
    Linea 'El APK estaba versionado: quitandolo del indice (el archivo se queda en disco).'
    git rm -r --cached 'apk' 2>$null | Out-Null
}

# ---------------------------------------------------------------- remoto
$remotos = @(git remote)
if ($remotos -contains 'origin') {
    git remote set-url origin $RepoUrl
} else {
    git remote add origin $RepoUrl
}
Linea 'OK  remoto origin configurado.'

# Credenciales: si GitHub CLI esta autenticado, que git lo use.
$gh = Get-Command gh -ErrorAction SilentlyContinue
if ($gh) {
    & gh auth setup-git 2>$null | Out-Null
}
else {
    Linea ''
    Linea 'AVISO: GitHub CLI (gh) no esta instalado. Solo se subira el codigo;'
    Linea '       el APK tendras que subirlo a mano como Release (docs\07-PUBLICAR-EN-GITHUB.md).'
}

# ---------------------------------------------------------------- commit
git add -A
$pendiente = @(git status --porcelain)
if ($pendiente.Count -gt 0) {
    git commit -m "Lector de Libros 4.0-f7: documentacion y paquete" | Out-Null
    if ($LASTEXITCODE -ne 0) { Fallo 'El commit fallo.' }
    Linea "OK  commit creado ($($pendiente.Count) archivos)."
} else {
    Linea 'Sin cambios nuevos que commitear.'
}

# ---------------------------------------------------------------- integrar remoto
Linea ''
Linea 'Consultando el repositorio remoto...'
git fetch origin 2>$null | Out-Null

$ramaRemota = (git ls-remote --heads origin main 2>$null)
if ($ramaRemota) {
    git merge-base HEAD origin/main 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Linea 'El remoto tiene historial distinto (README inicial de GitHub): uniendo.'
        git merge --allow-unrelated-histories -s ours origin/main -m 'Unir historial del repositorio remoto' | Out-Null
        if ($LASTEXITCODE -ne 0) {
            Fallo 'No se pudo unir el historial remoto. Revisa docs\07-PUBLICAR-EN-GITHUB.md.'
        }
        Linea 'OK  historial unido; tu contenido manda.'
    } else {
        $detras = @(git rev-list --count 'HEAD..origin/main')
        if ($detras.Count -gt 0 -and [int]$detras[0] -gt 0) {
            Linea "El remoto tiene $($detras[0]) commit(s) por delante: rebaseando."
            git rebase origin/main 2>$null | Out-Null
            if ($LASTEXITCODE -ne 0) {
                git rebase --abort 2>$null | Out-Null
                Fallo 'El rebase choco con cambios. Resuelvelo a mano o pide ayuda antes de forzar nada.'
            }
        }
    }
}

# ---------------------------------------------------------------- push
Linea ''
Linea 'Subiendo a GitHub...'
git push -u origin main
if ($LASTEXITCODE -ne 0) {
    Fallo 'El push fue rechazado. Suele ser autenticacion: ejecuta "gh auth login" y "gh auth setup-git", o revisa docs\07-PUBLICAR-EN-GITHUB.md.'
}
Linea 'OK  rama main subida.'

# ---------------------------------------------------------------- release
if ($gh) {
    Linea ''
    Linea 'Publicando el APK como Release (179 MB, unos minutos)...'

    & gh auth status *> $null
    if ($LASTEXITCODE -ne 0) {
        Linea 'gh no esta autenticado: no se publica el Release.'
        Linea '  gh auth login'
        Linea '  y vuelve a ejecutar este script (el push ya esta hecho).'
    }
    else {
        & gh release view $Tag 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Linea "El Release $Tag ya existia: reemplazando el APK."
            & gh release upload $Tag $apk --clobber
        } else {
            & gh release create $Tag $apk --title $Titulo --notes 'Version estable de la serie 4.0: fluidez, marcadores, busqueda, temporizador, modo pagina, zoom, OCR local, salto de pagina narrando y caratula en segundo plano.'
        }
        if ($LASTEXITCODE -ne 0) {
            Linea 'El Release fallo. El codigo ya esta subido; publica el APK a mano desde la web del repositorio.'
        } else {
            Linea "OK  Release $Tag publicado."
        }
    }
}

# ---------------------------------------------------------------- resumen
Linea ''
Linea '=== LISTO ==='
Linea "Repositorio:  https://github.com/wkamaldonado-sudo/LECTOR-EDGARDO"
Linea "Descarga APK: https://github.com/wkamaldonado-sudo/LECTOR-EDGARDO/releases/download/$Tag/Lector-de-Libros-4.0-f7.apk"
Linea ''
Read-Host 'Pulsa Enter para cerrar'
