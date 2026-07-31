# Do bbox CHINH XAC cua tung hinh (tron / pill / vom / blob) bang flood-fill tu mot diem SEED,
# gioi han trong mot cua so quanh seed de khong loang ra trang tri cung mau.
#
# Vi sao can: slot_bbox.ps1 dung connected-components toan anh, nen hinh nao dinh voi trang tri
# cung mau (vd 3 hinh tron sp12 bi cac net trang xoan noi vao nhau) se gop thanh mot khoi khong
# dung duoc. Flood tu seed + chan cua so cho ra dung tung hinh.
#
# Rect cua o CO MASK phai PHU TRUM bbox nay. Thieu vai don vi la hien VANH TRANG (hoac vanh mau
# khung) o mep hinh - loi "crescent" da gap o sp09 va sp12.
#
# Usage: powershell -File tools\shape_bbox.ps1 -Id sp12 -Mode WHITE -Seeds "250,830;760,1250;240,1250;235,1690"

param(
  [string]$Id = '',
  [string]$Mode = '',
  [string]$Seeds = '',      # "x,y;x,y;..." toa do LOGIC 1125x2000
  [int]$AW = 1125,          # do o do phan giai logic cho khoi phai doi don vi
  [int]$Win = 420           # ban kinh cua so quanh seed (don vi logic)
)

Add-Type -AssemblyName System.Drawing
. "D:\EZTech\EZTechApp\collage_pic_editor\tools\cells_config.ps1"
$SRC = "D:\EZTech\AppAssets\PhotoCollage\Category_Template"
$CATS = @{ 'bd' = 'Birthday'; 'cp' = 'Couple'; 'gs' = 'Glad season'; 'is' = 'IG Story'; 'sm' = 'Summer vibe'; 'sp' = 'Sports' }
$LOGIC_W = 1125; $LOGIC_H = 2000
$AH = [int][math]::Round($AW * $LOGIC_H / $LOGIC_W)

if (-not $Id) { Write-Host "Thieu -Id"; exit 1 }
$Id = $Id.Trim().ToLower()
$maskMode = if ($Mode) { $Mode.ToUpper() } else { $MASKS[$Id] }
if (-not $maskMode) { Write-Host "$Id : khong co mask, dung -Mode de ep"; exit 1 }
$pre = $Id.Substring(0, 2); $nn = $Id.Substring(2)
$path = Join-Path $SRC ("{0}\Temp_{1}{2}.png" -f $CATS[$pre], $pre.ToUpper(), $nn)
if (-not (Test-Path $path)) { Write-Host "$Id : khong co Temp_"; exit 1 }

$srcImg = [System.Drawing.Image]::FromFile($path)
$small = New-Object System.Drawing.Bitmap($AW, $AH, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$gfx = [System.Drawing.Graphics]::FromImage($small)
$gfx.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$gfx.DrawImage($srcImg, (New-Object System.Drawing.Rectangle(0, 0, $AW, $AH)))
$gfx.Dispose(); $srcImg.Dispose()
$rc = New-Object System.Drawing.Rectangle(0, 0, $AW, $AH)
$dt = $small.LockBits($rc, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$stride = $dt.Stride
$buf = New-Object byte[] ($stride * $AH)
[System.Runtime.InteropServices.Marshal]::Copy($dt.Scan0, $buf, 0, $buf.Length)
$small.UnlockBits($dt); $small.Dispose()

$total = $AW * $AH
$open = New-Object bool[] $total
for ($py = 0; $py -lt $AH; $py++) {
  $row = $py * $stride
  for ($px = 0; $px -lt $AW; $px++) {
    $o = $row + $px * 4
    $bb = $buf[$o]; $gg = $buf[$o + 1]; $rr = $buf[$o + 2]
    $mx = [math]::Max($rr, [math]::Max($gg, $bb)); $mn = [math]::Min($rr, [math]::Min($gg, $bb))
    $hit = $false
    switch ($maskMode) {
      'WHITE' { $hit = ($rr -gt 240 -and $gg -gt 240 -and $bb -gt 240) }
      'GRAY'  { $hit = ($mx -ge 231 -and $mx -le 243 -and ($mx - $mn) -le 7) }
      'GRAY2' { $hit = ($mx -ge 208 -and $mx -le 228 -and ($mx - $mn) -le 10) }
      'BLACK' { $hit = ($rr -lt 50 -and $gg -lt 50 -and $bb -lt 50) }
    }
    $open[$py * $AW + $px] = $hit
  }
}

$sx = $AW / [double]$LOGIC_W; $sy = $AH / [double]$LOGIC_H
$seedList = @($Seeds -split ';' | Where-Object { $_ -match '\S' })
$idx = 0
foreach ($s in $seedList) {
  $idx++
  $parts = $s -split ','
  $lx = [int]$parts[0]; $ly = [int]$parts[1]
  $px0 = [int][math]::Round($lx * $sx); $py0 = [int][math]::Round($ly * $sy)
  if (-not $open[$py0 * $AW + $px0]) { Write-Host ("seed {0} ({1},{2}): KHONG nam trong dai mau" -f $idx, $lx, $ly); continue }
  $xLo = [math]::Max(0, [int](($lx - $Win) * $sx)); $xHi = [math]::Min($AW - 1, [int](($lx + $Win) * $sx))
  $yLo = [math]::Max(0, [int](($ly - $Win) * $sy)); $yHi = [math]::Min($AH - 1, [int](($ly + $Win) * $sy))
  $seen = New-Object bool[] $total
  $stack = New-Object int[] $total
  $sp = 0; $stack[$sp++] = $py0 * $AW + $px0; $seen[$py0 * $AW + $px0] = $true
  $minX = $AW; $maxX = 0; $minY = $AH; $maxY = 0; $cnt = 0
  while ($sp -gt 0) {
    $cur = $stack[--$sp]; $cnt++
    $cy = [int][math]::Floor($cur / $AW); $cx = $cur - $cy * $AW
    if ($cx -lt $minX) { $minX = $cx }; if ($cx -gt $maxX) { $maxX = $cx }
    if ($cy -lt $minY) { $minY = $cy }; if ($cy -gt $maxY) { $maxY = $cy }
    foreach ($d in @(@(1, 0), @(-1, 0), @(0, 1), @(0, -1))) {
      $nx = $cx + $d[0]; $ny = $cy + $d[1]
      if ($nx -lt $xLo -or $nx -gt $xHi -or $ny -lt $yLo -or $ny -gt $yHi) { continue }
      $ni = $ny * $AW + $nx
      if ($open[$ni] -and -not $seen[$ni]) { $seen[$ni] = $true; $stack[$sp++] = $ni }
    }
  }
  $l = [int][math]::Round($minX / $sx); $r = [int][math]::Round(($maxX + 1) / $sx)
  $t = [int][math]::Round($minY / $sy); $b = [int][math]::Round(($maxY + 1) / $sy)
  $fill = [math]::Round($cnt / [double](($maxX - $minX + 1) * ($maxY - $minY + 1)), 2)
  Write-Host ("hinh {0} seed({1},{2}) -> bbox ({3},{4},{5},{6})  {7}x{8}  fill {9}" -f `
      $idx, $lx, $ly, $l, $t, $r, $b, ($r - $l), ($b - $t), $fill)

  # --- min-area ROTATED rect cua chinh vung vua loang (chi lay pixel BIEN cho nhanh) ---
  $edge = New-Object System.Collections.ArrayList
  for ($py = $minY; $py -le $maxY; $py++) {
    for ($px = $minX; $px -le $maxX; $px++) {
      $ii = $py * $AW + $px
      if (-not $seen[$ii]) { continue }
      $isEdge = $false
      foreach ($d in @(@(1, 0), @(-1, 0), @(0, 1), @(0, -1))) {
        $nx2 = $px + $d[0]; $ny2 = $py + $d[1]
        if ($nx2 -lt 0 -or $nx2 -ge $AW -or $ny2 -lt 0 -or $ny2 -ge $AH) { $isEdge = $true; break }
        if (-not $seen[$ny2 * $AW + $nx2]) { $isEdge = $true; break }
      }
      if ($isEdge) { [void]$edge.Add(@($px, $py)) }
    }
  }
  $bestA = [double]::MaxValue; $bestD = 0.0; $bl = 0.0; $bt = 0.0; $brr = 0.0; $bb2 = 0.0
  for ($deg = -30.0; $deg -le 30.0; $deg += 0.5) {
    $rad2 = [math]::PI * $deg / 180.0
    $co2 = [math]::Cos($rad2); $si2 = [math]::Sin($rad2)
    $mnx = [double]::MaxValue; $mxx = [double]::MinValue; $mny = [double]::MaxValue; $mxy = [double]::MinValue
    foreach ($p in $edge) {
      $rx2 = $p[0] * $co2 + $p[1] * $si2
      $ry2 = -$p[0] * $si2 + $p[1] * $co2
      if ($rx2 -lt $mnx) { $mnx = $rx2 }; if ($rx2 -gt $mxx) { $mxx = $rx2 }
      if ($ry2 -lt $mny) { $mny = $ry2 }; if ($ry2 -gt $mxy) { $mxy = $ry2 }
    }
    $a2 = ($mxx - $mnx) * ($mxy - $mny)
    if ($a2 -lt $bestA) { $bestA = $a2; $bestD = $deg; $bl = $mnx; $bt = $mny; $brr = $mxx; $bb2 = $mxy }
  }
  $rad3 = [math]::PI * $bestD / 180.0
  $co3 = [math]::Cos($rad3); $si3 = [math]::Sin($rad3)
  $ccx2 = ($bl + $brr) / 2.0; $ccy2 = ($bt + $bb2) / 2.0
  $ox2 = $ccx2 * $co3 - $ccy2 * $si3
  $oy2 = $ccx2 * $si3 + $ccy2 * $co3
  $wl = ($brr - $bl) / $sx; $hl = ($bb2 - $bt) / $sy
  $cxl = $ox2 / $sx; $cyl = $oy2 / $sy
  $mfill = [math]::Round($cnt / [double]$bestA, 2)
  Write-Host ("        rect xoay = ({0},{1},{2},{3},{4})   {5}x{6}  fill {7}" -f `
      [int]($cxl - $wl / 2), [int]($cyl - $hl / 2), [int]($cxl + $wl / 2), [int]($cyl + $hl / 2), $bestD, [int]$wl, [int]$hl, $mfill)
}
Write-Host "Rect dang dung:"
foreach ($c in @($CELLS[$Id] | Where-Object { $_ })) { Write-Host ("  ({0})" -f ($c -join ',')) }
