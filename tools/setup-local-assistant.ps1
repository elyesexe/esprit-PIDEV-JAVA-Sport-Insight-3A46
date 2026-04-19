param(
    [string]$Model = "qwen2.5:3b",
    [string]$Voice = "en_US-ryan-low"
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

function Get-OllamaExe {
    $candidatePaths = @(
        (Join-Path $env:LOCALAPPDATA "Programs\Ollama\ollama.exe"),
        (Join-Path $env:LOCALAPPDATA "Programs\Ollama\ollama app.exe")
    )

    foreach ($candidate in $candidatePaths) {
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    $command = Get-Command ollama -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    return $null
}

function Install-Ollama {
    $installerPath = Join-Path $env:TEMP ("OllamaSetup-" + [guid]::NewGuid().ToString() + ".exe")
    Write-Host "Downloading Ollama installer..."
    Invoke-WebRequest -Uri "https://ollama.com/download/OllamaSetup.exe" -OutFile $installerPath

    Write-Host "Installing Ollama silently..."
    Start-Process -FilePath $installerPath -ArgumentList "/S" -Wait

    Remove-Item -LiteralPath $installerPath -Force -ErrorAction SilentlyContinue
}

function Ensure-OllamaServer {
    param(
        [string]$OllamaExe
    )

    try {
        Invoke-RestMethod -Uri "http://127.0.0.1:11434/api/tags" -TimeoutSec 5 | Out-Null
        return
    } catch {
        Write-Host "Starting local Ollama server..."
        Start-Process -WindowStyle Hidden -FilePath $OllamaExe -ArgumentList "serve" | Out-Null
    }

    $deadline = (Get-Date).AddMinutes(5)
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 3
        try {
            Invoke-RestMethod -Uri "http://127.0.0.1:11434/api/tags" -TimeoutSec 5 | Out-Null
            return
        } catch {
        }
    }

    throw "Ollama server did not come online within 5 minutes."
}

function Get-PiperVoiceInfo {
    param(
        [string]$VoiceName
    )

    switch ($VoiceName) {
        "en_US-ryan-low" {
            return @{
                Label = "Piper Ryan"
                ModelUri = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/ryan/low/en_US-ryan-low.onnx?download=true"
                ConfigUri = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/ryan/low/en_US-ryan-low.onnx.json?download=true"
            }
        }
        default {
            throw "Unsupported voice '$VoiceName'. Add it to setup-local-assistant.ps1 before using it."
        }
    }
}

function Install-PiperAssets {
    param(
        [string]$VoiceName
    )

    $assistantRoot = Join-Path $env:USERPROFILE ".sport-insight\assistant\voice"
    $piperDir = Join-Path $assistantRoot "piper"
    $piperExe = Join-Path $piperDir "piper.exe"
    $voiceDir = Join-Path $assistantRoot "voices"
    $voiceModel = Join-Path $voiceDir ($VoiceName + ".onnx")
    $voiceConfig = Join-Path $voiceDir ($VoiceName + ".onnx.json")

    New-Item -ItemType Directory -Path $assistantRoot -Force | Out-Null

    if (-not (Test-Path $piperExe)) {
        $zipPath = Join-Path $assistantRoot "piper_windows_amd64.zip"
        Write-Host "Downloading Piper Windows runtime..."
        Invoke-WebRequest -Uri "https://github.com/rhasspy/piper/releases/download/2023.11.14-2/piper_windows_amd64.zip" -OutFile $zipPath
        Write-Host "Extracting Piper runtime..."
        Expand-Archive -Path $zipPath -DestinationPath $assistantRoot -Force
        Remove-Item -LiteralPath $zipPath -Force -ErrorAction SilentlyContinue
    }

    $voiceInfo = Get-PiperVoiceInfo -VoiceName $VoiceName
    New-Item -ItemType Directory -Path $voiceDir -Force | Out-Null

    if (-not (Test-Path $voiceModel)) {
        Write-Host "Downloading Piper voice model $VoiceName..."
        Invoke-WebRequest -Uri $voiceInfo.ModelUri -OutFile $voiceModel
    }

    if (-not (Test-Path $voiceConfig)) {
        Write-Host "Downloading Piper voice config $VoiceName..."
        Invoke-WebRequest -Uri $voiceInfo.ConfigUri -OutFile $voiceConfig
    }

    return @{
        PiperExe = $piperExe
        VoiceModel = $voiceModel
        VoiceLabel = $voiceInfo.Label
    }
}

function Install-WhisperAssets {
    $sttRoot = Join-Path $env:USERPROFILE ".sport-insight\assistant\stt"
    $runtimeDir = Join-Path $sttRoot "whisper"
    $runtimeZip = Join-Path $runtimeDir "whisper-bin-x64.zip"
    $runtimeExe = Join-Path $runtimeDir "Release\whisper-cli.exe"
    $modelDir = Join-Path $sttRoot "models"
    $modelPath = Join-Path $modelDir "ggml-base.bin"

    New-Item -ItemType Directory -Path $runtimeDir -Force | Out-Null
    New-Item -ItemType Directory -Path $modelDir -Force | Out-Null

    if (-not (Test-Path $runtimeExe)) {
        Write-Host "Downloading Whisper runtime..."
        Invoke-WebRequest -Uri "https://github.com/ggml-org/whisper.cpp/releases/download/v1.8.4/whisper-bin-x64.zip" -OutFile $runtimeZip
        Write-Host "Extracting Whisper runtime..."
        Expand-Archive -Path $runtimeZip -DestinationPath $runtimeDir -Force
        Remove-Item -LiteralPath $runtimeZip -Force -ErrorAction SilentlyContinue
    }

    if (-not (Test-Path $modelPath)) {
        Write-Host "Downloading Whisper speech model..."
        Invoke-WebRequest -Uri "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin?download=true" -OutFile $modelPath
    }

    return @{
        WhisperExe = $runtimeExe
        WhisperModel = $modelPath
    }
}

$ollamaExe = Get-OllamaExe
if (-not $ollamaExe) {
    Install-Ollama
    $ollamaExe = Get-OllamaExe
}

if (-not $ollamaExe) {
    throw "Ollama installation finished, but ollama.exe was not found."
}

$env:PATH = (Split-Path $ollamaExe -Parent) + ";" + $env:PATH

Ensure-OllamaServer -OllamaExe $ollamaExe

Write-Host "Pulling local model $Model ..."
& $ollamaExe pull $Model

$piperSetup = Install-PiperAssets -VoiceName $Voice
$whisperSetup = Install-WhisperAssets

Write-Host ""
Write-Host "Local assistant setup finished."
Write-Host "Ollama executable: $ollamaExe"
Write-Host "Installed model: $Model"
Write-Host "Piper executable: $($piperSetup.PiperExe)"
Write-Host "Installed voice: $($piperSetup.VoiceLabel) ($Voice)"
Write-Host "Whisper executable: $($whisperSetup.WhisperExe)"
Write-Host "Whisper model: $($whisperSetup.WhisperModel)"
