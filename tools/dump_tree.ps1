param([string]$want="cp01")
$f="C:\Users\CHATTT\.claude\projects\D--EZTech-EZTechApp-collage-pic-editor\be985b6f-1e7d-4eed-ac3c-1f0cd3c8d33e\tool-results\mcp-figma-mcp-go-get_node-1781512160423.txt"
$root = Get-Content -Raw $f | ConvertFrom-Json
$SCALE=1125.0/1080.0
function Fills($n){
  $out=""
  if($n.styles -and $n.styles.fills){ foreach($x in @($n.styles.fills)){ $out += ([string]$x)+" " } }
  if($n.fills){ foreach($x in @($n.fills)){ if($x.color){ $r=[int]($x.color.r*255);$g=[int]($x.color.g*255);$b=[int]($x.color.b*255); $out += ("rgb($r,$g,$b) ") } } }
  return $out.Trim()
}
function Walk($n,$ox,$oy,$d){
  foreach($c in $n.children){
    $cx=$ox+[double]$c.bounds.x; $cy=$oy+[double]$c.bounds.y
    $w=[double]$c.bounds.width; $h=[double]$c.bounds.height
    $l=[int][math]::Round($cx*$SCALE);$t=[int][math]::Round($cy*$SCALE);$r=[int][math]::Round(($cx+$w)*$SCALE);$bm=[int][math]::Round(($cy+$h)*$SCALE)
    $ind=" " * ($d*2)
    $kc=0; if($c.children){$kc=@($c.children).Count}
    Write-Output ("{0}{1} [{2}] fill='{3}' kids={4} rect=({5},{6},{7},{8}) wh=({9}x{10})" -f $ind,$c.name,$c.type,(Fills $c),$kc,$l,$t,$r,$bm,[int]$w,[int]$h)
    if($c.children){ Walk $c $cx $cy ($d+1) }
  }
}
foreach($ab in $root.children){
  if($ab.name -eq "Temp_$($want.ToUpper())"){
    Write-Output ("=== " + $ab.name + " ===")
    Walk $ab 0 0 1
  }
}
