Add-Type -AssemblyName System.Drawing
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
# first-cell rects in LOGIC 1125 space
$FIRST=@{
 "bd01"=@(343,496,801,985)
 "bd02"=@(0,1431,336,1796)
 "bd03"=@(497,90,1054,834)
 "bd04"=@(655,422,1082,986)
 "bd05"=@(267,642,955,1567)
 "bd06"=@(152,158,963,1230)
 "bd07"=@(118,566,966,1374)
 "bd08"=@(231,711,894,1457)
 "bd09"=@(66,557,585,1088)
 "bd10"=@(608,424,1083,888)
}
foreach($id in ($FIRST.Keys|Sort-Object)){
  $r=$FIRST[$id]
  $img=[System.Drawing.Image]::FromFile((Join-Path $resDir "temp_$id.jpg"))
  $bmp=New-Object System.Drawing.Bitmap $img
  $cx=[int]((($r[0]+$r[2])/2)/1125.0*$bmp.Width)
  $cy=[int]((($r[1]+$r[3])/2)/2000.0*$bmp.Height)
  if($cx -ge $bmp.Width){$cx=$bmp.Width-1}
  if($cy -ge $bmp.Height){$cy=$bmp.Height-1}
  $px=$bmp.GetPixel($cx,$cy)
  $mask = if($px.R -ge 247 -and $px.G -ge 247 -and $px.B -ge 247){"WHITE"}else{"NONE"}
  Write-Host ("{0}: center=({1},{2}) rgb=({3},{4},{5}) -> {6}" -f $id,$cx,$cy,$px.R,$px.G,$px.B,$mask)
  $bmp.Dispose();$img.Dispose()
}
