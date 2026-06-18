# Fill each cell 50% RED on the res JPEG (logic space) WITH rotation -> replicates device fill
# EXACTLY (same coord space). Iterate coords here without rebuilding app. ASCII.
Add-Type -AssemblyName System.Drawing
. "D:\EZTech\EZTechApp\collage_pic_editor\tools\cells_config.ps1"
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\vfill"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$LOGIC_W=1125; $LOGIC_H=2000; $OW=750; $OH=1333
$SX=$OW/$LOGIC_W; $SY=$OH/$LOGIC_H
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
$red=New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(128,255,0,0))
$only=@($args)
foreach($id in @($CELLS.Keys)){
  if($only.Count -gt 0 -and ($only -notcontains $id)){ continue }
  $img=[System.Drawing.Image]::FromFile((Join-Path $resDir "temp_$id.jpg"))
  $bmp=New-Object System.Drawing.Bitmap $OW,$OH
  $gr=[System.Drawing.Graphics]::FromImage($bmp)
  $gr.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $gr.SmoothingMode=[System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $gr.DrawImage($img,0,0,$OW,$OH)
  $rr=$CELLS[$id]
  if($rr.Count -ge 1 -and ($rr[0] -is [int] -or $rr[0] -is [double])){ $rr=@(,$rr) }
  foreach($c in $rr){
    $rw=($c[2]-$c[0])*$SX; $rh=($c[3]-$c[1])*$SY
    $cx=($c[0]+$c[2])/2.0*$SX; $cy=($c[1]+$c[3])/2.0*$SY
    $ang=0.0; if($c.Count -ge 5){ $ang=[double]$c[4] }
    $st=$gr.Save()
    $gr.TranslateTransform([float]$cx,[float]$cy)
    if($ang -ne 0){ $gr.RotateTransform([float]$ang) }
    $gr.FillRectangle($red,[float](-$rw/2),[float](-$rh/2),[float]$rw,[float]$rh)
    $gr.Restore($st)
  }
  $gr.Dispose()
  $eps=New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]92)
  $bmp.Save((Join-Path $outDir "$id.jpg"),$jpgEnc,$eps); $eps.Dispose(); $bmp.Dispose(); $img.Dispose()
  Write-Host "vfill -> $id"
}
