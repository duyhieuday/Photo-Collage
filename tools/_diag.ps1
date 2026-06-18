# Diagnostic: detect frame corners (tilted rect via extreme points), draw detected quad (RED)
# vs current baked cells (LIME). ASCII only.
Add-Type -AssemblyName System.Drawing
. "D:\EZTech\EZTechApp\collage_pic_editor\tools\cells_config.ps1"
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\diag"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$LOGIC_W=1125; $LOGIC_H=2000; $AW=562; $AH=1000
$SX=$AW/$LOGIC_W; $SY=$AH/$LOGIC_H
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
$mode=$args[0]
$ids=@($args[1..($args.Count-1)])
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
  # draw base
  $out=New-Object System.Drawing.Bitmap $AW,$AH
  $go=[System.Drawing.Graphics]::FromImage($out)
  $go.DrawImage($img,0,0,$AW,$AH);$img.Dispose()
  $go.SmoothingMode=[System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $penRed=New-Object System.Drawing.Pen ([System.Drawing.Color]::Red),3
  $penLime=New-Object System.Drawing.Pen ([System.Drawing.Color]::Lime),2
  $penLime.DashStyle=[System.Drawing.Drawing2D.DashStyle]::Dash
  # detect components, extract extreme-point corners
  $visited=New-Object 'bool[]' $N
  $st=New-Object System.Collections.Generic.Stack[int]
  Write-Host "==== $id ($mode) ===="
  for($i=0;$i -lt $N;$i++){
    if($fg[$i] -and -not $visited[$i]){
      $st.Clear();$st.Push($i);$visited[$i]=$true
      $cnt=0
      # extreme points: top(min y), bottom(max y), left(min x), right(max x)
      $tx=0;$ty=99999;$bx=0;$by=-1;$lx=99999;$ly=0;$rx=-1;$ry=0
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
      if($areaFrac -lt 0.02 -or $areaFrac -gt 0.6){continue}
      $fill=$cnt/[double]($bw*$bh)
      # draw detected quad from 4 extreme corners (top,right,bottom,left)
      $pts=@(
        (New-Object System.Drawing.PointF ([float]$tx,[float]$ty)),
        (New-Object System.Drawing.PointF ([float]$rx,[float]$ry)),
        (New-Object System.Drawing.PointF ([float]$bx,[float]$by)),
        (New-Object System.Drawing.PointF ([float]$lx,[float]$ly)))
      $go.DrawPolygon($penRed,$pts)
      # logic corners
      $L=@{tx=[int][math]::Round($tx/$SX);ty=[int][math]::Round($ty/$SY);rx=[int][math]::Round($rx/$SX);ry=[int][math]::Round($ry/$SY);bx=[int][math]::Round($bx/$SX);by=[int][math]::Round($by/$SY);lx=[int][math]::Round($lx/$SX);ly=[int][math]::Round($ly/$SY)}
      $angle=[Math]::Atan2(($L.rx-$L.tx),($L.ry-$L.ty))*180/[Math]::PI
      Write-Host ("  corners T({0},{1}) R({2},{3}) B({4},{5}) L({6},{7}) fill={8:N2} tilt~{9:N1}" -f $L.tx,$L.ty,$L.rx,$L.ry,$L.bx,$L.by,$L.lx,$L.ly,$fill,$angle)
    }
  }
  # draw current baked cells (lime dashed)
  $rr=$CELLS[$id]
  if($rr -and $rr.Count -ge 1 -and ($rr[0] -is [int] -or $rr[0] -is [double])){ $rr=@(,$rr) }
  if($rr){ foreach($c in $rr){
    $rx=$c[0]*$SX; $ry=$c[1]*$SY; $rw=($c[2]-$c[0])*$SX; $rh=($c[3]-$c[1])*$SY
    $go.DrawRectangle($penLime,[float]$rx,[float]$ry,[float]$rw,[float]$rh) } }
  $go.Dispose()
  $eps=New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]90)
  $out.Save((Join-Path $outDir "$id.jpg"),$jpgEnc,$eps)
  $eps.Dispose();$out.Dispose()
  Write-Host "  -> diag/$id.jpg"
}
