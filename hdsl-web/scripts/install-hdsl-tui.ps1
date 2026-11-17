<#
.SYNOPSIS
  hdsl-tui 一键安装（Windows）：从 GitHub Release 下载、校验 SHA256，再装到用户目录。

.DESCRIPTION
  不需要管理员权限；卸载就是删掉安装目录。

  源码安装（-Build）需要本机 Go；源码包内含 vendor/，编译过程不取网络模块。

.EXAMPLE
  irm https://raw.githubusercontent.com/nas200-timmy/HDSL-WEB/main/scripts/install-hdsl-tui.ps1 | iex

.EXAMPLE
  .\install-hdsl-tui.ps1 -Version 0.5.4 -Build
#>
[CmdletBinding()]
param(
    [string]$Version = $env:HDSL_TUI_VERSION,
    [string]$Prefix  = $env:HDSL_TUI_PREFIX,
    [switch]$Build,
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
$Repo = if ($env:HDSL_TUI_REPO) { $env:HDSL_TUI_REPO } else { 'nas200-timmy/HDSL-WEB' }

function Note($m) { Write-Host $m }
function Die($m) { Write-Error "install-hdsl-tui: $m" -ErrorAction Stop; exit 1 }

# ---------------------------------------------------------------- 平台判断 --

if ($env:OS -ne 'Windows_NT') { Die '这个脚本只用于 Windows；其它平台用 install-hdsl-tui.sh' }

switch ($env:PROCESSOR_ARCHITECTURE) {
    'AMD64' { $Arch = 'amd64' }
    'ARM64' { Die '当前 Release 只发布 Windows amd64；ARM64 请用 -Build 从源码编译，或改用 WSL2' }
    default { Die "不支持的架构：$($env:PROCESSOR_ARCHITECTURE)" }
}

if (-not $Prefix) { $Prefix = Join-Path $env:LOCALAPPDATA 'Programs\hdsl-tui' }

# ------------------------------------------------------------ 下载与校验 --

function Get-Asset([string]$Url, [string]$OutFile) {
    Invoke-WebRequest -Uri $Url -OutFile $OutFile -UseBasicParsing
}

function Get-LatestVersion {
    $release = Invoke-RestMethod -Uri "https://api.github.com/repos/$Repo/releases/latest" -UseBasicParsing
    $tag = $release.tag_name
    if (-not $tag) { Die 'Release 里没有 tag_name，请用 -Version 指定' }
    return $tag.TrimStart('v')
}

function Assert-Checksum([string]$File, [string]$SumsFile, [string]$AssetName) {
    $line = Select-String -Path $SumsFile -Pattern ("\s" + [regex]::Escape($AssetName) + '$') | Select-Object -First 1
    if (-not $line) { Die "校验文件里没有 $AssetName" }
    $expected = ($line.Line -split '\s+')[0].ToLower()
    $actual = (Get-FileHash -Algorithm SHA256 -Path $File).Hash.ToLower()
    if ($actual -ne $expected) { Die "$AssetName 校验失败：期望 $expected，实际 $actual" }
    Note "  ✓ sha256 $actual"
}

# ---------------------------------------------------------------- 安装 --

if (-not $Version) {
    Note '查询最新版本…'
    $Version = Get-LatestVersion
}
$Version = $Version.TrimStart('v')
$Base = "https://github.com/$Repo/releases/download/v$Version"

Note "hdsl-tui $Version · windows/$Arch · 安装到 $Prefix"
if ($DryRun) { Note '（dry-run：以上为计划，未执行）'; return }

$tmp = Join-Path ([System.IO.Path]::GetTempPath()) ("hdsl-tui-" + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $tmp | Out-Null

try {
    $sums = Join-Path $tmp 'SHA256SUMS-client.txt'
    Get-Asset "$Base/SHA256SUMS-client.txt" $sums

    if ($Build) {
        if (-not (Get-Command go -ErrorAction SilentlyContinue)) { Die '-Build 需要本机 Go' }
        $asset = "hdsl-tui-$Version-src.tar.gz"
        $file = Join-Path $tmp $asset
        Note "下载源码包 $asset（内含 vendor/，编译不取网络模块）"
        Get-Asset "$Base/$asset" $file
        Assert-Checksum $file $sums $asset

        tar -C $tmp -xzf $file
        $dir = Get-ChildItem -Path $tmp -Recurse -Filter go.mod |
            Select-Object -First 1 | ForEach-Object { $_.DirectoryName }
        if (-not $dir) { Die '源码包里没找到 go.mod' }

        New-Item -ItemType Directory -Force -Path $Prefix | Out-Null
        Note '编译中（CGO_ENABLED=0 GOFLAGS=-mod=vendor GOPROXY=off）…'
        $env:CGO_ENABLED = '0'
        $env:GOFLAGS = '-mod=vendor'
        $env:GOPROXY = 'off'
        Push-Location $dir
        try {
            go build -trimpath -ldflags "-s -w -X main.version=$Version" -o (Join-Path $Prefix 'hdsl-tui.exe') ./cmd/hdsl-tui
            if ($LASTEXITCODE -ne 0) { Die '编译失败' }
        } finally { Pop-Location }
    } else {
        $asset = "hdsl-tui-$Version-windows-$Arch.exe"
        $file = Join-Path $tmp $asset
        Note "下载 $asset"
        Get-Asset "$Base/$asset" $file
        Assert-Checksum $file $sums $asset

        New-Item -ItemType Directory -Force -Path $Prefix | Out-Null
        Copy-Item -Force $file (Join-Path $Prefix 'hdsl-tui.exe')
    }
} finally {
    Remove-Item -Recurse -Force $tmp -ErrorAction SilentlyContinue
}

Note ''
Note "已安装：$Prefix\hdsl-tui.exe"
$userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
if ($userPath -notlike "*$Prefix*") {
    Note "提示：把安装目录加进用户 PATH（当前会话生效）："
    Note "  `$env:Path = `"$Prefix;`$env:Path`""
    Note '  永久生效：setx PATH "%PATH%;'"'"$Prefix"'"'"  （或用「系统属性 → 环境变量」）'
}
Note '用法：hdsl-tui.exe https://panel.example.com'