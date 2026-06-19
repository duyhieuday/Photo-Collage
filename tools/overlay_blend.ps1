# Draw current cells (lime) over the temp+thumb blend to see cell vs frame vs photo together. ASCII only.
Add-Type -AssemblyName System.Drawing
. "D:\EZTech\EZTechApp\collage_pic_editor\tools\cells_config.ps1"
$id=$args[0]; if(-not $id){$id="cp01"}
$blend="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\blend\$id.jpg"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\overlayblend"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$OW=562;$OH=1000;$sX=$OW/1125.0;$sY=$OH/2000.0
$img=[System.Drawing.Image]::FromFile($blend)
$bmp=New-Object System.Drawing.Bitmap $OW,$OH
$gr=[System.Drawing.Graphics]::FromImage($bmp)
$gr.SmoothingMode=[System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$gr.DrawImage($img,0,0,$OW,$OH)
$pen=New-Object System.Drawing.Pen ([System.Drawing.Color]::Lime),3
$rr=$CELLS[$id]
if($rr.Count -ge 1 -and ($rr[0] -is [int] -or $rr[0] -is [double])){ $rr=@(,$rr) }
foreach($c in $rr){
  $rw=($c[2]-$c[0])*$sX; $rh=($c[3]-$c[1])*$sY; $cx=($c[0]+$c[2])/2*$sX; $cy=($c[1]+$c[3])/2*$sY
  $ang=0.0; if($c.Count -ge 5){$ang=[double]$c[4]}
  $st=$gr.Save(); $gr.TranslateTransform([float]$cx,[float]$cy); if($ang -ne 0){$gr.RotateTransform([float]$ang)}
  $gr.DrawRectangle($pen,[float](-$rw/2),[float](-$rh/2),[float]$rw,[float]$rh); $gr.Restore($st)
}
$gr.Dispose()
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
$eps=New-Object System.Drawing.Imaging.EncoderParameters 1
$eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]92)
$bmp.Save((Join-Path $outDir "$id.jpg"),$jpgEnc,$eps)
$eps.Dispose();$bmp.Dispose();$img.Dispose()
Write-Host "overlayblend -> $id"
