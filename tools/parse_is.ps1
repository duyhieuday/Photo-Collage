# Extract photo-cell rects for is01..is15 in logic space 1125x2000. ASCII only.
Add-Type -AssemblyName System.Drawing
$src="C:\Users\CHATTT\.claude\projects\D--EZTech-EZTechApp-collage-pic-editor\be985b6f-1e7d-4eed-ac3c-1f0cd3c8d33e\tool-results\mcp-figma-mcp-go-get_node-1781512172129.txt"
$resDir="D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$root=Get-Content -Raw $src | ConvertFrom-Json
$SCALE=1125.0/1080.0

# Per-id explicit list of cell node-names to pick (matched at any depth, with accumulated offset).
# We collect ALL nodes whose name matches and use their accumulated abs bounds.
$pick=@{
  'is01'=@('Frame 1000007301','Frame 1000007302','Frame 1000007303','Frame 1000007304')
  'is02'=@('Rectangle 1871')
  'is03'=@('Rectangle 1870')
  'is04'=@('Frame 1000007376','Frame 1000007377','Frame 1000007378')
  'is05'=@('Frame 1000007364','Frame 1000007365','Frame 1000007366','Frame 1000007367','Frame 1000007368')
  'is06'=@('Rectangle 1905','Rectangle 1906')  # plus two vector mask groups handled specially
  'is07'=@('Frame 1000007353','Frame 1000007354')
  'is08'=@('Frame 1000007327','Frame 1000007328','Frame 1000007329')
  'is09'=@('Frame 1000007332','Frame 1000007335','Frame 1000007333','Frame 1000007337')
  'is10'=@('Rectangle 1867','Rectangle 1868')
  'is11'=@()  # full-bleed single cell = whole artboard
  'is12'=@('Rectangle 1908','Rectangle 1909','Rectangle 1910')
  'is13'=@('Rectangle 1911','Rectangle 1912','Rectangle 1913','Rectangle 1914','Rectangle 1915')
  'is14'=@('Rectangle 1916','Rectangle 1917','Rectangle 1918','Rectangle 1919')
  'is15'=@('Rectangle 1920','Rectangle 1922')
}
# is06 mask-group photo openings are the white Vector/Rectangle FIRST child of each 'Mask group'.
# We'll grab all 4 mask-group first-children by special handling.

$found=@{}  # id -> list of @(l,t,r,b) abs(1080 space)

function Walk($node,$ox,$oy,$wanted,$acc){
  if(-not $node.children){ return }
  foreach($c in $node.children){
    $cx=$ox+[double]$c.bounds.x; $cy=$oy+[double]$c.bounds.y
    $w=[double]$c.bounds.width; $h=[double]$c.bounds.height
    if($wanted -contains [string]$c.name){
      $acc.Add(@($cx,$cy,($cx+$w),($cy+$h))) | Out-Null
    }
    Walk $c $cx $cy $wanted $acc
  }
}

# Special: is06 - first child of every 'Mask group'
function WalkMaskGroups($node,$ox,$oy,$acc){
  if(-not $node.children){ return }
  foreach($c in $node.children){
    $cx=$ox+[double]$c.bounds.x; $cy=$oy+[double]$c.bounds.y
    $w=[double]$c.bounds.width; $h=[double]$c.bounds.height
    if([string]$c.name -eq 'Mask group'){
      # Mask group bounds are already artboard-relative (its first child duplicates these coords).
      $acc.Add(@($cx,$cy,($cx+$w),($cy+$h))) | Out-Null
    }
    WalkMaskGroups $c $cx $cy $acc
  }
}

foreach($ab in $root.children){
  if($ab.name -notmatch '^Temp_(IS\d+)$'){ continue }
  $id=$matches[1].ToLower()
  $acc=New-Object System.Collections.ArrayList
  if($id -eq 'is11'){
    # Photo openings baked into background image; measured from PNG (already logic 1125x2000).
    # Stored in logic space; divide back by SCALE so the later x SCALE re-scaling is a no-op.
    $acc.Add(@((116/$SCALE),(252/$SCALE),(564/$SCALE),(884/$SCALE))) | Out-Null
    $acc.Add(@((468/$SCALE),(1120/$SCALE),(1004/$SCALE),(1880/$SCALE))) | Out-Null
  } elseif($id -eq 'is06'){
    WalkMaskGroups $ab 0 0 $acc
  } else {
    Walk $ab 0 0 $pick[$id] $acc
  }
  $found[$id]=$acc
}

# Build scaled/clamped rects and emit
function Pick-Mask($id,$rects){
  $p=Join-Path $resDir "temp_$id.jpg"
  if(-not (Test-Path $p)){ return 'WHITE' }
  $img=[System.Drawing.Image]::FromFile($p)
  $bmp=New-Object System.Drawing.Bitmap $img
  $r=$rects[0]
  $cx=[int]((($r[0]+$r[2])/2)/1125.0*$bmp.Width)
  $cy=[int]((($r[1]+$r[3])/2)/2000.0*$bmp.Height)
  if($cx -lt 0){$cx=0}; if($cx -ge $bmp.Width){$cx=$bmp.Width-1}
  if($cy -lt 0){$cy=0}; if($cy -ge $bmp.Height){$cy=$bmp.Height-1}
  $px=$bmp.GetPixel($cx,$cy)
  $bmp.Dispose();$img.Dispose()
  if($px.R -ge 247 -and $px.G -ge 247 -and $px.B -ge 247){ return 'WHITE' } else { return 'NONE' }
}

$lines=@(); $maskLines=@()
$ids=1..15 | ForEach-Object { "is{0:D2}" -f $_ }
foreach($id in $ids){
  $rects=$found[$id]
  if(-not $rects -or $rects.Count -eq 0){ Write-Host "$id : NO CELLS"; continue }
  $scaled=@()
  foreach($r in $rects){
    $l=[int][math]::Round($r[0]*$SCALE); $t=[int][math]::Round($r[1]*$SCALE)
    $rr=[int][math]::Round($r[2]*$SCALE); $bb=[int][math]::Round($r[3]*$SCALE)
    $l=[Math]::Max(0,$l); $t=[Math]::Max(0,$t); $rr=[Math]::Min(1125,$rr); $bb=[Math]::Min(2000,$bb)
    $scaled += ,@($l,$t,$rr,$bb)
  }
  $found[$id+"_scaled"]=$scaled
  $parts=@(); foreach($s in $scaled){ $parts += ("@({0},{1},{2},{3})" -f $s[0],$s[1],$s[2],$s[3]) }
  if($scaled.Count -eq 1){
    $lines += ('  "{0}"=@(,{1})' -f $id, $parts[0])
  } else {
    $lines += ('  "{0}"=@({1})' -f $id, ($parts -join ','))
  }
  $m=Pick-Mask $id $scaled
  if($m -eq 'NONE'){ $maskLines += ('  "{0}"="NONE"' -f $id) }
  Write-Host ("{0}: {1} cells mask={2}" -f $id,$scaled.Count,$m)
}
New-Item -ItemType Directory -Force -Path "D:\EZTech\EZTechApp\collage_pic_editor\tools\_out" | Out-Null
$out="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\cells_is.txt"
(($lines -join "`n") + "`n---MASKS---`n" + ($maskLines -join "`n")) | Out-File $out -Encoding ascii
Write-Host "-> $out"
