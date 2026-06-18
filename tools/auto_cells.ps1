# Auto-detect gray photo cells (no seeds): nearest-neighbor downscale + tight neutral
# band + connected components + filters. Avoids JPEG-bridge spill. ASCII only.
Add-Type -AssemblyName System.Drawing
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$LOGIC_W=1125; $LOGIC_H=2000; $AW=375; $AH=667
$ids=@($args)
$result=[ordered]@{}
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
  $N=$AW*$AH; $white=New-Object 'bool[]' $N
  for($y=0;$y -lt $AH;$y++){$row=$y*$stride
    for($x=0;$x -lt $AW;$x++){$o=$row+$x*3;$b=$buf[$o];$gr=$buf[$o+1];$r=$buf[$o+2]
      $mx=[Math]::Max($r,[Math]::Max($gr,$b));$mn=[Math]::Min($r,[Math]::Min($gr,$b))
      if($mx -ge 228 -and $mx -le 242 -and ($mx-$mn) -le 8){$white[$y*$AW+$x]=$true}}}
  $visited=New-Object 'bool[]' $N; $cells=@()
  $st=New-Object System.Collections.Generic.Stack[int]
  for($i=0;$i -lt $N;$i++){
    if($white[$i] -and -not $visited[$i]){
      $st.Clear();$st.Push($i);$visited[$i]=$true
      $minX=$AW;$minY=$AH;$maxX=0;$maxY=0;$cnt=0
      while($st.Count -gt 0){$q=$st.Pop();$cnt++;$qx=$q%$AW;$qy=[int]($q/$AW)
        if($qx -lt $minX){$minX=$qx};if($qx -gt $maxX){$maxX=$qx};if($qy -lt $minY){$minY=$qy};if($qy -gt $maxY){$maxY=$qy}
        if($qx -gt 0){$w=$q-1;if($white[$w]-and-not $visited[$w]){$visited[$w]=$true;$st.Push($w)}}
        if($qx -lt $AW-1){$w=$q+1;if($white[$w]-and-not $visited[$w]){$visited[$w]=$true;$st.Push($w)}}
        if($qy -gt 0){$w=$q-$AW;if($white[$w]-and-not $visited[$w]){$visited[$w]=$true;$st.Push($w)}}
        if($qy -lt $AH-1){$w=$q+$AW;if($white[$w]-and-not $visited[$w]){$visited[$w]=$true;$st.Push($w)}}}
      $bw=$maxX-$minX+1;$bh=$maxY-$minY+1;$fill=$cnt/[double]($bw*$bh);$areaFrac=($bw*$bh)/[double]$N
      $asp=$bw/[double]$bh
      if($areaFrac -gt 0.015 -and $areaFrac -lt 0.85 -and $fill -gt 0.7 -and $asp -gt 0.2 -and $asp -lt 5){
        $cells+=,@([int][math]::Round($minX*$LOGIC_W/$AW),[int][math]::Round($minY*$LOGIC_H/$AH),[int][math]::Round(($maxX+1)*$LOGIC_W/$AW),[int][math]::Round(($maxY+1)*$LOGIC_H/$AH))
      }
    }
  }
  $cells=@($cells | Sort-Object @{e={$_[1]}},@{e={$_[0]}})
  $result[$id]=$cells
  $parts=@(); foreach($c in $cells){$parts+=("@({0},{1},{2},{3})" -f $c[0],$c[1],$c[2],$c[3])}
  Write-Host ('  "{0}"=@({1})   # {2} cells' -f $id, ($parts -join ','), @($cells).Count)
}
