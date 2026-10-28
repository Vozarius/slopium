param([switch]$Offline)
$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'prepare-fsr.ps1') -Offline:$Offline
$workspace = Split-Path $PSScriptRoot
$sdk = Join-Path $workspace 'build/fsr-sdk/Kits/FidelityFX'
$output = Join-Path $workspace 'build/native'
$vswhere = Join-Path ${env:ProgramFiles(x86)} 'Microsoft Visual Studio/Installer/vswhere.exe'
$installation = (& $vswhere -latest -products '*' -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -format json | ConvertFrom-Json)[0]
if (!$installation) { throw 'Visual Studio C++ build tools are required' }
$generator = if (([version]$installation.installationVersion).Major -ge 17) { 'Visual Studio 17 2022' } else { 'Visual Studio 16 2019' }
$cache = Join-Path $output 'CMakeCache.txt'
if (Test-Path -LiteralPath $cache) {
    $previousGenerator = Select-String -LiteralPath $cache -Pattern '^CMAKE_GENERATOR:INTERNAL=(.+)$'
    if ($previousGenerator) { $generator = $previousGenerator.Matches[0].Groups[1].Value }
}
& cmake -S $PSScriptRoot -B $output -G $generator -A x64 "-DFSR_SDK=$sdk"
if ($LASTEXITCODE) { throw 'Native CMake configuration failed' }
& cmake --build $output --config Release
if ($LASTEXITCODE) { throw 'Native compilation failed' }
