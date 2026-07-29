# Detect photo cells by DIFFING Temp_ (empty frame) against Thumb_ (same design with sample
# photos dropped in). Wherever the two images disagree IS a photo slot -- so this works no matter
# what colour the slot is, survives circles/blobs, and never merges a slot into a same-coloured
# background the way a colour-band threshold does.
#
# Per component it fits a minimum-area rotated rect (brute-force angle sweep over boundary
# pixels), then samples the slot's mean brightness in Temp_ to suggest a MaskMode.
#
# Needs BOTH Temp_ and Thumb_. Templates shipped without a Thumb_ (cp10, sm11) can't use this --
# fall back to tools\auto_cells_new.ps1 for those.
#
# Windows PowerShell 5.1 + System.Drawing only. ASCII only.
#
# Usage: powershell -File tools\diff_cells.ps1 -Ids bd11,bd12 [-Thresh 26]

param(
  [string[]]$Ids = @(),
  [int]$Thresh = 26,      # per-pixel RGB-sum difference that counts as "photo here"
  [double]$MinAreaFrac = 0.008,
  # Morphological close passes. 1 is the sweet spot: it heals speckle without bridging two
  # slots that sit a few pixels apart. Raise to 2 only when one slot fragments; drop to 0 when
  # neighbouring slots merge into a single blob.
  [int]$Close = 1
)

Add-Type -AssemblyName System.Drawing

$SRC = "D:\EZTech\AppAssets\PhotoCollage\Category_Template"
$OUT = "D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\diffcells"
New-Item -ItemType Directory -Force -Path $OUT | Out-Null

$LOGIC_W = 1125; $LOGIC_H = 2000
$AW = 300; $AH = 533          # analysis res (diff signal is strong, so this is plenty)

$CATS = @{
  'bd' = @('Birthday', 'BD'); 'cp' = @('Couple', 'CP'); 'gs' = @('Glad season', 'GS')
  'is' = @('IG Story', 'IS'); 'sm' = @('Summer vibe', 'SM'); 'sp' = @('Sports', 'SP')
}

$jpgEnc = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq 'image/jpeg' }
function Save-Jpeg($bmp, $path, $q) {
  $eps = New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality, [long]$q)
  $bmp.Save($path, $jpgEnc, $eps); $eps.Dispose()
}

# Load a PNG downscaled to analysis res as a raw 24bpp byte buffer.
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
  $np = $bxs.Count
  $bestArea = [double]::MaxValue; $best = $null
  for ($deg = -45.0; $deg -lt 45.0; $deg += 0.5) {
    $rad = $deg * [Math]::PI / 180.0
    $cos = [Math]::Cos($rad); $sin = [Math]::Sin($rad)
    $minU = 1e9; $maxU = -1e9; $minV = 1e9; $maxV = -1e9
    for ($k = 0; $k -lt $np; $k++) {
      $u = $bxs[$k] * $cos + $bys[$k] * $sin
      $v = -$bxs[$k] * $sin + $bys[$k] * $cos
      if ($u -lt $minU) { $minU = $u }; if ($u -gt $maxU) { $maxU = $u }
      if ($v -lt $minV) { $minV = $v }; if ($v -gt $maxV) { $maxV = $v }
    }
    $a = ($maxU - $minU) * ($maxV - $minV)
    if ($a -lt $bestArea) { $bestArea = $a; $best = @{ deg = $deg; u0 = $minU; u1 = $maxU; v0 = $minV; v1 = $maxV } }
  }
  $w = $best.u1 - $best.u0; $h = $best.v1 - $best.v0
  $uc = ($best.u0 + $best.u1) / 2.0; $vc = ($best.v0 + $best.v1) / 2.0
  $rad = $best.deg * [Math]::PI / 180.0
  $cos = [Math]::Cos($rad); $sin = [Math]::Sin($rad)
  return @{ cx = ($uc * $cos - $vc * $sin); cy = ($uc * $sin + $vc * $cos); w = $w; h = $h; deg = $best.deg }
}

if ($Ids.Count -eq 0) { Write-Host "No -Ids."; exit 1 }
$Ids = @($Ids | ForEach-Object { $_ -split ',' } | Where-Object { $_ -match '\S' } | ForEach-Object { $_.Trim().ToLower() })

$configLines = @()
$maskLines   = @()

foreach ($id in $Ids) {
  $pre = $id.Substring(0, 2); $nn = $id.Substring(2)
  if (-not $CATS.ContainsKey($pre)) { Write-Host "$id : bad prefix"; continue }
  $folder = $CATS[$pre][0]; $P = $CATS[$pre][1]
  $tempP  = Join-Path $SRC ("{0}\Temp_{1}{2}.png"  -f $folder, $P, $nn)
  $thumbP = Join-Path $SRC ("{0}\Thumb_{1}{2}.png" -f $folder, $P, $nn)
  if (-not (Test-Path $tempP))  { Write-Host "$id : no Temp_";  continue }
  if (-not (Test-Path $thumbP)) { Write-Host "$id : no Thumb_ -> use auto_cells_new.ps1"; continue }

  $sA = 0; $sB = 0
  $bufT = Get-Buf $tempP  ([ref]$sA)
  $bufH = Get-Buf $thumbP ([ref]$sB)

  $N = $AW * $AH
  $m = New-Object 'bool[]' $N
  $brT = New-Object 'byte[]' $N
  $satT = New-Object 'byte[]' $N
  $nDiff = 0
  for ($y = 0; $y -lt $AH; $y++) {
    $rA = $y * $sA; $rB = $y * $sB; $base = $y * $AW
    for ($x = 0; $x -lt $AW; $x++) {
      $oA = $rA + $x * 3; $oB = $rB + $x * 3
      $b1 = $bufT[$oA]; $g1 = $bufT[$oA + 1]; $r1 = $bufT[$oA + 2]
      $b2 = $bufH[$oB]; $g2 = $bufH[$oB + 1]; $r2 = $bufH[$oB + 2]
      $d = [Math]::Abs([int]$r1 - [int]$r2) + [Math]::Abs([int]$g1 - [int]$g2) + [Math]::Abs([int]$b1 - [int]$b2)
      $i = $base + $x
      $mx = $r1; if ($g1 -gt $mx) { $mx = $g1 }; if ($b1 -gt $mx) { $mx = $b1 }
      $mn = $r1; if ($g1 -lt $mn) { $mn = $g1 }; if ($b1 -lt $mn) { $mn = $b1 }
      $brT[$i] = $mx; $satT[$i] = $mx - $mn
      if ($d -ge $Thresh) { $m[$i] = $true; $nDiff++ }
    }
  }

  # Morphological close (3x3 dilate then erode) so JPEG-ish speckle inside a slot doesn't
  # fragment it, and a sticker sitting on top of a slot doesn't split it in two.
  function Dilate($src) {
    $o = New-Object 'bool[]' $N
    for ($y = 0; $y -lt $AH; $y++) { for ($x = 0; $x -lt $AW; $x++) {
      $i = $y * $AW + $x
      if ($src[$i]) { $o[$i] = $true; continue }
      $hit = $false
      for ($dy = -1; $dy -le 1 -and -not $hit; $dy++) { for ($dx = -1; $dx -le 1; $dx++) {
        $ny = $y + $dy; $nx = $x + $dx
        if ($ny -lt 0 -or $ny -ge $AH -or $nx -lt 0 -or $nx -ge $AW) { continue }
        if ($src[$ny * $AW + $nx]) { $hit = $true; break }
      }}
      $o[$i] = $hit
    }}
    return $o
  }
  function Erode($src) {
    $o = New-Object 'bool[]' $N
    for ($y = 0; $y -lt $AH; $y++) { for ($x = 0; $x -lt $AW; $x++) {
      $i = $y * $AW + $x
      if (-not $src[$i]) { $o[$i] = $false; continue }
      $all = $true
      for ($dy = -1; $dy -le 1 -and $all; $dy++) { for ($dx = -1; $dx -le 1; $dx++) {
        $ny = $y + $dy; $nx = $x + $dx
        if ($ny -lt 0 -or $ny -ge $AH -or $nx -lt 0 -or $nx -ge $AW) { continue }
        if (-not $src[$ny * $AW + $nx]) { $all = $false; break }
      }}
      $o[$i] = $all
    }}
    return $o
  }
  if ($Close -gt 0) {
    for ($c = 0; $c -lt $Close; $c++) { $m = Dilate $m }
    for ($c = 0; $c -lt $Close; $c++) { $m = Erode  $m }
  }

  # connected components
  $visited = New-Object 'bool[]' $N
  $st = New-Object System.Collections.Generic.Stack[int]
  $cells = @()
  for ($i = 0; $i -lt $N; $i++) {
    if (-not $m[$i] -or $visited[$i]) { continue }
    $st.Clear(); $st.Push($i); $visited[$i] = $true
    $minX = $AW; $minY = $AH; $maxX = 0; $maxY = 0; $cnt = 0
    $sumB = 0.0; $sumS = 0.0
    $bxs = New-Object System.Collections.Generic.List[int]
    $bys = New-Object System.Collections.Generic.List[int]
    while ($st.Count -gt 0) {
      $q = $st.Pop(); $cnt++
      $qx = $q % $AW; $qy = [int]($q / $AW)
      $sumB += $brT[$q]; $sumS += $satT[$q]
      if ($qx -lt $minX) { $minX = $qx }; if ($qx -gt $maxX) { $maxX = $qx }
      if ($qy -lt $minY) { $minY = $qy }; if ($qy -gt $maxY) { $maxY = $qy }
      $isB = $false
      if ($qx -eq 0 -or $qx -eq $AW - 1 -or $qy -eq 0 -or $qy -eq $AH - 1) { $isB = $true }
      elseif (-not $m[$q - 1] -or -not $m[$q + 1] -or -not $m[$q - $AW] -or -not $m[$q + $AW]) { $isB = $true }
      if ($isB) { $bxs.Add($qx); $bys.Add($qy) }
      if ($qx -gt 0)       { $n2 = $q - 1;   if ($m[$n2] -and -not $visited[$n2]) { $visited[$n2] = $true; $st.Push($n2) } }
      if ($qx -lt $AW - 1) { $n2 = $q + 1;   if ($m[$n2] -and -not $visited[$n2]) { $visited[$n2] = $true; $st.Push($n2) } }
      if ($qy -gt 0)       { $n2 = $q - $AW; if ($m[$n2] -and -not $visited[$n2]) { $visited[$n2] = $true; $st.Push($n2) } }
      if ($qy -lt $AH - 1) { $n2 = $q + $AW; if ($m[$n2] -and -not $visited[$n2]) { $visited[$n2] = $true; $st.Push($n2) } }
    }
    $bw = $maxX - $minX + 1; $bh = $maxY - $minY + 1
    $areaFrac = ($bw * $bh) / [double]$N
    if ($areaFrac -lt $MinAreaFrac) { continue }
    $aspect = $bw / [double]$bh
    if ($aspect -lt 0.08 -or $aspect -gt 12) { continue }

    $mr = Fit-MinRect $bxs $bys
    $sx = $LOGIC_W / [double]$AW; $sy = $LOGIC_H / [double]$AH
    $lw = $mr.w * $sx; $lh = $mr.h * $sy
    $lcx = $mr.cx * $sx; $lcy = $mr.cy * $sy
    $deg = [math]::Round($mr.deg, 1)
    $mrFill = $cnt / [double]([Math]::Max(1.0, $mr.w * $mr.h))
    $shape = if ($mrFill -gt 0.90) { 'rect' } elseif ($mrFill -gt 0.72) { 'round' } else { 'blob' }
    $meanB = [int][math]::Round($sumB / $cnt); $meanS = [int][math]::Round($sumS / $cnt)

    # A circle/ellipse has no meaningful orientation -- the min-area sweep picks an arbitrary
    # angle, which would rotate the cell for no reason. Use the axis-aligned bbox instead.
    if ($shape -eq 'round') {
      $deg = 0
      $lcx = ($minX + $maxX + 1) / 2.0 * $sx; $lcy = ($minY + $maxY + 1) / 2.0 * $sy
      $lw = $bw * $sx; $lh = $bh * $sy
    }

    $rl = [int][math]::Round($lcx - $lw / 2); $rt = [int][math]::Round($lcy - $lh / 2)
    $rr = [int][math]::Round($lcx + $lw / 2); $rb = [int][math]::Round($lcy + $lh / 2)
    if ([math]::Abs($deg) -lt 0.5) {
      $rl = [Math]::Max(0, $rl); $rt = [Math]::Max(0, $rt)
      $rr = [Math]::Min($LOGIC_W, $rr); $rb = [Math]::Min($LOGIC_H, $rb)
    }
    $cells += [pscustomobject]@{
      l = $rl; t = $rt; r = $rr; b = $rb; deg = $deg
      fill = [math]::Round($mrFill, 2); shape = $shape; meanB = $meanB; meanS = $meanS; px = $cnt
    }
  }
  $cells = @($cells | Sort-Object t, l)

  # ---- suggest a MaskMode from the slot colour in Temp_ ----
  # WHITE >240 | GRAY 231..243 | GRAY2 200..228 (must match TemplateEditorView thresholds).
  # Non-rect slots NEED a colour mask to clip properly; clean rects can use NONE.
  $anyNonRect = @($cells | Where-Object { $_.shape -ne 'rect' }).Count -gt 0
  $avgB = if ($cells.Count) { [int](($cells | Measure-Object meanB -Average).Average) } else { 0 }
  $avgS = if ($cells.Count) { [int](($cells | Measure-Object meanS -Average).Average) } else { 0 }
  $suggest = 'NONE'
  if ($avgS -le 10) {
    if     ($avgB -ge 244)                    { $suggest = 'WHITE' }
    elseif ($avgB -ge 231 -and $avgB -le 243) { $suggest = 'GRAY'  }
    elseif ($avgB -ge 200 -and $avgB -le 228) { $suggest = 'GRAY2' }
  }
  $maskNote = if ($anyNonRect -and $suggest -eq 'NONE') { '  (non-rect slot but colour out of band - needs a look)' } else { '' }

  Write-Host ""
  Write-Host ("==== {0} ====  diffPx={1:P1}  cells={2}  slotB~{3} sat~{4}  mask->{5}{6}" -f `
              $id, ($nDiff / [double]$N), $cells.Count, $avgB, $avgS, $suggest, $maskNote)
  $k = 0
  foreach ($c in $cells) {
    $k++
    Write-Host ("   {0}. {1,-5} deg={2,6} fill={3,5} B={4,4} sat={5,3}  ({6},{7},{8},{9})" -f `
                $k, $c.shape, $c.deg, $c.fill, $c.meanB, $c.meanS, $c.l, $c.t, $c.r, $c.b)
  }

  if ($cells.Count) {
    $parts = @()
    foreach ($c in $cells) {
      if ([math]::Abs($c.deg) -ge 0.5) { $parts += ("@({0},{1},{2},{3},{4})" -f $c.l, $c.t, $c.r, $c.b, $c.deg) }
      else                             { $parts += ("@({0},{1},{2},{3})"     -f $c.l, $c.t, $c.r, $c.b) }
    }
    $configLines += if ($cells.Count -eq 1) { '  "{0}"=@(,{1})' -f $id, $parts[0] } else { '  "{0}"=@({1})' -f $id, ($parts -join ',') }
    if ($suggest -ne 'NONE') { $maskLines += ('"{0}"="{1}"' -f $id, $suggest) }
  }

  # ---- overlay: frame + detected rects + the diff mask tinted, for eyeball verification ----
  $OW = 360; $OH = 640
  $ov = New-Object System.Drawing.Bitmap $OW, $OH
  $go = [System.Drawing.Graphics]::FromImage($ov)
  $go.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $tImg = [System.Drawing.Image]::FromFile($tempP)
  $go.DrawImage($tImg, 0, 0, $OW, $OH); $tImg.Dispose()
  # tint the diff mask cyan at 40%
  $tint = New-Object System.Drawing.Bitmap $AW, $AH
  for ($y = 0; $y -lt $AH; $y++) { for ($x = 0; $x -lt $AW; $x++) {
    if ($m[$y * $AW + $x]) { $tint.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(100, 0, 200, 255)) }
  }}
  $go.DrawImage($tint, 0, 0, $OW, $OH); $tint.Dispose()
  $pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::Red), 2
  $fnt = New-Object System.Drawing.Font 'Consolas', 11, ([System.Drawing.FontStyle]::Bold)
  $spx = $OW / [double]$LOGIC_W; $spy = $OH / [double]$LOGIC_H
  $k = 0
  foreach ($c in $cells) {
    $k++
    $cw = ($c.r - $c.l) * $spx; $ch = ($c.b - $c.t) * $spy
    $ccx = ($c.l + $c.r) / 2.0 * $spx; $ccy = ($c.t + $c.b) / 2.0 * $spy
    $sv = $go.Save()
    $go.TranslateTransform([float]$ccx, [float]$ccy); $go.RotateTransform([float]$c.deg)
    $go.DrawRectangle($pen, [float](-$cw / 2), [float](-$ch / 2), [float]$cw, [float]$ch)
    $go.Restore($sv)
    $go.DrawString([string]$k, $fnt, [System.Drawing.Brushes]::Red, [float]($ccx - 7), [float]($ccy - 9))
  }
  $pen.Dispose(); $fnt.Dispose(); $go.Dispose()
  Save-Jpeg $ov (Join-Path $OUT "$id.jpg") 88
  $ov.Dispose()
}

Write-Host ""
Write-Host "======== paste into tools\cells_config.ps1 ========"
$configLines | ForEach-Object { Write-Host $_ }
Write-Host ""
Write-Host "======== MASKS suggestions ========"
Write-Host ($maskLines -join '; ')
Write-Host ""
Write-Host "Overlays -> $OUT"
