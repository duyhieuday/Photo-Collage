# Resize Sports Temp_SP02..10 + Thumb_SP02..10 -> res/drawable jpg (sp01 da co .webp -> BO QUA).
Add-Type -AssemblyName System.Drawing
$src="D:\EZTech\AppAssets\PhotoCollage\Category_Template\Sports"
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$TEMP_W=1125;$TEMP_H=2000;$THUMB_W=540;$THUMB_H=960
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
function Save-Jpeg($bmp,$path,$q){
  $eps=New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]$q)
  $bmp.Save($path,$jpgEnc,$eps);$eps.Dispose()
}
function Resize-Save($srcPath,$dstPath,$w,$h,$q){
  $img=[System.Drawing.Image]::FromFile($srcPath)
  $bmp=New-Object System.Drawing.Bitmap $w,$h
  $g=[System.Drawing.Graphics]::FromImage($bmp)
  $g.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.PixelOffsetMode=[System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $g.DrawImage($img,0,0,$w,$h);$g.Dispose()
  Save-Jpeg $bmp $dstPath $q; $bmp.Dispose();$img.Dispose()
}
for($i=2;$i -le 10;$i++){
  $nn="{0:D2}" -f $i; $id="sp$nn"
  $t=Join-Path $src "Temp_SP$nn.png"; $th=Join-Path $src "Thumb_SP$nn.png"
  if(-not (Test-Path $t)){Write-Host "MISSING $t";continue}
  Resize-Save $t  (Join-Path $resDir "temp_$id.jpg")  $TEMP_W  $TEMP_H  88
  Resize-Save $th (Join-Path $resDir "thumb_$id.jpg") $THUMB_W $THUMB_H 86
  Write-Host "OK $id"
}
Write-Host "DONE"
