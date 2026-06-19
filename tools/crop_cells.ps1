# Extract each cell's (rotated) photo region from thumb_<id> and save as place0/1.. for render_filled. ASCII only.
Add-Type -AssemblyName System.Drawing
. "D:\EZTech\EZTechApp\collage_pic_editor\tools\cells_config.ps1"
$id=$args[0]; if(-not $id){$id="cp01"}
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out"
$src=[System.Drawing.Image]::FromFile((Join-Path $resDir "thumb_$id.jpg"))
$SW=$src.Width; $SH=$src.Height
$logicToSrcX = $SW/1125.0; $logicToSrcY = $SH/2000.0
$rr=$CELLS[$id]
if($rr.Count -ge 1 -and ($rr[0] -is [int] -or $rr[0] -is [double])){ $rr=@(,$rr) }
$k=0
foreach($c in $rr){
  $wL=($c[2]-$c[0]); $hL=($c[3]-$c[1]); $cxL=($c[0]+$c[2])/2.0; $cyL=($c[1]+$c[3])/2.0
  $ang=0.0; if($c.Count -ge 5){$ang=[double]$c[4]}
  $wPx=[int]($wL*$logicToSrcX); $hPx=[int]($hL*$logicToSrcY)
  $cxPx=$cxL*$logicToSrcX; $cyPx=$cyL*$logicToSrcY
  $bmp=New-Object System.Drawing.Bitmap $wPx,$hPx
  $g=[System.Drawing.Graphics]::FromImage($bmp)
  $g.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  # map cell center -> bmp center, undo rotation
  $g.TranslateTransform([float]($wPx/2.0),[float]($hPx/2.0))
  $g.RotateTransform([float](-$ang))
  $g.TranslateTransform([float](-$cxPx),[float](-$cyPx))
  $g.DrawImage($src,0,0,$SW,$SH)
  $g.Dispose()
  $bmp.Save((Join-Path $outDir "place$k.png"),[System.Drawing.Imaging.ImageFormat]::Png)
  $bmp.Dispose()
  Write-Host "place$k = cell $k region ${wPx}x${hPx}"
  $k++
}
$src.Dispose()
