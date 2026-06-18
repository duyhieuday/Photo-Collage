# Emit rotated-rect (cx,cy,w,h,angle) per detected frame via extreme-point corners,
# and draw the rotated rect (GDI RotateTransform == Android canvas.rotate, CW+ y-down)
# to verify before baking. ASCII only.
Add-Type -AssemblyName System.Drawing
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\emit"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$LOGIC_W=1125; $LOGIC_H=2000; $AW=562; $AH=1000
$SX=$AW/$LOGIC_W; $SY=$AH/$LOGIC_H
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
$mode=$args[0]; $minFill=[double]$args[1]
$ids=@($args[2..($args.Count-1)])
foreach($id in $ids){
  $p=Join-Path $resDir "temp_$id.jpg"
  if(-not (Test-Path $p)){ Write-Host "$id no img"; continue }
  $img=[System.Drawing.Image]::FromFile($p)
  $small=New-Object System.Drawing.Bitmap $AW,$AH
  $g=[System.Drawing.Graphics]::FromImage($small)
  $g.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
  $g.PixelOffsetMode=[System.Drawing.Drawing2D.PixelOffsetMode]::Half
  $g.DrawImage($img,0,0,$AW,$AH);$g.Dispose()
  $rect=New-Object System.Drawing.Rectangle 0,0,$AW,$AH
  $data=$small.LockBits($rect,[System.Drawing.Imaging.ImageLockMode]::ReadOnly,[System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
  $stride=$data.Stride;$bufp=New-Object byte[] ($stride*$AH)
  [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0,$bufp,0,$bufp.Length)
  $small.UnlockBits($data);$small.Dispose()
  $N=$AW*$AH; $fg=New-Object 'bool[]' $N
  for($y=0;$y -lt $AH;$y++){$row=$y*$stride
    for($x=0;$x -lt $AW;$x++){$o=$row+$x*3;$bb=$bufp[$o];$gg=$bufp[$o+1];$rr2=$bufp[$o+2]
      $mx=[Math]::Max($rr2,[Math]::Max($gg,$bb));$mn=[Math]::Min($rr2,[Math]::Min($gg,$bb))
      $ok=$false
      if($mode -eq 'gray'){ if($mx -ge 224 -and $mx -le 245 -and ($mx-$mn) -le 10){$ok=$true} }
      else { if($mx -ge 247 -and ($mx-$mn) -le 8){$ok=$true} }
      if($ok){$fg[$y*$AW+$x]=$true}}}
  $out=New-Object System.Drawing.Bitmap $AW,$AH
  $go=[System.Drawing.Graphics]::FromImage($out)
  $go.DrawImage($img,0,0,$AW,$AH);$img.Dispose()
  $go.SmoothingMode=[System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $penRed=New-Object System.Drawing.Pen ([System.Drawing.Color]::Red),3
  $visited=New-Object 'bool[]' $N
  $st=New-Object System.Collections.Generic.Stack[int]
  $emitted=@()
  Write-Host "==== $id ($mode) ===="
  for($i=0;$i -lt $N;$i++){
    if($fg[$i] -and -not $visited[$i]){
      $st.Clear();$st.Push($i);$visited[$i]=$true
      $cnt=0;$tx=0;$ty=99999;$bx=0;$by=-1;$lx=99999;$ly=0;$rx=-1;$ry=0
      $minX=$AW;$minY=$AH;$maxX=0;$maxY=0
      while($st.Count -gt 0){$q=$st.Pop();$cnt++;$qx=$q%$AW;$qy=[int]($q/$AW)
        if($qy -lt $ty){$ty=$qy;$tx=$qx}; if($qy -gt $by){$by=$qy;$bx=$qx}
        if($qx -lt $lx){$lx=$qx;$ly=$qy}; if($qx -gt $rx){$rx=$qx;$ry=$qy}
        if($qx -lt $minX){$minX=$qx};if($qx -gt $maxX){$maxX=$qx};if($qy -lt $minY){$minY=$qy};if($qy -gt $maxY){$maxY=$qy}
        if($qx -gt 0){$w=$q-1;if($fg[$w]-and-not $visited[$w]){$visited[$w]=$true;$st.Push($w)}}
        if($qx -lt $AW-1){$w=$q+1;if($fg[$w]-and-not $visited[$w]){$visited[$w]=$true;$st.Push($w)}}
        if($qy -gt 0){$w=$q-$AW;if($fg[$w]-and-not $visited[$w]){$visited[$w]=$true;$st.Push($w)}}
        if($qy -lt $AH-1){$w=$q+$AW;if($fg[$w]-and-not $visited[$w]){$visited[$w]=$true;$st.Push($w)}}}
      $bw=$maxX-$minX+1;$bh=$maxY-$minY+1;$areaFrac=$cnt/[double]$N
      if($areaFrac -lt 0.02 -or $areaFrac -gt 0.5){continue}
      $fill=$cnt/[double]($bw*$bh)
      if($fill -lt $minFill){continue}
      # two adjacent edges from corner T: T->R and T->L
      $e1x=$rx-$tx;$e1y=$ry-$ty; $e2x=$lx-$tx;$e2y=$ly-$ty
      $len1=[Math]::Sqrt($e1x*$e1x+$e1y*$e1y); $len2=[Math]::Sqrt($e2x*$e2x+$e2y*$e2y)
      # pick more-horizontal edge as width axis
      if([Math]::Abs($e1x)/[Math]::Max($len1,1) -ge [Math]::Abs($e2x)/[Math]::Max($len2,1)){
        $wx=$e1x;$wy=$e1y;$wlen=$len1;$hlen=$len2
      } else { $wx=$e2x;$wy=$e2y;$wlen=$len2;$hlen=$len1 }
      if($wx -lt 0){$wx=-$wx;$wy=-$wy}   # point rightward
      $theta=[Math]::Atan2($wy,$wx)*180/[Math]::PI
      $cxp=($tx+$rx+$bx+$lx)/4.0; $cyp=($ty+$ry+$by+$ly)/4.0
      # to logic
      $cxL=[int][math]::Round($cxp/$SX); $cyL=[int][math]::Round($cyp/$SY)
      $wL=[int][math]::Round($wlen/$SX); $hL=[int][math]::Round($hlen/$SY)
      $x0=$cxL-[int]($wL/2); $y0=$cyL-[int]($hL/2); $x1=$cxL+[int]($wL/2); $y1=$cyL+[int]($hL/2)
      $emitted+=,@($x0,$y0,$x1,$y1,[math]::Round($theta,1))
      # draw rotated rect in display space
      $gs=$go.Save()
      $go.TranslateTransform([float]$cxp,[float]$cyp)
      $go.RotateTransform([float]$theta)
      $go.DrawRectangle($penRed,[float](-$wlen/2),[float](-$hlen/2),[float]$wlen,[float]$hlen)
      $go.Restore($gs)
      Write-Host ("  cell=({0},{1},{2},{3}) angle={4:N1}  (w={5} h={6})" -f $x0,$y0,$x1,$y1,$theta,$wL,$hL)
    }
  }
  $go.Dispose()
  $eps=New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]90)
  $out.Save((Join-Path $outDir "$id.jpg"),$jpgEnc,$eps)
  $eps.Dispose();$out.Dispose()
  # emit config line
  $parts=@(); foreach($c in $emitted){$parts+=("@({0},{1},{2},{3},{4})" -f $c[0],$c[1],$c[2],$c[3],$c[4])}
  Write-Host ('  CONFIG "{0}"=@({1})' -f $id, ($parts -join ','))
}
