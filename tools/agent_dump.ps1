# Diagnostic: dump artboard tree for the birthday figma file.
$file="C:\Users\CHATTT\.claude\projects\D--EZTech-EZTechApp-collage-pic-editor\be985b6f-1e7d-4eed-ac3c-1f0cd3c8d33e\tool-results\mcp-figma-mcp-go-get_node-1781512031398.txt"
$root = Get-Content -Raw $file | ConvertFrom-Json
Write-Host ("ROOT name={0} type={1} childcount={2}" -f $root.name,$root.type,@($root.children).Count)
foreach($ab in $root.children){
  Write-Host ("AB: name='{0}' type={1} w={2} h={3} x={4} y={5}" -f $ab.name,$ab.type,$ab.bounds.width,$ab.bounds.height,$ab.bounds.x,$ab.bounds.y)
}
