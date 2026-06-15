# Draw a labelled coordinate grid (logic space 1125x2000) on a template for manual measuring.
Add-Type -AssemblyName System.Drawing
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\agent_sm"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$id=$args[0]
$OW=562; $OH=1000
$LW=1125.0; $LH=2000.0
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
$img=[System.Drawing.Image]::FromFile((Join-Path $resDir "temp_$id.jpg"))
$bmp=New-Object System.Drawing.Bitmap $OW,$OH
$gr=[System.Drawing.Graphics]::FromImage($bmp)
$gr.DrawImage($img,0,0,$OW,$OH)
$penG=New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(160,255,0,255)),1
$fnt=New-Object System.Drawing.Font "Arial",8
$brush=[System.Drawing.Brushes]::Magenta
for($x=0;$x -le 1125;$x+=125){
  $px=$x/$LW*$OW
  $gr.DrawLine($penG,[float]$px,0,[float]$px,[float]$OH)
  $gr.DrawString([string]$x,$fnt,$brush,[float]$px,2)
}
for($y=0;$y -le 2000;$y+=125){
  $py=$y/$LH*$OH
  $gr.DrawLine($penG,0,[float]$py,[float]$OW,[float]$py)
  $gr.DrawString([string]$y,$fnt,$brush,2,[float]$py)
}
$gr.Dispose()
$eps=New-Object System.Drawing.Imaging.EncoderParameters 1
$eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]90)
$bmp.Save((Join-Path $outDir "grid_$id.jpg"),$jpgEnc,$eps)
$eps.Dispose();$bmp.Dispose();$img.Dispose()
"grid_$id done"
