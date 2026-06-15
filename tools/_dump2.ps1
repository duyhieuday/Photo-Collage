# Dump immediate children raw bounds (relative to parent) for an artboard, and one level deep.
$src="C:\Users\CHATTT\.claude\projects\D--EZTech-EZTechApp-collage-pic-editor\be985b6f-1e7d-4eed-ac3c-1f0cd3c8d33e\tool-results\mcp-figma-mcp-go-get_node-1781512145630.txt"
$root = Get-Content -Raw $src | ConvertFrom-Json
$target=$args[0]
foreach($ab in $root.children){
  if([string]$ab.name -eq $target){
    Write-Host ("ARTBOARD {0} rawbounds=({1},{2}) wh=({3}x{4})" -f $ab.name,$ab.bounds.x,$ab.bounds.y,$ab.bounds.width,$ab.bounds.height)
    foreach($c in $ab.children){
      $fill=""; if($c.styles -and $c.styles.fills){ $fill=(@($c.styles.fills)-join"|") }
      $cc=0; if($c.children){ $cc=@($c.children).Count }
      Write-Host ("  CHILD {0} [{1}] rel=({2},{3}) wh=({4}x{5}) ch={6} fill={7}" -f $c.name,$c.type,$c.bounds.x,$c.bounds.y,$c.bounds.width,$c.bounds.height,$cc,$fill)
    }
  }
}
