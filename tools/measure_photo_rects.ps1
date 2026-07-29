# Measure each photo's rotated rect from Thumb_ by finding the COLOURFUL regions.
#
# For a MaskMode.NONE template the cellRect IS the photo, so the designer's photo placement in
# Thumb_ is the target. On designs where the card stock / background is near-neutral (white
# polaroid mounts, grid paper, cream card) and the photos are colour, saturation separates them
# cleanly - unlike the grey-band scan, which merges touching cards and then returns a garbage
# min-area-rect angle (that is how bd13 ended up with -21.5 deg instead of ~-8).
#
# Usage: powershell -File tools\measure_photo_rects.ps1 -Ids bd13 [-MinSat 28] [-MinSidePx 40]

param(
  [string[]]$Ids = @(),
  [int]$MinSat = 28,        # max-min channel spread that counts as "colour, not paper"
  [int]$MinSidePx = 40,     # analysis px; drops stickers, letters, small flowers
  [int]$AW = 450
)

Add-Type -AssemblyName System.Drawing
$SRC = "D:\EZTech\AppAssets\PhotoCollage\Category_Template"
$CATS = @{ 'bd'='Birthday'; 'cp'='Couple'; 'gs'='Glad season'; 'is'='IG Story'; 'sm'='Summer vibe'; 'sp'='Sports' }
$LW = 1125; $LH = 2000
$AH = [int][math]::Round($AW * 16.0/9.0)
$N = $AW*$AH

function Fit-MinRect($bxs, $bys) {
  $np=$bxs.Count; $bestArea=[double]::MaxValue; $best=$null
  for ($deg=-45.0; $deg -lt 45.0; $deg+=0.25) {
    $rad=$deg*[Math]::PI/180.0; $cos=[Math]::Cos($rad); $sin=[Math]::Sin($rad)
    $minU=1e9;$maxU=-1e9;$minV=1e9;$maxV=-1e9
    for ($k=0;$k -lt $np;$k++) {
      $u=$bxs[$k]*$cos+$bys[$k]*$sin; $v=-$bxs[$k]*$sin+$bys[$k]*$cos
      if($u -lt $minU){$minU=$u}; if($u -gt $maxU){$maxU=$u}
      if($v -lt $minV){$minV=$v}; if($v -gt $maxV){$maxV=$v}
    }
    $a=($maxU-$minU)*($maxV-$minV)
    if($a -lt $bestArea){$bestArea=$a;$best=@{deg=$deg;u0=$minU;u1=$maxU;v0=$minV;v1=$maxV}}
  }
  $w=$best.u1-$best.u0; $h=$best.v1-$best.v0
  $uc=($best.u0+$best.u1)/2.0; $vc=($best.v0+$best.v1)/2.0
  $rad=$best.deg*[Math]::PI/180.0; $cos=[Math]::Cos($rad); $sin=[Math]::Sin($rad)
  return @{ cx=($uc*$cos-$vc*$sin); cy=($uc*$sin+$vc*$cos); w=$w; h=$h; deg=$best.deg }
}

$Ids = @($Ids | ForEach-Object { $_ -split ',' } | Where-Object { $_ -match '\S' } | ForEach-Object { $_.Trim().ToLower() })
if ($Ids.Count -eq 0) { Write-Host "No -Ids."; exit 1 }

foreach ($id in $Ids) {
  $pre=$id.Substring(0,2); $nn=$id.Substring(2)
  $p = Join-Path $SRC ("{0}\Thumb_{1}{2}.png" -f $CATS[$pre], $pre.ToUpper(), $nn)
  if (-not (Test-Path $p)) { Write-Host "$id : khong co Thumb_"; continue }

  $img=[System.Drawing.Image]::FromFile($p)
  $bm=New-Object System.Drawing.Bitmap $AW,$AH
  $g=[System.Drawing.Graphics]::FromImage($bm)
  $g.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.DrawImage($img,0,0,$AW,$AH); $g.Dispose(); $img.Dispose()
  $r=New-Object System.Drawing.Rectangle 0,0,$AW,$AH
  $d=$bm.LockBits($r,[System.Drawing.Imaging.ImageLockMode]::ReadOnly,[System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
  $stride=$d.Stride; $buf=New-Object byte[] ($stride*$AH)
  [System.Runtime.InteropServices.Marshal]::Copy($d.Scan0,$buf,0,$buf.Length)
  $bm.UnlockBits($d); $bm.Dispose()

  $mask=New-Object 'bool[]' $N
  for($y=0;$y -lt $AH;$y++){ $row=$y*$stride; $base=$y*$AW
    for($x=0;$x -lt $AW;$x++){ $o=$row+$x*3
      $b=$buf[$o];$gr=$buf[$o+1];$rr2=$buf[$o+2]
      $mx=$rr2; if($gr -gt $mx){$mx=$gr}; if($b -gt $mx){$mx=$b}
      $mn=$rr2; if($gr -lt $mn){$mn=$gr}; if($b -lt $mn){$mn=$b}
      if (($mx-$mn) -ge $MinSat -or $mx -lt 150) { $mask[$base+$x]=$true }
    }}
  # close 1px gaps so highlights inside a photo do not fragment it
  $tmp=New-Object 'bool[]' $N
  for($y=1;$y -lt $AH-1;$y++){ for($x=1;$x -lt $AW-1;$x++){
    $i=$y*$AW+$x
    if($mask[$i]){$tmp[$i]=$true;continue}
    if(($mask[$i-1] -and $mask[$i+1]) -or ($mask[$i-$AW] -and $mask[$i+$AW])){$tmp[$i]=$true}
  }}
  $mask=$tmp

  $vis=New-Object 'bool[]' $N
  $st=New-Object System.Collections.Generic.Stack[int]
  Write-Host ""
  Write-Host "==== $id ===="
  $out=@()
  for($i=0;$i -lt $N;$i++){
    if(-not $mask[$i] -or $vis[$i]){continue}
    $st.Clear();$st.Push($i);$vis[$i]=$true
    $mnX=$AW;$mnY=$AH;$mxX=0;$mxY=0;$cnt=0
    $bxs=New-Object System.Collections.Generic.List[int]
    $bys=New-Object System.Collections.Generic.List[int]
    while($st.Count -gt 0){
      $q=$st.Pop();$cnt++
      $qx=$q%$AW;$qy=[int]($q/$AW)
      if($qx -lt $mnX){$mnX=$qx};if($qx -gt $mxX){$mxX=$qx}
      if($qy -lt $mnY){$mnY=$qy};if($qy -gt $mxY){$mxY=$qy}
      $isB=$false
      if($qx -eq 0 -or $qx -eq $AW-1 -or $qy -eq 0 -or $qy -eq $AH-1){$isB=$true}
      elseif(-not $mask[$q-1] -or -not $mask[$q+1] -or -not $mask[$q-$AW] -or -not $mask[$q+$AW]){$isB=$true}
      if($isB){$bxs.Add($qx);$bys.Add($qy)}
      if($qx -gt 0){$n2=$q-1;if($mask[$n2]-and-not $vis[$n2]){$vis[$n2]=$true;$st.Push($n2)}}
      if($qx -lt $AW-1){$n2=$q+1;if($mask[$n2]-and-not $vis[$n2]){$vis[$n2]=$true;$st.Push($n2)}}
      if($qy -gt 0){$n2=$q-$AW;if($mask[$n2]-and-not $vis[$n2]){$vis[$n2]=$true;$st.Push($n2)}}
      if($qy -lt $AH-1){$n2=$q+$AW;if($mask[$n2]-and-not $vis[$n2]){$vis[$n2]=$true;$st.Push($n2)}}
    }
    $bw=$mxX-$mnX+1;$bh=$mxY-$mnY+1
    if([Math]::Min($bw,$bh) -lt $MinSidePx){continue}
    $mr = Fit-MinRect $bxs $bys
    $fill = $cnt/[double]([Math]::Max(1.0,$mr.w*$mr.h))
    $sx=$LW/[double]$AW; $sy=$LH/[double]$AH
    $lw=$mr.w*$sx; $lh=$mr.h*$sy; $lcx=$mr.cx*$sx; $lcy=$mr.cy*$sy
    $deg=[math]::Round($mr.deg,1)
    $out += [pscustomobject]@{
      l=[int]($lcx-$lw/2); t=[int]($lcy-$lh/2); r=[int]($lcx+$lw/2); b=[int]($lcy+$lh/2)
      deg=$deg; fill=[math]::Round($fill,2); px=$cnt
    }
  }
  $out=@($out | Sort-Object t,l)
  $k=0; $parts=@()
  foreach($o in $out){
    $k++
    Write-Host ("   {0}. deg={1,6} fill={2,5} ({3},{4},{5},{6})  {7}x{8}" -f $k,$o.deg,$o.fill,$o.l,$o.t,$o.r,$o.b,($o.r-$o.l),($o.b-$o.t))
    if([math]::Abs($o.deg) -ge 0.3){$parts+=("@({0},{1},{2},{3},{4})" -f $o.l,$o.t,$o.r,$o.b,$o.deg)}
    else{$parts+=("@({0},{1},{2},{3})" -f $o.l,$o.t,$o.r,$o.b)}
  }
  if($parts.Count){ Write-Host ('   "{0}"=@({1})' -f $id, ($parts -join ',')) }
}
