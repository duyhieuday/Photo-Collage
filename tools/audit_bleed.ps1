# Quet TOAN CATALOG tim loi "anh o nay tran sang o khac" tren template CO MASK.
#
# Vi sao can: mask theo mau la mask TOAN CUC - no choc thung MOI pixel dung dai mau nam trong
# cellRect, khong quan tam pixel do thuoc o nao. Nen khi rect cua o A phu len mot phan SLOT cua
# o B (rat de xay ra voi rect XOAY: goc cua rect thop ra ngoai khung), anh cua o A se dam mot
# vet vao trong o B - neu A ve SAU B. Mat thuong rat kho thay vi vet chi rong vai chuc don vi.
# Da bat duoc that: sm02 (o tem nghieng 9.5 do dam vet vao o trai) - thay tren may ao.
#
# Cach lam: mo phong dung pipeline onDraw:
#   - open(p)  = mask trong suot tai p (dung DUNG nguong cua TemplateEditorView)
#   - slot(p)  = thanh phan lien thong cua vung open  -> mot slot that su cua thiet ke
#   - owner(S) = o phu nhieu pixel cua slot S nhat     -> o LE RA so huu slot do
#   - painter(p)= o co INDEX LON NHAT trong so cac o chua p (o ve sau de len o ve truoc)
#   Bao loi khi painter(p) != owner(slot(p)).
#
# Bao them 2 loai:
#   [KHONG-CHU]  slot khong o nao phu du 30% nhung van bi mot o to vao  -> anh ro ra nen/trang tri
#   [HUT]        slot co chu nhung >12% dien tich khong o nao phu       -> o do khong lap kin slot
#
# Doc anh NGUON PNG (khong mat du lieu). App doc webp da nen nen nguong lech chut it, nhung day
# la loi HINH HOC (rect chong slot) nen khong phu thuoc vao vai don vi mau.
#
# Usage: powershell -File tools\audit_bleed.ps1 [-Ids sm02,bd11] [-AW 563] [-MinArea 400]

param(
  [string[]]$Ids = @(),
  [int]$AW = 563,
  [int]$MinArea = 400,       # dien tich toi thieu (don vi logic^2) moi bao
  [int]$MinComp = 150        # bo qua thanh phan open nho hon (px o do phan giai phan tich)
)

Add-Type -AssemblyName System.Drawing
. "D:\EZTech\EZTechApp\collage_pic_editor\tools\cells_config.ps1"

$SRC = "D:\EZTech\AppAssets\PhotoCollage\Category_Template"
$CATS = @{ 'bd' = 'Birthday'; 'cp' = 'Couple'; 'gs' = 'Glad season'; 'is' = 'IG Story'; 'sm' = 'Summer vibe'; 'sp' = 'Sports' }
$LOGIC_W = 1125; $LOGIC_H = 2000
$AH = [int][math]::Round($AW * $LOGIC_H / $LOGIC_W)
$PXAREA = ($LOGIC_W / [double]$AW) * ($LOGIC_H / [double]$AH)   # dien tich logic cua 1 pixel phan tich

if ($Ids.Count -eq 0) { $Ids = @($CELLS.Keys) }
$Ids = @($Ids | ForEach-Object { $_ -split ',' } | Where-Object { $_ -match '\S' } | ForEach-Object { $_.Trim().ToLower() })

# Hop chu nhat cua mot cell (da tinh goc xoay) -> @(l,t,r,b)
function RotBounds([object[]]$cell) {
  $cl = [double]$cell[0]; $ct = [double]$cell[1]; $cr = [double]$cell[2]; $cb = [double]$cell[3]
  $dg = 0.0; if ($cell.Count -ge 5) { $dg = [double]$cell[4] }
  if ($dg -eq 0.0) { return @($cl, $ct, $cr, $cb) }
  $rd = [math]::PI * $dg / 180.0
  $co = [math]::Cos($rd); $si = [math]::Sin($rd)
  $mx = ($cl + $cr) / 2.0; $my = ($ct + $cb) / 2.0
  $hw = ($cr - $cl) / 2.0; $hh = ($cb - $ct) / 2.0
  $ex = [math]::Abs($hw * $co) + [math]::Abs($hh * $si)
  $ey = [math]::Abs($hw * $si) + [math]::Abs($hh * $co)
  return @(($mx - $ex), ($my - $ey), ($mx + $ex), ($my + $ey))
}

$report = @()

foreach ($id in $Ids) {
  $maskMode = $MASKS[$id]
  if (-not $maskMode) { continue }                 # NONE: anh fill dung rect, khong co mask toan cuc
  $pre = $id.Substring(0, 2); $nn = $id.Substring(2)
  $path = Join-Path $SRC ("{0}\Temp_{1}{2}.png" -f $CATS[$pre], $pre.ToUpper(), $nn)
  if (-not (Test-Path $path)) { Write-Host ("{0} : khong co Temp_ (bo qua)" -f $id); continue }

  $cellList = @($CELLS[$id] | Where-Object { $_ })
  if ($cellList.Count -eq 0) { continue }

  # App co bat CellOwnerMask cho template nay khong? Cong bat = co 2 rect chong hoac ke sat
  # trong 8 don vi (giong CellOwnerMask.anyOverlap). Neu co thi muc TRAN da duoc app xu ly,
  # bao ra chi de biet, khong phai loi.
  $ownerFix = $false
  for ($a = 0; $a -lt $cellList.Count -and -not $ownerFix; $a++) {
    $ba = RotBounds $cellList[$a]
    for ($b = $a + 1; $b -lt $cellList.Count; $b++) {
      $bb2 = RotBounds $cellList[$b]
      if (($ba[0] - 8) -lt $bb2[2] -and ($ba[2] + 8) -gt $bb2[0] -and
          ($ba[1] - 8) -lt $bb2[3] -and ($ba[3] + 8) -gt $bb2[1]) { $ownerFix = $true; break }
    }
  }

  # --- doc anh o do phan giai phan tich, NearestNeighbor de khong che them mau xam gia ---
  $srcImg = [System.Drawing.Image]::FromFile($path)
  $small = New-Object System.Drawing.Bitmap($AW, $AH, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $gfx = [System.Drawing.Graphics]::FromImage($small)
  $gfx.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
  $gfx.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
  $gfx.DrawImage($srcImg, (New-Object System.Drawing.Rectangle(0, 0, $AW, $AH)))
  $gfx.Dispose(); $srcImg.Dispose()

  $rect = New-Object System.Drawing.Rectangle(0, 0, $AW, $AH)
  $data = $small.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $stride = $data.Stride
  $buf = New-Object byte[] ($stride * $AH)
  [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $buf, 0, $buf.Length)
  $small.UnlockBits($data); $small.Dispose()

  # --- open[]: pixel mask trong suot, dung nguong cua TemplateEditorView ---
  $total = $AW * $AH
  $open = New-Object bool[] $total
  for ($py = 0; $py -lt $AH; $py++) {
    $row = $py * $stride
    for ($px = 0; $px -lt $AW; $px++) {
      $o = $row + $px * 4
      $bb = $buf[$o]; $gg = $buf[$o + 1]; $rr = $buf[$o + 2]
      $mx = [math]::Max($rr, [math]::Max($gg, $bb)); $mn = [math]::Min($rr, [math]::Min($gg, $bb))
      $hit = $false
      switch ($maskMode) {
        'WHITE' { $hit = ($rr -gt 240 -and $gg -gt 240 -and $bb -gt 240) }
        'GRAY'  { $hit = ($mx -ge 231 -and $mx -le 243 -and ($mx - $mn) -le 7) }
        'GRAY2' { $hit = ($mx -ge 208 -and $mx -le 228 -and ($mx - $mn) -le 10) }
        'BLACK' { $hit = ($rr -lt 50 -and $gg -lt 50 -and $bb -lt 50) }
      }
      $open[$py * $AW + $px] = $hit
    }
  }

  # --- painter[]: o co index lon nhat chua pixel (o ve sau de len o ve truoc) ---
  # --- cover[]:   so o phu pixel (de biet pixel co duoc ve khong) ---
  $painter = New-Object int[] $total
  for ($i = 0; $i -lt $total; $i++) { $painter[$i] = -1 }
  for ($ci = 0; $ci -lt $cellList.Count; $ci++) {
    $c = $cellList[$ci]
    $cl = [double]$c[0]; $ct = [double]$c[1]; $cr = [double]$c[2]; $cb = [double]$c[3]
    $deg = 0.0; if ($c.Count -ge 5) { $deg = [double]$c[4] }
    $ccx = ($cl + $cr) / 2.0; $ccy = ($ct + $cb) / 2.0
    $rad = [math]::PI * (-$deg) / 180.0
    $cosA = [math]::Cos($rad); $sinA = [math]::Sin($rad)
    # duyet bounding box rong cho rect da xoay
    $half = [math]::Sqrt([math]::Pow($cr - $cl, 2) + [math]::Pow($cb - $ct, 2)) / 2.0
    $x0 = [int][math]::Floor((($ccx - $half) * $AW / $LOGIC_W)); $x1 = [int][math]::Ceiling((($ccx + $half) * $AW / $LOGIC_W))
    $y0 = [int][math]::Floor((($ccy - $half) * $AH / $LOGIC_H)); $y1 = [int][math]::Ceiling((($ccy + $half) * $AH / $LOGIC_H))
    if ($x0 -lt 0) { $x0 = 0 }; if ($y0 -lt 0) { $y0 = 0 }
    if ($x1 -ge $AW) { $x1 = $AW - 1 }; if ($y1 -ge $AH) { $y1 = $AH - 1 }
    for ($py = $y0; $py -le $y1; $py++) {
      $ly = ($py + 0.5) * $LOGIC_H / $AH
      for ($px = $x0; $px -le $x1; $px++) {
        $lx = ($px + 0.5) * $LOGIC_W / $AW
        $ox = $lx - $ccx; $oy = $ly - $ccy
        $rx = $ccx + ($ox * $cosA - $oy * $sinA)
        $ry = $ccy + ($ox * $sinA + $oy * $cosA)
        if ($rx -ge $cl -and $rx -lt $cr -and $ry -ge $ct -and $ry -lt $cb) { $painter[$py * $AW + $px] = $ci }
      }
    }
  }

  # --- thanh phan lien thong cua vung open (8-huong) ---
  $label = New-Object int[] $total
  for ($i = 0; $i -lt $total; $i++) { $label[$i] = -1 }
  $compSize = New-Object System.Collections.ArrayList
  $stack = New-Object int[] $total
  $nComp = 0
  for ($seed = 0; $seed -lt $total; $seed++) {
    if (-not $open[$seed] -or $label[$seed] -ge 0) { continue }
    $sp = 0; $stack[$sp++] = $seed; $label[$seed] = $nComp; $size = 0
    while ($sp -gt 0) {
      $cur = $stack[--$sp]; $size++
      $cy = [int][math]::Floor($cur / $AW); $cx = $cur - $cy * $AW
      for ($dy = -1; $dy -le 1; $dy++) {
        $ny = $cy + $dy; if ($ny -lt 0 -or $ny -ge $AH) { continue }
        for ($dx = -1; $dx -le 1; $dx++) {
          $nx = $cx + $dx; if ($nx -lt 0 -or $nx -ge $AW) { continue }
          $ni = $ny * $AW + $nx
          if ($open[$ni] -and $label[$ni] -lt 0) { $label[$ni] = $nComp; $stack[$sp++] = $ni }
        }
      }
    }
    [void]$compSize.Add($size); $nComp++
  }

  # --- thong ke: moi (comp, cell) bao nhieu pixel phu / bao nhieu pixel duoc to ---
  $coverCnt = @{}   # "comp:cell" -> so pixel cell do PHU
  $paintCnt = @{}   # "comp:cell" -> so pixel cell do TO THAT (painter)
  $paintedTotal = @{}
  for ($i = 0; $i -lt $total; $i++) {
    if (-not $open[$i]) { continue }
    $lab = $label[$i]
    if ($compSize[$lab] -lt $MinComp) { continue }
    $pj = $painter[$i]
    if ($pj -ge 0) {
      $k = "$lab`:$pj"
      $paintCnt[$k] = 1 + [int]$paintCnt[$k]
      $paintedTotal[$lab] = 1 + [int]$paintedTotal[$lab]
    }
  }
  # phu (khong quan tam ai ve sau) - can de xac dinh chu so huu.
  # Chi quet trong bounding box cua tung o (quet ca anh cham gap ~20 lan).
  for ($ci = 0; $ci -lt $cellList.Count; $ci++) {
    $c = $cellList[$ci]
    $cl = [double]$c[0]; $ct = [double]$c[1]; $cr = [double]$c[2]; $cb = [double]$c[3]
    $deg = 0.0; if ($c.Count -ge 5) { $deg = [double]$c[4] }
    $ccx = ($cl + $cr) / 2.0; $ccy = ($ct + $cb) / 2.0
    $rad = [math]::PI * (-$deg) / 180.0
    $cosA = [math]::Cos($rad); $sinA = [math]::Sin($rad)
    $half = [math]::Sqrt([math]::Pow($cr - $cl, 2) + [math]::Pow($cb - $ct, 2)) / 2.0
    $x0 = [int][math]::Floor((($ccx - $half) * $AW / $LOGIC_W)); $x1 = [int][math]::Ceiling((($ccx + $half) * $AW / $LOGIC_W))
    $y0 = [int][math]::Floor((($ccy - $half) * $AH / $LOGIC_H)); $y1 = [int][math]::Ceiling((($ccy + $half) * $AH / $LOGIC_H))
    if ($x0 -lt 0) { $x0 = 0 }; if ($y0 -lt 0) { $y0 = 0 }
    if ($x1 -ge $AW) { $x1 = $AW - 1 }; if ($y1 -ge $AH) { $y1 = $AH - 1 }
    for ($py = $y0; $py -le $y1; $py++) {
      $ly = ($py + 0.5) * $LOGIC_H / $AH
      $rowBase = $py * $AW
      for ($px = $x0; $px -le $x1; $px++) {
        $i = $rowBase + $px
        if (-not $open[$i]) { continue }
        $lab = $label[$i]
        if ($compSize[$lab] -lt $MinComp) { continue }
        $lx = ($px + 0.5) * $LOGIC_W / $AW
        $ox = $lx - $ccx; $oy = $ly - $ccy
        $rx = $ccx + ($ox * $cosA - $oy * $sinA)
        $ry = $ccy + ($ox * $sinA + $oy * $cosA)
        if ($rx -ge $cl -and $rx -lt $cr -and $ry -ge $ct -and $ry -lt $cb) {
          $k = "$lab`:$ci"
          $coverCnt[$k] = 1 + [int]$coverCnt[$k]
        }
      }
    }
  }

  # --- ket luan tung slot ---
  for ($lab = 0; $lab -lt $nComp; $lab++) {
    if ($compSize[$lab] -lt $MinComp) { continue }
    $size = [double]$compSize[$lab]
    $owner = -1; $best = 0
    for ($ci = 0; $ci -lt $cellList.Count; $ci++) {
      $v = [int]$coverCnt["$lab`:$ci"]
      if ($v -gt $best) { $best = $v; $owner = $ci }
    }
    $painted = [int]$paintedTotal[$lab]
    if ($owner -lt 0 -or ($best / $size) -lt 0.30) {
      if ($painted * $PXAREA -ge $MinArea) {
        $report += [pscustomobject]@{ Id = $id; Kind = 'KHONG-CHU'; Cell = ''; Into = ''; Area = [int]($painted * $PXAREA)
          Note = ("slot {0} khong o nao phu du 30% nhung bi to {1}%" -f $lab, [int](100 * $painted / $size)) }
      }
      continue
    }
    for ($ci = 0; $ci -lt $cellList.Count; $ci++) {
      if ($ci -eq $owner) { continue }
      $v = [int]$paintCnt["$lab`:$ci"]
      if ($v * $PXAREA -ge $MinArea) {
        $kind = if ($ownerFix) { 'TRAN-DA-FIX' } else { 'TRAN' }
        $note = if ($ownerFix) {
          "o{0} chong slot o{1} {2}% - app da cat bang CellOwnerMask" -f ($ci + 1), ($owner + 1), [int](100 * $v / $size)
        } else {
          "o{0} ve sau nen dam {1}% dien tich slot cua o{2}" -f ($ci + 1), [int](100 * $v / $size), ($owner + 1)
        }
        $report += [pscustomobject]@{ Id = $id; Kind = $kind; Cell = ("o{0}" -f ($ci + 1)); Into = ("o{0}" -f ($owner + 1)); Area = [int]($v * $PXAREA); Note = $note }
      }
    }
    $miss = $size - $painted
    if ($miss * $PXAREA -ge $MinArea -and ($miss / $size) -gt 0.12) {
      $report += [pscustomobject]@{ Id = $id; Kind = 'HUT'; Cell = ("o{0}" -f ($owner + 1)); Into = ''; Area = [int]($miss * $PXAREA)
        Note = ("{0}% slot cua o{1} khong o nao phu -> lo nen" -f [int](100 * $miss / $size), ($owner + 1)) }
    }
  }
  Write-Host ("  quet {0} ({1}, {2} o, {3} slot)" -f $id, $maskMode, $cellList.Count, $nComp)
}

Write-Host ""
if ($report.Count -eq 0) {
  Write-Host "=== KHONG co template nao bi tran / hut ==="
} else {
  Write-Host ("=== {0} van de (sap theo dien tich) ===" -f $report.Count)
  $report | Sort-Object -Property Area -Descending | Format-Table -AutoSize Id, Kind, Cell, Into, Area, Note
}
