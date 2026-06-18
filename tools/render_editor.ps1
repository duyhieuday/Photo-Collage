# Replicate the editor's masked compositing to detect photo bleed. ASCII only.
# result[p] = WHITE mask: (isWhite(tpl) && inCell) ? PHOTO : tpl
#            BLACK mask: (isBlack(tpl) && inCell) ? PHOTO : tpl
#            NONE: inCell ? PHOTO : tpl
Add-Type -AssemblyName System.Drawing
. "D:\EZTech\EZTechApp\collage_pic_editor\tools\cells_config.ps1"
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\render"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$LOGIC_W=1125; $LOGIC_H=2000
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}

$ids=@($args)
foreach($id in $ids){
  $p=Join-Path $resDir "temp_$id.jpg"
  if(-not (Test-Path $p)){ continue }
  $img=[System.Drawing.Image]::FromFile($p)
  $W=$img.Width; $H=$img.Height
  $bmp=New-Object System.Drawing.Bitmap $img
  $img.Dispose()
  $mode='NONE'; if($MASKS.ContainsKey($id)){ $mode=$MASKS[$id] }
  $rr=$CELLS[$id]
  if($rr.Count -ge 1 -and ($rr[0] -is [int] -or $rr[0] -is [double])){ $rr=@(,$rr) }
  # build cell mask in image space
  $rect=New-Object System.Drawing.Rectangle 0,0,$W,$H
  $data=$bmp.LockBits($rect,[System.Drawing.Imaging.ImageLockMode]::ReadWrite,[System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
  $stride=$data.Stride; $buf=New-Object byte[] ($stride*$H)
  [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0,$buf,0,$buf.Length)
  # precompute cell rects in pixel space
  $cells=@()
  foreach($c in $rr){ $cells += ,@([int]($c[0]*$W/$LOGIC_W),[int]($c[1]*$H/$LOGIC_H),[int]($c[2]*$W/$LOGIC_W),[int]($c[3]*$H/$LOGIC_H)) }
  for($y=0;$y -lt $H;$y++){ $row=$y*$stride
    for($x=0;$x -lt $W;$x++){ $o=$row+$x*3
      $b=$buf[$o]; $g=$buf[$o+1]; $r=$buf[$o+2]
      $inCell=$false
      foreach($c in $cells){ if($x -ge $c[0] -and $x -lt $c[2] -and $y -ge $c[1] -and $y -lt $c[3]){ $inCell=$true; break } }
      if(-not $inCell){ continue }
      $show=$false
      if($mode -eq 'WHITE'){ if($r -gt 240 -and $g -gt 240 -and $b -gt 240){ $show=$true } }
      elseif($mode -eq 'BLACK'){ if($r -lt 50 -and $g -lt 50 -and $b -lt 50){ $show=$true } }
      else { $show=$true }
      if($show){ $buf[$o]=255; $buf[$o+1]=0; $buf[$o+2]=255 }  # magenta photo
    }
  }
  [System.Runtime.InteropServices.Marshal]::Copy($buf,0,$data.Scan0,$buf.Length)
  $bmp.UnlockBits($data)
  $eps=New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]88)
  $bmp.Save((Join-Path $outDir "$id.jpg"),$jpgEnc,$eps); $eps.Dispose(); $bmp.Dispose()
  Write-Host "render $id ($mode)"
}
