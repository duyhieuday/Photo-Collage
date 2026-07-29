# Import the NEW templates only (ids 11..20 etc) into res/drawable as JPEG.
# Deliberately does NOT touch the 62 already-imported *.webp files.
# Mirrors the sizes/quality of tools/import_templates.ps1 so the new batch matches the old one.
# After running, convert the new *.jpg to WebP in Android Studio (right-click drawable ->
# "Convert to WebP...") to get back to the ~86 KB/file the existing templates sit at.
# Windows PowerShell 5.1 + System.Drawing only. ASCII only.
#
# Usage: powershell -File tools\import_new.ps1 -Ids bd11,bd12  [-WhatIf]

param(
  [string[]]$Ids = @(),
  [switch]$WhatIf
)

Add-Type -AssemblyName System.Drawing

$SRC    = "D:\EZTech\AppAssets\PhotoCollage\Category_Template"
$RESDIR = "D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"

$TEMP_W = 1125; $TEMP_H = 2000; $TEMP_Q = 88   # editor image
$THUMB_W = 540; $THUMB_H = 960;  $THUMB_Q = 86  # picker thumbnail

$CATS = @{
  'bd' = @('Birthday',    'BD')
  'cp' = @('Couple',      'CP')
  'gs' = @('Glad season', 'GS')
  'is' = @('IG Story',    'IS')
  'sm' = @('Summer vibe', 'SM')
  'sp' = @('Sports',      'SP')
}

$jpgEnc = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq 'image/jpeg' }
function Save-Jpeg($bmp, $path, $q) {
  $eps = New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality, [long]$q)
  $bmp.Save($path, $jpgEnc, $eps); $eps.Dispose()
}
function Resize-Save($srcPath, $dstPath, $w, $h, $q) {
  $img = [System.Drawing.Image]::FromFile($srcPath)
  $bmp = New-Object System.Drawing.Bitmap $w, $h
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.PixelOffsetMode   = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $g.DrawImage($img, 0, 0, $w, $h)
  $g.Dispose()
  Save-Jpeg $bmp $dstPath $q
  $bmp.Dispose(); $img.Dispose()
  return (Get-Item $dstPath).Length
}

if ($Ids.Count -eq 0) { Write-Host "No -Ids given."; exit 1 }
$Ids = @($Ids | ForEach-Object { $_ -split ',' } | Where-Object { $_ -match '\S' } | ForEach-Object { $_.Trim().ToLower() })

$totT = 0L; $totH = 0L; $n = 0; $fallback = @()
foreach ($id in $Ids) {
  $pre = $id.Substring(0, 2); $nn = $id.Substring(2)
  if (-not $CATS.ContainsKey($pre)) { Write-Host "  $id : unknown prefix"; continue }
  $folder = $CATS[$pre][0]; $P = $CATS[$pre][1]
  $tempSrc  = Join-Path $SRC ("{0}\Temp_{1}{2}.png"  -f $folder, $P, $nn)
  $thumbSrc = Join-Path $SRC ("{0}\Thumb_{1}{2}.png" -f $folder, $P, $nn)

  if (-not (Test-Path $tempSrc)) { Write-Host "  $id : MISSING Temp_ -> SKIP"; continue }
  # No Thumb_ shipped (cp10, sm11): fall back to the frame image, same as is10 already does.
  if (-not (Test-Path $thumbSrc)) { $thumbSrc = $tempSrc; $fallback += $id }

  # Guard: never clobber an already-imported template.
  $existing = Get-ChildItem $RESDIR -Filter "temp_$id.*" -ErrorAction SilentlyContinue
  if ($existing -and -not $WhatIf) {
    Write-Host ("  {0} : ALREADY in res/drawable as {1} -> SKIP" -f $id, $existing[0].Name)
    continue
  }

  if ($WhatIf) { Write-Host ("  {0} : would write temp_{0}.jpg + thumb_{0}.jpg" -f $id); continue }

  $a = Resize-Save $tempSrc  (Join-Path $RESDIR "temp_$id.jpg")  $TEMP_W  $TEMP_H  $TEMP_Q
  $b = Resize-Save $thumbSrc (Join-Path $RESDIR "thumb_$id.jpg") $THUMB_W $THUMB_H $THUMB_Q
  $totT += $a; $totH += $b; $n++
  Write-Host ("  {0,-6} temp={1,6:N0} KB  thumb={2,5:N0} KB" -f $id, ($a / 1KB), ($b / 1KB))
}

Write-Host ""
Write-Host ("Imported {0} templates. temp total={1:N1} MB, thumb total={2:N1} MB, sum={3:N1} MB" -f `
            $n, ($totT / 1MB), ($totH / 1MB), (($totT + $totH) / 1MB))
if ($fallback.Count) { Write-Host ("Thumb fallback (used Temp_ as thumb): {0}" -f ($fallback -join ', ')) }
