# Render a template with a labelled coordinate grid in LOGIC space (1125x2000) so cell rects
# can be read off by eye. For the handful of templates the auto-detectors can't crack --
# e.g. a slot the same colour as its background, with no Thumb_ to diff against.
#
# Usage: powershell -File tools\grid_measure.ps1 -Ids cp10,sm11 [-Step 125]

param(
  [string[]]$Ids = @(),
  [int]$Step = 125          # grid pitch in logic units
)

Add-Type -AssemblyName System.Drawing

$SRC = "D:\EZTech\AppAssets\PhotoCollage\Category_Template"
$OUT = "D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\grid"
New-Item -ItemType Directory -Force -Path $OUT | Out-Null

$LOGIC_W = 1125; $LOGIC_H = 2000
$CATS = @{
  'bd' = @('Birthday', 'BD'); 'cp' = @('Couple', 'CP'); 'gs' = @('Glad season', 'GS')
  'is' = @('IG Story', 'IS'); 'sm' = @('Summer vibe', 'SM'); 'sp' = @('Sports', 'SP')
}
$jpgEnc = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq 'image/jpeg' }

$Ids = @($Ids | ForEach-Object { $_ -split ',' } | Where-Object { $_ -match '\S' } | ForEach-Object { $_.Trim().ToLower() })
if ($Ids.Count -eq 0) { Write-Host "No -Ids."; exit 1 }

foreach ($id in $Ids) {
  $pre = $id.Substring(0, 2); $nn = $id.Substring(2)
  $p = Join-Path $SRC ("{0}\Temp_{1}{2}.png" -f $CATS[$pre][0], $CATS[$pre][1], $nn)
  if (-not (Test-Path $p)) { Write-Host "$id : missing"; continue }

  # 2x the logic width keeps small text legible while staying a manageable file.
  $W = 563; $H = 1000
  $bmp = New-Object System.Drawing.Bitmap $W, $H
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $img = [System.Drawing.Image]::FromFile($p)
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.DrawImage($img, 0, 0, $W, $H); $img.Dispose()

  $penMinor = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(120, 255, 0, 0)), 1
  $penMajor = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(220, 255, 0, 255)), 1
  $fnt = New-Object System.Drawing.Font 'Consolas', 8, ([System.Drawing.FontStyle]::Bold)
  $back = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(190, 0, 0, 0))

  for ($lx = 0; $lx -le $LOGIC_W; $lx += $Step) {
    $x = $lx * $W / [double]$LOGIC_W
    $pen = if ($lx % ($Step * 2) -eq 0) { $penMajor } else { $penMinor }
    $g.DrawLine($pen, [float]$x, 0, [float]$x, [float]$H)
    if ($lx % ($Step * 2) -eq 0) {
      $g.FillRectangle($back, [float]($x + 1), 0, 26, 11)
      $g.DrawString([string]$lx, $fnt, [System.Drawing.Brushes]::Yellow, [float]($x + 1), 0)
    }
  }
  for ($ly = 0; $ly -le $LOGIC_H; $ly += $Step) {
    $y = $ly * $H / [double]$LOGIC_H
    $pen = if ($ly % ($Step * 2) -eq 0) { $penMajor } else { $penMinor }
    $g.DrawLine($pen, 0, [float]$y, [float]$W, [float]$y)
    if ($ly % ($Step * 2) -eq 0) {
      $g.FillRectangle($back, 0, [float]($y + 1), 32, 11)
      $g.DrawString([string]$ly, $fnt, [System.Drawing.Brushes]::Cyan, 0, [float]($y + 1))
    }
  }
  $penMinor.Dispose(); $penMajor.Dispose(); $fnt.Dispose(); $back.Dispose(); $g.Dispose()

  $eps = New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality, [long]92)
  $bmp.Save((Join-Path $OUT "$id.jpg"), $jpgEnc, $eps); $eps.Dispose(); $bmp.Dispose()
  Write-Host "  $id -> grid\$id.jpg  (step=$Step, yellow=x, cyan=y)"
}
