# One-time setup: extracts the classes from YOUR Bitwig install into .work\ (git-ignored) and compiles the tools.
# Run from a normal terminal:  powershell -NoProfile -ExecutionPolicy Bypass -File scripts\setup.ps1
. "$PSScriptRoot\common.ps1"
if (-not (Test-Path $BitwigJar)) { throw "not found: $BitwigJar" }
if (-not (Test-Path $LibsJar))   { throw "not found: $LibsJar" }
$classes = Join-Path $Work "classes"; $tools = Join-Path $Work "tools"
if (Test-Path $classes) { Remove-Item $classes -Recurse -Force }
if (Test-Path $tools)   { Remove-Item $tools -Recurse -Force }
New-Item -ItemType Directory -Force $classes, $tools | Out-Null
Push-Location $classes
try {
  Write-Host "extracting $BitwigJar ..."
  Invoke-Native jar @("xf", $BitwigJar)
  Remove-Item "com.class" -Force -ErrorAction SilentlyContinue      # root class named "com" shadows the com. package
  Write-Host "extracting ANTLR runtime from $LibsJar ..."
  Invoke-Native jar @("xf", $LibsJar, "org/antlr/v4")
} finally { Pop-Location }
Invoke-Native java @((Join-Path $Root "tools\src\PatchClasses.java"), $classes)
Write-Host "compiling tools ..."
$sources = @(Get-ChildItem (Join-Path $Root "tools\src\*.java")) + @(Get-ChildItem (Join-Path $Root "src\*\*.java"))
Invoke-Native javac (@("-cp", $classes, "-d", $tools) + ($sources | ForEach-Object { $_.FullName }))
Write-Host "setup done"
