# Crop a logic-space region and upscale, with fine grid. args: id lx ly rx ry
Add-Type -AssemblyName System.Drawing
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\agent_sm"
$id=$args[0]; $lx=[double]$args[1]; $ly=[double]$args[2]; $rx=[double]$args[3]; $ry=[double]$args[4]
$img=[System.Drawing.Image]::FromFile((Join-Path $resDir "temp_$id.jpg"))
$IW=$img.Width; $IH=$img.Height   # 1125x2000
# source px (image is 1125x2000 already logic-aligned)
$sx=$lx; $sy=$ly; $sw=$rx-$lx; $sh=$ry-$ly
$scale=600.0/$sw
$OW=[int]($sw*$scale); $OH=[int]($sh*$scale)
$bmp=New-Object System.Drawing.Bitmap $OW,$OH
$gr=[System.Drawing.Graphics]::FromImage($bmp)
$srcRect=New-Object System.Drawing.Rectangle ([int]$sx,[int]$sy,[int]$sw,[int]$sh)
$dstRect=New-Object System.Drawing.Rectangle 0,0,$OW,$OH
$gr.DrawImage($img,$dstRect,$srcRect,[System.Drawing.GraphicsUnit]::Pixel)
$pen=New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(150,255,0,255)),1
$fnt=New-Object System.Drawing.Font "Arial",9
$br=[System.Drawing.Brushes]::Magenta
for($x=[math]::Ceiling($lx/50)*50; $x -le $rx; $x+=50){
  $px=($x-$lx)*$scale
  $gr.DrawLine($pen,[float]$px,0,[float]$px,[float]$OH)
  $gr.DrawString([string]$x,$fnt,$br,[float]$px,2)
}
for($y=[math]::Ceiling($ly/50)*50; $y -le $ry; $y+=50){
  $py=($y-$ly)*$scale
  $gr.DrawLine($pen,0,[float]$py,[float]$OW,[float]$py)
  $gr.DrawString([string]$y,$fnt,$br,2,[float]$py)
}
$gr.Dispose()
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
$eps=New-Object System.Drawing.Imaging.EncoderParameters 1
$eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]90)
$bmp.Save((Join-Path $outDir "crop_$id.jpg"),$jpgEnc,$eps)
$eps.Dispose();$bmp.Dispose();$img.Dispose()
"crop_$id done $OW x $OH"
