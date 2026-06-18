# Seeded flood-fill slot detector. Give one interior seed per slot (logic coords);
# flood the slot-color region -> exact AABB + rotation (4 extreme corners). ASCII.
# Avoids merging (one component per seed) and handles tilted slots.
Add-Type -AssemblyName System.Drawing
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\seed"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$IMW=1125; $IMH=2000
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
$id=$args[0]; $mode=$args[1]
$seeds=@(); for($i=2;$i -lt $args.Count;$i+=2){ $seeds+=,@([int]$args[$i],[int]$args[$i+1]) }
$p=Join-Path $resDir "temp_$id.jpg"
$img=[System.Drawing.Image]::FromFile($p)
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
    $ok=$false
    if($mode -eq 'gray'){ if($mxv -ge 228 -and $mxv -le 242 -and ($mxv-$mnv) -le 8){$ok=$true} }
    else { if($mxv -ge 244 -and ($mxv-$mnv) -le 12){$ok=$true} }
    if($ok){$mask[$yy*$IMW+$xx]=$true}}}
# preview
$OW=750;$OH=1333;$sxp=$OW/$IMW;$syp=$OH/$IMH
$out=New-Object System.Drawing.Bitmap $OW,$OH
$go=[System.Drawing.Graphics]::FromImage($out)
$go.SmoothingMode=[System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$go.DrawImage($img,0,0,$OW,$OH); $img.Dispose()
$penRed=New-Object System.Drawing.Pen ([System.Drawing.Color]::Red),2
$visited=New-Object 'bool[]' $Ntot
$st=New-Object System.Collections.Generic.Stack[int]
$emitted=@()
Write-Host "==== $id ($mode) ===="
foreach($sd in $seeds){
  $sx=$sd[0];$sy=$sd[1]; $si=$sy*$IMW+$sx
  if(-not $mask[$si]){
    # nudge search in small window for a masked pixel
    $found=$false
    for($dy=-12;$dy -le 12 -and -not $found;$dy++){for($dx=-12;$dx -le 12 -and -not $found;$dx++){
      $nx=$sx+$dx;$ny=$sy+$dy; if($nx -ge 0 -and $nx -lt $IMW -and $ny -ge 0 -and $ny -lt $IMH){ if($mask[$ny*$IMW+$nx]){$si=$ny*$IMW+$nx;$found=$true} }}}
    if(-not $found){ Write-Host "  seed ($sx,$sy) not on slot"; continue }
  }
  if($visited[$si]){ Write-Host "  seed ($sx,$sy) already covered"; continue }
  $st.Clear();$st.Push($si);$visited[$si]=$true
  $cnt=0;$tx=0;$ty=999999;$bx=0;$by=-1;$lx=999999;$ly=0;$rx=-1;$ry=0
  $minX=$IMW;$minY=$IMH;$maxX=0;$maxY=0
  while($st.Count -gt 0){$q=$st.Pop();$cnt++;$qx=$q%$IMW;$qy=[int]($q/$IMW)
    if($qy -lt $ty){$ty=$qy;$tx=$qx}; if($qy -gt $by){$by=$qy;$bx=$qx}
    if($qx -lt $lx){$lx=$qx;$ly=$qy}; if($qx -gt $rx){$rx=$qx;$ry=$qy}
    if($qx -lt $minX){$minX=$qx};if($qx -gt $maxX){$maxX=$qx};if($qy -lt $minY){$minY=$qy};if($qy -gt $maxY){$maxY=$qy}
    if($qx -gt 0){$nb=$q-1;if($mask[$nb]-and-not $visited[$nb]){$visited[$nb]=$true;$st.Push($nb)}}
    if($qx -lt $IMW-1){$nb=$q+1;if($mask[$nb]-and-not $visited[$nb]){$visited[$nb]=$true;$st.Push($nb)}}
    if($qy -gt 0){$nb=$q-$IMW;if($mask[$nb]-and-not $visited[$nb]){$visited[$nb]=$true;$st.Push($nb)}}
    if($qy -lt $IMH-1){$nb=$q+$IMW;if($mask[$nb]-and-not $visited[$nb]){$visited[$nb]=$true;$st.Push($nb)}}}
  $bw=$maxX-$minX+1;$bh=$maxY-$minY+1
  $e1x=$rx-$tx;$e1y=$ry-$ty; $e2x=$lx-$tx;$e2y=$ly-$ty
  $len1=[Math]::Sqrt($e1x*$e1x+$e1y*$e1y); $len2=[Math]::Sqrt($e2x*$e2x+$e2y*$e2y)
  if([Math]::Abs($e1x)/[Math]::Max($len1,1) -ge [Math]::Abs($e2x)/[Math]::Max($len2,1)){ $wx=$e1x;$wy=$e1y;$wl=$len1;$hl=$len2 } else { $wx=$e2x;$wy=$e2y;$wl=$len2;$hl=$len1 }
  if($wx -lt 0){$wx=-$wx;$wy=-$wy}
  $theta=[Math]::Atan2($wy,$wx)*180/[Math]::PI
  $cxp=($tx+$rx+$bx+$lx)/4.0; $cyp=($ty+$ry+$by+$ly)/4.0
  $fillr=$cnt/[double]($bw*$bh)
  # Irregular shape (cloud/heart) or leaked flood -> rotated interp meaningless: use AABB upright.
  if($fillr -lt 0.72){ $theta=0 }
  if([Math]::Abs($theta) -lt 3.5){
    $x0=$minX;$y0=$minY;$x1=$maxX+1;$y1=$maxY+1;$theta=0
    $go.DrawRectangle($penRed,[float]($x0*$sxp),[float]($y0*$syp),[float](($x1-$x0)*$sxp),[float](($y1-$y0)*$syp))
    $emitted+=,("@({0},{1},{2},{3})" -f $x0,$y0,$x1,$y1)
  } else {
    $x0=[int]($cxp-$wl/2);$y0=[int]($cyp-$hl/2);$x1=[int]($cxp+$wl/2);$y1=[int]($cyp+$hl/2)
    $gs2=$go.Save();$go.TranslateTransform([float]($cxp*$sxp),[float]($cyp*$syp));$go.RotateTransform([float]$theta)
    $go.DrawRectangle($penRed,[float](-$wl/2*$sxp),[float](-$hl/2*$syp),[float]($wl*$sxp),[float]($hl*$syp));$go.Restore($gs2)
    $ar=[math]::Round($theta,1)
    $emitted+=,("@({0},{1},{2},{3},{4})" -f $x0,$y0,$x1,$y1,$ar)
  }
  Write-Host ("  seed($sx,$sy) -> box=({0},{1},{2},{3}) angle={4:N1} px={5} bw={6} bh={7}" -f $x0,$y0,$x1,$y1,$theta,$cnt,$bw,$bh)
}
$go.Dispose()
$eps=New-Object System.Drawing.Imaging.EncoderParameters 1
$eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]92)
$out.Save((Join-Path $outDir "$id.jpg"),$jpgEnc,$eps); $eps.Dispose();$out.Dispose()
Write-Host ('  CONFIG "{0}"=@({1})' -f $id, ($emitted -join ','))
