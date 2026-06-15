# verify + grid combined, full half-res, args: id
Add-Type -AssemblyName System.Drawing
. "D:\EZTech\EZTechApp\collage_pic_editor\tools\_cells_sm.ps1"
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\agent_sm"
$id=$args[0]
$OW=562; $OH=1000; $LW=1125.0; $LH=2000.0
$sx=$OW/$LW; $sy=$OH/$LH
$img=[System.Drawing.Image]::FromFile((Join-Path $resDir "temp_$id.jpg"))
$bmp=New-Object System.Drawing.Bitmap $OW,$OH
$gr=[System.Drawing.Graphics]::FromImage($bmp)
$gr.DrawImage($img,0,0,$OW,$OH)
$penG=New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(120,255,0,255)),1
$fnt=New-Object System.Drawing.Font "Arial",7
$brM=[System.Drawing.Brushes]::Magenta
for($x=0;$x -le 1125;$x+=125){ $px=$x*$sx; $gr.DrawLine($penG,[float]$px,0,[float]$px,[float]$OH); $gr.DrawString([string]$x,$fnt,$brM,[float]$px,2) }
for($y=0;$y -le 2000;$y+=125){ $py=$y*$sy; $gr.DrawLine($penG,0,[float]$py,[float]$OW,[float]$py); $gr.DrawString([string]$y,$fnt,$brM,2,[float]$py) }
$pen=New-Object System.Drawing.Pen ([System.Drawing.Color]::Lime),3
foreach($c in $SMCELLS[$id]){ $rx=$c[0]*$sx;$ry=$c[1]*$sy;$rw=($c[2]-$c[0])*$sx;$rh=($c[3]-$c[1])*$sy; $gr.DrawRectangle($pen,[float]$rx,[float]$ry,[float]$rw,[float]$rh) }
$gr.Dispose()
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
$eps=New-Object System.Drawing.Imaging.EncoderParameters 1
$eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]88)
$bmp.Save((Join-Path $outDir "vg_$id.jpg"),$jpgEnc,$eps)
$eps.Dispose();$bmp.Dispose();$img.Dispose()
"vg_$id done"
