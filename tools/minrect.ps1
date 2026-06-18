# Minimum-area rotated rect via brute-force angle search on boundary pixels. Robust for any tilt. ASCII.
Add-Type -AssemblyName System.Drawing
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\minrect"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$IMW=1125; $IMH=2000
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
$id=$args[0]; $mode=$args[1]
if($mode -eq 'white'){ $LO=248;$HI=255;$SAT=8 }
elseif($mode -eq 'white2'){ $LO=222;$HI=255;$SAT=20 }  # white incl. shadowed/dim white (gs08 polaroid lower part)
elseif($mode -eq 'gray2'){ $LO=200;$HI=228;$SAT=10 }   # lighter neutral slots (sm02 ~217, sm04 ~222, bd04 ~205)
else { $LO=231;$HI=243;$SAT=8 }
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
Write-Host "==== $id ($mode) ===="
foreach($sd in $seeds){
  $si=$sd[1]*$IMW+$sd[0]
  if(-not $mask[$si]){ Write-Host "  seed ($($sd[0]),$($sd[1])) not on slot"; continue }
  $st.Clear();$st.Push($si);$visited[$si]=$true
  $bx=New-Object System.Collections.Generic.List[int]
  $by=New-Object System.Collections.Generic.List[int]
  $cnt=0
  while($st.Count -gt 0){$q=$st.Pop();$cnt++;$qx=$q%$IMW;$qy=[int]($q/$IMW)
    $isB=$false
    if($qx -eq 0 -or $qx -eq $IMW-1 -or $qy -eq 0 -or $qy -eq $IMH-1){$isB=$true}
    else {
      if(-not $mask[$q-1]){$isB=$true}elseif(-not $mask[$q+1]){$isB=$true}elseif(-not $mask[$q-$IMW]){$isB=$true}elseif(-not $mask[$q+$IMW]){$isB=$true}
    }
    if($isB){ $bx.Add($qx);$by.Add($qy) }
    if($qx -gt 0){$nb=$q-1;if($mask[$nb]-and-not $visited[$nb]){$visited[$nb]=$true;$st.Push($nb)}}
    if($qx -lt $IMW-1){$nb=$q+1;if($mask[$nb]-and-not $visited[$nb]){$visited[$nb]=$true;$st.Push($nb)}}
    if($qy -gt 0){$nb=$q-$IMW;if($mask[$nb]-and-not $visited[$nb]){$visited[$nb]=$true;$st.Push($nb)}}
    if($qy -lt $IMH-1){$nb=$q+$IMW;if($mask[$nb]-and-not $visited[$nb]){$visited[$nb]=$true;$st.Push($nb)}}}
  $np=$bx.Count
  $bestArea=[double]::MaxValue;$bestDeg=0;$bU0=0;$bU1=0;$bV0=0;$bV1=0
  for($deg=-25.0;$deg -le 25.0;$deg+=0.5){
    $rad=$deg*[Math]::PI/180;$cos=[Math]::Cos($rad);$sin=[Math]::Sin($rad)
    $minU=1e9;$maxU=-1e9;$minV=1e9;$maxV=-1e9
    for($k=0;$k -lt $np;$k++){$px=$bx[$k];$py=$by[$k]
      $u=$px*$cos+$py*$sin; $v=-$px*$sin+$py*$cos
      if($u -lt $minU){$minU=$u};if($u -gt $maxU){$maxU=$u};if($v -lt $minV){$minV=$v};if($v -gt $maxV){$maxV=$v}}
    $area=($maxU-$minU)*($maxV-$minV)
    if($area -lt $bestArea){$bestArea=$area;$bestDeg=$deg;$bU0=$minU;$bU1=$maxU;$bV0=$minV;$bV1=$maxV}
  }
  $w=$bU1-$bU0;$h=$bV1-$bV0;$uc=($bU0+$bU1)/2;$vc=($bV0+$bV1)/2
  $rad=$bestDeg*[Math]::PI/180;$cos=[Math]::Cos($rad);$sin=[Math]::Sin($rad)
  $cx=$uc*$cos-$vc*$sin; $cy=$uc*$sin+$vc*$cos
  $ang=$bestDeg
  if($w -gt $h -and [Math]::Abs($ang) -gt 45){ } # (range limited, skip)
  $x0=[int]($cx-$w/2);$y0=[int]($cy-$h/2);$x1=[int]($cx+$w/2);$y1=[int]($cy+$h/2)
  $ar=[math]::Round($ang,1)
  $gs=$go.Save(); $go.TranslateTransform([float]($cx*$sp),[float]($cy*$sp)); $go.RotateTransform([float]$ang)
  $go.DrawRectangle($penRed,[float](-$w/2*$sp),[float](-$h/2*$sp),[float]($w*$sp),[float]($h*$sp)); $go.Restore($gs)
  Write-Host ("  seed($($sd[0]),$($sd[1])) -> @({0},{1},{2},{3},{4})  area_px={5} bound_pts={6} w={7} h={8}" -f $x0,$y0,$x1,$y1,$ar,$cnt,$np,[int]$w,[int]$h)
}
$go.Dispose()
$eps=New-Object System.Drawing.Imaging.EncoderParameters 1
$eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]92)
$out.Save((Join-Path $outDir "$id.jpg"),$jpgEnc,$eps); $eps.Dispose();$out.Dispose()
Write-Host "  -> minrect/$id.jpg"
