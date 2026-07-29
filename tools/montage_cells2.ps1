# Tile the tools\_out\cells2 overlays into contact sheets so 55 templates can be eyeballed
# in a handful of images instead of one at a time. ASCII only.
#
# Usage: powershell -File tools\montage_cells2.ps1 [-Ids a,b,c] [-Cols 7] [-Name sheet]

param(
  [string[]]$Ids = @(),
  [int]$Cols = 7,
  [int]$PerSheet = 21,
  [string]$Name = 'cells2'
)

Add-Type -AssemblyName System.Drawing

$IN  = "D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\cells2"
$OUT = "D:\EZTech\EZTechApp\collage_pic_editor\tools\_out"

if ($Ids.Count -eq 0) {
  $Ids = @(Get-ChildItem $IN -Filter '*.jpg' | Sort-Object Name | ForEach-Object { $_.BaseName })
} else {
  $Ids = @($Ids | ForEach-Object { $_ -split ',' } | Where-Object { $_ -match '\S' } | ForEach-Object { $_.Trim().ToLower() })
}
if ($Ids.Count -eq 0) { Write-Host "nothing to montage"; exit 1 }

$jpgEnc = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq 'image/jpeg' }
$CW = 150; $CH = 267; $PAD = 4

$sheet = 0
for ($start = 0; $start -lt $Ids.Count; $start += $PerSheet) {
  $sheet++
  $chunk = @($Ids[$start..([Math]::Min($start + $PerSheet - 1, $Ids.Count - 1))])
  $rows = [math]::Ceiling($chunk.Count / [double]$Cols)
  $W = $Cols * ($CW + $PAD) + $PAD
  $H = [int]($rows * ($CH + $PAD) + $PAD)
  $bmp = New-Object System.Drawing.Bitmap $W, $H
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.Clear([System.Drawing.Color]::FromArgb(255, 20, 20, 24))
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  for ($i = 0; $i -lt $chunk.Count; $i++) {
    $p = Join-Path $IN ("{0}.jpg" -f $chunk[$i])
    $x = $PAD + ($i % $Cols) * ($CW + $PAD)
    $y = $PAD + [math]::Floor($i / $Cols) * ($CH + $PAD)
    if (Test-Path $p) {
      $im = [System.Drawing.Image]::FromFile($p)
      $g.DrawImage($im, $x, $y, $CW, $CH); $im.Dispose()
    } else {
      $g.FillRectangle([System.Drawing.Brushes]::DarkRed, $x, $y, $CW, $CH)
    }
  }
  $g.Dispose()
  $dst = Join-Path $OUT ("{0}_sheet{1}.jpg" -f $Name, $sheet)
  $eps = New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality, [long]92)
  $bmp.Save($dst, $jpgEnc, $eps); $eps.Dispose(); $bmp.Dispose()
  Write-Host ("sheet {0}: {1} templates -> {2}" -f $sheet, $chunk.Count, $dst)
}
