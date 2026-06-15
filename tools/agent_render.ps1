# Render candidate bd cells (given in 1080 space) onto temp_bdNN.jpg for visual verify.
Add-Type -AssemblyName System.Drawing
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\agent_bd"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$SCALE = 1125.0/1080.0

# cells in 1080 space: l,t,r,b
$RAW = @{
  "bd01"=@(@(329,476,769,946),@(124,947,564,1417),@(502,1434,942,1904))
  "bd02"=@(@(-3,1374,323,1724),@(377,1374,703,1724),@(757,1374,1083,1724))
  "bd03"=@(@(477,86,1012,801),@(105,1032,627,1730))
  "bd04"=@(@(629,405,1039,947),@(667,1132,1019,1601))
  "bd05"=@(@(256,616,917,1504))
  "bd06"=@(@(146,152,924,1181))
  "bd07"=@(@(113,543,927,1319))
  "bd08"=@(@(222,683,858,1399))
  "bd09"=@(@(63,535,562,1044),@(575,1171,993,1596))
  "bd10"=@(@(584,407,1040,853),@(584,898,1040,1346),@(584,1391,1040,1847),@(84,1391,540,1847))
}

$OW=450; $OH=800
$sx=$OW/1125.0; $sy=$OH/2000.0
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
$pen=New-Object System.Drawing.Pen ([System.Drawing.Color]::Lime),3

$only=@($args)
foreach($id in ($RAW.Keys | Sort-Object)){
  if($only.Count -gt 0 -and ($only -notcontains $id)){ continue }
  $img=[System.Drawing.Image]::FromFile((Join-Path $resDir "temp_$id.jpg"))
  $bmp=New-Object System.Drawing.Bitmap $OW,$OH
  $gr=[System.Drawing.Graphics]::FromImage($bmp)
  $gr.DrawImage($img,0,0,$OW,$OH)
  foreach($c in $RAW[$id]){
    # scale 1080->1125 logic, clamp
    $L=[Math]::Max(0,[Math]::Min(1125,$c[0]*$SCALE))
    $T=[Math]::Max(0,[Math]::Min(2000,$c[1]*$SCALE))
    $R=[Math]::Max(0,[Math]::Min(1125,$c[2]*$SCALE))
    $B=[Math]::Max(0,[Math]::Min(2000,$c[3]*$SCALE))
    $rx=$L*$sx; $ry=$T*$sy; $rw=($R-$L)*$sx; $rh=($B-$T)*$sy
    $gr.DrawRectangle($pen,[float]$rx,[float]$ry,[float]$rw,[float]$rh)
  }
  $gr.Dispose()
  $eps=New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]90)
  $bmp.Save((Join-Path $outDir "$id.jpg"),$jpgEnc,$eps)
  $eps.Dispose();$bmp.Dispose();$img.Dispose()
  Write-Host "rendered $id"
}
