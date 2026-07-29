# Measure cellRects from the TEMPLATE'S OWN FRAME, not from where the sample photo landed.
#
# Why this replaces cells2.ps1:
#   cells2 derived the rect from the DIFF between Temp_ and Thumb_, i.e. from the sample photo.
#   That is the wrong target twice over:
#     1. the sample photo usually sits INSIDE the frame (mounts, borders, stickers overlapping),
#        so the rect came out smaller and offset vs the frame the editor actually shows;
#     2. a slot the designer left EMPTY in Thumb_ produces no diff at all, so the cell was simply
#        missed (sm17 lost 2 of 4 slots, sm18 lost 2 of 5).
#   The frame is a flat neutral region in Temp_ itself, present whether or not Thumb_ filled it.
#
# Also fixes: cells2 forced angle=0 whenever a component filled <90% of its min-area rect, which
# silently threw away the real tilt of soft-edged polaroid frames (cp01, cp17, bd13). Here the
# angle is only zeroed when the shape is genuinely round (near-square bbox AND fill near pi/4),
# where orientation is meaningless.
#
# Diff is still read when Thumb_ exists, but only as a HINT for slot-vs-decoration, never as a
# hard gate on geometry.
#
# Windows PowerShell 5.1 + System.Drawing. ASCII only.
# Usage: powershell -File tools\cells3.ps1 -Ids bd12,cp01 [-AW 600] [-Band auto|white|gray|gray2]

param(
  [string[]]$Ids = @(),
  [string]$Band = 'auto',
  [int]$AW = 600,
  [double]$MinAreaFrac = 0.006,
  [double]$MinShapeFill = 0.62,
  [int]$MinSide = 110,
  [int]$DiffThresh = 26
)

Add-Type -AssemblyName System.Drawing

$SRC = "D:\EZTech\AppAssets\PhotoCollage\Category_Template"
$OUT = "D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\cells3"
New-Item -ItemType Directory -Force -Path $OUT | Out-Null

$LOGIC_W = 1125; $LOGIC_H = 2000
$AH = [int][math]::Round($AW * 16.0 / 9.0)
$N  = $AW * $AH

$CATS = @{
  'bd' = @('Birthday', 'BD'); 'cp' = @('Couple', 'CP'); 'gs' = @('Glad season', 'GS')
  'is' = @('IG Story', 'IS'); 'sm' = @('Summer vibe', 'SM'); 'sp' = @('Sports', 'SP')
}
# Slightly wider than the runtime bands so an anti-aliased frame edge still joins its region.
# The runtime mask stays the authority on what is punched through; this is only for geometry.
$BANDS = [ordered]@{
  'white' = @(243, 255, 12, 'WHITE')
  'gray'  = @(228, 246, 12, 'GRAY')
  'gray2' = @(196, 232, 14, 'GRAY2')
}
$jpgEnc = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq 'image/jpeg' }

function Get-Buf($path, [ref]$strideOut) {
  $img = [System.Drawing.Image]::FromFile($path)
  $bm = New-Object System.Drawing.Bitmap $AW, $AH
  $g = [System.Drawing.Graphics]::FromImage($bm)
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.PixelOffsetMode   = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $g.DrawImage($img, 0, 0, $AW, $AH); $g.Dispose(); $img.Dispose()
  $r = New-Object System.Drawing.Rectangle 0, 0, $AW, $AH
  $d = $bm.LockBits($r, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
  $strideOut.Value = $d.Stride
  $buf = New-Object byte[] ($d.Stride * $AH)
  [System.Runtime.InteropServices.Marshal]::Copy($d.Scan0, $buf, 0, $buf.Length)
  $bm.UnlockBits($d); $bm.Dispose()
  return $buf
}

function Fit-MinRect($bxs, $bys) {
  $np = $bxs.Count; $bestArea = [double]::MaxValue; $best = $null
  for ($deg = -45.0; $deg -lt 45.0; $deg += 0.25) {
    $rad = $deg * [Math]::PI / 180.0; $cos = [Math]::Cos($rad); $sin = [Math]::Sin($rad)
    $minU = 1e9; $maxU = -1e9; $minV = 1e9; $maxV = -1e9
    for ($k = 0; $k -lt $np; $k++) {
      $u = $bxs[$k] * $cos + $bys[$k] * $sin; $v = -$bxs[$k] * $sin + $bys[$k] * $cos
      if ($u -lt $minU) { $minU = $u }; if ($u -gt $maxU) { $maxU = $u }
      if ($v -lt $minV) { $minV = $v }; if ($v -gt $maxV) { $maxV = $v }
    }
    $a = ($maxU - $minU) * ($maxV - $minV)
    if ($a -lt $bestArea) { $bestArea = $a; $best = @{ deg = $deg; u0 = $minU; u1 = $maxU; v0 = $minV; v1 = $maxV } }
  }
  $w = $best.u1 - $best.u0; $h = $best.v1 - $best.v0
  $uc = ($best.u0 + $best.u1) / 2.0; $vc = ($best.v0 + $best.v1) / 2.0
  $rad = $best.deg * [Math]::PI / 180.0; $cos = [Math]::Cos($rad); $sin = [Math]::Sin($rad)
  return @{ cx = ($uc * $cos - $vc * $sin); cy = ($uc * $sin + $vc * $cos); w = $w; h = $h; deg = $best.deg }
}

$Ids = @($Ids | ForEach-Object { $_ -split ',' } | Where-Object { $_ -match '\S' } | ForEach-Object { $_.Trim().ToLower() })
if ($Ids.Count -eq 0) { Write-Host "No -Ids."; exit 1 }

foreach ($id in $Ids) {
  $pre = $id.Substring(0, 2); $nn = $id.Substring(2)
  if (-not $CATS.ContainsKey($pre)) { Write-Host "$id : bad prefix"; continue }
  $tempP  = Join-Path $SRC ("{0}\Temp_{1}{2}.png"  -f $CATS[$pre][0], $CATS[$pre][1], $nn)
  $thumbP = Join-Path $SRC ("{0}\Thumb_{1}{2}.png" -f $CATS[$pre][0], $CATS[$pre][1], $nn)
  if (-not (Test-Path $tempP)) { Write-Host "$id : no Temp_"; continue }

  $sA = 0; $bufT = Get-Buf $tempP ([ref]$sA)
  $brT = New-Object 'byte[]' $N; $satT = New-Object 'byte[]' $N
  for ($y = 0; $y -lt $AH; $y++) {
    $rA = $y * $sA; $base = $y * $AW
    for ($x = 0; $x -lt $AW; $x++) {
      $o = $rA + $x * 3
      $b = $bufT[$o]; $g = $bufT[$o + 1]; $r = $bufT[$o + 2]
      $mx = $r; if ($g -gt $mx) { $mx = $g }; if ($b -gt $mx) { $mx = $b }
      $mn = $r; if ($g -lt $mn) { $mn = $g }; if ($b -lt $mn) { $mn = $b }
      $i = $base + $x; $brT[$i] = $mx; $satT[$i] = $mx - $mn
    }
  }

  # Diff is advisory only: it says which flat regions the designer actually filled.
  $Dm = New-Object 'bool[]' $N
  $haveThumb = Test-Path $thumbP
  if ($haveThumb) {
    $sB = 0; $bufH = Get-Buf $thumbP ([ref]$sB)
    for ($y = 0; $y -lt $AH; $y++) {
      $rA = $y * $sA; $rB = $y * $sB; $base = $y * $AW
      for ($x = 0; $x -lt $AW; $x++) {
        $oA = $rA + $x * 3; $oB = $rB + $x * 3
        $d = [Math]::Abs([int]$bufT[$oA] - [int]$bufH[$oB]) + `
             [Math]::Abs([int]$bufT[$oA + 1] - [int]$bufH[$oB + 1]) + `
             [Math]::Abs([int]$bufT[$oA + 2] - [int]$bufH[$oB + 2])
        if ($d -ge $DiffThresh) { $Dm[$base + $x] = $true }
      }
    }
  }

  $bandNames = if ($Band -eq 'auto') { @($BANDS.Keys) } else { @($Band) }
  $bestName = $null; $bestCells = @(); $bestScore = -1

  foreach ($bn in $bandNames) {
    $lo = $BANDS[$bn][0]; $hi = $BANDS[$bn][1]; $ms = $BANDS[$bn][2]
    $mask = New-Object 'bool[]' $N
    for ($i = 0; $i -lt $N; $i++) {
      if ($brT[$i] -ge $lo -and $brT[$i] -le $hi -and $satT[$i] -le $ms) { $mask[$i] = $true }
    }

    $visited = New-Object 'bool[]' $N
    $st = New-Object System.Collections.Generic.Stack[int]
    $found = @()
    $sx = $LOGIC_W / [double]$AW; $sy = $LOGIC_H / [double]$AH

    for ($i = 0; $i -lt $N; $i++) {
      if (-not $mask[$i] -or $visited[$i]) { continue }
      $st.Clear(); $st.Push($i); $visited[$i] = $true
      $minX = $AW; $minY = $AH; $maxX = 0; $maxY = 0; $cnt = 0; $diffHits = 0
      $bxs = New-Object System.Collections.Generic.List[int]
      $bys = New-Object System.Collections.Generic.List[int]
      while ($st.Count -gt 0) {
        $q = $st.Pop(); $cnt++
        $qx = $q % $AW; $qy = [int]($q / $AW)
        if ($Dm[$q]) { $diffHits++ }
        if ($qx -lt $minX) { $minX = $qx }; if ($qx -gt $maxX) { $maxX = $qx }
        if ($qy -lt $minY) { $minY = $qy }; if ($qy -gt $maxY) { $maxY = $qy }
        $isB = $false
        if ($qx -eq 0 -or $qx -eq $AW - 1 -or $qy -eq 0 -or $qy -eq $AH - 1) { $isB = $true }
        elseif (-not $mask[$q - 1] -or -not $mask[$q + 1] -or -not $mask[$q - $AW] -or -not $mask[$q + $AW]) { $isB = $true }
        if ($isB) { $bxs.Add($qx); $bys.Add($qy) }
        if ($qx -gt 0)       { $n2 = $q - 1;   if ($mask[$n2] -and -not $visited[$n2]) { $visited[$n2] = $true; $st.Push($n2) } }
        if ($qx -lt $AW - 1) { $n2 = $q + 1;   if ($mask[$n2] -and -not $visited[$n2]) { $visited[$n2] = $true; $st.Push($n2) } }
        if ($qy -gt 0)       { $n2 = $q - $AW; if ($mask[$n2] -and -not $visited[$n2]) { $visited[$n2] = $true; $st.Push($n2) } }
        if ($qy -lt $AH - 1) { $n2 = $q + $AW; if ($mask[$n2] -and -not $visited[$n2]) { $visited[$n2] = $true; $st.Push($n2) } }
      }

      $bw = $maxX - $minX + 1; $bh = $maxY - $minY + 1
      $areaFrac = ($bw * $bh) / [double]$N
      if ($areaFrac -lt $MinAreaFrac -or $areaFrac -gt 0.97) { continue }
      $asp = $bw / [double]$bh
      if ($asp -lt 0.08 -or $asp -gt 12) { continue }

      $mr = Fit-MinRect $bxs $bys
      $mrFill = $cnt / [double]([Math]::Max(1.0, $mr.w * $mr.h))
      if ($mrFill -lt $MinShapeFill) { continue }

      $deg = [math]::Round($mr.deg, 1)
      $lw = $mr.w * $sx; $lh = $mr.h * $sy
      $lcx = $mr.cx * $sx; $lcy = $mr.cy * $sy
      # Only a genuinely round blob has no orientation. A tilted rectangle with soft edges can
      # sit at fill ~0.8 and MUST keep its angle -- zeroing it was the cp01/cp17/bd13 bug.
      $ratio = if ($mr.h -gt 0) { $mr.w / $mr.h } else { 9 }
      $isRound = ($mrFill -lt 0.86 -and $ratio -gt 0.80 -and $ratio -lt 1.25)
      if ($isRound) {
        $deg = 0
        $lcx = ($minX + $maxX + 1) / 2.0 * $sx; $lcy = ($minY + $maxY + 1) / 2.0 * $sy
        $lw = $bw * $sx; $lh = $bh * $sy
      }
      if ([Math]::Min($lw, $lh) -lt $MinSide) { continue }

      $rl = [int][math]::Round($lcx - $lw / 2); $rt = [int][math]::Round($lcy - $lh / 2)
      $rr2 = [int][math]::Round($lcx + $lw / 2); $rb = [int][math]::Round($lcy + $lh / 2)
      if ([math]::Abs($deg) -lt 0.3) {
        $rl = [Math]::Max(0, $rl); $rt = [Math]::Max(0, $rt)
        $rr2 = [Math]::Min($LOGIC_W, $rr2); $rb = [Math]::Min($LOGIC_H, $rb)
      }

      $found += [pscustomobject]@{
        l = $rl; t = $rt; r = $rr2; b = $rb; deg = $deg
        fill = [math]::Round($mrFill, 2); px = $cnt
        diffFrac = if ($cnt -gt 0) { [math]::Round($diffHits / [double]$cnt, 2) } else { 0 }
        round = $isRound
      }
    }

    $found = @($found | Sort-Object t, l)
    if ($found.Count -eq 0) { continue }
    # Prefer the band whose regions the designer actually filled with photos.
    $filled = @($found | Where-Object { $_.diffFrac -ge 0.30 }).Count
    $score = if ($haveThumb) { $filled + 0.01 * $found.Count } else { $found.Count }
    if ($score -gt $bestScore) { $bestScore = $score; $bestName = $bn; $bestCells = $found }
  }

  if (-not $bestName) { Write-Host ("==== {0} ====  KHONG TIM THAY O NAO" -f $id); continue }

  $maskName = $BANDS[$bestName][3]
  $filledN = @($bestCells | Where-Object { $_.diffFrac -ge 0.30 }).Count
  Write-Host ""
  Write-Host ("==== {0} ====  band={1}  o={2}  ({3} o co anh trong Thumb_)  mask->{4}" -f `
              $id, $bestName, $bestCells.Count, $filledN, $maskName)
  $k = 0
  foreach ($c in $bestCells) {
    $k++
    $tag = if ($c.diffFrac -ge 0.30) { 'co anh' } else { 'Thumb_ de TRONG' }
    Write-Host ("   {0}. deg={1,6} fill={2,5} ({3},{4},{5},{6})  {7}" -f $k, $c.deg, $c.fill, $c.l, $c.t, $c.r, $c.b, $tag)
  }
  $parts = @()
  foreach ($c in $bestCells) {
    if ([math]::Abs($c.deg) -ge 0.3) { $parts += ("@({0},{1},{2},{3},{4})" -f $c.l, $c.t, $c.r, $c.b, $c.deg) }
    else                             { $parts += ("@({0},{1},{2},{3})"     -f $c.l, $c.t, $c.r, $c.b) }
  }
  $line = if ($bestCells.Count -eq 1) { '  "{0}"=@(,{1})' -f $id, $parts[0] } else { '  "{0}"=@({1})' -f $id, ($parts -join ',') }
  Write-Host "   $line"

  # overlay on the TEMPLATE (the thing the rect must match), full width for real inspection
  $OW = 560; $OH = [int]($OW * 16 / 9)
  $ov = New-Object System.Drawing.Bitmap $OW, $OH
  $go = [System.Drawing.Graphics]::FromImage($ov)
  $go.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $ti = [System.Drawing.Image]::FromFile($tempP)
  $go.DrawImage($ti, 0, 0, $OW, $OH); $ti.Dispose()
  $pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 0, 230, 60)), 3
  $fnt = New-Object System.Drawing.Font 'Consolas', 13, ([System.Drawing.FontStyle]::Bold)
  $spx = $OW / [double]$LOGIC_W; $spy = $OH / [double]$LOGIC_H
  $k = 0
  foreach ($c in $bestCells) {
    $k++
    $rw = ($c.r - $c.l) * $spx; $rh = ($c.b - $c.t) * $spy
    $ccx = ($c.l + $c.r) / 2.0 * $spx; $ccy = ($c.t + $c.b) / 2.0 * $spy
    $rad = $c.deg * [Math]::PI / 180.0
    $cos = [Math]::Cos($rad); $sin = [Math]::Sin($rad)
    $hw = $rw / 2.0; $hh = $rh / 2.0
    $pts = @()
    foreach ($cc in @(@(-$hw, -$hh), @($hw, -$hh), @($hw, $hh), @(-$hw, $hh))) {
      $pts += New-Object System.Drawing.PointF ([float]($ccx + $cc[0] * $cos - $cc[1] * $sin)), ([float]($ccy + $cc[0] * $sin + $cc[1] * $cos))
    }
    $go.DrawPolygon($pen, [System.Drawing.PointF[]]$pts)
    $go.DrawString([string]$k, $fnt, [System.Drawing.Brushes]::Lime, [float]($ccx - 7), [float]($ccy - 9))
  }
  $pen.Dispose(); $fnt.Dispose(); $go.Dispose()
  $eps = New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality, [long]93)
  $ov.Save((Join-Path $OUT "$id.jpg"), $jpgEnc, $eps); $eps.Dispose(); $ov.Dispose()
}
Write-Host ""
Write-Host "Overlay ve tren TEMP_ (khung that) -> $OUT"
