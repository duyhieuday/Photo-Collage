Add-Type -AssemblyName System.Drawing
. "D:\EZTech\EZTechApp\collage_pic_editor\tools\_cells_sm.ps1"
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\agent_sm"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$LW=1125.0; $LH=2000.0
$OW=450; $OH=800
$sclx=$OW/$LW; $scly=$OH/$LH
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
$pen=New-Object System.Drawing.Pen ([System.Drawing.Color]::Lime),3
$only=@($args)
foreach($id in @($SMCELLS.Keys | Sort-Object)){
  if($only.Count -gt 0 -and ($only -notcontains $id)){ continue }
  $img=[System.Drawing.Image]::FromFile((Join-Path $resDir "temp_$id.jpg"))
  $bmp=New-Object System.Drawing.Bitmap $OW,$OH
  $gr=[System.Drawing.Graphics]::FromImage($bmp)
  $gr.DrawImage($img,0,0,$OW,$OH)
  foreach($c in $SMCELLS[$id]){
    $rx=$c[0]*$sclx; $ry=$c[1]*$scly; $rw=($c[2]-$c[0])*$sclx; $rh=($c[3]-$c[1])*$scly
    $gr.DrawRectangle($pen,[float]$rx,[float]$ry,[float]$rw,[float]$rh)
  }
  $gr.Dispose()
  $eps=New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]85)
  $bmp.Save((Join-Path $outDir "$id.jpg"),$jpgEnc,$eps)
  $eps.Dispose();$bmp.Dispose();$img.Dispose()
  "verify -> $id"
}
