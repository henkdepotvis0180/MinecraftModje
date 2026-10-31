$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    # Gradle stays local to the project. TLS and artifact checks remain enabled.
    $version = '8.14.3'
    $tools = Join-Path $PSScriptRoot '.tools'
    $gradle = Join-Path $tools "gradle-$version\bin\gradle.bat"
    if (!(Test-Path $gradle)) {
        New-Item -ItemType Directory -Force $tools | Out-Null
        $zip = Join-Path $tools "gradle-$version-bin.zip"
        $url = "https://downloads.gradle.org/distributions/gradle-$version-bin.zip"
        Invoke-WebRequest -Uri $url -OutFile $zip
        $expected = ((Invoke-WebRequest -Uri "$url.sha256" -UseBasicParsing).Content).Trim()
        $actual = (Get-FileHash $zip -Algorithm SHA256).Hash
        if ($actual -ine $expected) {
            Remove-Item $zip
            throw 'Gradle-controle mislukt. Het bestand is verwijderd.'
        }
        Expand-Archive $zip -DestinationPath $tools -Force
        Remove-Item $zip
    }
    & $gradle --no-daemon build
    if ($LASTEXITCODE -ne 0) { throw 'De modbuild is mislukt; zie de foutmelding hierboven.' }
    Write-Host 'Mod: helpers\build\libs\henk-helpers-0.1.0.jar' -ForegroundColor Green
} finally {
    Pop-Location
}
