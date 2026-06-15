# Tile verify overlays into labelled contact sheets for quick review. ASCII only.
Add-Type -AssemblyName System.Drawing
$vdir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\verify"
$odir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\sheets"
New-Item -ItemType Directory -Force -Path $odir | Out-Null
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
$font=New-Object System.Drawing.Font("Arial",20,[System.Drawing.FontStyle]::Bold)
$brush=[System.Drawing.Brushes]::Red
$brushBg=New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(220,255,255,0))

$ids=@($args)
$cw=300; $ch=534; $cols=6; $pad=6; $labelH=26
$rows=[math]::Ceiling($ids.Count/$cols)
$W=$cols*($cw+$pad)+$pad; $H=$rows*($ch+$labelH+$pad)+$pad
$bmp=New-Object System.Drawing.Bitmap $W,$H
$g=[System.Drawing.Graphics]::FromImage($bmp)
$g.Clear([System.Drawing.Color]::White)
for($i=0;$i -lt $ids.Count;$i++){
  $id=$ids[$i]; $p=Join-Path $vdir "$id.jpg"
  $col=$i % $cols; $row=[math]::Floor($i/$cols)
  $x=$pad+$col*($cw+$pad); $y=$pad+$row*($ch+$labelH+$pad)
  $g.FillRectangle($brushBg,[float]$x,[float]$y,[float]$cw,[float]$labelH)
  $g.DrawString($id,$font,$brush,[float]($x+4),[float]($y+1))
  if(Test-Path $p){ $img=[System.Drawing.Image]::FromFile($p); $g.DrawImage($img,[float]$x,[float]($y+$labelH),[float]$cw,[float]$ch); $img.Dispose() }
}
$g.Dispose()
$name=$ids[0]+"_"+$ids[$ids.Count-1]
$eps=New-Object System.Drawing.Imaging.EncoderParameters 1
$eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]82)
$bmp.Save((Join-Path $odir "$name.jpg"),$jpgEnc,$eps)
$eps.Dispose();$bmp.Dispose()
Write-Host "sheet -> $name.jpg"
