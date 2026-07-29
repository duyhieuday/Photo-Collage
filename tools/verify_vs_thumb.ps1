# Final check on cellRects: draw the configured rects straight onto the designer's Thumb_.
#
# Thumb_ is the same design with sample photos already dropped in, so a correct cell rect lands
# squarely on a photo. This catches what an overlay on the empty frame cannot: a rect sitting on
# a caption box, a slot that was missed entirely, or a rect that drifted off its slot.
#
# Reads $CELLS from tools\cells_config.ps1, so it verifies exactly what gen_cells.ps1 will emit.
# Windows PowerShell 5.1 + System.Drawing only. ASCII only.
#
# Usage: powershell -File tools\verify_vs_thumb.ps1 -Ids bd12,cp18 [-PerSheet 12]

param(
  [string[]]$Ids = @(),
  [int]$PerSheet = 12,
  [string]$Name = 'vsthumb'
)

Add-Type -AssemblyName System.Drawing
. "D:\EZTech\EZTechApp\collage_pic_editor\tools\cells_config.ps1"

$SRC = "D:\EZTech\AppAssets\PhotoCollage\Category_Template"
$OUT = "D:\EZTech\EZTechApp\collage_pic_editor\tools\_out"
$LOGIC_W = 1125; $LOGIC_H = 2000
$CATS = @{
  'bd' = @('Birthday', 'BD'); 'cp' = @('Couple', 'CP'); 'gs' = @('Glad season', 'GS')
  'is' = @('IG Story', 'IS'); 'sm' = @('Summer vibe', 'SM'); 'sp' = @('Sports', 'SP')
}
$jpgEnc = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq 'image/jpeg' }

$Ids = @($Ids | ForEach-Object { $_ -split ',' } | Where-Object { $_ -match '\S' } | ForEach-Object { $_.Trim().ToLower() })
if ($Ids.Count -eq 0) { Write-Host "No -Ids."; exit 1 }

$CW = 145; $CH = 258; $PAD = 5; $LBL = 13; $COLS = 6

$sheet = 0
for ($start = 0; $start -lt $Ids.Count; $start += $PerSheet) {
  $sheet++
  $chunk = @($Ids[$start..([Math]::Min($start + $PerSheet - 1, $Ids.Count - 1))])
  $cols = [Math]::Min($COLS, $chunk.Count)
  $rows = [math]::Ceiling($chunk.Count / [double]$cols)
  $W = [int]($cols * ($CW + $PAD) + $PAD)
  $H = [int]($rows * ($CH + $LBL + $PAD) + $PAD)
  $bmp = New-Object System.Drawing.Bitmap $W, $H
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.Clear([System.Drawing.Color]::FromArgb(255, 18, 18, 22))
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $fnt = New-Object System.Drawing.Font 'Consolas', 8, ([System.Drawing.FontStyle]::Bold)
  $num = New-Object System.Drawing.Font 'Consolas', 9, ([System.Drawing.FontStyle]::Bold)
  $pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 0, 255, 60)), 2

  for ($i = 0; $i -lt $chunk.Count; $i++) {
    $id = $chunk[$i]
    $x0 = $PAD + ($i % $cols) * ($CW + $PAD)
    $y0 = $PAD + [math]::Floor($i / $cols) * ($CH + $LBL + $PAD)

    $pre = $id.Substring(0, 2); $nn = $id.Substring(2)
    $tp = Join-Path $SRC ("{0}\Thumb_{1}{2}.png" -f $CATS[$pre][0], $CATS[$pre][1], $nn)
    if (-not (Test-Path $tp)) { $tp = Join-Path $SRC ("{0}\Temp_{1}{2}.png" -f $CATS[$pre][0], $CATS[$pre][1], $nn) }
    if (Test-Path $tp) { $im = [System.Drawing.Image]::FromFile($tp); $g.DrawImage($im, $x0, $y0, $CW, $CH); $im.Dispose() }
    else { $g.FillRectangle([System.Drawing.Brushes]::DarkRed, $x0, $y0, $CW, $CH) }

    $rr = $CELLS[$id]
    if ($null -eq $rr) {
      $g.DrawString("no cells", $fnt, [System.Drawing.Brushes]::Orange, [float]($x0 + 4), [float]($y0 + $CH / 2))
      $g.DrawString($id, $fnt, [System.Drawing.Brushes]::Orange, [float]$x0, [float]($y0 + $CH + 1))
      continue
    }
    # a lone cell written as @(x0,y0,x1,y1) arrives flattened -> rewrap, same as gen_cells.ps1
    if ($rr.Count -ge 1 -and ($rr[0] -is [int] -or $rr[0] -is [double])) { $rr = @(, $rr) }

    $spx = $CW / [double]$LOGIC_W; $spy = $CH / [double]$LOGIC_H
    $k = 0
    foreach ($r in $rr) {
      $k++
      $deg = if ($r.Count -ge 5) { [double]$r[4] } else { 0.0 }
      # NB: not $cw/$ch -- PowerShell is case-insensitive, so those would clobber the tile
      # size $CW/$CH and every tile after the first would be drawn a few pixels wide.
      $rw = ($r[2] - $r[0]) * $spx; $rh = ($r[3] - $r[1]) * $spy
      $ccx = $x0 + ($r[0] + $r[2]) / 2.0 * $spx; $ccy = $y0 + ($r[1] + $r[3]) / 2.0 * $spy
      # Rotate the four corners by hand and DrawPolygon. Using Graphics transforms here leaked
      # state between tiles no matter how it was unwound, skewing every later thumbnail.
      $rad = $deg * [Math]::PI / 180.0
      $cos = [Math]::Cos($rad); $sin = [Math]::Sin($rad)
      $hw = $rw / 2.0; $hh = $rh / 2.0
      $pts = @()
      foreach ($c in @(@(-$hw, -$hh), @($hw, -$hh), @($hw, $hh), @(-$hw, $hh))) {
        $pts += New-Object System.Drawing.PointF `
                  ([float]($ccx + $c[0] * $cos - $c[1] * $sin)), `
                  ([float]($ccy + $c[0] * $sin + $c[1] * $cos))
      }
      $g.DrawPolygon($pen, [System.Drawing.PointF[]]$pts)
      $g.DrawString([string]$k, $num, [System.Drawing.Brushes]::Yellow, [float]($ccx - 5), [float]($ccy - 7))
    }
    $g.DrawString(("{0} n={1}" -f $id, @($rr).Count), $fnt, [System.Drawing.Brushes]::White, [float]$x0, [float]($y0 + $CH + 1))
  }
  $fnt.Dispose(); $num.Dispose(); $pen.Dispose(); $g.Dispose()
  $dst = Join-Path $OUT ("{0}_sheet{1}.jpg" -f $Name, $sheet)
  $eps = New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality, [long]93)
  $bmp.Save($dst, $jpgEnc, $eps); $eps.Dispose(); $bmp.Dispose()
  Write-Host ("sheet {0}: {1} templates -> {2}" -f $sheet, $chunk.Count, $dst)
}
