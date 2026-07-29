# Measure where the designer actually placed each PHOTO, by finding the non-paper regions in
# Thumb_. With MaskMode.NONE the cellRect IS the photo, so the designer's placement in Thumb_
# is the target - not the frame outline, and not the diff-vs-Temp_ blob.
#
# Works on designs whose photos are clearly darker/more saturated than the card stock
# (e.g. black-and-white photos on cream cards). Text and small marks are filtered out by size.
#
# Usage: powershell -File tools\measure_thumb_photos.ps1 -Ids bd12 [-MaxPaper 232] [-MinSidePx 60]

param(
  [string[]]$Ids = @(),
  [int]$MaxPaper = 232,     # anything brighter than this counts as card stock / background
  [int]$MinSidePx = 60      # in analysis pixels; drops letters, hearts, small marks
)

Add-Type -AssemblyName System.Drawing
$SRC = "D:\EZTech\AppAssets\PhotoCollage\Category_Template"
$CATS = @{ 'bd'='Birthday'; 'cp'='Couple'; 'gs'='Glad season'; 'is'='IG Story'; 'sm'='Summer vibe'; 'sp'='Sports' }
$LW = 1125; $LH = 2000
$AW = 450; $AH = 800

$Ids = @($Ids | ForEach-Object { $_ -split ',' } | Where-Object { $_ -match '\S' } | ForEach-Object { $_.Trim().ToLower() })
if ($Ids.Count -eq 0) { Write-Host "No -Ids."; exit 1 }

foreach ($id in $Ids) {
  $pre = $id.Substring(0,2); $nn = $id.Substring(2)
  $p = Join-Path $SRC ("{0}\Thumb_{1}{2}.png" -f $CATS[$pre], $pre.ToUpper(), $nn)
  if (-not (Test-Path $p)) { Write-Host "$id : khong co Thumb_"; continue }

  $img = [System.Drawing.Image]::FromFile($p)
  $bm = New-Object System.Drawing.Bitmap $AW, $AH
  $g = [System.Drawing.Graphics]::FromImage($bm)
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.DrawImage($img, 0, 0, $AW, $AH); $g.Dispose(); $img.Dispose()

  $r = New-Object System.Drawing.Rectangle 0,0,$AW,$AH
  $d = $bm.LockBits($r, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
  $stride = $d.Stride; $buf = New-Object byte[] ($stride*$AH)
  [System.Runtime.InteropServices.Marshal]::Copy($d.Scan0, $buf, 0, $buf.Length)
  $bm.UnlockBits($d); $bm.Dispose()

  $N = $AW*$AH
  $mask = New-Object 'bool[]' $N
  for ($y=0; $y -lt $AH; $y++) {
    $row = $y*$stride; $base = $y*$AW
    for ($x=0; $x -lt $AW; $x++) {
      $o = $row + $x*3
      $b=$buf[$o]; $gr=$buf[$o+1]; $rr2=$buf[$o+2]
      $mx = $rr2; if($gr -gt $mx){$mx=$gr}; if($b -gt $mx){$mx=$b}
      if ($mx -le $MaxPaper) { $mask[$base+$x] = $true }
    }
  }
  # close small gaps so a light patch inside a photo does not split it
  $tmp = New-Object 'bool[]' $N
  for ($y=1; $y -lt $AH-1; $y++) { for ($x=1; $x -lt $AW-1; $x++) {
    $i=$y*$AW+$x
    if ($mask[$i]) { $tmp[$i]=$true; continue }
    if ($mask[$i-1] -and $mask[$i+1]) { $tmp[$i]=$true; continue }
    if ($mask[$i-$AW] -and $mask[$i+$AW]) { $tmp[$i]=$true }
  }}
  $mask = $tmp

  $vis = New-Object 'bool[]' $N
  $st = New-Object System.Collections.Generic.Stack[int]
  Write-Host ""
  Write-Host "==== $id ===="
  $found = @()
  for ($i=0; $i -lt $N; $i++) {
    if (-not $mask[$i] -or $vis[$i]) { continue }
    $st.Clear(); $st.Push($i); $vis[$i]=$true
    $mnX=$AW;$mnY=$AH;$mxX=0;$mxY=0;$cnt=0
    while ($st.Count -gt 0) {
      $q=$st.Pop(); $cnt++
      $qx=$q%$AW; $qy=[int]($q/$AW)
      if($qx -lt $mnX){$mnX=$qx}; if($qx -gt $mxX){$mxX=$qx}
      if($qy -lt $mnY){$mnY=$qy}; if($qy -gt $mxY){$mxY=$qy}
      if($qx -gt 0){$n2=$q-1;if($mask[$n2]-and-not $vis[$n2]){$vis[$n2]=$true;$st.Push($n2)}}
      if($qx -lt $AW-1){$n2=$q+1;if($mask[$n2]-and-not $vis[$n2]){$vis[$n2]=$true;$st.Push($n2)}}
      if($qy -gt 0){$n2=$q-$AW;if($mask[$n2]-and-not $vis[$n2]){$vis[$n2]=$true;$st.Push($n2)}}
      if($qy -lt $AH-1){$n2=$q+$AW;if($mask[$n2]-and-not $vis[$n2]){$vis[$n2]=$true;$st.Push($n2)}}
    }
    $bw=$mxX-$mnX+1; $bh=$mxY-$mnY+1
    if ([Math]::Min($bw,$bh) -lt $MinSidePx) { continue }
    $fill = $cnt/[double]($bw*$bh)
    $found += [pscustomobject]@{
      l=[int]($mnX*$LW/$AW); t=[int]($mnY*$LH/$AH)
      r=[int](($mxX+1)*$LW/$AW); b=[int](($mxY+1)*$LH/$AH)
      fill=[math]::Round($fill,2); px=$cnt
    }
  }
  $found = @($found | Sort-Object t, l)
  $k=0
  foreach ($f in $found) {
    $k++
    Write-Host ("   {0}. ({1},{2},{3},{4})   {5}x{6}  fill={7}" -f $k,$f.l,$f.t,$f.r,$f.b,($f.r-$f.l),($f.b-$f.t),$f.fill)
  }
  if ($found.Count -eq 0) { Write-Host "   (khong tim thay vung anh nao - thu doi -MaxPaper)" }
}
