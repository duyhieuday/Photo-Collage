# Photo-cell detector combining the two signals that each solve half the problem:
#
#   1. DIFF  Temp_ (empty frame) vs Thumb_ (same design, photos dropped in).
#            Tells us exactly WHERE photos go. Immune to slot colour. But slots that touch
#            each other fuse into one component, so it can't say HOW MANY slots there are.
#
#   2. BAND   flat neutral colour range inside Temp_ (white / #ededed / light gray).
#            Separates individual slots, because adjacent slots are divided by a differently
#            coloured border. But on its own it also lights up white text, paper backgrounds
#            and decorations.
#
# Intersecting them cancels both weaknesses: band components are kept only where the diff says
# a photo actually lands. Each surviving component gets a minimum-area rotated rect.
#
# Bands mirror the runtime thresholds in TemplateEditorView.createMaskFrom{White,Gray,Gray2}
# so the suggested MaskMode is the one that will actually clip correctly at runtime.
#
# Windows PowerShell 5.1 + System.Drawing only. ASCII only.
#
# Usage: powershell -File tools\cells2.ps1 -Ids bd13,gs15 [-Band auto|white|gray|gray2|diff]

param(
  [string[]]$Ids = @(),
  [string]$Band = 'auto',
  # Diff threshold (RGB-sum) that counts as "photo here". Raise it (~45-70) when white text or
  # a caption box gets picked up as a slot: text renders near-identically in Temp_ and Thumb_,
  # so it only clears a low threshold.
  [int]$Thresh = 26,
  [double]$MinAreaFrac = 0.008,
  [double]$MinFill = 0.80,     # component must fill this much of its own min-area rect
  # Analysis resolution. Raise it (450/600) when slots divided by a hairline separator fuse
  # into one component -- at 300px wide a 2px gridline disappears.
  [int]$AW = 300,
  # How solid a component must be to count as a slot (fraction of its own min-area rect).
  # 1.0 = perfect rect, ~0.79 = circle. Lower it to diagnose why a template yields no cells.
  [double]$MinShapeFill = 0.68,
  # Minimum length of a slot's shorter side, in logic units, to reject text strips.
  [int]$MinSide = 130
)

Add-Type -AssemblyName System.Drawing

$SRC = "D:\EZTech\AppAssets\PhotoCollage\Category_Template"
$OUT = "D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\cells2"
New-Item -ItemType Directory -Force -Path $OUT | Out-Null

$LOGIC_W = 1125; $LOGIC_H = 2000
$AH = [int][math]::Round($AW * 16.0 / 9.0)   # keep the 9:16 template aspect

$CATS = @{
  'bd' = @('Birthday', 'BD'); 'cp' = @('Couple', 'CP'); 'gs' = @('Glad season', 'GS')
  'is' = @('IG Story', 'IS'); 'sm' = @('Summer vibe', 'SM'); 'sp' = @('Sports', 'SP')
}
# band -> lo, hi, maxSat, MaskMode name
$BANDS = [ordered]@{
  'white' = @(244, 255, 10, 'WHITE')
  'gray'  = @(229, 245, 10, 'GRAY')
  'gray2' = @(198, 230, 12, 'GRAY2')
}

$jpgEnc = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq 'image/jpeg' }
function Save-Jpeg($bmp, $path, $q) {
  $eps = New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality, [long]$q)
  $bmp.Save($path, $jpgEnc, $eps); $eps.Dispose()
}
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
  for ($deg = -45.0; $deg -lt 45.0; $deg += 0.5) {
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

$N = $AW * $AH
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
    if (-not $src[$i]) { continue }
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

# Connected components over $mask, each fitted with a min-area rotated rect.
function Get-Cells($mask, $brT, $satT) {
  $visited = New-Object 'bool[]' $N
  $st = New-Object System.Collections.Generic.Stack[int]
  $out = @()
  $sx = $LOGIC_W / [double]$AW; $sy = $LOGIC_H / [double]$AH
  for ($i = 0; $i -lt $N; $i++) {
    if (-not $mask[$i] -or $visited[$i]) { continue }
    $st.Clear(); $st.Push($i); $visited[$i] = $true
    $minX = $AW; $minY = $AH; $maxX = 0; $maxY = 0; $cnt = 0; $sumB = 0.0; $sumS = 0.0
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
      elseif (-not $mask[$q - 1] -or -not $mask[$q + 1] -or -not $mask[$q - $AW] -or -not $mask[$q + $AW]) { $isB = $true }
      if ($isB) { $bxs.Add($qx); $bys.Add($qy) }
      if ($qx -gt 0)       { $n2 = $q - 1;   if ($mask[$n2] -and -not $visited[$n2]) { $visited[$n2] = $true; $st.Push($n2) } }
      if ($qx -lt $AW - 1) { $n2 = $q + 1;   if ($mask[$n2] -and -not $visited[$n2]) { $visited[$n2] = $true; $st.Push($n2) } }
      if ($qy -gt 0)       { $n2 = $q - $AW; if ($mask[$n2] -and -not $visited[$n2]) { $visited[$n2] = $true; $st.Push($n2) } }
      if ($qy -lt $AH - 1) { $n2 = $q + $AW; if ($mask[$n2] -and -not $visited[$n2]) { $visited[$n2] = $true; $st.Push($n2) } }
    }
    $bw = $maxX - $minX + 1; $bh = $maxY - $minY + 1
    $areaFrac = ($bw * $bh) / [double]$N
    if ($areaFrac -lt $MinAreaFrac) { continue }
    $asp = $bw / [double]$bh
    if ($asp -lt 0.08 -or $asp -gt 12) { continue }

    $mr = Fit-MinRect $bxs $bys
    $mrFill = $cnt / [double]([Math]::Max(1.0, $mr.w * $mr.h))
    # A photo slot is a solid shape: a rect fills ~1.0 of its min-area rect, a circle ~0.79.
    # Anything ragged is a caption box, a run of white text, or background leaking through --
    # never a slot. This single test replaces the old size-conditional variants.
    if ($mrFill -lt $MinShapeFill) { continue }
    $deg = [math]::Round($mr.deg, 1)
    $lw = $mr.w * $sx; $lh = $mr.h * $sy; $lcx = $mr.cx * $sx; $lcy = $mr.cy * $sy
    # Only a GENUINELY round shape has no orientation. The old test ("fill < 0.90 -> treat as
    # round, force deg=0") threw away the real tilt of soft-edged tilted frames, which sit around
    # fill 0.80-0.88 -- that is what flattened cp01, cp17 and bd13 and made their photos sit
    # crooked in the frame. Require BOTH a near-square min-rect AND a circle-like fill (pi/4).
    $ratio = if ($mr.h -gt 0) { $mr.w / $mr.h } else { 9 }
    $isRound = ($mrFill -lt 0.86 -and $ratio -gt 0.80 -and $ratio -lt 1.25)
    $shape = if ($isRound) { 'round' } elseif ($mrFill -gt 0.90) { 'rect' } else { 'tilted' }
    if ($isRound) {
      $deg = 0
      $lcx = ($minX + $maxX + 1) / 2.0 * $sx; $lcy = ($minY + $maxY + 1) / 2.0 * $sy
      $lw = $bw * $sx; $lh = $bh * $sy
    }
    # Slivers: a real slot holds a photo, so neither side is narrow. Catches thin strips that
    # survive the fill test -- a vertical run of rotated text is both solid and slot-coloured.
    # 130 of 1125 logic units; the smallest genuine slot in this asset set is ~300 wide.
    if ([Math]::Min($lw, $lh) -lt $MinSide) { continue }

    $rl = [int][math]::Round($lcx - $lw / 2); $rt = [int][math]::Round($lcy - $lh / 2)
    $rr = [int][math]::Round($lcx + $lw / 2); $rb = [int][math]::Round($lcy + $lh / 2)
    if ([math]::Abs($deg) -lt 0.5) {
      $rl = [Math]::Max(0, $rl); $rt = [Math]::Max(0, $rt)
      $rr = [Math]::Min($LOGIC_W, $rr); $rb = [Math]::Min($LOGIC_H, $rb)
    }
    $out += [pscustomobject]@{
      l = $rl; t = $rt; r = $rr; b = $rb; deg = $deg; fill = [math]::Round($mrFill, 2)
      shape = $shape; meanB = [int][math]::Round($sumB / $cnt); meanS = [int][math]::Round($sumS / $cnt)
      px = $cnt
    }
  }
  return @($out | Sort-Object t, l)
}

if ($Ids.Count -eq 0) { Write-Host "No -Ids."; exit 1 }
$Ids = @($Ids | ForEach-Object { $_ -split ',' } | Where-Object { $_ -match '\S' } | ForEach-Object { $_.Trim().ToLower() })

$cfg = @(); $msk = @(); $review = @()

foreach ($id in $Ids) {
  $pre = $id.Substring(0, 2); $nn = $id.Substring(2)
  if (-not $CATS.ContainsKey($pre)) { Write-Host "$id : bad prefix"; continue }
  $folder = $CATS[$pre][0]; $P = $CATS[$pre][1]
  $tempP  = Join-Path $SRC ("{0}\Temp_{1}{2}.png"  -f $folder, $P, $nn)
  $thumbP = Join-Path $SRC ("{0}\Thumb_{1}{2}.png" -f $folder, $P, $nn)
  if (-not (Test-Path $tempP)) { Write-Host "$id : no Temp_"; continue }
  $haveThumb = Test-Path $thumbP

  $sA = 0; $bufT = Get-Buf $tempP ([ref]$sA)
  $brT  = New-Object 'byte[]' $N
  $satT = New-Object 'byte[]' $N
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

  # ---- diff mask (photo regions) ----
  $Dm = New-Object 'bool[]' $N
  $nD = 0
  if ($haveThumb) {
    $sB = 0; $bufH = Get-Buf $thumbP ([ref]$sB)
    for ($y = 0; $y -lt $AH; $y++) {
      $rA = $y * $sA; $rB = $y * $sB; $base = $y * $AW
      for ($x = 0; $x -lt $AW; $x++) {
        $oA = $rA + $x * 3; $oB = $rB + $x * 3
        $d = [Math]::Abs([int]$bufT[$oA] - [int]$bufH[$oB]) + `
             [Math]::Abs([int]$bufT[$oA + 1] - [int]$bufH[$oB + 1]) + `
             [Math]::Abs([int]$bufT[$oA + 2] - [int]$bufH[$oB + 2])
        if ($d -ge $Thresh) { $Dm[$base + $x] = $true; $nD++ }
      }
    }
    $Dm = Erode (Dilate $Dm)          # heal speckle without bridging neighbours
    $Dm = Dilate (Dilate $Dm)         # grow a little so band pixels at slot edges survive the gate
  } else {
    for ($i = 0; $i -lt $N; $i++) { $Dm[$i] = $true }   # no Thumb_ -> no gate
    $nD = $N
  }

  # ---- candidate cell sets: each band gated by the diff, plus raw diff as fallback ----
  $tries = [ordered]@{}
  foreach ($bn in $BANDS.Keys) {
    $lo = $BANDS[$bn][0]; $hi = $BANDS[$bn][1]; $ms = $BANDS[$bn][2]
    $bm = New-Object 'bool[]' $N
    for ($i = 0; $i -lt $N; $i++) {
      if (-not $Dm[$i]) { continue }
      if ($brT[$i] -ge $lo -and $brT[$i] -le $hi -and $satT[$i] -le $ms) { $bm[$i] = $true }
    }
    $bm = Erode (Dilate $bm)
    $tries[$bn] = @(Get-Cells $bm $brT $satT)
  }
  if ($haveThumb) { $tries['diff'] = @(Get-Cells $Dm $brT $satT) }

  # ---- score: prefer the set whose cells are clean rects and cover most of the diff area ----
  $bestName = $null; $bestScore = -1
  foreach ($k in $tries.Keys) {
    $cs = $tries[$k]
    if ($cs.Count -eq 0) { continue }
    $clean = @($cs | Where-Object { $_.fill -ge $MinFill }).Count
    $cover = (($cs | Measure-Object px -Sum).Sum) / [double][Math]::Max(1, $nD)
    if ($cover -gt 1) { $cover = 1 / $cover }         # overshooting the diff area is bad too
    $score = $cover * (0.35 + 0.65 * ($clean / [double]$cs.Count))
    if ($score -gt $bestScore) { $bestScore = $score; $bestName = $k }
  }
  $pick = if ($Band -ne 'auto' -and $tries.Contains($Band)) { $Band } else { $bestName }
  if (-not $pick) { Write-Host ("==== {0} ====  NO CELLS FOUND" -f $id); $review += $id; continue }
  $cells = @($tries[$pick])

  $maskName = if ($pick -eq 'diff') { 'NONE' } else { $BANDS[$pick][3] }
  $nonRect  = @($cells | Where-Object { $_.shape -ne 'rect' }).Count
  # A non-rectangular slot can only be clipped by a colour mask, never by MaskMode.NONE.
  $flag = @()
  if ($nonRect -gt 0 -and $maskName -eq 'NONE') { $flag += 'nonrect-needs-mask' }
  if ($nonRect -gt 0) { $flag += "$nonRect nonrect" }
  if ($bestScore -lt 0.45) { $flag += ('lowscore={0:N2}' -f $bestScore) }
  if (-not $haveThumb) { $flag += 'no-thumb(ungated)' }
  if ($flag.Count) { $review += $id }

  Write-Host ""
  Write-Host ("==== {0} ====  band={1}  cells={2}  mask->{3}  score={4:N2}  {5}" -f `
              $id, $pick, $cells.Count, $maskName, $bestScore, ($flag -join ' | '))
  foreach ($k in $tries.Keys) {
    $mark = if ($k -eq $pick) { '*' } else { ' ' }
    Write-Host ("     {0}{1,-6} -> {2} cells" -f $mark, $k, $tries[$k].Count)
  }
  $i2 = 0
  foreach ($c in $cells) {
    $i2++
    Write-Host ("   {0}. {1,-5} deg={2,6} fill={3,5} B={4,4} ({5},{6},{7},{8})" -f `
                $i2, $c.shape, $c.deg, $c.fill, $c.meanB, $c.l, $c.t, $c.r, $c.b)
  }

  $parts = @()
  foreach ($c in $cells) {
    if ([math]::Abs($c.deg) -ge 0.5) { $parts += ("@({0},{1},{2},{3},{4})" -f $c.l, $c.t, $c.r, $c.b, $c.deg) }
    else                             { $parts += ("@({0},{1},{2},{3})"     -f $c.l, $c.t, $c.r, $c.b) }
  }
  $cfg += if ($cells.Count -eq 1) { '  "{0}"=@(,{1})' -f $id, $parts[0] } else { '  "{0}"=@({1})' -f $id, ($parts -join ',') }
  if ($maskName -ne 'NONE') { $msk += ('"{0}"="{1}"' -f $id, $maskName) }

  # ---- overlay ----
  $OW = 360; $OH = 640
  $ov = New-Object System.Drawing.Bitmap $OW, $OH
  $go = [System.Drawing.Graphics]::FromImage($ov)
  $go.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $tImg = [System.Drawing.Image]::FromFile($tempP)
  $go.DrawImage($tImg, 0, 0, $OW, $OH); $tImg.Dispose()
  $pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::Red), 2
  $fnt = New-Object System.Drawing.Font 'Consolas', 11, ([System.Drawing.FontStyle]::Bold)
  $lbl = New-Object System.Drawing.Font 'Consolas', 10, ([System.Drawing.FontStyle]::Bold)
  $spx = $OW / [double]$LOGIC_W; $spy = $OH / [double]$LOGIC_H
  $i2 = 0
  foreach ($c in $cells) {
    $i2++
    $cw = ($c.r - $c.l) * $spx; $ch = ($c.b - $c.t) * $spy
    $ccx = ($c.l + $c.r) / 2.0 * $spx; $ccy = ($c.t + $c.b) / 2.0 * $spy
    $sv = $go.Save()
    $go.TranslateTransform([float]$ccx, [float]$ccy); $go.RotateTransform([float]$c.deg)
    $go.DrawRectangle($pen, [float](-$cw / 2), [float](-$ch / 2), [float]$cw, [float]$ch)
    $go.Restore($sv)
    $go.DrawString([string]$i2, $fnt, [System.Drawing.Brushes]::Red, [float]($ccx - 7), [float]($ccy - 9))
  }
  $hdr = "$id $pick/$maskName n=$($cells.Count)"
  $go.FillRectangle([System.Drawing.Brushes]::Black, 0, 0, $OW, 16)
  $go.DrawString($hdr, $lbl, [System.Drawing.Brushes]::Yellow, 2, 1)
  $pen.Dispose(); $fnt.Dispose(); $lbl.Dispose(); $go.Dispose()
  Save-Jpeg $ov (Join-Path $OUT "$id.jpg") 88
  $ov.Dispose()
}

Write-Host ""
Write-Host "======== cells_config.ps1 ========"
$cfg | ForEach-Object { Write-Host $_ }
Write-Host ""
Write-Host "======== MASKS ========"
Write-Host ($msk -join '; ')
Write-Host ""
Write-Host ("======== NEEDS EYEBALL ({0}): {1}" -f $review.Count, ($review -join ', '))
Write-Host "Overlays -> $OUT"
