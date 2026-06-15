# dump children of a named node found anywhere under an artboard, with abs coords (artboard origin=0,0)
$src="C:\Users\CHATTT\.claude\projects\D--EZTech-EZTechApp-collage-pic-editor\be985b6f-1e7d-4eed-ac3c-1f0cd3c8d33e\tool-results\mcp-figma-mcp-go-get_node-1781512145630.txt"
$root = Get-Content -Raw $src | ConvertFrom-Json
$target=$args[0]      # artboard
$nodename=$args[1]    # node name to find (first match)
$matchidx=if($args.Count -ge 3){[int]$args[2]}else{0}
$found=$null; $foundCount=0
function Find($node,$ax,$ay){
  foreach($c in $node.children){
    $cx=$ax+[double]$c.bounds.x; $cy=$ay+[double]$c.bounds.y
    if([string]$c.name -eq $script:nodename){
      if($script:foundCount -eq $script:matchidx){ $script:found=@{n=$c;x=$cx;y=$cy}; }
      $script:foundCount++
    }
    if($c.children){ Find $c $cx $cy }
  }
}
foreach($ab in $root.children){
  if([string]$ab.name -eq $target){ Find $ab 0 0 }
}
if($null -eq $found){ Write-Host "NOT FOUND"; return }
$n=$found.n; $bx=$found.x; $by=$found.y
Write-Host ("NODE {0} [{1}] abs=({2:N1},{3:N1}) wh=({4}x{5})" -f $n.name,$n.type,$bx,$by,$n.bounds.width,$n.bounds.height)
function Walk($node,$ax,$ay,$d){
  foreach($c in $node.children){
    $cx=$ax+[double]$c.bounds.x; $cy=$ay+[double]$c.bounds.y
    $fill=""; if($c.styles -and $c.styles.fills){ $fill=(@($c.styles.fills)-join"|") }
    $cc=0; if($c.children){ $cc=@($c.children).Count }
    Write-Host ("{0}{1} [{2}] abs=({3:N1},{4:N1}) wh=({5}x{6}) ch={7} fill={8}" -f ("  "*$d),$c.name,$c.type,$cx,$cy,$c.bounds.width,$c.bounds.height,$cc,$fill)
    if($c.children -and $d -lt 3){ Walk $c $cx $cy ($d+1) }
  }
}
Walk $n $bx $by 1
