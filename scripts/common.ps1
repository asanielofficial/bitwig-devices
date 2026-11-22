# shared by the other scripts (dot-source it, do not run it)
$script:Root = Split-Path -Parent $PSScriptRoot
$script:Work = Join-Path $Root ".work"
if (-not $env:BITWIG_DIR) { $env:BITWIG_DIR = "C:\Program Files\Bitwig Studio" }
$script:BitwigJar   = if ($env:BITWIG_JAR)   { $env:BITWIG_JAR }   else { Join-Path $env:BITWIG_DIR "bin\bitwig.jar" }
$script:LibsJar     = if ($env:LIBS_JAR)     { $env:LIBS_JAR }     else { Join-Path $env:BITWIG_DIR "bin\libs.jar" }
$script:BitwigIcons = if ($env:BITWIG_ICONS) { $env:BITWIG_ICONS } else { Join-Path $env:BITWIG_DIR "resources\icons" }
$script:ClassPath   = (Join-Path $Work "classes") + ";" + (Join-Path $Work "tools")
$ErrorActionPreference = "Stop"
function Invoke-Native { param([string]$Exe, [string[]]$Arguments)
  & $Exe @Arguments
  if ($LASTEXITCODE -ne 0) { throw "$Exe failed with exit code $LASTEXITCODE" } }
