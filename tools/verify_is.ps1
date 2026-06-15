# Overlay cells_is.txt rects onto temp_isNN.jpg. ASCII only.
Add-Type -AssemblyName System.Drawing
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\agent_is"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$txt="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\cells_is.txt"

# parse the hashtable lines into $CELLS
$CELLS=@{}
$content=Get-Content $txt
$inMask=$false
foreach($ln in $content){
  if($ln -match '^---MASKS---'){ $inMask=$true; continue }
  if($inMask){ continue }
  if($ln -match '"(is\d+)"=@\((.*)\)\s*$'){
    $id=$matches[1]; $body=$matches[2]
    $rects=@()
    $rx=[regex]::Matches($body,'@\((-?\d+),(-?\d+),(-?\d+),(-?\d+)\)')
    foreach($m in $rx){ $rects += ,@([int]$m.Groups[1].Value,[int]$m.Groups[2].Value,[int]$m.Groups[3].Value,[int]$m.Groups[4].Value) }
    $CELLS[$id]=$rects
  }
}

$LOGIC_W=1125; $LOGIC_H=2000
$OW=562; $OH=1000
$sx=$OW/$LOGIC_W; $sy=$OH/$LOGIC_H
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
$pen=New-Object System.Drawing.Pen ([System.Drawing.Color]::Lime),3

$only=@($args)
foreach($id in ($CELLS.Keys | Sort-Object)){
  if($only.Count -gt 0 -and ($only -notcontains $id)){ continue }
  $img=[System.Drawing.Image]::FromFile((Join-Path $resDir "temp_$id.jpg"))
  $bmp=New-Object System.Drawing.Bitmap $OW,$OH
  $gr=[System.Drawing.Graphics]::FromImage($bmp)
  $gr.DrawImage($img,0,0,$OW,$OH)
  foreach($c in $CELLS[$id]){
    $rx=$c[0]*$sx; $ry=$c[1]*$sy; $rw=($c[2]-$c[0])*$sx; $rh=($c[3]-$c[1])*$sy
    $gr.DrawRectangle($pen,[float]$rx,[float]$ry,[float]$rw,[float]$rh)
  }
  $gr.Dispose()
  $eps=New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]90)
  $bmp.Save((Join-Path $outDir "$id.jpg"),$jpgEnc,$eps)
  $eps.Dispose();$bmp.Dispose();$img.Dispose()
  Write-Host "verify -> $id ($($CELLS[$id].Count))"
}
