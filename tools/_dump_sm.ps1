# Dump tree structure for sm artboards (Temp_SMnn). ASCII only.
$src="C:\Users\CHATTT\.claude\projects\D--EZTech-EZTechApp-collage-pic-editor\be985b6f-1e7d-4eed-ac3c-1f0cd3c8d33e\tool-results\mcp-figma-mcp-go-get_node-1781512145630.txt"
$root = Get-Content -Raw $src | ConvertFrom-Json

function FillStr($node){
  $out=""
  if($node.styles -and $node.styles.fills){
    $out = (@($node.styles.fills) -join "|")
  }
  return $out
}

function Walk($node,$depth,$ox,$oy){
  $nm=[string]$node.name
  $ty=[string]$node.type
  $bx=[double]$node.bounds.x; $by=[double]$node.bounds.y
  $bw=[double]$node.bounds.width; $bh=[double]$node.bounds.height
  $ax=$ox+$bx; $ay=$oy+$by
  $pad = "  " * $depth
  $fill=FillStr $node
  $cc=0; if($node.children){ $cc=@($node.children).Count }
  Write-Host ("{0}{1} [{2}] abs=({3:N0},{4:N0}) wh=({5:N0}x{6:N0}) ch={7} fill={8}" -f $pad,$nm,$ty,$ax,$ay,$bw,$bh,$cc,$fill)
  if($node.children -and $depth -lt 6){
    foreach($c in $node.children){ Walk $c ($depth+1) $ax $ay }
  }
}

$target=$args[0]
foreach($ab in $root.children){
  $nm=[string]$ab.name
  if($nm -eq $target){
    Walk $ab 0 0 0
  }
}
