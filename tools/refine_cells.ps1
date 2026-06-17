# Seed-based flood fill to find precise photo-cell rects.
# Each template: a mode (white frame >248, or gray frame neutral band) + seed points
# (fx,fy fractions inside each cell). Flood from interior -> avoids bg merge.
# ASCII only (PowerShell 5.1 mis-decodes non-ASCII .ps1 without BOM).

Add-Type -AssemblyName System.Drawing

$resDir = "D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir = "D:\EZTech\EZTechApp\collage_pic_editor\tools\_out"
$ovDir  = Join-Path $outDir "refine"
New-Item -ItemType Directory -Force -Path $ovDir | Out-Null

$LOGIC_W = 1125; $LOGIC_H = 2000
$AW = 562; $AH = 1000

# id -> @{ mode='white'|'gray'; pts=@(@(fx,fy),...) }
$cfg = @{
  "sm05" = @{ mode='gray'; pts=@(@(0.30,0.18),@(0.42,0.27),@(0.39,0.43),@(0.36,0.59)) }
}

function Get-Mask($path,$mode){
  $img=[System.Drawing.Image]::FromFile($path)
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
  $mask=New-Object 'bool[]' ($AW*$AH)
  for($y=0;$y -lt $AH;$y++){$row=$y*$stride
    for($x=0;$x -lt $AW;$x++){$o=$row+$x*3
      $b=$buf[$o];$gr=$buf[$o+1];$r=$buf[$o+2]
      if($mode -eq 'white'){
        if($r -ge 248 -and $gr -ge 248 -and $b -ge 248){$mask[$y*$AW+$x]=$true}
      } else {
        $mx=[Math]::Max($r,[Math]::Max($gr,$b));$mn=[Math]::Min($r,[Math]::Min($gr,$b))
        if($mx -ge 228 -and $mx -le 242 -and ($mx-$mn) -le 8){$mask[$y*$AW+$x]=$true}
      }
    }
  }
  return $mask
}

function Flood($mask,$visited,$sx,$sy){
  if(-not ($mask[$sy*$AW+$sx])){
    $found=$false
    for($r=1;$r -le 40 -and -not $found;$r++){
      for($dy=-$r;$dy -le $r -and -not $found;$dy++){ for($dx=-$r;$dx -le $r -and -not $found;$dx++){
        $nx=$sx+$dx;$ny=$sy+$dy
        if($nx -ge 0 -and $nx -lt $AW -and $ny -ge 0 -and $ny -lt $AH){ if($mask[$ny*$AW+$nx]){$sx=$nx;$sy=$ny;$found=$true} }
      }}
    }
    if(-not $found){return $null}
  }
  $start=$sy*$AW+$sx
  $st=New-Object System.Collections.Generic.Stack[int];$st.Push($start);$visited[$start]=$true
  $minX=$AW;$minY=$AH;$maxX=0;$maxY=0;$cnt=0
  while($st.Count -gt 0){
    $p=$st.Pop();$cnt++;$px=$p%$AW;$py=[int]($p/$AW)
    if($px -lt $minX){$minX=$px};if($px -gt $maxX){$maxX=$px}
    if($py -lt $minY){$minY=$py};if($py -gt $maxY){$maxY=$py}
    if($px -gt 0){$q=$p-1;if($mask[$q] -and -not $visited[$q]){$visited[$q]=$true;$st.Push($q)}}
    if($px -lt $AW-1){$q=$p+1;if($mask[$q] -and -not $visited[$q]){$visited[$q]=$true;$st.Push($q)}}
    if($py -gt 0){$q=$p-$AW;if($mask[$q] -and -not $visited[$q]){$visited[$q]=$true;$st.Push($q)}}
    if($py -lt $AH-1){$q=$p+$AW;if($mask[$q] -and -not $visited[$q]){$visited[$q]=$true;$st.Push($q)}}
  }
  return [pscustomobject]@{minX=$minX;minY=$minY;maxX=$maxX;maxY=$maxY;cnt=$cnt}
}

$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
function Save-Overlay($path,$cells,$dst){
  $img=[System.Drawing.Image]::FromFile($path);$W=360;$H=640
  $bmp=New-Object System.Drawing.Bitmap $W,$H;$g=[System.Drawing.Graphics]::FromImage($bmp)
  $g.DrawImage($img,0,0,$W,$H)
  $pen=New-Object System.Drawing.Pen ([System.Drawing.Color]::Red),3
  foreach($c in $cells){$x=$c[0]*$W/$LOGIC_W;$y=$c[1]*$H/$LOGIC_H;$w=($c[2]-$c[0])*$W/$LOGIC_W;$h=($c[3]-$c[1])*$H/$LOGIC_H;$g.DrawRectangle($pen,[float]$x,[float]$y,[float]$w,[float]$h)}
  $pen.Dispose();$g.Dispose()
  $eps=New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]88)
  $bmp.Save($dst,$jpgEnc,$eps);$eps.Dispose();$bmp.Dispose();$img.Dispose()
}

$result=[ordered]@{}
foreach($id in $cfg.Keys){
  $path=Join-Path $resDir "temp_$id.jpg"
  $mask=Get-Mask $path $cfg[$id].mode
  $visited=New-Object 'bool[]' ($AW*$AH)
  $cells=@()
  foreach($s in $cfg[$id].pts){
    $sx=[int]($s[0]*$AW);$sy=[int]($s[1]*$AH)
    $bb=Flood $mask $visited $sx $sy
    if($bb -ne $null -and $bb.cnt -gt 80){
      $cells+=,@([int][math]::Round($bb.minX*$LOGIC_W/$AW),[int][math]::Round($bb.minY*$LOGIC_H/$AH),[int][math]::Round(($bb.maxX+1)*$LOGIC_W/$AW),[int][math]::Round(($bb.maxY+1)*$LOGIC_H/$AH))
    }
  }
  $result[$id]=$cells
  Save-Overlay $path $cells (Join-Path $ovDir "$id.jpg")
  Write-Host ("{0} ({1}): {2} cells" -f $id,$cfg[$id].mode,$cells.Count)
}
$result|ConvertTo-Json -Depth 5|Out-File (Join-Path $outDir "cells_refined.json") -Encoding utf8
