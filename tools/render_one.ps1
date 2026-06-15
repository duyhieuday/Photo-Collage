param([string]$id)
Add-Type -AssemblyName System.Drawing
$C=@{}
$C["cp01"]=@(@(222,295,990,775),@(165,775,905,1295))
$C["cp02"]=@(@(0,0,1125,667),@(0,667,1125,1333),@(0,1333,1125,2000))
$C["cp03"]=@(@(31,161,689,665),@(152,704,810,1200),@(271,1239,929,1735))
$C["cp04"]=@(@(500,40,1069,788),@(111,1085,672,1833))
$C["cp05"]=@(@(117,161,531,712),@(416,826,855,1412))
$C["cp06"]=@(@(487,481,832,941),@(180,1309,534,1664))
$C["cp07"]=@(@(167,357,723,842),@(365,1285,918,1766))
$C["cp08"]=@(@(459,559,993,966),@(296,1145,661,1561))
$C["cp09"]=@(@(145,344,560,759),@(603,344,1017,759),@(145,803,560,1217),@(603,803,1017,1217),@(145,1261,560,1676),@(603,1261,1017,1676))
$img=[System.Drawing.Image]::FromFile("D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable\temp_$id.jpg")
$OUTW=450;$OUTH=800
$bmp=New-Object System.Drawing.Bitmap $OUTW,$OUTH
$gr=[System.Drawing.Graphics]::FromImage($bmp)
$gr.DrawImage($img,0,0,$OUTW,$OUTH)
$pen=New-Object System.Drawing.Pen ([System.Drawing.Color]::Magenta),4
$sx=$OUTW/1125.0;$sy=$OUTH/2000.0
foreach($c in $C[$id]){ $gr.DrawRectangle($pen,[float]($c[0]*$sx),[float]($c[1]*$sy),[float](($c[2]-$c[0])*$sx),[float](($c[3]-$c[1])*$sy)) }
$pen.Dispose();$gr.Dispose()
$bmp.Save("D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\agent_cp\$id.jpg",[System.Drawing.Imaging.ImageFormat]::Jpeg)
$bmp.Dispose();$img.Dispose();Write-Output "rendered $id"
