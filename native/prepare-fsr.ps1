param([switch]$Offline)
$ErrorActionPreference = 'Stop'
$workspace = Split-Path $PSScriptRoot
$manifest = Get-Content (Join-Path $PSScriptRoot 'fsr-dependencies.json') -Raw | ConvertFrom-Json
foreach ($file in $manifest.files) {
    $relative = [string]$file.path
    if ($relative.Contains('..') -or !$relative.StartsWith('Kits/FidelityFX/')) { throw 'Invalid SDK manifest path' }
    $destination = Join-Path (Join-Path $workspace 'build/fsr-sdk') $relative
    if (!(Test-Path -LiteralPath $destination)) {
        if ($Offline) { throw "Missing FSR dependency $relative; run native/prepare-fsr.ps1 once with network access" }
        New-Item -ItemType Directory -Force (Split-Path $destination) | Out-Null
        Invoke-WebRequest "https://raw.githubusercontent.com/GPUOpen-LibrariesAndSDKs/FidelityFX-SDK/$($manifest.version)/$relative" -OutFile $destination
    }
    if ((Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash -ne $file.sha256) { throw "FSR dependency hash mismatch: $relative" }
}
Write-Output "Verified AMD FidelityFX $($manifest.version) dependencies. The runtime license is build/fsr-sdk/Kits/FidelityFX/docs/license.md."
