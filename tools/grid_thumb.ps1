# Coordinate grid (logic 1125x2000) over thumb_<id> to read SAMPLE-PHOTO corners. ASCII only.
Add-Type -AssemblyName System.Drawing
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\gridthumb"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$LOGIC_W=1125; $LOGIC_H=2000
$W=900; $H=1600
$sx=$W/$LOGIC_W; $sy=$H/$LOGIC_H
$ids=@($args); if($ids.Count -eq 0){ $ids=@("cp01") }
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
$fontBig=New-Object System.Drawing.Font("Arial",10,[System.Drawing.FontStyle]::Bold)
$penMinor=New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(110,255,0,0)),1
$penMajor=New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(200,0,128,255)),1
$brush=New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,0,90,200))
$brushBg=New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(170,255,255,255))
foreach($id in $ids){
  $path=Join-Path $resDir "thumb_$id.jpg"
  if(-not (Test-Path $path)){ Write-Host "missing $id"; continue }
  $img=[System.Drawing.Image]::FromFile($path)
  $bmp=New-Object System.Drawing.Bitmap $W,$H
  $g=[System.Drawing.Graphics]::FromImage($bmp)
  $g.DrawImage($img,0,0,$W,$H)
  for($x=0;$x -le $LOGIC_W;$x+=50){ $px=$x*$sx; if($x % 500 -eq 0){$g.DrawLine($penMajor,[float]$px,0,[float]$px,[float]$H)}else{$g.DrawLine($penMinor,[float]$px,0,[float]$px,[float]$H)} }
  for($y=0;$y -le $LOGIC_H;$y+=50){ $py=$y*$sy; if($y % 500 -eq 0){$g.DrawLine($penMajor,0,[float]$py,[float]$W,[float]$py)}else{$g.DrawLine($penMinor,0,[float]$py,[float]$W,[float]$py)} }
  for($x=0;$x -le $LOGIC_W;$x+=100){ $px=$x*$sx; $g.FillRectangle($brushBg,[float]$px,0,26,13); $g.DrawString([string]$x,$fontBig,$brush,[float]$px,0) }
  for($y=0;$y -le $LOGIC_H;$y+=100){ $py=$y*$sy; $g.FillRectangle($brushBg,0,[float]$py,32,13); $g.DrawString([string]$y,$fontBig,$brush,0,[float]$py) }
  $g.Dispose()
  $eps=New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]92)
  $bmp.Save((Join-Path $outDir "$id.jpg"),$jpgEnc,$eps)
  $eps.Dispose();$bmp.Dispose();$img.Dispose()
  Write-Host "gridthumb -> $id"
}
