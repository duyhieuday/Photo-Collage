# Detect gray photo-opening regions in a template PNG, report bounding boxes in logic space 1125x2000.
# Connected-component labelling on a downsampled grayscale mask.
Add-Type -AssemblyName System.Drawing
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$id=$args[0]
$loMin=[int]$args[1]   # gray min brightness
$loMax=[int]$args[2]   # gray max brightness
$img=[System.Drawing.Image]::FromFile((Join-Path $resDir "temp_$id.jpg"))
$bmp=New-Object System.Drawing.Bitmap $img
$W=$bmp.Width; $H=$bmp.Height
# downsample factor
$step=5
$gw=[int][math]::Floor($W/$step); $gh=[int][math]::Floor($H/$step)
$mask=New-Object 'int[,]' $gw,$gh
for($gy=0;$gy -lt $gh;$gy++){
  for($gx=0;$gx -lt $gw;$gx++){
    $px=$bmp.GetPixel($gx*$step,$gy*$step)
    $mx=[math]::Max($px.R,[math]::Max($px.G,$px.B)); $mn=[math]::Min($px.R,[math]::Min($px.G,$px.B))
    if(($mx-$mn) -le 14 -and $mx -ge $loMin -and $mx -le $loMax){ $mask[$gx,$gy]=-1 } else { $mask[$gx,$gy]=0 }
  }
}
$bmp.Dispose();$img.Dispose()
# flood fill label
$lbl=1
$comp=@{}
$stack=New-Object System.Collections.Stack
for($gy=0;$gy -lt $gh;$gy++){
  for($gx=0;$gx -lt $gw;$gx++){
    if($mask[$gx,$gy] -eq -1){
      $stack.Clear(); $stack.Push(@($gx,$gy)); $mask[$gx,$gy]=$lbl
      $minx=$gx;$maxx=$gx;$miny=$gy;$maxy=$gy;$cnt=0
      while($stack.Count -gt 0){
        $p=$stack.Pop(); $cx=$p[0];$cy=$p[1];$cnt++
        if($cx -lt $minx){$minx=$cx}; if($cx -gt $maxx){$maxx=$cx}
        if($cy -lt $miny){$miny=$cy}; if($cy -gt $maxy){$maxy=$cy}
        foreach($d in @(@(1,0),@(-1,0),@(0,1),@(0,-1))){
          $nx=$cx+$d[0];$ny=$cy+$d[1]
          if($nx -ge 0 -and $nx -lt $gw -and $ny -ge 0 -and $ny -lt $gh -and $mask[$nx,$ny] -eq -1){
            $mask[$nx,$ny]=$lbl; $stack.Push(@($nx,$ny))
          }
        }
      }
      $comp[$lbl]=@{minx=$minx;maxx=$maxx;miny=$miny;maxy=$maxy;cnt=$cnt}
      $lbl++
    }
  }
}
$SX=1125.0/$W; $SY=2000.0/$H
$results=@()
foreach($k in $comp.Keys){
  $c=$comp[$k]
  $area=$c.cnt
  if($area -lt 200){ continue }   # ignore small
  $lx=[int]($c.minx*$step*$SX); $ty=[int]($c.miny*$step*$SY)
  $rx=[int](($c.maxx+1)*$step*$SX); $by=[int](($c.maxy+1)*$step*$SY)
  $results += [pscustomobject]@{area=$area;l=$lx;t=$ty;r=$rx;b=$by}
}
$results | Sort-Object -Property area -Descending | ForEach-Object {
  "area={0} rect=@({1},{2},{3},{4}) wh={5}x{6}" -f $_.area,$_.l,$_.t,$_.r,$_.b,($_.r-$_.l),($_.b-$_.t)
}
