# Realistic preview: place sample photos into cells WITH rotation (GDI == app canvas.rotate).
# Center-crop fill per cell, clipped to the (possibly rotated) rect. ASCII only.
Add-Type -AssemblyName System.Drawing
. "D:\EZTech\EZTechApp\collage_pic_editor\tools\cells_config.ps1"
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\filled"
$smpDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$LOGIC_W=1125; $LOGIC_H=2000; $OW=562; $OH=1000
$SX=$OW/$LOGIC_W; $SY=$OH/$LOGIC_H
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
$samples=@()
foreach($n in 0..4){ $sp=Join-Path $smpDir "place$n.png"; if(Test-Path $sp){ $samples+=[System.Drawing.Image]::FromFile($sp) } }
if($samples.Count -eq 0){ $samples=@([System.Drawing.Image]::FromFile((Join-Path $smpDir 'testimg.jpg'))) }

$ids=@($args)
foreach($id in $ids){
  $p=Join-Path $resDir "temp_$id.jpg"
  if(-not (Test-Path $p)){ continue }
  $img=[System.Drawing.Image]::FromFile($p)
  $bmp=New-Object System.Drawing.Bitmap $OW,$OH
  $gr=[System.Drawing.Graphics]::FromImage($bmp)
  $gr.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $gr.SmoothingMode=[System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $gr.DrawImage($img,0,0,$OW,$OH)
  $rr=$CELLS[$id]
  if($rr.Count -ge 1 -and ($rr[0] -is [int] -or $rr[0] -is [double])){ $rr=@(,$rr) }
  $k=0
  foreach($c in $rr){
    $rw=($c[2]-$c[0])*$SX; $rh=($c[3]-$c[1])*$SY
    $cx=($c[0]+$c[2])/2.0*$SX; $cy=($c[1]+$c[3])/2.0*$SY
    $ang=0.0; if($c.Count -ge 5){ $ang=[double]$c[4] }
    $smp=$samples[$k % $samples.Count]; $k++
    $st=$gr.Save()
    $gr.TranslateTransform([float]$cx,[float]$cy)
    if($ang -ne 0){ $gr.RotateTransform([float]$ang) }
    $clip=New-Object System.Drawing.RectangleF ([float](-$rw/2),[float](-$rh/2),[float]$rw,[float]$rh)
    $gr.SetClip($clip)
    # center-crop fill
    $s=[Math]::Max($rw/$smp.Width,$rh/$smp.Height)
    $dw=$smp.Width*$s; $dh=$smp.Height*$s
    $gr.DrawImage($smp,[float](-$dw/2),[float](-$dh/2),[float]$dw,[float]$dh)
    $gr.ResetClip()
    $gr.Restore($st)
  }
  $gr.Dispose()
  $eps=New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]90)
  $bmp.Save((Join-Path $outDir "$id.jpg"),$jpgEnc,$eps); $eps.Dispose(); $bmp.Dispose(); $img.Dispose()
  Write-Host "filled -> $id"
}
foreach($s in $samples){ $s.Dispose() }
