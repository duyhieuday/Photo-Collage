# Side-by-side verification sheet: for each template, the cells2 overlay (frame + detected
# rects) next to the designer's Thumb_ (the same design with sample photos dropped in).
#
# The Thumb_ is ground truth for HOW MANY slots there are and where they sit, so a wrong cell
# count or a false positive on white text is obvious at a glance -- which a lone overlay can't
# tell you.
#
# Usage: powershell -File tools\verify_sheet.ps1 [-Ids a,b] [-PerSheet 8] [-Name verify]

param(
  [string[]]$Ids = @(),
  [int]$PerSheet = 8,
  [string]$Name = 'verify'
)

Add-Type -AssemblyName System.Drawing

$SRC = "D:\EZTech\AppAssets\PhotoCollage\Category_Template"
$OVR = "D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\cells2"
$OUT = "D:\EZTech\EZTechApp\collage_pic_editor\tools\_out"

$CATS = @{
  'bd' = @('Birthday', 'BD'); 'cp' = @('Couple', 'CP'); 'gs' = @('Glad season', 'GS')
  'is' = @('IG Story', 'IS'); 'sm' = @('Summer vibe', 'SM'); 'sp' = @('Sports', 'SP')
}
$jpgEnc = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq 'image/jpeg' }

if ($Ids.Count -eq 0) {
  $Ids = @(Get-ChildItem $OVR -Filter '*.jpg' | Sort-Object Name | ForEach-Object { $_.BaseName })
} else {
  $Ids = @($Ids | ForEach-Object { $_ -split ',' } | Where-Object { $_ -match '\S' } | ForEach-Object { $_.Trim().ToLower() })
}
if ($Ids.Count -eq 0) { Write-Host "nothing to do"; exit 1 }

$CW = 108; $CH = 192; $GAP = 3; $PAIR = $CW * 2 + $GAP; $PAD = 6; $LBL = 13

$sheet = 0
for ($start = 0; $start -lt $Ids.Count; $start += $PerSheet) {
  $sheet++
  $chunk = @($Ids[$start..([Math]::Min($start + $PerSheet - 1, $Ids.Count - 1))])
  $cols = [Math]::Min(4, $chunk.Count)
  $rows = [math]::Ceiling($chunk.Count / [double]$cols)
  $W = [int]($cols * ($PAIR + $PAD) + $PAD)
  $H = [int]($rows * ($CH + $LBL + $PAD) + $PAD)
  $bmp = New-Object System.Drawing.Bitmap $W, $H
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.Clear([System.Drawing.Color]::FromArgb(255, 18, 18, 22))
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $fnt = New-Object System.Drawing.Font 'Consolas', 8, ([System.Drawing.FontStyle]::Bold)

  for ($i = 0; $i -lt $chunk.Count; $i++) {
    $id = $chunk[$i]
    $x = $PAD + ($i % $cols) * ($PAIR + $PAD)
    $y = $PAD + [math]::Floor($i / $cols) * ($CH + $LBL + $PAD)

    # left: detection overlay
    $op = Join-Path $OVR "$id.jpg"
    if (Test-Path $op) { $im = [System.Drawing.Image]::FromFile($op); $g.DrawImage($im, $x, $y, $CW, $CH); $im.Dispose() }
    else { $g.FillRectangle([System.Drawing.Brushes]::DarkRed, $x, $y, $CW, $CH) }

    # right: designer's filled preview = ground truth for slot count
    $pre = $id.Substring(0, 2); $nn = $id.Substring(2)
    $tp = Join-Path $SRC ("{0}\Thumb_{1}{2}.png" -f $CATS[$pre][0], $CATS[$pre][1], $nn)
    $x2 = $x + $CW + $GAP
    if (Test-Path $tp) { $im = [System.Drawing.Image]::FromFile($tp); $g.DrawImage($im, $x2, $y, $CW, $CH); $im.Dispose() }
    else {
      $g.FillRectangle([System.Drawing.Brushes]::FromArgb(255, 60, 60, 60), $x2, $y, $CW, $CH)
      $g.DrawString("no Thumb_", $fnt, [System.Drawing.Brushes]::Orange, [float]($x2 + 4), [float]($y + $CH / 2))
    }
    $g.DrawString($id, $fnt, [System.Drawing.Brushes]::White, [float]$x, [float]($y + $CH + 1))
  }
  $fnt.Dispose(); $g.Dispose()
  $dst = Join-Path $OUT ("{0}_sheet{1}.jpg" -f $Name, $sheet)
  $eps = New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality, [long]93)
  $bmp.Save($dst, $jpgEnc, $eps); $eps.Dispose(); $bmp.Dispose()
  Write-Host ("sheet {0}: {1} -> {2}" -f $sheet, ($chunk -join ','), $dst)
}
