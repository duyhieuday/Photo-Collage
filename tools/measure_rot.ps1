# Detect large WHITE frame regions in temp_<id>.jpg as ROTATED rects.
# Output each as (L,T,R,B,angle) in logic 1125x2000 (unrotated rect about center + tilt). ASCII only.
Add-Type -AssemblyName System.Drawing

$id = $args[0]; if (-not $id) { $id = "cp01" }
$thr = 244; if ($args.Count -ge 2) { $thr = [int]$args[1] }

$path = "D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable\temp_$id.jpg"
$src = [System.Drawing.Bitmap]::FromFile($path)
$OW = $src.Width; $OH = $src.Height
$dW = 562; $dH = [int][math]::Round($dW * $OH / $OW)
$bmp = New-Object System.Drawing.Bitmap $dW, $dH
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.DrawImage($src, 0, 0, $dW, $dH); $g.Dispose(); $src.Dispose()

$rect = New-Object System.Drawing.Rectangle 0,0,$dW,$dH
$data = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
$stride = $data.Stride
$buf = New-Object byte[] ($stride * $dH)
[System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $buf, 0, $buf.Length)
$bmp.UnlockBits($data); $bmp.Dispose()

$N = $dW * $dH
$white = New-Object bool[] $N
for ($y = 0; $y -lt $dH; $y++) {
  $row = $y * $stride
  for ($x = 0; $x -lt $dW; $x++) {
    $o = $row + $x * 3
    if ($buf[$o] -ge $thr -and $buf[$o+1] -ge $thr -and $buf[$o+2] -ge $thr) { $white[$y*$dW+$x] = $true }
  }
}

$lab = New-Object int[] $N
$comp = 0
$stX = New-Object int[] $N
$stY = New-Object int[] $N
$sLX = 1125.0 / $dW; $sLY = 2000.0 / $dH
$results = @()
for ($i = 0; $i -lt $N; $i++) {
  if ($white[$i] -and $lab[$i] -eq 0) {
    $comp++; $sp = 0
    $stX[0] = $i % $dW; $stY[0] = [int][math]::Floor($i / $dW); $sp = 1; $lab[$i] = $comp
    $area = 0; $minX=99999;$maxX=-1;$minY=99999;$maxY=-1
    $tx=0;$ty=0;$bx=0;$by=0;$lx=0;$ly=0;$rx=0;$ry=0
    while ($sp -gt 0) {
      $sp--; $cx=$stX[$sp]; $cy=$stY[$sp]; $area++
      if ($cy -lt $minY){$minY=$cy;$tx=$cx;$ty=$cy}
      if ($cy -gt $maxY){$maxY=$cy;$bx=$cx;$by=$cy}
      if ($cx -lt $minX){$minX=$cx;$lx=$cx;$ly=$cy}
      if ($cx -gt $maxX){$maxX=$cx;$rx=$cx;$ry=$cy}
      $nx=$cx+1; if($nx -lt $dW){$ni=$cy*$dW+$nx; if($white[$ni] -and $lab[$ni] -eq 0){$lab[$ni]=$comp;$stX[$sp]=$nx;$stY[$sp]=$cy;$sp++}}
      $nx=$cx-1; if($nx -ge 0){$ni=$cy*$dW+$nx; if($white[$ni] -and $lab[$ni] -eq 0){$lab[$ni]=$comp;$stX[$sp]=$nx;$stY[$sp]=$cy;$sp++}}
      $ny=$cy+1; if($ny -lt $dH){$ni=$ny*$dW+$cx; if($white[$ni] -and $lab[$ni] -eq 0){$lab[$ni]=$comp;$stX[$sp]=$cx;$stY[$sp]=$ny;$sp++}}
      $ny=$cy-1; if($ny -ge 0){$ni=$ny*$dW+$cx; if($white[$ni] -and $lab[$ni] -eq 0){$lab[$ni]=$comp;$stX[$sp]=$cx;$stY[$sp]=$ny;$sp++}}
    }
    if ($area -gt 1500) {
      $results += [pscustomobject]@{ area=[int]$area; tx=[int]$tx; ty=[int]$ty; bx=[int]$bx; by=[int]$by; lx=[int]$lx; ly=[int]$ly; rx=[int]$rx; ry=[int]$ry }
    }
  }
}

$results = $results | Sort-Object area -Descending | Select-Object -First 6
Write-Host "id=$id detSize=${dW}x${dH} origin=${OW}x${OH} thr=$thr"
foreach ($q in $results) {
  $Tx=[double]$q.tx*$sLX; $Ty=[double]$q.ty*$sLY
  $Bx=[double]$q.bx*$sLX; $By=[double]$q.by*$sLY
  $Lx=[double]$q.lx*$sLX; $Ly=[double]$q.ly*$sLY
  $Rx=[double]$q.rx*$sLX; $Ry=[double]$q.ry*$sLY
  $eRx=$Rx-$Tx; $eRy=$Ry-$Ty; $eLx=$Lx-$Tx; $eLy=$Ly-$Ty
  $lenR=[math]::Sqrt($eRx*$eRx+$eRy*$eRy); $lenL=[math]::Sqrt($eLx*$eLx+$eLy*$eLy)
  if ($lenR -ge $lenL){$wx=$eRx;$wy=$eRy;$w=$lenR;$h=$lenL}else{$wx=$eLx;$wy=$eLy;$w=$lenL;$h=$lenR}
  $ang=[math]::Atan2($wy,$wx)*180.0/[math]::PI
  while($ang -gt 45){$ang-=90}; while($ang -lt -45){$ang+=90}
  $cx=($Tx+$Bx+$Lx+$Rx)/4.0; $cy=($Ty+$By+$Ly+$Ry)/4.0
  $L0=[int][math]::Round($cx-$w/2);$T0=[int][math]::Round($cy-$h/2);$R0=[int][math]::Round($cx+$w/2);$B0=[int][math]::Round($cy+$h/2)
  $aL=[int]([double]$q.area*$sLX*$sLY)
  Write-Host ("({0},{1},{2},{3},{4})  w={5} h={6} center=({7},{8}) areaLogic={9}" -f $L0,$T0,$R0,$B0,[math]::Round($ang,1),[int]$w,[int]$h,[int]$cx,[int]$cy,$aL)
}
