# Draw a labelled grid over a template to measure cell openings by eye (logic 1125x2000).
Add-Type -AssemblyName System.Drawing
$id=$args[0]
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\agent_bd"
$OW=450; $OH=800
$sx=$OW/1125.0; $sy=$OH/2000.0
$img=[System.Drawing.Image]::FromFile((Join-Path $resDir "temp_$id.jpg"))
$bmp=New-Object System.Drawing.Bitmap $OW,$OH
$gr=[System.Drawing.Graphics]::FromImage($bmp)
$gr.DrawImage($img,0,0,$OW,$OH)
$penG=New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(140,255,0,0)),1
$font=New-Object System.Drawing.Font("Arial",8)
$brush=[System.Drawing.Brushes]::Red
for($x=0;$x -le 1125;$x+=125){
  $px=$x*$sx
  $gr.DrawLine($penG,[float]$px,0,[float]$px,[float]$OH)
  $gr.DrawString([string]$x,$font,$brush,[float]($px+1),2)
}
for($y=0;$y -le 2000;$y+=125){
  $py=$y*$sy
  $gr.DrawLine($penG,0,[float]$py,[float]$OW,[float]$py)
  $gr.DrawString([string]$y,$font,$brush,1,[float]($py+1))
}
$gr.Dispose()
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
$eps=New-Object System.Drawing.Imaging.EncoderParameters 1
$eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]92)
$bmp.Save((Join-Path $outDir "grid_$id.jpg"),$jpgEnc,$eps)
$bmp.Dispose();$img.Dispose()
Write-Host "grid -> grid_$id.jpg"
