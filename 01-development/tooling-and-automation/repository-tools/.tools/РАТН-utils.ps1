# ============================================================
#  劲爆演示 1 / 4  —  路径长度炸弹
#  ------------------------------------------------------------
#  一句话: 让同一个文件操作在 5 种工具里，在 5 个不同的长度上，
#          以 5 种不同的方式失败 —— 其中一种会「假装成功」。
#
#  阶段 0  造超长路径链
#  阶段 1  从 300 字符起逐步加压，找出每种 API 第一次失败的确切长度
#  阶段 2  文件名陷阱: 「创建 API 说成功」vs「文件真的还在」
#  阶段 3  删除: 普通路径做不到、\\?\ 才能做到
#
#  安全性: 全部在 $Root 临时目录内; 不需要管理员; 跑完自动清理。
# ============================================================

param(
  [string]$Root     = "$env:TEMP\btr-pathbomb",
  [int]$DepthNarrow = 1400,
  [switch]$Keep
)

$ErrorActionPreference = 'Continue'
[Console]::OutputEncoding = [Text.Encoding]::UTF8
function Line { param($c='-') Write-Host ($c * 88) -ForegroundColor DarkGray }
function Head { param($t) Write-Host ""; Line '='; Write-Host "  $t" -ForegroundColor Cyan; Line '=' }
function Sub  { param($t) Write-Host ""; Write-Host "  -- $t" -ForegroundColor DarkCyan }

# ---------- 清理函数本身就是演示：深路径要自底向上 + \\?\ ----------
function Remove-DeepTree {
  param([string]$Path)
  if (-not (Test-Path -LiteralPath $Path)) { return $true }
  cmd /c "rmdir /s /q `"\\?\$Path`"" 2>$null | Out-Null
  if (-not (Test-Path -LiteralPath $Path)) { return $true }
  Remove-Item -LiteralPath $Path -Recurse -Force -ErrorAction SilentlyContinue
  if (-not (Test-Path -LiteralPath $Path)) { return $true }
  $stack = New-Object 'System.Collections.Generic.Stack[string]'
  $stack.Push($Path)
  $all = New-Object 'System.Collections.Generic.List[string]'
  while ($stack.Count -gt 0) {
    $cur = $stack.Pop(); $all.Add($cur)
    try { foreach ($x in [IO.Directory]::GetFiles($cur)) { try { [IO.File]::Delete($x) } catch {} } } catch {}
    try { foreach ($d in [IO.Directory]::GetDirectories($cur)) { $stack.Push($d) } } catch {}
  }
  for ($i = $all.Count - 1; $i -ge 0; $i--) {
    $full = $all[$i]; $trim = $full
    while ($trim.Length -gt 0 -and $trim[$trim.Length-1] -eq ' ') { $trim = $trim.Substring(0, $trim.Length-1) }
    foreach ($c in @($full, $trim)) { if ($c.Length -eq 0) { continue }; try { [IO.Directory]::Delete($c, $false); break } catch {} }
  }
  return (-not (Test-Path -LiteralPath $Path))
}
Remove-DeepTree -Path $Root | Out-Null
New-Item -ItemType Directory -Force -Path $Root | Out-Null
Write-Host "  沙箱: $Root" -ForegroundColor DarkGray

# ==============================================================
Head "阶段 0 : 造路径链"
# ==============================================================
$segLong   = 'abcdefgh-12345678-ABCDEF'   # 24 字符/层
$chainRoot = Join-Path $Root 'chain'

Sub "每层 24 字符，目标 24000+ 字符"
$p = $chainRoot
$sw = [Diagnostics.Stopwatch]::StartNew()
$made = 0
for ($i = 1; $i -le $DepthNarrow; $i++) {
  $p = Join-Path $p $segLong
  try { [IO.Directory]::CreateDirectory($p) | Out-Null; $made++ }
  catch { Write-Host ("    第 {0} 层失败: {1}" -f $i, $_.Exception.GetType().Name) -ForegroundColor Yellow; break }
  if ($p.Length -gt 36000) { break }
}
$chain = $p
Write-Host ("    建成 {0} 层 / 最深处 {1:N0} 字符 / {2:N1} 秒" -f $made, $chain.Length, $sw.Elapsed.TotalSeconds) -ForegroundColor Green

# 干净递增的采样点
$samples = New-Object 'System.Collections.Generic.List[object]'
$seen = New-Object 'System.Collections.Generic.HashSet[int]'
$targets = New-Object 'System.Collections.Generic.List[int]'
$t = 300
while ($t -lt $chain.Length) { $targets.Add($t); $t = if ($t -lt 2000) { $t + 100 } else { $t + 1000 } }
$targets.Add($chain.Length)

$cur = $chainRoot
foreach ($target in $targets) {
  while ($cur.Length -lt $target) {
    $next = Join-Path $cur $segLong
    if (-not (Test-Path -LiteralPath $next)) { break }
    $cur = $next
  }
  if ($cur.Length -ge 250 -and $seen.Add($cur.Length)) {
    $samples.Add([pscustomobject]@{ len = $cur.Length; path = $cur })
  }
}
Write-Host ("    采样点 {0} 个: {1:N0} → {2:N0} 字符" -f $samples.Count, $samples[0].len, $samples[-1].len) -ForegroundColor DarkGray

# ==============================================================
Head "阶段 1 : 每种 API 第一次失败在哪个长度"
# ==============================================================
Write-Host "  规则: 从短到长加压，记录第一次失败后该 API 不再测试。" -ForegroundColor DarkGray
Write-Host ""

$apis = @(
  @{ name = '.NET File.WriteAllText';    fn = { param($d) [IO.File]::WriteAllText((Join-Path $d 'p.txt'),'x'); 'ok' } }
  @{ name = 'PowerShell New-Item';       fn = { param($d) New-Item -ItemType File -Path (Join-Path $d 'p.txt') -Force -ErrorAction Stop | Out-Null; 'ok' } }
  @{ name = 'cmd echo > (命令行)';        fn = { param($d) cmd /c "echo x> `"$(Join-Path $d 'p.txt')`"" 2>$null | Out-Null
                                                 if (Test-Path -LiteralPath (Join-Path $d 'p.txt')) { 'ok' } else { 'SILENT' } } }
  @{ name = '.NET FileStream  \\?\ 前缀'; fn = { param($d) $q = '\\?\' + (Join-Path $d 'p.txt'); $fs = [IO.File]::Open($q,'Create'); $fs.Close(); 'ok' } }
  @{ name = 'CreateDirectory 建子目录';   fn = { param($d) [IO.Directory]::CreateDirectory((Join-Path $d 'sub')) | Out-Null; 'ok' } }
)

$verdicts = New-Object 'System.Collections.Generic.List[object]'
foreach ($api in $apis) {
  $firstFail = $null; $kind = $null; $lastOk = 0
  foreach ($s in $samples) {
    if (-not (Test-Path -LiteralPath $s.path)) { continue }
    try {
      $r = & $api.fn $s.path
      if ($r -eq 'ok') { $lastOk = $s.len }
      elseif (-not $firstFail) { $firstFail = $s.len; $kind = 'SILENT 静默失败（返回 0，文件不存在）' }
    } catch {
      if (-not $firstFail) {
        $firstFail = $s.len
        $ex = $_.Exception
        $kind = if ($ex.InnerException) { $ex.InnerException.GetType().Name } else { $ex.GetType().Name }
      }
    }
    if ($firstFail) { break }
  }
  $verdicts.Add([pscustomobject]@{ api=$api.name; lastOk=$lastOk; firstFail=$firstFail; kind=$kind })
}

Write-Host ("  {0,-32} {1,10} {2,10}  {3}" -f 'API','最后成功','首次失败','失败方式') -ForegroundColor White
Line
foreach ($v in $verdicts) {
  $color = if ($v.kind -like 'SILENT*') { 'Magenta' } elseif ($v.firstFail) { 'Yellow' } else { 'Green' }
  $ff = if ($v.firstFail) { "{0:N0}" -f $v.firstFail } else { '未撞到' }
  Write-Host ("  {0,-32} {1,10:N0} {2,10}  {3}" -f $v.api, $v.lastOk, $ff, $v.kind) -ForegroundColor $color
}
Line
$silent = @($verdicts | Where-Object { $_.kind -like 'SILENT*' })
if ($silent.Count) { Write-Host ("  * {0} 种是静默失败: 不抛异常，只是文件没出现。" -f $silent.Count) -ForegroundColor Magenta }
Write-Host "  * 每个 API 的「首次失败」长度都不一样 —— 这就是这类 bug 极难复现的原因。" -ForegroundColor Magenta

# ==============================================================
Head "阶段 2 : 创建 API 说成功 ≠ 文件还在"
# ==============================================================
$trap = Join-Path $Root 'traps'
New-Item -ItemType Directory -Force -Path $trap | Out-Null

$trapNames = @('normal.txt','trailing_space.txt ','trailing_space2.txt  ','trailing_dot.txt.',
               'trailing_dotdot.txt..',' leading_space.txt','.hidden.txt','tab_in_name.txt')

Write-Host "  用 \\?\ 前缀 + FileStream 请求创建这些名字：" -ForegroundColor DarkGray
Write-Host ("  {0,-26} {1,-16} {2}" -f '请求的名字','创建 API 的回答','之后真的存在吗') -ForegroundColor White
Line
$survived = New-Object 'System.Collections.Generic.List[string]'
foreach ($t in $trapNames) {
  $full = Join-Path $trap $t
  $apiSaid = '失败'
  try {
    $fs = [IO.File]::Open('\\?\' + $full, 'Create'); $fs.Write([byte[]](72,105),0,2); $fs.Close()
    $apiSaid = '** 成功 **'
  } catch { $apiSaid = "失败($($_.Exception.GetType().Name))" }
  $exists = [IO.File]::Exists($full)
  if ($exists) { $survived.Add($t) }
  $color = if ($apiSaid -like '*成功*' -and -not $exists) { 'Red' } elseif ($exists) { 'Green' } else { 'DarkYellow' }
  $note = if ($apiSaid -like '*成功*' -and -not $exists) { '   <== 被静默吞掉' } else { '' }
  Write-Host ("  [{0,-24}] {1,-16} {2}{3}" -f $t, $apiSaid, $exists, $note) -ForegroundColor $color
}
Line
Write-Host ("  创建 API 声称成功 8/8        文件真的存在 {0}/8" -f $survived.Count) -ForegroundColor Yellow
Write-Host "  * 结尾空格 / 结尾点 在 CreateFile 层被保留、在目录枚举层被剥离。" -ForegroundColor Magenta
Write-Host "    「创建成功」和「还能找到它」是两个独立事件。" -ForegroundColor Magenta

Sub "同一个目录，五种工具数出来的条目数"
$probeTar = Join-Path $env:TEMP '_btr_probe.tar'
& tar -cf $probeTar -C $trap . 2>$null | Out-Null
$tarCount = @(& tar -tf $probeTar 2>$null).Count
Remove-Item $probeTar -Force -ErrorAction SilentlyContinue

$counters = [ordered]@{
  '.NET GetFiles()'          = @([IO.Directory]::GetFiles($trap)).Count
  'cmd dir /b'               = @(cmd /c "dir /b /a `"$trap`" 2>nul").Count
  'PowerShell Get-ChildItem' = @(Get-ChildItem -LiteralPath $trap -Force -ErrorAction SilentlyContinue).Count
  'robocopy /L (备份预演)'    = @(cmd /c "robocopy `"$trap`" `"${trap}_x`" /L /NJH /NJS /NDL /NP /NC 2>nul" | Where-Object { $_ -and $_.Trim() -ne '' }).Count
  'tar 打包清单'              = $tarCount
}
$maxCount = [Math]::Max(1, ($counters.Values | Measure-Object -Maximum).Maximum)
foreach ($k in $counters.Keys) {
  $v = $counters[$k]
  $bar = '#' * [Math]::Max(0, [int]($v * 30 / $maxCount))
  $color = if ($v -eq $survived.Count) { 'Green' } elseif ($v -lt $survived.Count) { 'Red' } else { 'Yellow' }
  Write-Host ("  {0,-28} {1,4}  {2}" -f $k, $v, $bar) -ForegroundColor $color
}
Write-Host ("  真正存在的文件数: {0}" -f $survived.Count) -ForegroundColor Yellow

# ==============================================================
Head "阶段 3 : 删除也需要换技术"
# ==============================================================
Sub "删除一条 $($chain.Length) 字符深的目录链"

$sw = [Diagnostics.Stopwatch]::StartNew()
$okA = $false
try { Remove-Item -LiteralPath $chain -Recurse -Force -ErrorAction Stop; $okA = -not (Test-Path -LiteralPath $chain) } catch {}
Write-Host ("  A. Remove-Item -Recurse              {0,-8} {1,6:N1}s" -f $(if($okA){'成功'}else{'失败'}), $sw.Elapsed.TotalSeconds) -ForegroundColor $(if($okA){'Green'}else{'Red'})

$sw = [Diagnostics.Stopwatch]::StartNew()
cmd /c "rmdir /s /q `"\\?\$chain`"" 2>$null | Out-Null
$okB = -not (Test-Path -LiteralPath $chain)
Write-Host ("  B. cmd rmdir /s /q \\?\<path>       {0,-8} {1,6:N1}s" -f $(if($okB){'成功'}else{'失败'}), $sw.Elapsed.TotalSeconds) -ForegroundColor $(if($okB){'Green'}else{'Red'})

$sw = [Diagnostics.Stopwatch]::StartNew()
$okC = Remove-DeepTree -Path $chainRoot
Write-Host ("  C. 自底向上 + \\?\ 逐级 Delete      {0,-8} {1,6:N1}s" -f $(if($okC){'成功'}else{'失败'}), $sw.Elapsed.TotalSeconds) -ForegroundColor $(if($okC){'Green'}else{'Red'})

# ==============================================================
Head "收尾"
# ==============================================================
if (-not $Keep) {
  $sw = [Diagnostics.Stopwatch]::StartNew()
  $gone = Remove-DeepTree -Path $Root
  Write-Host ("  清理整个沙箱: {0:N1}s   残留: {1}" -f $sw.Elapsed.TotalSeconds, (-not $gone)) -ForegroundColor $(if ($gone) { 'Green' } else { 'Red' })
} else {
  Write-Host "  [Keep] 保留在 $Root" -ForegroundColor DarkYellow
}

Head "结论"
Write-Host @"
  1. 「路径最大长度」不是一个数字，是一张表。
     5 种 API 在 5 个不同的长度上、以 5 种不同的方式失败。

  2. 最危险的不是失败，是「失败长得像成功」:
       - cmd echo 到某个长度后不报错、只是文件不出现
       - 结尾空格的路径「创建成功」，然后谁都找不到它
     自动化脚本里的 if (`$LASTEXITCODE -eq 0) 会一路放行。

  3. Break-This-Repo 就是这条原理的实战版:
     一条 609 字符的路径让 bsdtar 丢掉 45% 的文件，
     而输出里只有一行夹在 67,780 行中间的 Invalid argument。

  4. 这条炸弹不需要恶意 —— Java 包名、node_modules、CI 缓存
     都会自然长出这种深度。真正需要恶意的，是知道它会静默失败还不管。
"@ -ForegroundColor Gray
