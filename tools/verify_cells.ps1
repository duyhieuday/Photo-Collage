# Draw baked cellRects (from cells_config.ps1) onto templates to self-verify. ASCII only.
Add-Type -AssemblyName System.Drawing
. "D:\EZTech\EZTechApp\collage_pic_editor\tools\cells_config.ps1"
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\verify"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$LOGIC_W=1125; $LOGIC_H=2000
$OW=562; $OH=1000
$scaleX=$OW/$LOGIC_W; $scaleY=$OH/$LOGIC_H
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
$pen=New-Object System.Drawing.Pen ([System.Drawing.Color]::Lime),3

$only=@($args)
foreach($id in @($CELLS.Keys)){
  if($only.Count -gt 0 -and ($only -notcontains $id)){ continue }
  $img=[System.Drawing.Image]::FromFile((Join-Path $resDir "temp_$id.jpg"))
  $bmp=New-Object System.Drawing.Bitmap $OW,$OH
  $gr=[System.Drawing.Graphics]::FromImage($bmp)
  $gr.DrawImage($img,0,0,$OW,$OH)
  $rr=$CELLS[$id]
  if($rr.Count -ge 1 -and ($rr[0] -is [int] -or $rr[0] -is [double])){ $rr=@(,$rr) }
  foreach($c in $rr){
    $rx=$c[0]*$scaleX; $ry=$c[1]*$scaleY; $rw=($c[2]-$c[0])*$scaleX; $rh=($c[3]-$c[1])*$scaleY
    $gr.DrawRectangle($pen,[float]$rx,[float]$ry,[float]$rw,[float]$rh)
  }
  $gr.Dispose()
  $eps=New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]88)
  $bmp.Save((Join-Path $outDir "$id.jpg"),$jpgEnc,$eps)
  $eps.Dispose();$bmp.Dispose();$img.Dispose()
  Write-Host "verify -> $id"
}
