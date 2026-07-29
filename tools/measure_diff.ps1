# Do o anh bang MIN-AREA ROTATED RECT tren vung DIFF (Temp_ vs Thumb_).
#
# Dung khi slot khong the do theo dai mau: o mau TRANG nam tren nen TRANG (sp16 3 panel trai)
# thi khong co dai mau nao tach duoc, nhung diff voi Thumb_ thi hien ro vi designer da dat anh
# vao do. Diff cho biet CHO NAO dat anh, min-area-rect cho ra rect + goc nghieng.
#
# Usage: powershell -File tools\measure_diff.ps1 -Id sp16 [-AW 375] [-MinArea 20000] [-Thresh 45]

param(
  [string]$Id = '',
  [int]$AW = 375,
  [int]$MinArea = 20000,
  [int]$Thresh = 45,
  # Ep mot goc co dinh thay vi quet. Dung khi ca template nghieng cung mot goc nhung mot vai
  # vung bi canh anh cat cut lam min-area-rect tra ve 0 do (vd panel duoi cung cua sp16).
  # TEN PHAI KHAC $deg trong vong lap: PowerShell KHONG phan biet hoa/thuong nen dat la $Deg
  # thi vong lap ghi de len tham so, moi vung ra mot goc khac nhau (da dinh).
  [double]$ForceDeg = 999
)

Add-Type -AssemblyName System.Drawing
$SRC = "D:\EZTech\AppAssets\PhotoCollage\Category_Template"
$CATS = @{ 'bd' = 'Birthday'; 'cp' = 'Couple'; 'gs' = 'Glad season'; 'is' = 'IG Story'; 'sm' = 'Summer vibe'; 'sp' = 'Sports' }
$LOGIC_W = 1125; $LOGIC_H = 2000
$AH = [int][math]::Round($AW * $LOGIC_H / $LOGIC_W)
$PXAREA = ($LOGIC_W / [double]$AW) * ($LOGIC_H / [double]$AH)

if (-not $Id) { Write-Host "Thieu -Id"; exit 1 }
$Id = $Id.Trim().ToLower()
$pre = $Id.Substring(0, 2); $nn = $Id.Substring(2)
$pTemp = Join-Path $SRC ("{0}\Temp_{1}{2}.png" -f $CATS[$pre], $pre.ToUpper(), $nn)
$pThumb = Join-Path $SRC ("{0}\Thumb_{1}{2}.png" -f $CATS[$pre], $pre.ToUpper(), $nn)
if (-not (Test-Path $pTemp) -or -not (Test-Path $pThumb)) { Write-Host "$Id : thieu Temp_ hoac Thumb_"; exit 1 }

function LoadBuf([string]$p, [int]$gw, [int]$gh) {
  $img = [System.Drawing.Image]::FromFile($p)
  $bmp = New-Object System.Drawing.Bitmap($gw, $gh, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $gg = [System.Drawing.Graphics]::FromImage($bmp)
  $gg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $gg.DrawImage($img, (New-Object System.Drawing.Rectangle(0, 0, $gw, $gh)))
  $gg.Dispose(); $img.Dispose()
  $rc = New-Object System.Drawing.Rectangle(0, 0, $gw, $gh)
  $dt = $bmp.LockBits($rc, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $bytes = New-Object byte[] ($dt.Stride * $gh)
  [System.Runtime.InteropServices.Marshal]::Copy($dt.Scan0, $bytes, 0, $bytes.Length)
  $st = $dt.Stride
  $bmp.UnlockBits($dt); $bmp.Dispose()
  return @{ Buf = $bytes; Stride = $st }
}

$ta = LoadBuf $pTemp $AW $AH
$tb = LoadBuf $pThumb $AW $AH
$total = $AW * $AH
$diff = New-Object bool[] $total
for ($py = 0; $py -lt $AH; $py++) {
  $row = $py * $ta.Stride
  for ($px = 0; $px -lt $AW; $px++) {
    $o = $row + $px * 4
    $d = [math]::Abs([int]$ta.Buf[$o] - [int]$tb.Buf[$o]) +
         [math]::Abs([int]$ta.Buf[$o + 1] - [int]$tb.Buf[$o + 1]) +
         [math]::Abs([int]$ta.Buf[$o + 2] - [int]$tb.Buf[$o + 2])
    $diff[$py * $AW + $px] = ($d / 3) -ge $Thresh
  }
}

$label = New-Object int[] $total
for ($i = 0; $i -lt $total; $i++) { $label[$i] = -1 }
$stack = New-Object int[] $total
$nComp = 0
$out = @()
for ($seed = 0; $seed -lt $total; $seed++) {
  if (-not $diff[$seed] -or $label[$seed] -ge 0) { continue }
  $sp = 0; $stack[$sp++] = $seed; $label[$seed] = $nComp
  $pts = New-Object System.Collections.ArrayList
  while ($sp -gt 0) {
    $cur = $stack[--$sp]
    $cy = [int][math]::Floor($cur / $AW); $cx = $cur - $cy * $AW
    [void]$pts.Add(@($cx, $cy))
    for ($dy = -1; $dy -le 1; $dy++) {
      $ny = $cy + $dy; if ($ny -lt 0 -or $ny -ge $AH) { continue }
      for ($dx = -1; $dx -le 1; $dx++) {
        $nx = $cx + $dx; if ($nx -lt 0 -or $nx -ge $AW) { continue }
        $ni = $ny * $AW + $nx
        if ($diff[$ni] -and $label[$ni] -lt 0) { $label[$ni] = $nComp; $stack[$sp++] = $ni }
      }
    }
  }
  $nComp++
  $areaLogic = $pts.Count * $PXAREA
  if ($areaLogic -lt $MinArea) { continue }

  # min-area rotated rect: quet goc, lay goc cho AABB nho nhat
  $bestDeg = 0.0; $bestArea = [double]::MaxValue
  $bestL = 0.0; $bestT = 0.0; $bestR = 0.0; $bestB = 0.0
  $degFrom = -25.0; $degTo = 25.0; $degStep = 0.5
  if ($ForceDeg -ne 999) { $degFrom = $ForceDeg; $degTo = $ForceDeg; $degStep = 1.0 }
  for ($deg = $degFrom; $deg -le $degTo; $deg += $degStep) {
    $rad = [math]::PI * $deg / 180.0
    $co = [math]::Cos($rad); $si = [math]::Sin($rad)
    $mnX = [double]::MaxValue; $mxX = [double]::MinValue
    $mnY = [double]::MaxValue; $mxY = [double]::MinValue
    foreach ($p in $pts) {
      $x = $p[0]; $y = $p[1]
      $rx = $x * $co + $y * $si
      $ry = -$x * $si + $y * $co
      if ($rx -lt $mnX) { $mnX = $rx }; if ($rx -gt $mxX) { $mxX = $rx }
      if ($ry -lt $mnY) { $mnY = $ry }; if ($ry -gt $mxY) { $mxY = $ry }
    }
    $a = ($mxX - $mnX) * ($mxY - $mnY)
    if ($a -lt $bestArea) { $bestArea = $a; $bestDeg = $deg; $bestL = $mnX; $bestT = $mnY; $bestR = $mxX; $bestB = $mxY }
  }
  # tam trong he da xoay -> doi ve he goc
  $rad = [math]::PI * $bestDeg / 180.0
  $co = [math]::Cos($rad); $si = [math]::Sin($rad)
  $ccx = ($bestL + $bestR) / 2.0; $ccy = ($bestT + $bestB) / 2.0
  $ox = $ccx * $co - $ccy * $si
  $oy = $ccx * $si + $ccy * $co
  $w2 = ($bestR - $bestL) / 2.0; $h2 = ($bestB - $bestT) / 2.0
  $sx = $LOGIC_W / [double]$AW; $sy = $LOGIC_H / [double]$AH
  $cxL = $ox * $sx; $cyL = $oy * $sy
  $wL = $w2 * 2 * $sx; $hL = $h2 * 2 * $sy
  $fill = [math]::Round(($pts.Count * 1.0) / $bestArea, 2)
  # Goc: he anh y-down, xoay duong = chieu kim dong ho (khop canvas.rotate cua Android)
  $out += [pscustomobject]@{
    Area = [int]$areaLogic; Deg = $bestDeg; Fill = $fill
    Rect = ("({0},{1},{2},{3},{4})" -f [int]($cxL - $wL / 2), [int]($cyL - $hL / 2), [int]($cxL + $wL / 2), [int]($cyL + $hL / 2), $bestDeg)
    Size = ("{0}x{1}" -f [int]$wL, [int]$hL)
  }
}

Write-Host ("=== {0} : {1} vung anh (diff Temp_/Thumb_) ===" -f $Id, $out.Count)
$out | Sort-Object -Property Area -Descending | Format-Table -AutoSize
