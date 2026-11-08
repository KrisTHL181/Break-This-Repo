# ---- 准备 ----
$root = $PSScriptRoot
$audioPath = Join-Path $root "BadApple.wav"

# 异步播放（对应 PlaySound + SND_ASYNC）
$player = New-Object System.Media.SoundPlayer
$player.SoundLocation = $audioPath
$player.Load()
$player.Play()

# 预加载所有帧到内存（对应 C 里的 fopen/fread 循环，但只读一次）
$frames = New-Object System.Collections.Generic.List[string]
for ($f = 1; $f -le 6571; $f++) {
    $path = Join-Path $root ("out\BA ({0}).txt" -f $f)
    if (Test-Path $path) {
        $frames.Add([System.IO.File]::ReadAllText($path))
    } else {
        $frames.Add("")
    }
}

# ---- 主循环 ----
$sw    = [System.Diagnostics.Stopwatch]::StartNew()
$stime = 0
$i     = 0

while ($i -le 6570) {
    $caf = if ($i % 30 -eq 0) { 43 } else { 33 }

    $ftime = $sw.ElapsedMilliseconds
    if (($ftime - $stime) -ge $caf) {
        $i++

        # 光标归零 + 输出帧
        [Console]::SetCursorPosition(0, 0)
        [Console]::Write($frames[$i - 1])
        [Console]::Write("Frame:$i")

        $stime += $caf
    }
    Start-Sleep -Milliseconds 1
}

# ---- 收尾 ----
[Console]::Clear()
$player.Stop()
$player.Dispose()