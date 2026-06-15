# Find largest white rectangular openings in a temp_isNN.jpg (logic 1125x2000). ASCII only.
Add-Type -AssemblyName System.Drawing
$id=$args[0]
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$img=[System.Drawing.Image]::FromFile((Join-Path $resDir "temp_$id.jpg"))
$bmp=New-Object System.Drawing.Bitmap $img
$IW=$bmp.Width; $IH=$bmp.Height
$step=4
$cols=[int]($IW/$step); $rows=[int]($IH/$step)
$mask=New-Object 'bool[,]' $rows,$cols
for($ry=0;$ry -lt $rows;$ry++){
  for($rx=0;$rx -lt $cols;$rx++){
    $px=$bmp.GetPixel($rx*$step,$ry*$step)
    $mx=[Math]::Max($px.R,[Math]::Max($px.G,$px.B)); $mn=[Math]::Min($px.R,[Math]::Min($px.G,$px.B))
    if($px.R -ge 248 -and $px.G -ge 248 -and $px.B -ge 248 -and ($mx-$mn) -le 6){ $mask[$ry,$rx]=$true }
  }
}
$visited=New-Object 'bool[,]' $rows,$cols
$blobs=@()
for($ry=0;$ry -lt $rows;$ry++){
  for($rx=0;$rx -lt $cols;$rx++){
    if($mask[$ry,$rx] -and -not $visited[$ry,$rx]){
      $stack=New-Object System.Collections.Stack
      $stack.Push(@($ry,$rx)); $visited[$ry,$rx]=$true
      $minx=$rx;$maxx=$rx;$miny=$ry;$maxy=$ry;$cnt=0
      while($stack.Count -gt 0){
        $p=$stack.Pop(); $cyy=$p[0];$cxx=$p[1]; $cnt++
        if($cxx -lt $minx){$minx=$cxx}; if($cxx -gt $maxx){$maxx=$cxx}
        if($cyy -lt $miny){$miny=$cyy}; if($cyy -gt $maxy){$maxy=$cyy}
        foreach($d in @(@(-1,0),@(1,0),@(0,-1),@(0,1))){
          $ny=$cyy+$d[0]; $nx=$cxx+$d[1]
          if($ny -ge 0 -and $ny -lt $rows -and $nx -ge 0 -and $nx -lt $cols -and $mask[$ny,$nx] -and -not $visited[$ny,$nx]){
            $visited[$ny,$nx]=$true; $stack.Push(@($ny,$nx))
          }
        }
      }
      $bw=($maxx-$minx+1); $bh=($maxy-$miny+1)
      $blobs += [pscustomobject]@{area=$cnt; x0=$minx*$step; y0=$miny*$step; x1=$maxx*$step; y1=$maxy*$step; fill=($cnt/($bw*$bh))}
    }
  }
}
$bmp.Dispose();$img.Dispose()
$top=$blobs | Where-Object {$_.area -gt 300 -and $_.fill -gt 0.7} | Sort-Object area -Descending | Select-Object -First 8
foreach($b in $top){
  $l=[int][math]::Round($b.x0*1125.0/$IW); $t=[int][math]::Round($b.y0*2000.0/$IH)
  $r=[int][math]::Round($b.x1*1125.0/$IW); $bb=[int][math]::Round($b.y1*2000.0/$IH)
  "area=$($b.area) fill=$([math]::Round($b.fill,2)) rect=@($l,$t,$r,$bb)"
}
