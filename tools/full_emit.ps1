# FULL-RES frame detector: load JPEG at native 1125x2000 (1px==1logic), detect slots,
# 4-extreme-corner -> (cx,cy,w,h,angle). Draw rotated rect, save preview @562x1000. ASCII.
Add-Type -AssemblyName System.Drawing
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\full"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$W=1125; $H=2000
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
$mode=$args[0]; $minFill=[double]$args[1]; $minAreaFrac=[double]$args[2]
$ids=@($args[3..($args.Count-1)])
foreach($id in $ids){
  $p=Join-Path $resDir "temp_$id.jpg"
  if(-not (Test-Path $p)){ Write-Host "$id no img"; continue }
  $img=[System.Drawing.Image]::FromFile($p)
  $bmp=New-Object System.Drawing.Bitmap $W,$H
  $gg=[System.Drawing.Graphics]::FromImage($bmp)
  $gg.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
  $gg.PixelOffsetMode=[System.Drawing.Drawing2D.PixelOffsetMode]::Half
  $gg.DrawImage($img,0,0,$W,$H); $gg.Dispose()
  $rect=New-Object System.Drawing.Rectangle 0,0,$W,$H
  $data=$bmp.LockBits($rect,[System.Drawing.Imaging.ImageLockMode]::ReadOnly,[System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
  $stride=$data.Stride;$buf=New-Object byte[] ($stride*$H)
  [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0,$buf,0,$buf.Length)
  $bmp.UnlockBits($data); $bmp.Dispose()
  $N=$W*$H; $fg=New-Object 'bool[]' $N
  for($y=0;$y -lt $H;$y++){$row=$y*$stride
    for($x=0;$x -lt $W;$x++){$o=$row+$x*3;$b=$buf[$o];$g=$buf[$o+1];$r=$buf[$o+2]
      $mx=[Math]::Max($r,[Math]::Max($g,$b));$mn=[Math]::Min($r,[Math]::Min($g,$b))
      $ok=$false
      if($mode -eq 'gray'){ if($mx -ge 222 -and $mx -le 244 -and ($mx-$mn) -le 12){$ok=$true} }
      else { if($mx -ge 246 -and ($mx-$mn) -le 10){$ok=$true} }
      if($ok){$fg[$y*$W+$x]=$true}}}
  # preview base @562x1000
  $OW=562;$OH=1000
  $out=New-Object System.Drawing.Bitmap $OW,$OH
  $go=[System.Drawing.Graphics]::FromImage($out)
  $go.SmoothingMode=[System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $go.DrawImage($img,0,0,$OW,$OH);$img.Dispose()
  $sx=$OW/$W; $sy=$OH/$H
  $penRed=New-Object System.Drawing.Pen ([System.Drawing.Color]::Red),2
  $visited=New-Object 'bool[]' $N
  $st=New-Object System.Collections.Generic.Stack[int]
  $emitted=@()
  Write-Host "==== $id ($mode) ===="
  for($i=0;$i -lt $N;$i++){
    if($fg[$i] -and -not $visited[$i]){
      $st.Clear();$st.Push($i);$visited[$i]=$true
      $cnt=0;$tx=0;$ty=999999;$bx=0;$by=-1;$lx=999999;$ly=0;$rx=-1;$ry=0
      $minX=$W;$minY=$H;$maxX=0;$maxY=0
      while($st.Count -gt 0){$q=$st.Pop();$cnt++;$qx=$q%$W;$qy=[int]($q/$W)
        if($qy -lt $ty){$ty=$qy;$tx=$qx}; if($qy -gt $by){$by=$qy;$bx=$qx}
        if($qx -lt $lx){$lx=$qx;$ly=$qy}; if($qx -gt $rx){$rx=$qx;$ry=$qy}
        if($qx -lt $minX){$minX=$qx};if($qx -gt $maxX){$maxX=$qx};if($qy -lt $minY){$minY=$qy};if($qy -gt $maxY){$maxY=$qy}
        if($qx -gt 0){$nb=$q-1;if($fg[$nb]-and-not $visited[$nb]){$visited[$nb]=$true;$st.Push($nb)}}
        if($qx -lt $W-1){$nb=$q+1;if($fg[$nb]-and-not $visited[$nb]){$visited[$nb]=$true;$st.Push($nb)}}
        if($qy -gt 0){$nb=$q-$W;if($fg[$nb]-and-not $visited[$nb]){$visited[$nb]=$true;$st.Push($nb)}}
        if($qy -lt $H-1){$nb=$q+$W;if($fg[$nb]-and-not $visited[$nb]){$visited[$nb]=$true;$st.Push($nb)}}}
      $bw=$maxX-$minX+1;$bh=$maxY-$minY+1;$areaFrac=$cnt/[double]$N
      if($areaFrac -lt $minAreaFrac -or $areaFrac -gt 0.6){continue}
      $fill=$cnt/[double]($bw*$bh)
      if($fill -lt $minFill){continue}
      $e1x=$rx-$tx;$e1y=$ry-$ty; $e2x=$lx-$tx;$e2y=$ly-$ty
      $len1=[Math]::Sqrt($e1x*$e1x+$e1y*$e1y); $len2=[Math]::Sqrt($e2x*$e2x+$e2y*$e2y)
      if([Math]::Abs($e1x)/[Math]::Max($len1,1) -ge [Math]::Abs($e2x)/[Math]::Max($len2,1)){
        $wx=$e1x;$wy=$e1y;$wlen=$len1;$hlen=$len2
      } else { $wx=$e2x;$wy=$e2y;$wlen=$len2;$hlen=$len1 }
      if($wx -lt 0){$wx=-$wx;$wy=-$wy}
      $theta=[Math]::Atan2($wy,$wx)*180/[Math]::PI
      $cxp=($tx+$rx+$bx+$lx)/4.0; $cyp=($ty+$ry+$by+$ly)/4.0
      # near-upright: snap to AABB (corner method noisy for axis-aligned rounded rects)
      if([Math]::Abs($theta) -lt 4){
        $x0=$minX;$y0=$minY;$x1=$maxX+1;$y1=$maxY+1; $theta=0
        $go.DrawRectangle($penRed,[float]($x0*$sx),[float]($y0*$sy),[float](($x1-$x0)*$sx),[float](($y1-$y0)*$sy))
      } else {
        $x0=[int]($cxp-$wlen/2);$y0=[int]($cyp-$hlen/2);$x1=[int]($cxp+$wlen/2);$y1=[int]($cyp+$hlen/2)
        $gs=$go.Save(); $go.TranslateTransform([float]($cxp*$sx),[float]($cyp*$sy)); $go.RotateTransform([float]$theta)
        $go.DrawRectangle($penRed,[float](-$wlen/2*$sx),[float](-$hlen/2*$sy),[float]($wlen*$sx),[float]($hlen*$sy)); $go.Restore($gs)
      }
      $ar=[math]::Round($theta,1)
      if($ar -eq 0){ $emitted+=,("@({0},{1},{2},{3})" -f $x0,$y0,$x1,$y1) }
      else { $emitted+=,("@({0},{1},{2},{3},{4})" -f $x0,$y0,$x1,$y1,$ar) }
      Write-Host ("  box=({0},{1},{2},{3}) angle={4} fill={5:N2} area%={6:N2}" -f $x0,$y0,$x1,$y1,$ar,$fill,($areaFrac*100))
    }
  }
  $go.Dispose()
  $eps=New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]90)
  $out.Save((Join-Path $outDir "$id.jpg"),$jpgEnc,$eps); $eps.Dispose();$out.Dispose()
  $srt=$emitted   # keep detection order; will sort when baking
  Write-Host ('  CONFIG "{0}"=@({1})' -f $id, ($emitted -join ','))
}
