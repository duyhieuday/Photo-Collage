# Static sanity audit over the WHOLE catalog: reads TemplateCells.kt (the generated file the app
# actually ships) plus TemplateData.kt, and reports defects that are invisible in a screenshot.
#
# Checks:
#   1. registered template with no cellRects  -> editor opens with nowhere to put a photo
#   2. angles list length != rects length     -> angles land on the wrong cells
#   3. rect outside the 1125x2000 canvas      -> part of the slot unreachable
#   4. degenerate rect (zero/negative side)   -> invisible cell
#   5. suspiciously small rect                -> probably a text strip, not a photo slot
#   6. duplicate identical rects              -> two cells stacked on the same spot
#   7. cellRects entry not registered         -> dead config
#
# Windows PowerShell 5.1. ASCII only.
# Usage: powershell -File tools\audit_cells.ps1

$ErrorActionPreference = 'Stop'
$ROOT = "D:\EZTech\EZTechApp\collage_pic_editor"
$CELLS_KT = Join-Path $ROOT "app\src\main\java\com\example\piceditor\templates_editor\TemplateCells.kt"
$DATA_KT  = Join-Path $ROOT "app\src\main\java\com\example\piceditor\templates_editor\TemplateData.kt"
$LOGIC_W = 1125; $LOGIC_H = 2000
$MIN_SIDE = 120

# ---- parse TemplateCells.kt ----
$txt = Get-Content $CELLS_KT -Raw
$rects  = @{}
$angles = @{}
$masks  = @{}

# section boundaries so a "id to listOf(...)" line is attributed to the right map
$iRects  = $txt.IndexOf('val rects')
$iAngles = $txt.IndexOf('val angles')
$iMasks  = $txt.IndexOf('val masks')

foreach ($m in [regex]::Matches($txt, '"(?<id>[a-z]{2}\d{2})"\s+to\s+listOf\((?<body>[^\r\n]*)\)')) {
  $id = $m.Groups['id'].Value
  $body = $m.Groups['body'].Value
  $pos = $m.Index
  if ($pos -gt $iRects -and $pos -lt $iAngles) {
    $list = @()
    foreach ($r in [regex]::Matches($body, 'RectF\(\s*([-\d.]+)f,\s*([-\d.]+)f,\s*([-\d.]+)f,\s*([-\d.]+)f\s*\)')) {
      $list += ,@([double]$r.Groups[1].Value, [double]$r.Groups[2].Value, [double]$r.Groups[3].Value, [double]$r.Groups[4].Value)
    }
    $rects[$id] = $list
  } elseif ($pos -gt $iAngles -and ($iMasks -lt 0 -or $pos -lt $iMasks)) {
    $a = @()
    foreach ($r in [regex]::Matches($body, '([-\d.]+)f')) { $a += [double]$r.Groups[1].Value }
    $angles[$id] = $a
  }
}
foreach ($m in [regex]::Matches($txt, '"(?<id>[a-z]{2}\d{2})"\s+to\s+MaskMode\.(?<mode>[A-Z0-9]+)')) {
  $masks[$m.Groups['id'].Value] = $m.Groups['mode'].Value
}

# ---- parse registered ids from TemplateData.kt (uncommented lines only) ----
$registered = @()
foreach ($line in (Get-Content $DATA_KT)) {
  if ($line -match '^\s*//') { continue }
  $mm = [regex]::Match($line, 'TemplateData\("([a-z]{2}\d{2})"')
  if ($mm.Success) { $registered += $mm.Groups[1].Value }
}

Write-Host ("TemplateCells.kt : {0} rect-set, {1} angle-set, {2} mask" -f $rects.Count, $angles.Count, $masks.Count)
Write-Host ("TemplateData.kt  : {0} template dang ky" -f $registered.Count)
Write-Host ""

$problems = 0
function Bad($id, $msg) { $script:problems++; Write-Host ("  [{0}] {1}" -f $id, $msg) }

Write-Host "=== 1. Template dang ky nhung KHONG co cellRects ==="
foreach ($id in $registered) {
  if (-not $rects.ContainsKey($id) -or @($rects[$id]).Count -eq 0) { Bad $id "khong co o anh -> editor mo ra khong dat duoc anh" }
}
Write-Host "  (het)"

Write-Host ""
Write-Host "=== 2. So goc xoay != so o ==="
foreach ($id in $registered) {
  if (-not $angles.ContainsKey($id)) { continue }
  $nr = @($rects[$id]).Count; $na = @($angles[$id]).Count
  if ($nr -ne $na) { Bad $id "$na goc cho $nr o -> goc ap sai o" }
}
Write-Host "  (het)"

Write-Host ""
Write-Host "=== 3. Rect vuot ra ngoai canvas 1125x2000 ==="
foreach ($id in $registered) {
  $i = 0
  foreach ($r in @($rects[$id])) {
    $i++
    $ang = if ($angles.ContainsKey($id)) { @($angles[$id])[$i-1] } else { 0 }
    # a rotated cell may legitimately poke past its axis-aligned box; only flag straight ones
    if ([math]::Abs($ang) -ge 0.5) { continue }
    if ($r[0] -lt -1 -or $r[1] -lt -1 -or $r[2] -gt $LOGIC_W + 1 -or $r[3] -gt $LOGIC_H + 1) {
      Bad $id ("o$i ngoai canvas: ({0},{1},{2},{3})" -f $r[0], $r[1], $r[2], $r[3])
    }
  }
}
Write-Host "  (het)"

Write-Host ""
Write-Host "=== 4. Rect suy bien (canh <= 0) ==="
foreach ($id in $registered) {
  $i = 0
  foreach ($r in @($rects[$id])) {
    $i++
    if (($r[2] - $r[0]) -le 0 -or ($r[3] - $r[1]) -le 0) { Bad $id ("o$i canh <= 0: ({0},{1},{2},{3})" -f $r[0], $r[1], $r[2], $r[3]) }
  }
}
Write-Host "  (het)"

Write-Host ""
Write-Host ("=== 5. O qua nho (canh ngan < {0}) ===" -f $MIN_SIDE)
foreach ($id in $registered) {
  $i = 0
  foreach ($r in @($rects[$id])) {
    $i++
    $w = $r[2] - $r[0]; $h = $r[3] - $r[1]
    $mn = [Math]::Min($w, $h)
    if ($mn -gt 0 -and $mn -lt $MIN_SIDE) { Bad $id ("o$i canh ngan {0:N0} (w={1:N0} h={2:N0}) -> co the la dai chu, khong phai o anh" -f $mn, $w, $h) }
  }
}
Write-Host "  (het)"

Write-Host ""
Write-Host "=== 6. Hai o trung khit nhau ==="
foreach ($id in $registered) {
  $rr = @($rects[$id]); $n = $rr.Count
  for ($i = 0; $i -lt $n; $i++) {
    for ($j = $i + 1; $j -lt $n; $j++) {
      if ($rr[$i][0] -eq $rr[$j][0] -and $rr[$i][1] -eq $rr[$j][1] -and $rr[$i][2] -eq $rr[$j][2] -and $rr[$i][3] -eq $rr[$j][3]) {
        Bad $id ("o{0} va o{1} trung khit -> mot o bi che hoan toan" -f ($i+1), ($j+1))
      }
    }
  }
}
Write-Host "  (het)"

Write-Host ""
Write-Host "=== 7. Co cellRects nhung KHONG dang ky (config chet) ==="
foreach ($id in $rects.Keys) {
  if ($registered -notcontains $id) { Write-Host ("  [{0}] co {1} o trong TemplateCells nhung khong co trong catalog" -f $id, @($rects[$id]).Count) }
}
Write-Host "  (het)"

Write-Host ""
Write-Host "=== Thong ke so o moi template ==="
$byCount = $registered | ForEach-Object { @($rects[$_]).Count } | Group-Object | Sort-Object { [int]$_.Name }
foreach ($g in $byCount) { Write-Host ("  {0,2} o : {1,3} template" -f $g.Name, $g.Count) }

Write-Host ""
Write-Host ("TONG: {0} van de can xem" -f $problems)
