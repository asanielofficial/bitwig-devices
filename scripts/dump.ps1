# Decode any .bwdevice / .bwmodulator to a readable tree:  scripts\dump.ps1 devices\Tremolo+.bwdevice out.txt
param([Parameter(Mandatory=$true)][string]$InFile, [Parameter(Mandatory=$true)][string]$OutFile)
. "$PSScriptRoot\common.ps1"
Invoke-Native java @("-cp", $ClassPath, "Dump", (Resolve-Path $InFile).Path, $OutFile)
