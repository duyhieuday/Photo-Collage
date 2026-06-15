Add-Type -AssemblyName System.Drawing
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\agent_cp"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$LW=1125.0; $LH=2000.0
$OUTW=450; $OUTH=800
$sx=$OUTW/$LW; $sy=$OUTH/$LH
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}

$C=@{}
$C["cp01"]=@(@(215,339,956,860),@(164,773,905,1294))
$C["cp02"]=@(@(0,0,1125,667),@(0,667,1125,1333),@(0,1333,1125,2000))
$C["cp03"]=@(@(31,161,689,665),@(152,704,810,1200),@(271,1239,929,1735))
$C["cp04"]=@(@(500,40,1069,788),@(111,1085,672,1833))
$C["cp05"]=@(@(117,161,531,712),@(416,826,855,1412))
$C["cp06"]=@(@(487,481,832,941),@(180,1309,534,1664))
$C["cp07"]=@(@(200,557,833,1365))
$C["cp08"]=@(@(459,559,993,966),@(296,1145,661,1561))
$C["cp09"]=@(@(145,344,561,760),@(1019,344,1435,760),@(144,802,560,1219),@(1020,802,1436,1219),@(145,1260,561,1677),@(1019,1260,1435,1677))

$only=@($args)
foreach($id in @($C.Keys)){
  if($only.Count -gt 0 -and ($only -notcontains $id)){ continue }
  $p=Join-Path $resDir "temp_$id.jpg"
  $img=[System.Drawing.Image]::FromFile($p)
  $bmp=New-Object System.Drawing.Bitmap $OUTW,$OUTH
  $gr=[System.Drawing.Graphics]::FromImage($bmp)
  $gr.DrawImage($img,0,0,$OUTW,$OUTH)
  $pen=New-Object System.Drawing.Pen ([System.Drawing.Color]::Magenta),4
  foreach($c in $C[$id]){
    $rx=$c[0]*$sx; $ry=$c[1]*$sy; $rw=($c[2]-$c[0])*$sx; $rh=($c[3]-$c[1])*$sy
    $gr.DrawRectangle($pen,[float]$rx,[float]$ry,[float]$rw,[float]$rh)
  }
  $pen.Dispose(); $gr.Dispose()
  $eps=New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]90)
  $bmp.Save((Join-Path $outDir "$id.jpg"),$jpgEnc,$eps)
  $eps.Dispose();$bmp.Dispose();$img.Dispose()
  Write-Output "rendered $id"
}
