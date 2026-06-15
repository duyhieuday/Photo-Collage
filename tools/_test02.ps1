Add-Type -AssemblyName System.Drawing
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\agent_sm"
# raw figma-scaled candidate rects for sm02
$cells=@(
  @(149,520,587,957),
  @(149,989,587,1426),
  @(952,698,1485,1032),
  @(537,1284,1005,1650)
)
$OW=562; $OH=1000; $LW=1125.0; $LH=2000.0
$sx=$OW/$LW; $sy=$OH/$LH
$img=[System.Drawing.Image]::FromFile((Join-Path $resDir "temp_sm02.jpg"))
$bmp=New-Object System.Drawing.Bitmap $OW,$OH
$gr=[System.Drawing.Graphics]::FromImage($bmp)
$gr.DrawImage($img,0,0,$OW,$OH)
$pen=New-Object System.Drawing.Pen ([System.Drawing.Color]::Red),3
foreach($c in $cells){ $gr.DrawRectangle($pen,[float]($c[0]*$sx),[float]($c[1]*$sy),[float](($c[2]-$c[0])*$sx),[float](($c[3]-$c[1])*$sy)) }
$gr.Dispose()
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
$eps=New-Object System.Drawing.Imaging.EncoderParameters 1
$eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]88)
$bmp.Save((Join-Path $outDir "test02.jpg"),$jpgEnc,$eps)
"done"
