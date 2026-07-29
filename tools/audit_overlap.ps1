# Audit cellRects for OVERLAPPING pairs.
#
# Cells are drawn in list order, each clipped to its own rect (TemplateEditorView.onDraw), so a
# later cell paints over an earlier one wherever their rects intersect. When a design layers a
# small frame on top of a big shape, the big shape must come FIRST in the list or it will cover
# the frame's photo -- which is exactly what went wrong on bd11 (the centre circle was 3rd and
# covered the polaroid above it).
#
# Reports each overlapping pair with the intersection area as a fraction of the SMALLER rect,
# so the ones that actually matter float to the top. Ignores rotation: an axis-aligned overlap
# is a necessary condition, so this is a superset of the real problems.
#
# Usage: powershell -File tools\audit_overlap.ps1 [-Ids bd11,sp15] [-MinFrac 0.05]

param([string[]]$Ids = @(), [double]$MinFrac = 0.05)

. "D:\EZTech\EZTechApp\collage_pic_editor\tools\cells_config.ps1"

if ($Ids.Count -eq 0) { $Ids = @($CELLS.Keys) }
else { $Ids = @($Ids | ForEach-Object { $_ -split ',' } | Where-Object { $_ -match '\S' } | ForEach-Object { $_.Trim().ToLower() }) }

$hits = 0
foreach ($id in $Ids) {
  $rr = $CELLS[$id]
  if ($null -eq $rr) { continue }
  if ($rr.Count -ge 1 -and ($rr[0] -is [int] -or $rr[0] -is [double])) { $rr = @(, $rr) }
  $n = @($rr).Count
  if ($n -lt 2) { continue }

  $pairs = @()
  for ($i = 0; $i -lt $n; $i++) {
    for ($j = $i + 1; $j -lt $n; $j++) {
      $a = $rr[$i]; $b = $rr[$j]
      $ox = [Math]::Min($a[2], $b[2]) - [Math]::Max($a[0], $b[0])
      $oy = [Math]::Min($a[3], $b[3]) - [Math]::Max($a[1], $b[1])
      if ($ox -le 0 -or $oy -le 0) { continue }
      $inter = [double]($ox * $oy)
      $areaA = [double](($a[2] - $a[0]) * ($a[3] - $a[1]))
      $areaB = [double](($b[2] - $b[0]) * ($b[3] - $b[1]))
      $frac = $inter / [Math]::Min($areaA, $areaB)
      if ($frac -lt $MinFrac) { continue }
      # The cell drawn later wins the overlap. If the later one is the BIGGER rect, it is
      # probably a backdrop covering a frame that should sit on top -> suspicious order.
      $laterIsBigger = if ($areaB -gt $areaA) { $true } else { $false }
      $pairs += [pscustomobject]@{
        i = $i + 1; j = $j + 1; frac = [math]::Round($frac, 2)
        note = if ($laterIsBigger) { "cell$($j+1) (LON hon) ve SAU -> de len cell$($i+1)  << XEM LAI THU TU" } else { "cell$($j+1) (nho hon) ve sau -> nam tren, OK" }
      }
    }
  }
  if ($pairs.Count) {
    $hits++
    Write-Host ""
    Write-Host ("==== {0} ({1} o) ====" -f $id, $n)
    foreach ($p in $pairs) { Write-Host ("   cell{0} x cell{1}  chong {2,5:P0} o nho hon  |  {3}" -f $p.i, $p.j, $p.frac, $p.note) }
  }
}
Write-Host ""
Write-Host ("{0} template co cap o chong nhau (>= {1:P0})" -f $hits, $MinFrac)
