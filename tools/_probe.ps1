# Probe: dump ALL gray/white components (no area filter) + PCA angle. ASCII only.
Add-Type -AssemblyName System.Drawing
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$LOGIC_W=1125; $LOGIC_H=2000; $AW=562; $AH=1000
$mode=$args[0]   # 'gray' or 'white'
$ids=@($args[1..($args.Count-1)])
foreach($id in $ids){
  $p=Join-Path $resDir "temp_$id.jpg"
  if(-not (Test-Path $p)){ Write-Host "$id no img"; continue }
  $img=[System.Drawing.Image]::FromFile($p)
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
  $N=$AW*$AH; $fg=New-Object 'bool[]' $N
  for($y=0;$y -lt $AH;$y++){$row=$y*$stride
    for($x=0;$x -lt $AW;$x++){$o=$row+$x*3;$b=$buf[$o];$gr=$buf[$o+1];$r=$buf[$o+2]
      $mx=[Math]::Max($r,[Math]::Max($gr,$b));$mn=[Math]::Min($r,[Math]::Min($gr,$b))
      $ok=$false
      if($mode -eq 'gray'){ if($mx -ge 224 -and $mx -le 245 -and ($mx-$mn) -le 10){$ok=$true} }
      else { if($mx -ge 247 -and ($mx-$mn) -le 8){$ok=$true} }
      if($ok){$fg[$y*$AW+$x]=$true}}}
  $visited=New-Object 'bool[]' $N
  $st=New-Object System.Collections.Generic.Stack[int]
  Write-Host "==== $id ($mode) ===="
  for($i=0;$i -lt $N;$i++){
    if($fg[$i] -and -not $visited[$i]){
      $st.Clear();$st.Push($i);$visited[$i]=$true
      $minX=$AW;$minY=$AH;$maxX=0;$maxY=0;$cnt=0
      $sx=0.0;$sy=0.0;$sxx=0.0;$syy=0.0;$sxy=0.0
      while($st.Count -gt 0){$q=$st.Pop();$cnt++;$qx=$q%$AW;$qy=[int]($q/$AW)
        if($qx -lt $minX){$minX=$qx};if($qx -gt $maxX){$maxX=$qx};if($qy -lt $minY){$minY=$qy};if($qy -gt $maxY){$maxY=$qy}
        $sx+=$qx;$sy+=$qy;$sxx+=$qx*$qx;$syy+=$qy*$qy;$sxy+=$qx*$qy
        if($qx -gt 0){$w=$q-1;if($fg[$w]-and-not $visited[$w]){$visited[$w]=$true;$st.Push($w)}}
        if($qx -lt $AW-1){$w=$q+1;if($fg[$w]-and-not $visited[$w]){$visited[$w]=$true;$st.Push($w)}}
        if($qy -gt 0){$w=$q-$AW;if($fg[$w]-and-not $visited[$w]){$visited[$w]=$true;$st.Push($w)}}
        if($qy -lt $AH-1){$w=$q+$AW;if($fg[$w]-and-not $visited[$w]){$visited[$w]=$true;$st.Push($w)}}}
      $bw=$maxX-$minX+1;$bh=$maxY-$minY+1;$areaFrac=$cnt/[double]$N
      if($areaFrac -lt 0.008){continue}
      # PCA angle
      $mxx=$sxx/$cnt-($sx/$cnt)*($sx/$cnt);$myy=$syy/$cnt-($sy/$cnt)*($sy/$cnt);$mxy=$sxy/$cnt-($sx/$cnt)*($sy/$cnt)
      $ang=0.5*[Math]::Atan2(2*$mxy,$mxx-$myy)*180/[Math]::PI
      # to logic coords
      $lx0=[int][math]::Round($minX*$LOGIC_W/$AW);$ly0=[int][math]::Round($minY*$LOGIC_H/$AH)
      $lx1=[int][math]::Round(($maxX+1)*$LOGIC_W/$AW);$ly1=[int][math]::Round(($maxY+1)*$LOGIC_H/$AH)
      $fill=$cnt/[double]($bw*$bh)
      Write-Host ("  aabb=({0},{1},{2},{3}) area%={4:N3} fill={5:N2} angle={6:N1}" -f $lx0,$ly0,$lx1,$ly1,($areaFrac*100),$fill,$ang)
    }
  }
}
