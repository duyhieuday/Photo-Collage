# Dump full node tree for each Temp_ISnn artboard. ASCII only.
$src="C:\Users\CHATTT\.claude\projects\D--EZTech-EZTechApp-collage-pic-editor\be985b6f-1e7d-4eed-ac3c-1f0cd3c8d33e\tool-results\mcp-figma-mcp-go-get_node-1781512172129.txt"
$root=Get-Content -Raw $src | ConvertFrom-Json
$target=$args[0]

function FillStr($node){
  $out=@()
  if($node.fills){
    foreach($f in @($node.fills)){
      $out += ([string]$f)
    }
  }
  if($node.styles -and $node.styles.fills){
    foreach($f in @($node.styles.fills)){ $out += ("S:"+[string]$f) }
  }
  return ($out -join "|")
}

function Dump($node,$depth){
  $ind=("  "*$depth)
  $b=$node.bounds
  $bs = if($b){ "$([math]::Round($b.x)),$([math]::Round($b.y)) $([math]::Round($b.width))x$([math]::Round($b.height))" } else { "no-bounds" }
  $kids = if($node.children){ @($node.children).Count } else { 0 }
  $fill=FillStr $node
  "$ind[$($node.type)] '$($node.name)' $bs kids=$kids fill=$fill"
  if($node.children){ foreach($c in $node.children){ Dump $c ($depth+1) } }
}

foreach($ab in $root.children){
  if($ab.name -eq $target){ Dump $ab 0 }
}
