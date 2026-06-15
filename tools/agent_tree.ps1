# Dump full nested tree (with accumulated offsets) for one artboard index.
param([int]$idx)
$file="C:\Users\CHATTT\.claude\projects\D--EZTech-EZTechApp-collage-pic-editor\be985b6f-1e7d-4eed-ac3c-1f0cd3c8d33e\tool-results\mcp-figma-mcp-go-get_node-1781512031398.txt"
$root = Get-Content -Raw $file | ConvertFrom-Json
$ab=$root.children[$idx]
Write-Host ("=== [{0}] '{1}' w={2} h={3} ===" -f $idx,$ab.name,$ab.bounds.width,$ab.bounds.height)

function FillStr($node){
  $s=""
  if($node.styles -and $node.styles.fills){ $s=(@($node.styles.fills) -join '|') }
  if($node.fills){
    foreach($f in @($node.fills)){
      if($f.type -eq 'IMAGE'){ $s+=" IMG" }
      elseif($f.color){ $s+=(" rgb({0:N2},{1:N2},{2:N2})" -f $f.color.r,$f.color.g,$f.color.b) }
    }
  }
  return $s
}

function Walk($node,$ox,$oy,$depth){
  if(-not $node.children){ return }
  foreach($c in $node.children){
    $cx=$ox+[double]$c.bounds.x; $cy=$oy+[double]$c.bounds.y
    $w=[double]$c.bounds.width; $h=[double]$c.bounds.height
    $kids=0; if($c.children){$kids=@($c.children).Count}
    $ind=("  "*$depth)
    Write-Host ("{0}{1} '{2}' abs=({3:N0},{4:N0}) wh=({5:N0}x{6:N0}) kids={7} fill=[{8}]" -f $ind,$c.type,$c.name,$cx,$cy,$w,$h,$kids,(FillStr $c))
    if($kids -gt 0){ Walk $c $cx $cy ($depth+1) }
  }
}
Walk $ab 0 0 0
