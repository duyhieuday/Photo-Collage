# Like verify_cells but overlays cells onto thumb_<id> (filled preview) to match SAMPLE photos. ASCII only.
Add-Type -AssemblyName System.Drawing
. "D:\EZTech\EZTechApp\collage_pic_editor\tools\cells_config.ps1"
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\verifythumb"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$LOGIC_W=1125; $LOGIC_H=2000
$OW=562; $OH=1000
$scaleX=$OW/$LOGIC_W; $scaleY=$OH/$LOGIC_H
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
$pen=New-Object System.Drawing.Pen ([System.Drawing.Color]::Lime),3
$only=@($args)
foreach($id in @($CELLS.Keys)){
  if($only.Count -gt 0 -and ($only -notcontains $id)){ continue }
  $tp=Join-Path $resDir "thumb_$id.jpg"
  if(-not (Test-Path $tp)){ Write-Host "no thumb $id"; continue }
  $img=[System.Drawing.Image]::FromFile($tp)
  $bmp=New-Object System.Drawing.Bitmap $OW,$OH
  $gr=[System.Drawing.Graphics]::FromImage($bmp)
  $gr.SmoothingMode=[System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $gr.DrawImage($img,0,0,$OW,$OH)
  $rr=$CELLS[$id]
  if($rr.Count -ge 1 -and ($rr[0] -is [int] -or $rr[0] -is [double])){ $rr=@(,$rr) }
  foreach($c in $rr){
    $rx=$c[0]*$scaleX; $ry=$c[1]*$scaleY; $rw=($c[2]-$c[0])*$scaleX; $rh=($c[3]-$c[1])*$scaleY
    if($c.Count -ge 5 -and ([double]$c[4]) -ne 0){
      $cx=($c[0]+$c[2])/2*$scaleX; $cy=($c[1]+$c[3])/2*$scaleY
      $st=$gr.Save(); $gr.TranslateTransform([float]$cx,[float]$cy); $gr.RotateTransform([float]$c[4])
      $gr.DrawRectangle($pen,[float](-$rw/2),[float](-$rh/2),[float]$rw,[float]$rh); $gr.Restore($st)
    } else { $gr.DrawRectangle($pen,[float]$rx,[float]$ry,[float]$rw,[float]$rh) }
  }
  $gr.Dispose()
  $eps=New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]90)
  $bmp.Save((Join-Path $outDir "$id.jpg"),$jpgEnc,$eps); $eps.Dispose();$bmp.Dispose();$img.Dispose()
  Write-Host "verifythumb -> $id"
}
