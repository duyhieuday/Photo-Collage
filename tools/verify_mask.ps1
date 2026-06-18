# Mask-aware preview: replicate app render = template -> photos in cells (rotated) -> mask overlay on top.
# Mask cut thresholds MATCH app: WHITE r,g,b>240 ; GRAY mx 231..243 sat<=7 ; GRAY2 mx 208..228 sat<=10.
# ASCII only.
Add-Type -AssemblyName System.Drawing
. "D:\EZTech\EZTechApp\collage_pic_editor\tools\cells_config.ps1"
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\maskchk"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$LOGIC_W=1125; $LOGIC_H=2000; $OW=562; $OH=1000
$SX=$OW/$LOGIC_W; $SY=$OH/$LOGIC_H
$jpgEnc=[System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object{$_.MimeType -eq 'image/jpeg'}
$smpDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\vsamp"
if(-not (Test-Path (Join-Path $smpDir 'place0.png'))){ $smpDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out" }
$samples=@(); foreach($n in 0..4){ $sp=Join-Path $smpDir "place$n.png"; if(Test-Path $sp){ $samples+=[System.Drawing.Image]::FromFile($sp) } }
if($samples.Count -eq 0){ $samples=@([System.Drawing.Image]::FromFile((Join-Path $smpDir 'testimg.jpg'))) }

function Cut($mode,$r,$g,$b){
  $mx=[Math]::Max($r,[Math]::Max($g,$b)); $mn=[Math]::Min($r,[Math]::Min($g,$b))
  switch($mode){
    'WHITE' { return ($r -gt 240 -and $g -gt 240 -and $b -gt 240) }
    'GRAY'  { return ($mx -ge 231 -and $mx -le 243 -and ($mx-$mn) -le 7) }
    'GRAY2' { return ($mx -ge 208 -and $mx -le 228 -and ($mx-$mn) -le 10) }
    'BLACK' { return ($r -lt 50 -and $g -lt 50 -and $b -lt 50) }
  }
  return $false
}

foreach($id in @($args)){
  $p=Join-Path $resDir "temp_$id.jpg"
  if(-not (Test-Path $p)){ continue }
  $img=[System.Drawing.Image]::FromFile($p)
  # base = template + photos
  $base=New-Object System.Drawing.Bitmap $OW,$OH
  $gr=[System.Drawing.Graphics]::FromImage($base)
  $gr.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $gr.SmoothingMode=[System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $gr.DrawImage($img,0,0,$OW,$OH)
  $rr=$CELLS[$id]
  if($rr.Count -ge 1 -and ($rr[0] -is [int] -or $rr[0] -is [double])){ $rr=@(,$rr) }
  $k=0
  foreach($c in $rr){
    $rw=($c[2]-$c[0])*$SX; $rh=($c[3]-$c[1])*$SY
    $cx=($c[0]+$c[2])/2.0*$SX; $cy=($c[1]+$c[3])/2.0*$SY
    $ang=0.0; if($c.Count -ge 5){ $ang=[double]$c[4] }
    $smp=$samples[$k % $samples.Count]; $k++
    $st=$gr.Save()
    $gr.TranslateTransform([float]$cx,[float]$cy)
    if($ang -ne 0){ $gr.RotateTransform([float]$ang) }
    $gr.SetClip((New-Object System.Drawing.RectangleF ([float](-$rw/2),[float](-$rh/2),[float]$rw,[float]$rh)))
    $s=[Math]::Max($rw/$smp.Width,$rh/$smp.Height); $dw=$smp.Width*$s; $dh=$smp.Height*$s
    $gr.DrawImage($smp,[float](-$dw/2),[float](-$dh/2),[float]$dw,[float]$dh)
    $gr.ResetClip(); $gr.Restore($st)
  }
  $gr.Dispose()

  $mode=$MASKS[$id]
  if($mode){
    # overlay = template (nearest-neighbor at OWxOH) with cut pixels -> transparent, drawn on top
    $ov=New-Object System.Drawing.Bitmap $OW,$OH,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $go=[System.Drawing.Graphics]::FromImage($ov)
    $go.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $go.PixelOffsetMode=[System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $go.DrawImage($img,0,0,$OW,$OH); $go.Dispose()
    $rect=New-Object System.Drawing.Rectangle 0,0,$OW,$OH
    $data=$ov.LockBits($rect,[System.Drawing.Imaging.ImageLockMode]::ReadWrite,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $stride=$data.Stride; $buf=New-Object byte[] ($stride*$OH)
    [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0,$buf,0,$buf.Length)
    for($y=0;$y -lt $OH;$y++){ $row=$y*$stride
      for($x=0;$x -lt $OW;$x++){ $o=$row+$x*4
        $b=$buf[$o]; $g=$buf[$o+1]; $r=$buf[$o+2]
        if(Cut $mode $r $g $b){ $buf[$o+3]=0 }
      }
    }
    [System.Runtime.InteropServices.Marshal]::Copy($buf,0,$data.Scan0,$buf.Length)
    $ov.UnlockBits($data)
    $g2=[System.Drawing.Graphics]::FromImage($base)
    $g2.DrawImage($ov,0,0,$OW,$OH); $g2.Dispose(); $ov.Dispose()
  }
  $eps=New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0]=New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality,[long]90)
  $base.Save((Join-Path $outDir "$id.jpg"),$jpgEnc,$eps); $eps.Dispose(); $base.Dispose(); $img.Dispose()
  Write-Host ("maskchk -> {0} (mask={1})" -f $id, $(if($mode){$mode}else{'NONE'}))
}
foreach($s in $samples){ $s.Dispose() }
