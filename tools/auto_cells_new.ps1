# Auto-detect photo cells for NEW templates, straight from source PNG (lossless).
# Builds 3 neutral colour-band masks in ONE pixel pass (white / gray #ededed / gray2 light),
# finds connected components per band, then fits a min-area rotated rect to each.
# Emits cells_config.ps1-format lines + an overlay JPEG per template for eyeball verification.
# Windows PowerShell 5.1 + System.Drawing only. ASCII only.
#
# Usage:  powershell -File tools\auto_cells_new.ps1 [-Ids bd11,bd12] [-Band all|white|gray|gray2]

param(
  [string[]]$Ids = @(),
  [string]$Band = 'all'
)

Add-Type -AssemblyName System.Drawing

$SRC     = "D:\EZTech\AppAssets\PhotoCollage\Category_Template"
$OUT     = "D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\newcells"
New-Item -ItemType Directory -Force -Path $OUT | Out-Null

# Logic space must match TemplateEditor (TEMPLATE_W=1125, 9:16)
$LOGIC_W = 1125; $LOGIC_H = 2000
# Analysis resolution: big enough for a stable min-area-rect angle, small enough for PS loops.
$AW = 450; $AH = 800

# id -> source folder + Temp_ file prefix
$CATS = @{
  'bd' = @('Birthday',    'BD')
  'cp' = @('Couple',      'CP')
  'gs' = @('Glad season', 'GS')
  'is' = @('IG Story',    'IS')
  'sm' = @('Summer vibe', 'SM')
  'sp' = @('Sports',      'SP')
}

# Colour bands: name -> @(lo, hi, maxSaturation). Mirrors the runtime mask thresholds in
# TemplateEditorView.createMaskFromWhite / ...Gray / ...Gray2.
$BANDS = [ordered]@{
  'white' = @(248, 255, 8)
  'gray'  = @(231, 243, 8)
  'gray2' = @(200, 228, 10)
}

$jpgEnc = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq 'image/jpeg' }
function Save-Jpeg($bmp, $path, $q) {
  $eps = New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality, [long]$q)
  $bmp.Save($path, $jpgEnc, $eps); $eps.Dispose()
}

function Get-SourcePath([string]$id) {
  $pre = $id.Substring(0, 2)
  if (-not $CATS.ContainsKey($pre)) { return $null }
  $nn = $id.Substring(2)
  return Join-Path $SRC ("{0}\Temp_{1}{2}.png" -f $CATS[$pre][0], $CATS[$pre][1], $nn)
}

# Fit the minimum-area rotated rect over boundary points (brute-force angle sweep).
# Returns @{cx cy w h deg} in ANALYSIS space.
function Fit-MinRect($bxs, $bys) {
  $np = $bxs.Count
  $bestArea = [double]::MaxValue; $best = $null
  for ($deg = -45.0; $deg -lt 45.0; $deg += 0.5) {
    $rad = $deg * [Math]::PI / 180.0
    $cos = [Math]::Cos($rad); $sin = [Math]::Sin($rad)
    $minU = 1e9; $maxU = -1e9; $minV = 1e9; $maxV = -1e9
    for ($k = 0; $k -lt $np; $k++) {
      $px = $bxs[$k]; $py = $bys[$k]
      $u = $px * $cos + $py * $sin
      $v = -$px * $sin + $py * $cos
      if ($u -lt $minU) { $minU = $u }; if ($u -gt $maxU) { $maxU = $u }
      if ($v -lt $minV) { $minV = $v }; if ($v -gt $maxV) { $maxV = $v }
    }
    $area = ($maxU - $minU) * ($maxV - $minV)
    if ($area -lt $bestArea) {
      $bestArea = $area
      $best = @{ deg = $deg; u0 = $minU; u1 = $maxU; v0 = $minV; v1 = $maxV }
    }
  }
  $w = $best.u1 - $best.u0; $h = $best.v1 - $best.v0
  $uc = ($best.u0 + $best.u1) / 2.0; $vc = ($best.v0 + $best.v1) / 2.0
  $rad = $best.deg * [Math]::PI / 180.0
  $cos = [Math]::Cos($rad); $sin = [Math]::Sin($rad)
  return @{
    cx = ($uc * $cos - $vc * $sin); cy = ($uc * $sin + $vc * $cos)
    w = $w; h = $h; deg = $best.deg
  }
}

# ---------------- main ----------------
if ($Ids.Count -eq 0) {
  Write-Host "No -Ids given. Nothing to do."
  exit 1
}
# `powershell -File x.ps1 -Ids a,b,c` arrives as one string -> split it here.
$Ids = @($Ids | ForEach-Object { $_ -split ',' } | Where-Object { $_ -match '\S' } | ForEach-Object { $_.Trim().ToLower() })

foreach ($id in $Ids) {
  $p = Get-SourcePath $id
  if (-not $p -or -not (Test-Path $p)) { Write-Host "  $id : SOURCE MISSING ($p)"; continue }

  # --- downscale once, nearest-neighbour so flat slot colours stay exact (no interpolation bleed)
  $img = [System.Drawing.Image]::FromFile($p)
  $small = New-Object System.Drawing.Bitmap $AW, $AH
  $g = [System.Drawing.Graphics]::FromImage($small)
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
  $g.PixelOffsetMode   = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
  $g.DrawImage($img, 0, 0, $AW, $AH); $g.Dispose()

  $rect = New-Object System.Drawing.Rectangle 0, 0, $AW, $AH
  $data = $small.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
  $stride = $data.Stride
  $buf = New-Object byte[] ($stride * $AH)
  [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $buf, 0, $buf.Length)
  $small.UnlockBits($data); $small.Dispose()

  $N = $AW * $AH
  # ONE pixel pass -> three band masks + brightness cache
  $mW  = New-Object 'bool[]' $N
  $mG  = New-Object 'bool[]' $N
  $mG2 = New-Object 'bool[]' $N
  $bright = New-Object 'byte[]' $N
  for ($y = 0; $y -lt $AH; $y++) {
    $row = $y * $stride; $base = $y * $AW
    for ($x = 0; $x -lt $AW; $x++) {
      $o = $row + $x * 3
      $b = $buf[$o]; $gr = $buf[$o + 1]; $r = $buf[$o + 2]
      $mx = $r; if ($gr -gt $mx) { $mx = $gr }; if ($b -gt $mx) { $mx = $b }
      $mn = $r; if ($gr -lt $mn) { $mn = $gr }; if ($b -lt $mn) { $mn = $b }
      $sat = $mx - $mn
      $i = $base + $x
      $bright[$i] = $mx
      if ($mx -ge 248 -and $sat -le 8)                 { $mW[$i]  = $true }
      if ($mx -ge 231 -and $mx -le 243 -and $sat -le 8){ $mG[$i]  = $true }
      if ($mx -ge 200 -and $mx -le 228 -and $sat -le 10){ $mG2[$i] = $true }
    }
  }

  $bandList = if ($Band -eq 'all') { @($BANDS.Keys) } else { @($Band) }

  Write-Host ""
  Write-Host "==== $id ===="

  foreach ($bname in $bandList) {
    $mask = switch ($bname) { 'white' { $mW } 'gray' { $mG } 'gray2' { $mG2 } }

    $visited = New-Object 'bool[]' $N
    $st = New-Object System.Collections.Generic.Stack[int]
    $cells = @()

    for ($i = 0; $i -lt $N; $i++) {
      if (-not $mask[$i] -or $visited[$i]) { continue }
      $st.Clear(); $st.Push($i); $visited[$i] = $true
      $minX = $AW; $minY = $AH; $maxX = 0; $maxY = 0; $cnt = 0
      $sumB = 0.0
      $bxs = New-Object System.Collections.Generic.List[int]
      $bys = New-Object System.Collections.Generic.List[int]
      while ($st.Count -gt 0) {
        $q = $st.Pop(); $cnt++
        $qx = $q % $AW; $qy = [int]($q / $AW)
        $sumB += $bright[$q]
        if ($qx -lt $minX) { $minX = $qx }; if ($qx -gt $maxX) { $maxX = $qx }
        if ($qy -lt $minY) { $minY = $qy }; if ($qy -gt $maxY) { $maxY = $qy }
        # boundary test (for min-area-rect fit)
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
      $bboxArea = $bw * $bh
      $areaFrac = $bboxArea / [double]$N
      $fill     = $cnt / [double]$bboxArea
      $aspect   = $bw / [double]$bh
      # keep: big enough to be a photo slot, not the whole canvas, reasonably solid
      if ($areaFrac -lt 0.010 -or $areaFrac -gt 0.95) { continue }
      if ($fill -lt 0.45) { continue }
      if ($aspect -lt 0.10 -or $aspect -gt 10) { continue }

      $mr = Fit-MinRect $bxs $bys
      # min-area-rect in logic space
      $sx = $LOGIC_W / [double]$AW; $sy = $LOGIC_H / [double]$AH
      $lw = $mr.w * $sx; $lh = $mr.h * $sy
      $lcx = $mr.cx * $sx; $lcy = $mr.cy * $sy
      $deg = [math]::Round($mr.deg, 1)
      # rotated-rect fill: component area vs min-rect area -> tells rect / ellipse / blob
      $mrFill = $cnt / [double]([Math]::Max(1.0, $mr.w * $mr.h))
      $shape = if ($mrFill -gt 0.90) { 'rect' } elseif ($mrFill -gt 0.70) { 'round?' } else { 'blob' }

      $rl = [int][math]::Round($lcx - $lw / 2); $rt = [int][math]::Round($lcy - $lh / 2)
      $rr = [int][math]::Round($lcx + $lw / 2); $rb = [int][math]::Round($lcy + $lh / 2)
      # An unrotated cell must live inside the canvas. A rotated one may legitimately poke
      # outside its axis-aligned box, so only clamp when the angle is ~0.
      $oob = ($rl -lt -4 -or $rt -lt -4 -or $rr -gt $LOGIC_W + 4 -or $rb -gt $LOGIC_H + 4)
      if ([math]::Abs($deg) -lt 0.5) {
        $rl = [Math]::Max(0, $rl); $rt = [Math]::Max(0, $rt)
        $rr = [Math]::Min($LOGIC_W, $rr); $rb = [Math]::Min($LOGIC_H, $rb)
      }
      $warn = @()
      if ($mrFill -lt 0.90) { $warn += 'shape' }   # not a clean rect: circle / blob / merged slots
      if ($oob)             { $warn += 'oob' }     # min-rect spills off canvas -> bad fit
      if ($areaFrac -gt 0.80) { $warn += 'huge' }  # probably background, not a slot

      $cells += [pscustomobject]@{
        l = $rl; t = $rt; r = $rr; b = $rb
        deg = $deg; fill = [math]::Round($mrFill, 2); shape = $shape
        meanB = [int][math]::Round($sumB / $cnt)
        bboxFill = [math]::Round($fill, 2)
        warn = ($warn -join '+')
      }
    }

    $cells = @($cells | Sort-Object t, l)
    if ($cells.Count -eq 0) { Write-Host ("  [{0,-5}] -" -f $bname); continue }

    # cells_config.ps1-format line (angle omitted when ~0)
    $parts = @()
    foreach ($c in $cells) {
      if ([math]::Abs($c.deg) -ge 0.5) { $parts += ("@({0},{1},{2},{3},{4})" -f $c.l, $c.t, $c.r, $c.b, $c.deg) }
      else                             { $parts += ("@({0},{1},{2},{3})"     -f $c.l, $c.t, $c.r, $c.b) }
    }
    $line = if ($cells.Count -eq 1) { '  "{0}"=@(,{1})' -f $id, $parts[0] } else { '  "{0}"=@({1})' -f $id, ($parts -join ',') }
    Write-Host ("  [{0,-5}] {1} cells" -f $bname, $cells.Count)
    Write-Host ("            $line")
    foreach ($c in $cells) {
      $flag = if ($c.warn) { "  <<< $($c.warn)" } else { '' }
      Write-Host ("            .. {0,-6} deg={1,6} mrFill={2,5} meanB={3,4}{4}" -f $c.shape, $c.deg, $c.fill, $c.meanB, $flag)
    }

    # ---- overlay for eyeball check ----
    $OW = 420; $OH = 747
    $ov = New-Object System.Drawing.Bitmap $OW, $OH
    $go = [System.Drawing.Graphics]::FromImage($ov)
    $go.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $go.Clear([System.Drawing.Color]::Magenta)
    $go.DrawImage($img, 0, 0, $OW, $OH)
    $pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::Red), 2
    $fnt = New-Object System.Drawing.Font 'Consolas', 10, ([System.Drawing.FontStyle]::Bold)
    $spx = $OW / [double]$LOGIC_W; $spy = $OH / [double]$LOGIC_H
    $k = 0
    foreach ($c in $cells) {
      $k++
      $cw = ($c.r - $c.l) * $spx; $ch = ($c.b - $c.t) * $spy
      $ccx = ($c.l + $c.r) / 2.0 * $spx; $ccy = ($c.t + $c.b) / 2.0 * $spy
      $sv = $go.Save()
      $go.TranslateTransform([float]$ccx, [float]$ccy)
      $go.RotateTransform([float]$c.deg)
      $go.DrawRectangle($pen, [float](-$cw / 2), [float](-$ch / 2), [float]$cw, [float]$ch)
      $go.Restore($sv)
      $go.DrawString([string]$k, $fnt, [System.Drawing.Brushes]::Red, [float]($ccx - 6), [float]($ccy - 8))
    }
    $pen.Dispose(); $fnt.Dispose(); $go.Dispose()
    Save-Jpeg $ov (Join-Path $OUT ("{0}_{1}.jpg" -f $id, $bname)) 88
    $ov.Dispose()
  }

  $img.Dispose()
}

Write-Host ""
Write-Host "Overlays -> $OUT"
