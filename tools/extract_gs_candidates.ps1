# List photo-cell candidates per artboard with accumulated abs coords (1080 space). ASCII only.
param([string]$Target)
$p="C:\Users\CHATTT\.claude\projects\D--EZTech-EZTechApp-collage-pic-editor\be985b6f-1e7d-4eed-ac3c-1f0cd3c8d33e\tool-results\mcp-figma-mcp-go-get_node-1781512171273.txt"
$root = Get-Content -Raw $p | ConvertFrom-Json

function GrayFill($node){
  if($node.styles -and $node.styles.fills){
    foreach($f in @($node.styles.fills)){
      $s=([string]$f).ToLower()
      if($s -match '^#([0-9a-f]{2})([0-9a-f]{2})([0-9a-f]{2})$'){
        $r=[Convert]::ToInt32($matches[1],16); $g=[Convert]::ToInt32($matches[2],16); $b=[Convert]::ToInt32($matches[3],16)
        $mx=[Math]::Max($r,[Math]::Max($g,$b)); $mn=[Math]::Min($r,[Math]::Min($g,$b))
        if(($mx-$mn) -le 12 -and $mx -ge 190 -and $mx -le 250){ return $true }
      }
    }
  }
  return $false
}
function FillStr($node){
  $out=@()
  if($node.styles -and $node.styles.fills){ foreach($f in @($node.styles.fills)){ $out += [string]$f } }
  return ($out -join ';')
}

$global:cands=@()
function Walk($node,$ox,$oy,$path){
  $cx=$ox + [double]$node.bounds.x
  $cy=$oy + [double]$node.bounds.y
  $w=[double]$node.bounds.width; $h=[double]$node.bounds.height
  $nm=[string]$node.name
  $kids=0; if($node.children){ $kids=@($node.children).Count }
  $isCell=$false; $reason=''
  if(($node.type -eq 'FRAME' -or $node.type -eq 'RECTANGLE') -and $w -ge 100 -and $h -ge 100 -and -not($w -gt 1050 -and $h -gt 1850)){
    if(GrayFill $node){ $isCell=$true; $reason='gray:'+(FillStr $node) }
    elseif($node.type -eq 'FRAME' -and $nm -match 'Frame \d{4,}'){ $isCell=$true; $reason='frameNNN kids='+$kids }
  }
  if($isCell){
    $global:cands += [pscustomobject]@{ path=("$path/$nm"); type=$node.type; x=$cx; y=$cy; w=$w; h=$h; kids=$kids; reason=$reason }
  }
  if($node.children){ foreach($c in $node.children){ Walk $c $cx $cy ("$path/$nm") } }
}

$matches2 = $root.children | Where-Object { $_.name -eq $Target }
if(@($matches2).Count -gt 1){ $ab = $matches2 | Where-Object { $_.bounds.y -eq 3044 } | Select-Object -First 1 }
else { $ab = $matches2 | Select-Object -First 1 }
if(-not $ab){ "NOT FOUND: $Target"; exit }
foreach($c in $ab.children){ Walk $c 0 0 $Target }
"=== $Target candidates (1080 space) ==="
foreach($c in $global:cands){
  "x={0,7:n1} y={1,7:n1} w={2,7:n1} h={3,7:n1}  {4,-9} kids={5} [{6}]  {7}" -f $c.x,$c.y,$c.w,$c.h,$c.type,$c.kids,$c.reason,$c.path
}
