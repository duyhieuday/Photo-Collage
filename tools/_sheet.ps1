# Contact sheet of final device captures (red = photo area). ASCII only.
Add-Type -AssemblyName System.Drawing
$dir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\dev2"
$files=@("cp03.png","m_cp06.png","gs03.png","is05.png","m_is06.png","is12.png","sm05.png","sp01.png")
$labels=@("cp03","cp06","gs03","is05","is06","is12","sm05","sp01")
$cols=4;$rows=2;$cw=300;$ch=560;$pad=10;$lab=22
$W=$cols*$cw+($cols+1)*$pad; $H=$rows*($ch+$lab)+($rows+1)*$pad
$bmp=New-Object System.Drawing.Bitmap $W,$H
$g=[System.Drawing.Graphics]::FromImage($bmp)
$g.Clear([System.Drawing.Color]::White)
$g.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$font=New-Object System.Drawing.Font("Arial",13,[System.Drawing.FontStyle]::Bold)
$blk=[System.Drawing.Brushes]::Black
for($i=0;$i -lt $files.Count;$i++){
  $p=Join-Path $dir $files[$i]
  if(-not (Test-Path $p)){ continue }
  $c=$i%$cols; $r=[int]($i/$cols)
  $x=$pad+$c*($cw+$pad); $y=$pad+$r*($ch+$lab+$pad)
  $g.DrawString($labels[$i],$font,$blk,[float]($x+4),[float]$y)
  $img=[System.Drawing.Image]::FromFile($p)
  $g.DrawImage($img,[float]$x,[float]($y+$lab),[float]$cw,[float]$ch)
  $img.Dispose()
}
$g.Dispose()
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
$eps=New-Object System.Drawing.Imaging.EncoderParameters 1
$eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]90)
$out="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\popular_after.jpg"
$bmp.Save($out,$jpgEnc,$eps); $eps.Dispose();$bmp.Dispose()
Write-Host "saved $out"
