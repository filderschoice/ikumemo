<#
.SYNOPSIS
    署名付きのリリース AAB（Android App Bundle）をビルドする（PLAN §6、sesami-wear と同じ手順）。

.DESCRIPTION
    scripts/version.properties に記録した現在のバージョンを基準に、versionCode を 1 増やしてビルドする。
    パラメータを指定した場合は、その値をそのまま使う（増やさない）。
    ビルドが成功したときだけ version.properties を新しい値へ更新する。

    署名情報は local.properties（.gitignore 対象）から読み込む。未設定の場合、AAB は署名なしのまま
    作られる。アップロード鍵は手元の PC だけに置き、リポジトリや GitHub へは渡さない。

.PARAMETER VersionCode
    固定で使う versionCode（整数）。省略時は現在値から 1 増やす。

.PARAMETER VersionName
    固定で使う versionName（文字列）。省略時は現在値のまま。

.EXAMPLE
    scripts\release-build.bat
    versionCode を 1 増やしてビルドする（versionName は変えない）。

.EXAMPLE
    scripts\release-build.bat -VersionCode 10 -VersionName 1.0.0
    指定した値でビルドする。
#>
param(
    [int]$VersionCode,
    [string]$VersionName
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$versionFile = Join-Path $PSScriptRoot "version.properties"

$currentVersionCode = 0
$currentVersionName = "0.1.0"

if (Test-Path $versionFile) {
    foreach ($line in Get-Content $versionFile) {
        if ($line -match "^VERSION_CODE=(.+)$") { $currentVersionCode = [int]$Matches[1] }
        if ($line -match "^VERSION_NAME=(.+)$") { $currentVersionName = $Matches[1] }
    }
} else {
    Write-Warning "scripts/version.properties が見つからないため、既定値（VERSION_CODE=0）から始めます。"
}

if ($PSBoundParameters.ContainsKey("VersionCode")) {
    $newVersionCode = $VersionCode
} else {
    $newVersionCode = $currentVersionCode + 1
}

if ($PSBoundParameters.ContainsKey("VersionName")) {
    $newVersionName = $VersionName
} else {
    $newVersionName = $currentVersionName
}

Write-Host "リリースビルドを開始します: versionCode=$newVersionCode / versionName=$newVersionName"

$gradlewPath = Join-Path $repoRoot "gradlew.bat"
if (-not (Test-Path $gradlewPath)) {
    throw "gradlew.bat が見つかりません: $gradlewPath"
}

$gradleArgs = @(
    ":mobile:bundleRelease"
    "-PappVersionCode=$newVersionCode"
    "-PappVersionName=$newVersionName"
    "--no-daemon"
)

Push-Location $repoRoot
try {
    & $gradlewPath @gradleArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle のビルドに失敗しました（終了コード: $LASTEXITCODE）。version.properties は更新していません。"
    }
} finally {
    Pop-Location
}

# Set-Content は環境依存の改行コードを付けて、実行のたびに無意味な git 差分が出るため、
# 改行を LF に固定し BOM なし UTF-8 で書き出す（sesami-wear と同じ）。
$versionContent = "VERSION_CODE=$newVersionCode`nVERSION_NAME=$newVersionName`n"
[System.IO.File]::WriteAllText($versionFile, $versionContent, (New-Object System.Text.UTF8Encoding $false))

Write-Host ""
Write-Host "リリースビルドが完了しました: versionCode=$newVersionCode / versionName=$newVersionName"
Write-Host "  AAB: mobile/build/outputs/bundle/release/mobile-release.aab"
Write-Host "  → Play Console へ手動でアップロードする（内部テスト → クローズドテスト → 製品版）"
Write-Host ""
Write-Host "署名設定（local.properties）が無い場合、上記 AAB は署名なしのままです。"
Write-Host "署名の手順は README.md「リリースビルド」を参照してください。"
