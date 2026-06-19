# Blend temp_<id> and thumb_<id> 50/50 to check if frames/photos align. ASCII only.
Add-Type -AssemblyName System.Drawing
$id=$args[0]; if(-not $id){$id="cp01"}
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\blend"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$OW=562;$OH=1000
$a=[System.Drawing.Image]::FromFile((Join-Path $resDir "temp_$id.jpg"))
$b=[System.Drawing.Image]::FromFile((Join-Path $resDir "thumb_$id.jpg"))
$bmp=New-Object System.Drawing.Bitmap $OW,$OH
$gr=[System.Drawing.Graphics]::FromImage($bmp)
$gr.DrawImage($a,0,0,$OW,$OH)
$cm=New-Object System.Drawing.Imaging.ColorMatrix
$cm.Matrix33=0.5
$ia=New-Object System.Drawing.Imaging.ImageAttributes
$ia.SetColorMatrix($cm)
$dest=New-Object System.Drawing.Rectangle 0,0,$OW,$OH
$gr.DrawImage($b,$dest,0,0,$b.Width,$b.Height,[System.Drawing.GraphicsUnit]::Pixel,$ia)
$gr.Dispose()
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
$eps=New-Object System.Drawing.Imaging.EncoderParameters 1
$eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]92)
$bmp.Save((Join-Path $outDir "$id.jpg"),$jpgEnc,$eps)
$eps.Dispose();$bmp.Dispose();$a.Dispose();$b.Dispose()
Write-Host "blend -> $id"
