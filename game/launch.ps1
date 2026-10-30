param([ValidateSet('game', 'editor')][string]$Target = 'game')
$ErrorActionPreference = 'Stop'
$jarName = if ($Target -eq 'editor') { 'AnimationEditor.jar' } else { 'Sumrak-desktop.jar' }
$jarPath = Join-Path $PSScriptRoot ('dist/' + $jarName)
$configPath = Join-Path $PSScriptRoot 'toolchain.local.json'
$jdkPath = $env:JAVA_HOME
if (-not $jdkPath -and (Test-Path -LiteralPath $configPath)) {
    $jdkPath = (Get-Content -LiteralPath $configPath -Raw -Encoding UTF8 | ConvertFrom-Json).jdk
    if ($jdkPath -and -not [System.IO.Path]::IsPathRooted($jdkPath)) { $jdkPath = Join-Path $PSScriptRoot $jdkPath }
}
$javaPath = if ($jdkPath) { Join-Path $jdkPath 'bin/javaw.exe' } else { '' }
if (-not $javaPath -or -not (Test-Path -LiteralPath $javaPath)) {
    $javaCommand = Get-Command javaw.exe -ErrorAction SilentlyContinue
    if (-not $javaCommand) { throw 'Set the JDK path in toolchain.local.json or JAVA_HOME.' }
    $javaPath = $javaCommand.Source
}
if (-not (Test-Path -LiteralPath $jarPath)) { throw 'Run Build.cmd first.' }
# This launcher opens the interactive game/editor requested by the user.
Start-Process -FilePath $javaPath -ArgumentList @('-jar', ('"' + $jarPath + '"')) -WorkingDirectory $PSScriptRoot -WindowStyle Normal
