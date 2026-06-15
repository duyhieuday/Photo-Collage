# Emit cells_sm.txt: hashtable lines + masks (non-WHITE only). ASCII.
Add-Type -AssemblyName System.Drawing
. "D:\EZTech\EZTechApp\collage_pic_editor\tools\_cells_sm.ps1"
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$ids = $SMCELLS.Keys | Sort-Object
$lines=@(); $maskLines=@()
foreach($id in $ids){
  $cells=$SMCELLS[$id]
  $parts=@()
  foreach($c in $cells){ $parts += ("@({0},{1},{2},{3})" -f $c[0],$c[1],$c[2],$c[3]) }
  if($cells.Count -eq 1){
    $lines += ('  "{0}"=@(,{1})' -f $id, ($parts -join ','))
  } else {
    $lines += ('  "{0}"=@({1})' -f $id, ($parts -join ','))
  }
  # mask: sample center of FIRST cell in temp_<id>.jpg
  $p=Join-Path $resDir "temp_$id.jpg"
  $img=[System.Drawing.Image]::FromFile($p)
  $bmp=New-Object System.Drawing.Bitmap $img
  $f=$cells[0]
  $cx=[int]((($f[0]+$f[2])/2.0)/1125.0*$bmp.Width)
  $cy=[int]((($f[1]+$f[3])/2.0)/2000.0*$bmp.Height)
  if($cx -lt 0){$cx=0}; if($cx -ge $bmp.Width){$cx=$bmp.Width-1}
  if($cy -lt 0){$cy=0}; if($cy -ge $bmp.Height){$cy=$bmp.Height-1}
  $px=$bmp.GetPixel($cx,$cy)
  $bmp.Dispose(); $img.Dispose()
  $mask = if($px.R -ge 247 -and $px.G -ge 247 -and $px.B -ge 247){'WHITE'}else{'NONE'}
  Write-Host ("{0}: center=({1},{2}) RGB=({3},{4},{5}) -> {6}" -f $id,$cx,$cy,$px.R,$px.G,$px.B,$mask)
  if($mask -eq 'NONE'){ $maskLines += ('  "{0}"="NONE"' -f $id) }
}
$content = ($lines -join "`n") + "`n---MASKS---`n" + ($maskLines -join "`n") + "`n"
$content | Out-File (Join-Path $outDir "cells_sm.txt") -Encoding ascii
Write-Host "----- FILE -----"
Write-Host $content
