# ---- Bad Apple ASCII Art Player - PowerShell 版 ----

# 打印标题
Write-Host "-----Bad Apple ASCII art player-----"
Write-Host "Press Enter to play."
[void][Console]::ReadLine()
[Console]::Clear()

# 异步播放音频（对应 PlaySound 的 SND_ASYNC）
$player = New-Object System.Media.SoundPlayer
$player.SoundLocation = "BadApple.wav"
$player.Load()
$player.Play()

# 计时器（对应 clock()）
$sw = [System.Diagnostics.Stopwatch]::StartNew()
$stime = 0
$i = 0

while ($i -le 6570) {
    # 每 30 帧切换一次阈值
    $caf = if ($i % 30 -eq 0) { 43 } else { 33 }

    # 用当前的 i 拼出文件名
    $seat = "out\BA ($i).txt"

    $ftime = $sw.ElapsedMilliseconds
    if (($ftime - $stime) -ge $caf) {
        $i++

        # 读取帧文件内容
        $buf = [System.IO.File]::ReadAllText($seat)

        # 打印内容 + 帧号
        [Console]::Write($buf)
        [Console]::Write("Frame:$i")

        # 光标归零（对应 recursur()）
        [Console]::SetCursorPosition(0, 0)

        $stime += $caf
    }
}

# 播完清屏 + 结束语
[Console]::Clear()
Write-Host "-----Bad Apple ASCII art player-----"
Write-Host "Thanks for watching!"
Write-Host "Made by chuan."
Write-Host ""
Write-Host "Press Enter to Exit."
[void][Console]::ReadLine()

$player.Stop()
$player.Dispose()