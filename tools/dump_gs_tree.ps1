# Dump tree of one artboard to inspect node structure. ASCII only.
param([string]$Target)
$p="C:\Users\CHATTT\.claude\projects\D--EZTech-EZTechApp-collage-pic-editor\be985b6f-1e7d-4eed-ac3c-1f0cd3c8d33e\tool-results\mcp-figma-mcp-go-get_node-1781512171273.txt"
$root = Get-Content -Raw $p | ConvertFrom-Json

function FillStr($node){
  $out=@()
  if($node.styles -and $node.styles.fills){
    foreach($f in @($node.styles.fills)){ $out += [string]$f }
  }
  if($node.fills){
    foreach($f in @($node.fills)){
      if($f.color){ $out += ("rgba({0:n2},{1:n2},{2:n2},{3:n2})" -f $f.color.r,$f.color.g,$f.color.b,$f.color.a) }
    }
  }
  return ($out -join ';')
}

function Walk($node,$depth,$ox,$oy){
  $cx=$ox + [double]$node.bounds.x
  $cy=$oy + [double]$node.bounds.y
  $ind = ' ' * ($depth*2)
  $kids = 0; if($node.children){ $kids = @($node.children).Count }
  "{0}{1} [{2}] abs=({3:n0},{4:n0}) wh=({5:n0}x{6:n0}) kids={7} fill={8}" -f $ind,$node.name,$node.type,$cx,$cy,$node.bounds.width,$node.bounds.height,$kids,(FillStr $node)
  if($node.children){
    foreach($c in $node.children){ Walk $c ($depth+1) $cx $cy }
  }
}

# pick target: prefer exact name match; for GS06-10 pick the y=3044 row
$ab = $null
$matches2 = $root.children | Where-Object { $_.name -eq $Target }
if(@($matches2).Count -gt 1){
  $ab = $matches2 | Where-Object { $_.bounds.y -eq 3044 } | Select-Object -First 1
} else {
  $ab = $matches2 | Select-Object -First 1
}
if(-not $ab){ "NOT FOUND: $Target"; exit }
# seed offset at 0,0 (ignore artboard's own absolute bounds)
$ind0=''
$kids0=0; if($ab.children){ $kids0=@($ab.children).Count }
"{0} [{1}] origin wh=({2:n0}x{3:n0}) kids={4}" -f $ab.name,$ab.type,$ab.bounds.width,$ab.bounds.height,$kids0
foreach($c in $ab.children){ Walk $c 1 0 0 }
