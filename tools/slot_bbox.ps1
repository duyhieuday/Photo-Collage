# Liet ke tung VUNG TRONG SUOT (slot) cua mask, kem bbox toa do logic 1125x2000.
#
# Dung khi audit_bleed bao "HUT" (rect khong phu het slot) hoac "KHONG-CHU": can biet slot that
# su nam dau, rong bao nhieu, de sua cellRect cho khop thay vi doan.
#
# Nguong mask lay DUNG tu TemplateEditorView (WHITE / GRAY / GRAY2 / BLACK).
#
# Usage: powershell -File tools\slot_bbox.ps1 -Id gs05 [-Mode WHITE] [-AW 563] [-MinArea 3000]

param(
  [string]$Id = '',
  [string]$Mode = '',
  [int]$AW = 563,
  [int]$MinArea = 3000      # dien tich toi thieu (don vi logic^2)
)

Add-Type -AssemblyName System.Drawing
. "D:\EZTech\EZTechApp\collage_pic_editor\tools\cells_config.ps1"

$SRC = "D:\EZTech\AppAssets\PhotoCollage\Category_Template"
$CATS = @{ 'bd' = 'Birthday'; 'cp' = 'Couple'; 'gs' = 'Glad season'; 'is' = 'IG Story'; 'sm' = 'Summer vibe'; 'sp' = 'Sports' }
$LOGIC_W = 1125; $LOGIC_H = 2000
$AH = [int][math]::Round($AW * $LOGIC_H / $LOGIC_W)
$PXAREA = ($LOGIC_W / [double]$AW) * ($LOGIC_H / [double]$AH)

if (-not $Id) { Write-Host "Thieu -Id"; exit 1 }
$Id = $Id.Trim().ToLower()
$maskMode = if ($Mode) { $Mode.ToUpper() } else { $MASKS[$Id] }
if (-not $maskMode) { Write-Host "$Id : khong co mask (NONE) - dung -Mode de ep"; exit 1 }

$pre = $Id.Substring(0, 2); $nn = $Id.Substring(2)
$path = Join-Path $SRC ("{0}\Temp_{1}{2}.png" -f $CATS[$pre], $pre.ToUpper(), $nn)
if (-not (Test-Path $path)) { Write-Host "$Id : khong co Temp_"; exit 1 }

$srcImg = [System.Drawing.Image]::FromFile($path)
$small = New-Object System.Drawing.Bitmap($AW, $AH, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$gfx = [System.Drawing.Graphics]::FromImage($small)
$gfx.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$gfx.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$gfx.DrawImage($srcImg, (New-Object System.Drawing.Rectangle(0, 0, $AW, $AH)))
$gfx.Dispose(); $srcImg.Dispose()

$rect = New-Object System.Drawing.Rectangle(0, 0, $AW, $AH)
$data = $small.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$stride = $data.Stride
$buf = New-Object byte[] ($stride * $AH)
[System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $buf, 0, $buf.Length)
$small.UnlockBits($data); $small.Dispose()

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

$label = New-Object int[] $total
for ($i = 0; $i -lt $total; $i++) { $label[$i] = -1 }
$stack = New-Object int[] $total
$rows = @()
$nComp = 0
for ($seed = 0; $seed -lt $total; $seed++) {
  if (-not $open[$seed] -or $label[$seed] -ge 0) { continue }
  $sp = 0; $stack[$sp++] = $seed; $label[$seed] = $nComp
  $size = 0; $minX = $AW; $maxX = 0; $minY = $AH; $maxY = 0
  while ($sp -gt 0) {
    $cur = $stack[--$sp]; $size++
    $cy = [int][math]::Floor($cur / $AW); $cx = $cur - $cy * $AW
    if ($cx -lt $minX) { $minX = $cx }; if ($cx -gt $maxX) { $maxX = $cx }
    if ($cy -lt $minY) { $minY = $cy }; if ($cy -gt $maxY) { $maxY = $cy }
    for ($dy = -1; $dy -le 1; $dy++) {
      $ny = $cy + $dy; if ($ny -lt 0 -or $ny -ge $AH) { continue }
      for ($dx = -1; $dx -le 1; $dx++) {
        $nx = $cx + $dx; if ($nx -lt 0 -or $nx -ge $AW) { continue }
        $ni = $ny * $AW + $nx
        if ($open[$ni] -and $label[$ni] -lt 0) { $label[$ni] = $nComp; $stack[$sp++] = $ni }
      }
    }
  }
  $nComp++
  $area = [int]($size * $PXAREA)
  if ($area -lt $MinArea) { continue }
  $lx0 = [int][math]::Round($minX * $LOGIC_W / $AW); $lx1 = [int][math]::Round(($maxX + 1) * $LOGIC_W / $AW)
  $ly0 = [int][math]::Round($minY * $LOGIC_H / $AH); $ly1 = [int][math]::Round(($maxY + 1) * $LOGIC_H / $AH)
  $bboxArea = ($lx1 - $lx0) * ($ly1 - $ly0)
  $fill = if ($bboxArea -gt 0) { [math]::Round($area / [double]$bboxArea, 2) } else { 0 }
  $rows += [pscustomobject]@{ Area = $area; Rect = ("({0},{1},{2},{3})" -f $lx0, $ly0, $lx1, $ly1)
    W = ($lx1 - $lx0); H = ($ly1 - $ly0); Fill = $fill }
}

Write-Host ("=== {0}  mask={1}  {2} slot >= {3} ===" -f $Id, $maskMode, $rows.Count, $MinArea)
$rows | Sort-Object -Property Area -Descending | Format-Table -AutoSize
Write-Host "Rect dang dung:"
foreach ($c in @($CELLS[$Id] | Where-Object { $_ })) { Write-Host ("  ({0})" -f ($c -join ',')) }
