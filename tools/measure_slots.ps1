# Measure photo slots from Temp_ by colour band + erosion, reporting a min-area ROTATED rect.
#
# Why this beats the earlier tools on hard templates:
#  - measures the SLOT in Temp_, so it works even where Thumb_ left a slot empty;
#  - erodes first, which snaps the thin bridges (stickers, anti-aliased seams, a shared corner)
#    that otherwise fuse neighbouring slots into one blob whose min-area-rect angle is garbage;
#  - erosion cannot rotate anything, so the ANGLE stays exact; the shaved margin is added back.
#  - reports fill, so a merged blob (low fill) is distinguishable from a real slot (fill ~0.9+).
#
# On bd13 this cleanly separated 4 strongly-tilted polaroids at fill 0.80-0.97 and confirmed
# angles -21.8 / 10.8 / 5.8 / -19.8, after diff-based and frame-based scans had both failed.
#
# LESSON: trust a high-fill measurement over eyeballing an overlay. Judging "that tilt looks too
# strong" by eye once led to reverting a correct angle and making the template worse.
#
# Usage: powershell -File tools\measure_slots.ps1 -Ids bd13 [-Lo 228] [-Hi 242] [-Sat 8]
#                   [-Erode 3] [-MinPx 3000]

param(
  [string[]]$Ids = @(),
  [int]$Lo = 228, [int]$Hi = 242, [int]$Sat = 8,
  [int]$Erode = 3,
  [int]$MinPx = 3000,
  [int]$AW = 560
)

Add-Type -AssemblyName System.Drawing
$SRC="D:\EZTech\AppAssets\PhotoCollage\Category_Template"
$CATS=@{ 'bd'='Birthday';'cp'='Couple';'gs'='Glad season';'is'='IG Story';'sm'='Summer vibe';'sp'='Sports' }
$LW=1125;$LH=2000
$AH=[int][math]::Round($AW*16.0/9.0); $N=$AW*$AH

$Ids = @($Ids | ForEach-Object { $_ -split ',' } | Where-Object { $_ -match '\S' } | ForEach-Object { $_.Trim().ToLower() })
if ($Ids.Count -eq 0) { Write-Host "No -Ids."; exit 1 }

foreach ($id in $Ids) {
  $pre=$id.Substring(0,2);$nn=$id.Substring(2)
  $p=Join-Path $SRC ("{0}\Temp_{1}{2}.png" -f $CATS[$pre],$pre.ToUpper(),$nn)
  if(-not (Test-Path $p)){ Write-Host "$id : khong co Temp_"; continue }

  $img=[System.Drawing.Image]::FromFile($p)
  $bm=New-Object System.Drawing.Bitmap $AW,$AH
  $g=[System.Drawing.Graphics]::FromImage($bm)
  $g.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.DrawImage($img,0,0,$AW,$AH);$g.Dispose();$img.Dispose()
  $r=New-Object System.Drawing.Rectangle 0,0,$AW,$AH
  $d=$bm.LockBits($r,[System.Drawing.Imaging.ImageLockMode]::ReadOnly,[System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
  $stride=$d.Stride;$buf=New-Object byte[] ($stride*$AH)
  [System.Runtime.InteropServices.Marshal]::Copy($d.Scan0,$buf,0,$buf.Length)
  $bm.UnlockBits($d);$bm.Dispose()

  $mask=New-Object 'bool[]' $N
  for($y=0;$y -lt $AH;$y++){ $row=$y*$stride;$base=$y*$AW
    for($x=0;$x -lt $AW;$x++){ $o=$row+$x*3
      $b=$buf[$o];$gg=$buf[$o+1];$rr=$buf[$o+2]
      $mx=$rr;if($gg -gt $mx){$mx=$gg};if($b -gt $mx){$mx=$b}
      $mn=$rr;if($gg -lt $mn){$mn=$gg};if($b -lt $mn){$mn=$b}
      if($mx -ge $Lo -and $mx -le $Hi -and ($mx-$mn) -le $Sat){$mask[$base+$x]=$true}
    }}

  for($e=0;$e -lt $Erode;$e++){
    $er=New-Object 'bool[]' $N
    for($y=1;$y -lt $AH-1;$y++){ for($x=1;$x -lt $AW-1;$x++){ $i=$y*$AW+$x
      if($mask[$i] -and $mask[$i-1] -and $mask[$i+1] -and $mask[$i-$AW] -and $mask[$i+$AW]){$er[$i]=$true}
    }}
    $mask=$er
  }

  $vis=New-Object 'bool[]' $N;$st=New-Object System.Collections.Generic.Stack[int]
  $sx=$LW/[double]$AW;$sy=$LH/[double]$AH
  $out=@()
  for($i=0;$i -lt $N;$i++){
    if(-not $mask[$i] -or $vis[$i]){continue}
    $st.Clear();$st.Push($i);$vis[$i]=$true;$cnt=0
    $bxs=New-Object System.Collections.Generic.List[int];$bys=New-Object System.Collections.Generic.List[int]
    while($st.Count -gt 0){
      $q=$st.Pop();$cnt++
      $qx=$q%$AW;$qy=[int]($q/$AW);$isB=$false
      if($qx -eq 0 -or $qx -eq $AW-1 -or $qy -eq 0 -or $qy -eq $AH-1){$isB=$true}
      elseif(-not $mask[$q-1] -or -not $mask[$q+1] -or -not $mask[$q-$AW] -or -not $mask[$q+$AW]){$isB=$true}
      if($isB){$bxs.Add($qx);$bys.Add($qy)}
      if($qx -gt 0){$n2=$q-1;if($mask[$n2]-and-not $vis[$n2]){$vis[$n2]=$true;$st.Push($n2)}}
      if($qx -lt $AW-1){$n2=$q+1;if($mask[$n2]-and-not $vis[$n2]){$vis[$n2]=$true;$st.Push($n2)}}
      if($qy -gt 0){$n2=$q-$AW;if($mask[$n2]-and-not $vis[$n2]){$vis[$n2]=$true;$st.Push($n2)}}
      if($qy -lt $AH-1){$n2=$q+$AW;if($mask[$n2]-and-not $vis[$n2]){$vis[$n2]=$true;$st.Push($n2)}}
    }
    if($cnt -lt $MinPx){continue}
    $ba=[double]::MaxValue;$best=$null
    for($deg=-45.0;$deg -lt 45.0;$deg+=0.25){
      $rad=$deg*[Math]::PI/180;$cos=[Math]::Cos($rad);$sin=[Math]::Sin($rad)
      $u0=1e9;$u1=-1e9;$v0=1e9;$v1=-1e9
      for($k=0;$k -lt $bxs.Count;$k++){
        $u=$bxs[$k]*$cos+$bys[$k]*$sin;$v=-$bxs[$k]*$sin+$bys[$k]*$cos
        if($u -lt $u0){$u0=$u};if($u -gt $u1){$u1=$u};if($v -lt $v0){$v0=$v};if($v -gt $v1){$v1=$v}}
      $a=($u1-$u0)*($v1-$v0)
      if($a -lt $ba){$ba=$a;$best=@{deg=$deg;u0=$u0;u1=$u1;v0=$v0;v1=$v1}}}
    $w=$best.u1-$best.u0;$h=$best.v1-$best.v0
    $uc=($best.u0+$best.u1)/2;$vc=($best.v0+$best.v1)/2
    $rad=$best.deg*[Math]::PI/180;$cos=[Math]::Cos($rad);$sin=[Math]::Sin($rad)
    $cx=($uc*$cos-$vc*$sin);$cy=($uc*$sin+$vc*$cos)
    $fill=$cnt/[double]([Math]::Max(1.0,$w*$h))
    $lw=($w+2*$Erode)*$sx;$lh=($h+2*$Erode)*$sy;$lcx=$cx*$sx;$lcy=$cy*$sy
    $out += [pscustomobject]@{
      l=[int]($lcx-$lw/2);t=[int]($lcy-$lh/2);r=[int]($lcx+$lw/2);b=[int]($lcy+$lh/2)
      deg=[math]::Round($best.deg,1);fill=[math]::Round($fill,2);px=$cnt
    }
  }
  $out=@($out | Sort-Object t,l)
  Write-Host ""
  Write-Host "==== $id ====  (fill >= 0.85 = o that; thap hon = con dinh nhau, tang -Erode)"
  $k=0;$parts=@()
  foreach($o in $out){
    $k++
    Write-Host ("   {0}. deg={1,6} fill={2,5} px={3,7}  ({4},{5},{6},{7})  {8}x{9}" -f $k,$o.deg,$o.fill,$o.px,$o.l,$o.t,$o.r,$o.b,($o.r-$o.l),($o.b-$o.t))
    if([math]::Abs($o.deg) -ge 0.3){$parts+=("@({0},{1},{2},{3},{4})" -f $o.l,$o.t,$o.r,$o.b,$o.deg)}
    else{$parts+=("@({0},{1},{2},{3})" -f $o.l,$o.t,$o.r,$o.b)}
  }
  if($parts.Count){ Write-Host ('   "{0}"=@({1})' -f $id,($parts -join ',')) }
}
