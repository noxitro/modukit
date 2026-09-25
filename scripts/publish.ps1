<#
  .SYNOPSIS
  modukit のアプリをビルドして、nox-apk-manager の配布フォルダ(Google Drive の builds/)に置く。

  .DESCRIPTION
  - ビルドは Android Studio 同梱の JBR で行う(PATH の java は古いことがある)
  - 配布は nox-apk-manager の scripts/publish-apk.ps1 に任せる(APK のコピーと meta.json の更新)
  - project 名(builds/ 直下のフォルダ名)は apps/ のフォルダ名と同じ
  - 説明文は apps/<App>/distribution/description.txt、未インストールの行に出すアイコンは同じ場所の icon.png
  - 配布ルートは publish-apk.ps1 と同じく環境変数 NOX_BUILDS_ROOT で差し替えられる

  .EXAMPLE
  pwsh scripts\publish.ps1                   # wake-update の release を置く
  pwsh scripts\publish.ps1 -Variant both     # release と debug の両方を置く
  pwsh scripts\publish.ps1 -SkipBuild        # ビルド済みの APK をそのまま置く
#>
param(
    [string]$App = "wake-update",
    [ValidateSet("release", "debug", "both")][string]$Variant = "release",
    [switch]$SkipBuild,
    # nox-apk-manager の clone 先。既定は環境変数 NOX_APK_MANAGER、無ければ modukit と同じ親フォルダ
    [string]$ManagerDir = $null
)

$ErrorActionPreference = "Stop"
$repo = Split-Path -Parent $PSScriptRoot
$appDir = Join-Path $repo "apps\$App"
if (-not (Test-Path $appDir)) { throw "アプリが無い: $appDir" }

if (-not $ManagerDir) {
    $ManagerDir = if ($env:NOX_APK_MANAGER) { $env:NOX_APK_MANAGER } else { Join-Path (Split-Path -Parent $repo) "nox-apk-manager" }
}
$publishApk = Join-Path $ManagerDir "scripts\publish-apk.ps1"
if (-not (Test-Path $publishApk)) {
    throw "publish-apk.ps1 が無い: $publishApk (-ManagerDir か環境変数 NOX_APK_MANAGER で nox-apk-manager の場所を指定)"
}

$variants = if ($Variant -eq "both") { @("release", "debug") } else { @($Variant) }

if (-not $SkipBuild) {
    $jbr = "C:\Program Files\Android\Android Studio\jbr"
    if (Test-Path $jbr) { $env:JAVA_HOME = $jbr }
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
    # Android Studio で一度も開いていないと local.properties が無いので、既定の SDK の場所を教える
    if (-not $env:ANDROID_HOME -and -not (Test-Path (Join-Path $repo "local.properties"))) {
        $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
    }
    $tasks = $variants | ForEach-Object { ":apps:${App}:assemble" + $_.Substring(0, 1).ToUpper() + $_.Substring(1) }
    Push-Location $repo
    try {
        & .\gradlew.bat @tasks
        if ($LASTEXITCODE -ne 0) { throw "gradle failed with exit code $LASTEXITCODE" }
    } finally {
        Pop-Location
    }
}

$descriptionFile = Join-Path $appDir "distribution\description.txt"
$description = if (Test-Path $descriptionFile) { (Get-Content -Raw -Encoding UTF8 $descriptionFile).Trim() } else { $null }

foreach ($v in $variants) {
    $apk = Join-Path $appDir "build\outputs\apk\$v\$App-$v.apk"
    & $publishApk -Project $App -Apk $apk -Variant $v -Description $description
}

$root = if ($env:NOX_BUILDS_ROOT) { $env:NOX_BUILDS_ROOT } else { "G:\マイドライブ\builds" }
$icon = Join-Path $appDir "distribution\icon.png"
if (Test-Path $icon) {
    $iconDest = Join-Path $root "$App\icon.png"
    Copy-Item -Path $icon -Destination $iconDest -Force
    Write-Output "icon: $iconDest"
}
