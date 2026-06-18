# Measure rotated rect of a gray photo-window via flood (wide band) + PCA principal axis.
# Reliable tilt for landscape windows. Draw overlay (red) to verify. ASCII.
Add-Type -AssemblyName System.Drawing
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\rrect"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$IMW=1125; $IMH=2000
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
$id=$args[0]; $mode=$args[1]
if($mode -eq 'white'){ $LO=248;$HI=255;$SAT=8 } else { $LO=231;$HI=243;$SAT=8 }
$seeds=@(); for($i=2;$i -lt $args.Count;$i+=2){ $seeds+=,@([int]$args[$i],[int]$args[$i+1]) }
$img=[System.Drawing.Image]::FromFile((Join-Path $resDir "temp_$id.jpg"))
$bmp=New-Object System.Drawing.Bitmap $IMW,$IMH
$gg=[System.Drawing.Graphics]::FromImage($bmp)
$gg.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$gg.PixelOffsetMode=[System.Drawing.Drawing2D.PixelOffsetMode]::Half
$gg.DrawImage($img,0,0,$IMW,$IMH); $gg.Dispose()
$rectFull=New-Object System.Drawing.Rectangle 0,0,$IMW,$IMH
$data=$bmp.LockBits($rectFull,[System.Drawing.Imaging.ImageLockMode]::ReadOnly,[System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
$stride=$data.Stride;$buf=New-Object byte[] ($stride*$IMH)
[System.Runtime.InteropServices.Marshal]::Copy($data.Scan0,$buf,0,$buf.Length)
$bmp.UnlockBits($data); $bmp.Dispose()
$Ntot=$IMW*$IMH; $mask=New-Object 'bool[]' $Ntot
for($yy=0;$yy -lt $IMH;$yy++){$row=$yy*$stride
  for($xx=0;$xx -lt $IMW;$xx++){$o=$row+$xx*3;$b=$buf[$o];$g=$buf[$o+1];$r=$buf[$o+2]
    $mxv=[Math]::Max($r,[Math]::Max($g,$b));$mnv=[Math]::Min($r,[Math]::Min($g,$b))
    if($mxv -ge $LO -and $mxv -le $HI -and ($mxv-$mnv) -le $SAT){$mask[$yy*$IMW+$xx]=$true}}}
$out=New-Object System.Drawing.Bitmap 750,1333
$go=[System.Drawing.Graphics]::FromImage($out); $go.SmoothingMode=[System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$go.DrawImage($img,0,0,750,1333); $img.Dispose(); $sp=750.0/$IMW
$penRed=New-Object System.Drawing.Pen ([System.Drawing.Color]::Red),2
$visited=New-Object 'bool[]' $Ntot
$st=New-Object System.Collections.Generic.Stack[int]
foreach($sd in $seeds){
  $si=$sd[1]*$IMW+$sd[0]
  if(-not $mask[$si]){ Write-Host "seed ($($sd[0]),$($sd[1])) not gray"; continue }
  $st.Clear();$st.Push($si);$visited[$si]=$true
  $cnt=0;$minX=$IMW;$minY=$IMH;$maxX=0;$maxY=0
  [double]$sx=0;[double]$sy=0;[double]$sxx=0;[double]$syy=0;[double]$sxy=0
  while($st.Count -gt 0){$q=$st.Pop();$cnt++;$qx=$q%$IMW;$qy=[int]($q/$IMW)
    $sx+=$qx;$sy+=$qy;$sxx+=[double]$qx*$qx;$syy+=[double]$qy*$qy;$sxy+=[double]$qx*$qy
    if($qx -lt $minX){$minX=$qx};if($qx -gt $maxX){$maxX=$qx};if($qy -lt $minY){$minY=$qy};if($qy -gt $maxY){$maxY=$qy}
    if($qx -gt 0){$nb=$q-1;if($mask[$nb]-and-not $visited[$nb]){$visited[$nb]=$true;$st.Push($nb)}}
    if($qx -lt $IMW-1){$nb=$q+1;if($mask[$nb]-and-not $visited[$nb]){$visited[$nb]=$true;$st.Push($nb)}}
    if($qy -gt 0){$nb=$q-$IMW;if($mask[$nb]-and-not $visited[$nb]){$visited[$nb]=$true;$st.Push($nb)}}
    if($qy -lt $IMH-1){$nb=$q+$IMW;if($mask[$nb]-and-not $visited[$nb]){$visited[$nb]=$true;$st.Push($nb)}}}
  $mx=$sx/$cnt;$my=$sy/$cnt
  $cxx=$sxx/$cnt-$mx*$mx;$cyy=$syy/$cnt-$my*$my;$cxy=$sxy/$cnt-$mx*$my
  $theta=0.5*[Math]::Atan2(2*$cxy,$cxx-$cyy)   # principal axis (rad)
  $cosT=[Math]::Cos($theta);$sinT=[Math]::Sin($theta)
  # pass 2 over bbox: project gray pixels onto principal axes
  $minU=1e9;$maxU=-1e9;$minV=1e9;$maxV=-1e9
  for($yy=$minY;$yy -le $maxY;$yy++){$row=$yy*$stride
    for($xx=$minX;$xx -le $maxX;$xx++){$o=$row+$xx*3;$b=$buf[$o];$g=$buf[$o+1];$r=$buf[$o+2]
      $mxv=[Math]::Max($r,[Math]::Max($g,$b));$mnv=[Math]::Min($r,[Math]::Min($g,$b))
      if($mxv -ge $LO -and $mxv -le $HI -and ($mxv-$mnv) -le $SAT){
        $u=$xx*$cosT+$yy*$sinT; $v=-$xx*$sinT+$yy*$cosT
        if($u -lt $minU){$minU=$u};if($u -gt $maxU){$maxU=$u};if($v -lt $minV){$minV=$v};if($v -gt $maxV){$maxV=$v}}}}
  $uc=($minU+$maxU)/2; $vc=($minV+$maxV)/2
  $cx=$uc*$cosT-$vc*$sinT; $cy=$uc*$sinT+$vc*$cosT
  $w=$maxU-$minU; $h=$maxV-$minV
  $angDeg=$theta*180/[Math]::PI
  # chuan hoa goc ve [-45,45]: neu truc chinh la chieu doc (portrait) thi tru 90 + doi w/h
  if($angDeg -gt 45){ $angDeg-=90; $tmp=$w;$w=$h;$h=$tmp }
  elseif($angDeg -lt -45){ $angDeg+=90; $tmp=$w;$w=$h;$h=$tmp }
  $x0=[int]($cx-$w/2);$y0=[int]($cy-$h/2);$x1=[int]($cx+$w/2);$y1=[int]($cy+$h/2)
  $ar=[math]::Round($angDeg,1)
  # draw
  $gs=$go.Save(); $go.TranslateTransform([float]($cx*$sp),[float]($cy*$sp)); $go.RotateTransform([float]$angDeg)
  $go.DrawRectangle($penRed,[float](-$w/2*$sp),[float](-$h/2*$sp),[float]($w*$sp),[float]($h*$sp)); $go.Restore($gs)
  Write-Host ("  seed($($sd[0]),$($sd[1])) -> @({0},{1},{2},{3},{4})  px={5} w={6} h={7}" -f $x0,$y0,$x1,$y1,$ar,$cnt,[int]$w,[int]$h)
}
$go.Dispose()
$eps=New-Object System.Drawing.Imaging.EncoderParameters 1
$eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]92)
$out.Save((Join-Path $outDir "$id.jpg"),$jpgEnc,$eps); $eps.Dispose();$out.Dispose()
Write-Host "  -> rrect/$id.jpg"
