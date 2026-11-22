# Syntax-check a Nitro source file with Bitwig own parser:  scripts\nitcheck.ps1 src\tremolo-plus\tremolo_plus.nit
param([Parameter(Mandatory=$true)][string]$File)
. "$PSScriptRoot\common.ps1"
Invoke-Native java @("-cp", $ClassPath, "NitCompile", (Resolve-Path $File).Path)
