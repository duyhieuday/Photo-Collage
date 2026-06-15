# Parse Figma node JSON dumps -> exact photo-cell rects (logic space 1125x2000).
# Cells = FRAME nodes with gray fill (#ededed/#e3e3e3), bounds relative to artboard.
# Also samples each cell-center in the actual PNG to pick WHITE vs NONE mask.
# ASCII only.
Add-Type -AssemblyName System.Drawing

$dir="C:\Users\CHATTT\.claude\projects\D--EZTech-EZTechApp-collage-pic-editor\be985b6f-1e7d-4eed-ac3c-1f0cd3c8d33e\tool-results"
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$files=@(
  "$dir\mcp-figma-mcp-go-get_node-1781512031398.txt"  # Birthday
  "$dir\mcp-figma-mcp-go-get_node-1781512145630.txt"  # Summer
  "$dir\mcp-figma-mcp-go-get_node-1781512160423.txt"  # Couple
  "$dir\mcp-figma-mcp-go-get_node-1781512171273.txt"  # Glad season
  "$dir\mcp-figma-mcp-go-get_node-1781512172129.txt"  # IG Story
)
$SCALE = 1125.0/1080.0

function Has-GrayFill($node){
  if($node.styles -and $node.styles.fills){
    foreach($f in @($node.styles.fills)){
      $s=([string]$f).ToLower()
      if($s -match '^#([0-9a-f]{2})([0-9a-f]{2})([0-9a-f]{2})$'){
        $r=[Convert]::ToInt32($matches[1],16); $g=[Convert]::ToInt32($matches[2],16); $b=[Convert]::ToInt32($matches[3],16)
        $mx=[Math]::Max($r,[Math]::Max($g,$b)); $mn=[Math]::Min($r,[Math]::Min($g,$b))
        if(($mx-$mn) -le 10 -and $mx -ge 195 -and $mx -le 248){ return $true }
      }
    }
  }
  return $false
}

function Is-CellNode($c){
  if($c.type -ne 'FRAME' -and $c.type -ne 'RECTANGLE'){ return $false }
  $nm=[string]$c.name
  if($nm -match '^(Temp_|Thumb_)'){ return $false }
  $w=[double]$c.bounds.width; $h=[double]$c.bounds.height
  if($w -gt 1000 -and $h -gt 1750){ return $false }   # artboard-sized
  if($w -lt 110 -or $h -lt 110){ return $false }       # too small (decoration)
  # gray placeholder cell
  if(Has-GrayFill $c){ return $true }
  # white empty photo-frame named "Frame NNNNN" (no children = leaf placeholder)
  if($c.type -eq 'FRAME' -and $nm -match 'Frame \d{4,}'){
    $cnt = 0; if($c.children){ $cnt = @($c.children).Count }
    if($cnt -eq 0){ return $true }
  }
  return $false
}

function Collect($node,$ox,$oy,$acc){
  if(-not $node.children){ return }
  foreach($c in $node.children){
    $cx=$ox + [double]$c.bounds.x; $cy=$oy + [double]$c.bounds.y
    $w=[double]$c.bounds.width; $h=[double]$c.bounds.height
    if(Is-CellNode $c){
      $acc.Add(@([int][math]::Round($cx*$SCALE),[int][math]::Round($cy*$SCALE),[int][math]::Round(($cx+$w)*$SCALE),[int][math]::Round(($cy+$h)*$SCALE))) | Out-Null
    } elseif($c.type -eq 'FRAME' -or $c.type -eq 'GROUP'){
      Collect $c $cx $cy $acc
    }
  }
}

$cells = @{}
$srcKind = @{}   # id -> 'Temp' or 'Thumb' (prefer Temp)
foreach($file in $files){
  if(-not (Test-Path $file)){ Write-Host "MISSING $file"; continue }
  $root = Get-Content -Raw $file | ConvertFrom-Json
  foreach($ab in $root.children){
    $nm=[string]$ab.name
    $kind=$null; $id=$null
    if($nm -match '^Temp_([A-Za-z]+[0-9]+)$'){ $kind='Temp'; $id=$matches[1].ToLower() }
    elseif($nm -match '^Thumb_([A-Za-z]+[0-9]+)$'){ $kind='Thumb'; $id=$matches[1].ToLower() }
    if($id -eq $null){ continue }
    # prefer Temp over Thumb
    if($cells.ContainsKey($id) -and $srcKind[$id] -eq 'Temp' -and $kind -eq 'Thumb'){ continue }
    $acc=New-Object System.Collections.ArrayList
    Collect $ab 0 0 $acc
    if($acc.Count -gt 0){ $cells[$id]=$acc; $srcKind[$id]=$kind }
  }
}

# Sample PNG cell-center -> mask decision
function Pick-Mask($id,$rects){
  $p=Join-Path $resDir "temp_$id.jpg"
  if(-not (Test-Path $p)){ return $null }   # no image for this id -> skip
  $img=[System.Drawing.Image]::FromFile($p)
  $bmp=New-Object System.Drawing.Bitmap $img
  $whiteVotes=0; $grayVotes=0
  foreach($r in $rects){
    $cx=[int]((($r[0]+$r[2])/2)/1125.0*$bmp.Width)
    $cy=[int]((($r[1]+$r[3])/2)/2000.0*$bmp.Height)
    if($cx -lt 0){$cx=0}; if($cx -ge $bmp.Width){$cx=$bmp.Width-1}
    if($cy -lt 0){$cy=0}; if($cy -ge $bmp.Height){$cy=$bmp.Height-1}
    $px=$bmp.GetPixel($cx,$cy)
    if($px.R -ge 247 -and $px.G -ge 247 -and $px.B -ge 247){ $whiteVotes++ } else { $grayVotes++ }
  }
  $bmp.Dispose();$img.Dispose()
  if($grayVotes -gt $whiteVotes){ return 'NONE' } else { return 'WHITE' }
}

# Emit config snippet + diagnostics. Clamp minor overflow; reject templates with wild coords.
$lines=@(); $maskLines=@()
foreach($id in ($cells.Keys | Sort-Object)){
  $rects=$cells[$id]
  $wild=$false
  foreach($r in $rects){ if($r[0] -lt -80 -or $r[1] -lt -80 -or $r[2] -gt 1205 -or $r[3] -gt 2150){ $wild=$true } }
  if($wild){ Write-Host ("{0}: SUSPECT (out-of-bounds) - skipped" -f $id); continue }
  if(-not (Test-Path (Join-Path $resDir "temp_$id.jpg"))){ Write-Host ("{0}: no image - skipped" -f $id); continue }
  $parts=@()
  foreach($r in $rects){
    $l=[Math]::Max(0,$r[0]); $t=[Math]::Max(0,$r[1]); $rr=[Math]::Min(1125,$r[2]); $bb=[Math]::Min(2000,$r[3])
    $parts += ("@({0},{1},{2},{3})" -f $l,$t,$rr,$bb)
  }
  $lines += ('  "{0}"=@({1})' -f $id, ($parts -join ','))
  $m=Pick-Mask $id $rects
  if($m -eq 'NONE'){ $maskLines += ('  "{0}"="NONE"' -f $id) }
  Write-Host ("{0}: {1} cells, mask={2} ACCEPTED" -f $id, $rects.Count, $m)
}
$out="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\figma_cells_snippet.txt"
($lines -join "`n") + "`n---MASKS---`n" + ($maskLines -join "`n") | Out-File $out -Encoding ascii
Write-Host "snippet -> $out"
