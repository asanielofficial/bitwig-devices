# Build one device:  powershell -NoProfile -File scripts\build.ps1 tremolo-plus
param([Parameter(Mandatory=$true)][string]$Device)
. "$PSScriptRoot\common.ps1"
$propFile = Join-Path $Root "src\$Device\device.properties"
if (-not (Test-Path $propFile)) { throw "missing $propFile" }
$props = @{}
Get-Content $propFile | Where-Object { $_ -match "^\s*[^#].*=" } | ForEach-Object { $k,$v = $_ -split "=",2; $props[$k.Trim()] = $v.Trim() }
$ref = if ($env:REFERENCE_DIR) { $env:REFERENCE_DIR } else { Join-Path $Root "reference" }
foreach ($t in ($props["templates"] -split "\s+")) { if (-not (Test-Path (Join-Path $ref $t))) { throw "missing template $ref\$t (see docs\BUILDING.md)" } }
if (-not (Test-Path $BitwigIcons)) { throw "icons folder not found: $BitwigIcons (set BITWIG_ICONS)" }
Invoke-Native java @("-cp", $ClassPath, $props["main"], "--reference", $ref, "--icons", $BitwigIcons,
  "--nit", (Join-Path $Root $props["nit"]), "--out", (Join-Path $Root $props["output"]))
