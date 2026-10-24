@echo off
rem 署名付きのリリース AAB をビルドするバッチファイル（PLAN §6）。
rem 実処理は release-build.ps1（PowerShell）に任せる。使い方は README.md を参照。
rem pwsh（PowerShell 7）が使える場合はそちらを優先する
rem （Windows PowerShell 5.1 はスクリプト内の日本語の扱いで問題が起きることがあるため）。
rem 例: release-build.bat
rem 例: release-build.bat -VersionCode 10 -VersionName 1.0.0
where pwsh >nul 2>nul
if %errorlevel%==0 (
    pwsh -NoProfile -ExecutionPolicy Bypass -File "%~dp0release-build.ps1" %*
) else (
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0release-build.ps1" %*
)
exit /b %errorlevel%
