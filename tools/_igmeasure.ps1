# Region-constrained gray bbox to isolate sm05 IG frame (left card), away from film strip. ASCII.
Add-Type -AssemblyName System.Drawing
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$LOGIC_W=1125; $LOGIC_H=2000; $AW=562; $AH=1000
$SX=$AW/$LOGIC_W; $SY=$AH/$LOGIC_H
$id=$args[0]
# logic region to search (x0,y0,x1,y1)
$rx0=[double]$args[1];$ry0=[double]$args[2];$rx1=[double]$args[3];$ry1=[double]$args[4]
$dx0=$rx0*$SX;$dy0=$ry0*$SY;$dx1=$rx1*$SX;$dy1=$ry1*$SY
$img=[System.Drawing.Image]::FromFile((Join-Path $resDir "temp_$id.jpg"))
$small=New-Object System.Drawing.Bitmap $AW,$AH
$g=[System.Drawing.Graphics]::FromImage($small)
$g.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode=[System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.DrawImage($img,0,0,$AW,$AH);$g.Dispose();$img.Dispose()
$rect=New-Object System.Drawing.Rectangle 0,0,$AW,$AH
$data=$small.LockBits($rect,[System.Drawing.Imaging.ImageLockMode]::ReadOnly,[System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
$stride=$data.Stride;$buf=New-Object byte[] ($stride*$AH)
[System.Runtime.InteropServices.Marshal]::Copy($data.Scan0,$buf,0,$buf.Length)
$small.UnlockBits($data);$small.Dispose()
$minX=9999;$minY=9999;$maxX=-1;$maxY=-1;$cnt=0
for($y=[int]$dy0;$y -lt [int]$dy1;$y++){$row=$y*$stride
  for($x=[int]$dx0;$x -lt [int]$dx1;$x++){$o=$row+$x*3;$b=$buf[$o];$gr=$buf[$o+1];$r=$buf[$o+2]
    $mx=[Math]::Max($r,[Math]::Max($gr,$b));$mn=[Math]::Min($r,[Math]::Min($gr,$b))
    if($mx -ge 224 -and $mx -le 245 -and ($mx-$mn) -le 10){
      if($x -lt $minX){$minX=$x};if($x -gt $maxX){$maxX=$x};if($y -lt $minY){$minY=$y};if($y -gt $maxY){$maxY=$y};$cnt++}}}
$lx0=[int][math]::Round($minX/$SX);$ly0=[int][math]::Round($minY/$SY)
$lx1=[int][math]::Round(($maxX+1)/$SX);$ly1=[int][math]::Round(($maxY+1)/$SY)
Write-Host ("$id IG region bbox: ({0},{1},{2},{3})  pixels={4}" -f $lx0,$ly0,$lx1,$ly1,$cnt)
