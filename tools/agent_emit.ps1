# Emit final cells_bd.txt from 1080-space raw cells (matches verified agent_render).
$SCALE = 1125.0/1080.0
$RAW = [ordered]@{
  "bd01"=@(@(329,476,769,946),@(124,947,564,1417),@(502,1434,942,1904))
  "bd02"=@(@(-3,1374,323,1724),@(377,1374,703,1724),@(757,1374,1083,1724))
  "bd03"=@(@(477,86,1012,801),@(105,1032,627,1730))
  "bd04"=@(@(629,405,1039,947),@(667,1132,1019,1601))
  "bd05"=@(,@(256,616,917,1504))
  "bd06"=@(,@(146,152,924,1181))
  "bd07"=@(,@(113,543,927,1319))
  "bd08"=@(,@(222,683,858,1399))
  "bd09"=@(@(63,535,562,1044),@(575,1171,993,1596))
  "bd10"=@(@(584,407,1040,853),@(584,898,1040,1346),@(584,1391,1040,1847),@(84,1391,540,1847))
}
$MASKS=@{ "bd02"="NONE"; "bd04"="NONE"; "bd08"="NONE" }

function Scale-Clamp($v,$max){
  $x=[int][math]::Round($v*$SCALE)
  if($x -lt 0){$x=0}; if($x -gt $max){$x=$max}
  return $x
}
$lines=@()
foreach($id in $RAW.Keys){
  $parts=@()
  foreach($c in $RAW[$id]){
    $l=Scale-Clamp $c[0] 1125; $t=Scale-Clamp $c[1] 2000; $r=Scale-Clamp $c[2] 1125; $b=Scale-Clamp $c[3] 2000
    $parts += ("@({0},{1},{2},{3})" -f $l,$t,$r,$b)
  }
  if($parts.Count -eq 1){
    $lines += ('  "{0}"=@(,{1})' -f $id,$parts[0])
  } else {
    $lines += ('  "{0}"=@({1})' -f $id, ($parts -join ','))
  }
}
$maskLines=@()
foreach($id in $RAW.Keys){ if($MASKS.ContainsKey($id)){ $maskLines += ('  "{0}"="{1}"' -f $id,$MASKS[$id]) } }
$out="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\cells_bd.txt"
($lines -join "`r`n") + "`r`n---MASKS---`r`n" + ($maskLines -join "`r`n") + "`r`n" | Out-File $out -Encoding ascii
Get-Content $out
