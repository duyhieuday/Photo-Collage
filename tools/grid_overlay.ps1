# Render templates with a labelled coordinate grid (logic space 1125x2000)
# so frame edges can be read precisely by eye. ASCII only.
Add-Type -AssemblyName System.Drawing

$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\grid"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$LOGIC_W=1125; $LOGIC_H=2000
$W=787; $H=1400            # 0.7x logic, readable
$sx=$W/$LOGIC_W; $sy=$H/$LOGIC_H

$ids=@($args)
if($ids.Count -eq 0){ $ids=@("cp03","cp06","cp07","cp08","cp02") }

$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
$fontBig=New-Object System.Drawing.Font("Arial",11,[System.Drawing.FontStyle]::Bold)
$penMinor=New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(120,255,0,0)),1
$penMajor=New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(200,0,128,255)),1
$brush=New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,0,90,200))
$brushBg=New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(180,255,255,255))

foreach($id in $ids){
  $path=Join-Path $resDir "temp_$id.jpg"
  if(-not (Test-Path $path)){ Write-Host "missing $id"; continue }
  $img=[System.Drawing.Image]::FromFile($path)
  $bmp=New-Object System.Drawing.Bitmap $W,$H
  $g=[System.Drawing.Graphics]::FromImage($bmp)
  $g.DrawImage($img,0,0,$W,$H)
  # vertical lines every 100 logic
  for($x=0;$x -le $LOGIC_W;$x+=100){
    $px=$x*$sx
    if($x % 500 -eq 0){ $g.DrawLine($penMajor,[float]$px,0,[float]$px,[float]$H) }
    else { $g.DrawLine($penMinor,[float]$px,0,[float]$px,[float]$H) }
  }
  for($y=0;$y -le $LOGIC_H;$y+=100){
    $py=$y*$sy
    if($y % 500 -eq 0){ $g.DrawLine($penMajor,0,[float]$py,[float]$W,[float]$py) }
    else { $g.DrawLine($penMinor,0,[float]$py,[float]$W,[float]$py) }
  }
  # labels: x along top, y along left, every 100
  for($x=0;$x -le $LOGIC_W;$x+=100){ $px=$x*$sx; $g.FillRectangle($brushBg,[float]$px,0,28,14); $g.DrawString([string]$x,$fontBig,$brush,[float]$px,0) }
  for($y=0;$y -le $LOGIC_H;$y+=100){ $py=$y*$sy; $g.FillRectangle($brushBg,0,[float]$py,34,14); $g.DrawString([string]$y,$fontBig,$brush,0,[float]$py) }
  $g.Dispose()
  $eps=New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]92)
  $bmp.Save((Join-Path $outDir "$id.jpg"),$jpgEnc,$eps)
  $eps.Dispose();$bmp.Dispose();$img.Dispose()
  Write-Host "grid -> $id.jpg"
}
