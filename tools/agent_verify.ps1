# Overlay candidate cells (logic 1125x2000) on temp PNGs -> tools/_out/agent_gs/<id>.jpg. ASCII only.
Add-Type -AssemblyName System.Drawing
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\agent_gs"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$CELLS=@{
  "gs01"=@(,@(224,836,944,1684))
  "gs02"=@(@(188,352,936,836),@(188,872,936,1284))
  "gs03"=@(@(88,484,552,1092),@(568,484,1024,1084),@(580,1120,1032,1708),@(120,1124,552,1700))
  "gs04"=@(@(32,52,368,708),@(392,52,732,708),@(756,52,1092,708),@(32,1292,368,1944),@(392,1292,732,1944),@(756,1292,1092,1944))
  "gs05"=@(@(77,77,1049,1051),@(58,1591,459,1970))
  "gs06"=@(@(68,620,1056,1140),@(552,1172,1056,1628))
  "gs07"=@(@(61,186,526,662),@(634,780,1072,1223),@(44,1393,500,1854))
  "gs08"=@(@(68,478,515,1065),@(665,949,1082,1378))
  "gs09"=@(@(519,477,997,814),@(91,1006,600,1370))
  "gs10"=@(,@(222,628,856,1306))
}

$LOGIC_W=1125.0; $LOGIC_H=2000.0
$OW=562; $OH=1000
$sx=$OW/$LOGIC_W; $sy=$OH/$LOGIC_H
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
$pen=New-Object System.Drawing.Pen ([System.Drawing.Color]::Lime),3

foreach($id in @($CELLS.Keys|Sort-Object)){
  $img=[System.Drawing.Image]::FromFile((Join-Path $resDir "temp_$id.jpg"))
  $bmp=New-Object System.Drawing.Bitmap $OW,$OH
  $gr=[System.Drawing.Graphics]::FromImage($bmp)
  $gr.DrawImage($img,0,0,$OW,$OH)
  $rr=$CELLS[$id]
  foreach($c in $rr){
    $rx=$c[0]*$sx; $ry=$c[1]*$sy; $rw=($c[2]-$c[0])*$sx; $rh=($c[3]-$c[1])*$sy
    $gr.DrawRectangle($pen,[float]$rx,[float]$ry,[float]$rw,[float]$rh)
  }
  $gr.Dispose()
  $eps=New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]90)
  $bmp.Save((Join-Path $outDir "$id.jpg"),$jpgEnc,$eps)
  $eps.Dispose();$bmp.Dispose();$img.Dispose()
  Write-Host "verify -> $id ($($rr.Count) cells)"
}
