# Cham diem tung O ANH: rect co NAM DUNG cho dat anh khong, va co DU rong khong.
#
# audit_bleed bat loi "anh ro sang o khac", audit_missing bat "thieu han o". Con mot loai loi
# thu ba khong tool nao bat: rect DUNG HINH nhung LECH hoac SAI KICH THUOC. Mat nhin overlay
# thu nho khong thay lech vai chuc don vi (bai hoc bd12: hut 64 don vi ma van tuong dung).
#
# Cach cham: vung DIFF (Temp_ vs Thumb_) = cho designer dat anh, day la ground truth.
#   HIT = ti le dien tich RECT nam trong vung diff   -> thap = rect thua/lech ra ngoai anh
#   COV = ti le vung diff (cua slot khop nhat) nam trong RECT -> thap = rect hut, khong phu het
#
# DOC KET QUA:
#  - Template CO MASK: rect co y de bao trum rong (mask lo cat), nen HIT thap la BINH THUONG,
#    chi nhin COV.
#  - Diff cung bat ca sticker/trang tri chi co trong Thumb_, va bo sot cho anh mau trung mau
#    template -> vai % nhieu la thuong. Day la danh sach de SOI, khong phai phan quyet.
#
# Usage: powershell -File tools\audit_fit.ps1 -Ids bd11,bd12 [-AW 375] [-MinHit 0.75] [-MinCov 0.75]

param(
  [string[]]$Ids = @(),
  [int]$AW = 375,
  [int]$Thresh = 45,
  [double]$MinHit = 0.75,
  [double]$MinCov = 0.75,
  [int]$MinComp = 8000       # dien tich logic^2 toi thieu de coi la mot vung anh
)

Add-Type -AssemblyName System.Drawing
. "D:\EZTech\EZTechApp\collage_pic_editor\tools\cells_config.ps1"

$SRC = "D:\EZTech\AppAssets\PhotoCollage\Category_Template"
$CATS = @{ 'bd' = 'Birthday'; 'cp' = 'Couple'; 'gs' = 'Glad season'; 'is' = 'IG Story'; 'sm' = 'Summer vibe'; 'sp' = 'Sports' }
$LOGIC_W = 1125; $LOGIC_H = 2000
$AH = [int][math]::Round($AW * $LOGIC_H / $LOGIC_W)
$PXAREA = ($LOGIC_W / [double]$AW) * ($LOGIC_H / [double]$AH)

if ($Ids.Count -eq 0) { $Ids = @($CELLS.Keys) }
$Ids = @($Ids | ForEach-Object { $_ -split ',' } | Where-Object { $_ -match '\S' } | ForEach-Object { $_.Trim().ToLower() })

function LoadBuf([string]$p, [int]$gw, [int]$gh) {
  $img = [System.Drawing.Image]::FromFile($p)
  $bmp = New-Object System.Drawing.Bitmap($gw, $gh, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $gg = [System.Drawing.Graphics]::FromImage($bmp)
  $gg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $gg.DrawImage($img, (New-Object System.Drawing.Rectangle(0, 0, $gw, $gh)))
  $gg.Dispose(); $img.Dispose()
  $rc = New-Object System.Drawing.Rectangle(0, 0, $gw, $gh)
  $dt = $bmp.LockBits($rc, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $bytes = New-Object byte[] ($dt.Stride * $gh)
  [System.Runtime.InteropServices.Marshal]::Copy($dt.Scan0, $bytes, 0, $bytes.Length)
  $st = $dt.Stride
  $bmp.UnlockBits($dt); $bmp.Dispose()
  return @{ Buf = $bytes; Stride = $st }
}

$report = @()

foreach ($id in $Ids) {
  $pre = $id.Substring(0, 2); $nn = $id.Substring(2)
  $pTemp = Join-Path $SRC ("{0}\Temp_{1}{2}.png" -f $CATS[$pre], $pre.ToUpper(), $nn)
  $pThumb = Join-Path $SRC ("{0}\Thumb_{1}{2}.png" -f $CATS[$pre], $pre.ToUpper(), $nn)
  if (-not (Test-Path $pTemp) -or -not (Test-Path $pThumb)) { Write-Host ("  bo qua {0} (thieu Temp_/Thumb_)" -f $id); continue }
  $cellList = @($CELLS[$id] | Where-Object { $_ })
  if ($cellList.Count -eq 0) { continue }

  $ta = LoadBuf $pTemp $AW $AH
  $tb = LoadBuf $pThumb $AW $AH
  $total = $AW * $AH
  $diff = New-Object bool[] $total
  for ($py = 0; $py -lt $AH; $py++) {
    $row = $py * $ta.Stride
    for ($px = 0; $px -lt $AW; $px++) {
      $o = $row + $px * 4
      $d = [math]::Abs([int]$ta.Buf[$o] - [int]$tb.Buf[$o]) +
           [math]::Abs([int]$ta.Buf[$o + 1] - [int]$tb.Buf[$o + 1]) +
           [math]::Abs([int]$ta.Buf[$o + 2] - [int]$tb.Buf[$o + 2])
      $diff[$py * $AW + $px] = ($d / 3) -ge $Thresh
    }
  }

  # thanh phan lien thong cua vung diff (moi thanh phan = mot vung dat anh)
  $label = New-Object int[] $total
  for ($i = 0; $i -lt $total; $i++) { $label[$i] = -1 }
  $stack = New-Object int[] $total
  $compArea = New-Object System.Collections.ArrayList
  $nComp = 0
  for ($seed = 0; $seed -lt $total; $seed++) {
    if (-not $diff[$seed] -or $label[$seed] -ge 0) { continue }
    $sp = 0; $stack[$sp++] = $seed; $label[$seed] = $nComp; $size = 0
    while ($sp -gt 0) {
      $cur = $stack[--$sp]; $size++
      $cy = [int][math]::Floor($cur / $AW); $cx = $cur - $cy * $AW
      for ($dy = -1; $dy -le 1; $dy++) {
        $ny = $cy + $dy; if ($ny -lt 0 -or $ny -ge $AH) { continue }
        for ($dx = -1; $dx -le 1; $dx++) {
          $nx = $cx + $dx; if ($nx -lt 0 -or $nx -ge $AW) { continue }
          $ni = $ny * $AW + $nx
          if ($diff[$ni] -and $label[$ni] -lt 0) { $label[$ni] = $nComp; $stack[$sp++] = $ni }
        }
      }
    }
    [void]$compArea.Add($size); $nComp++
  }

  $isMasked = [bool]$MASKS[$id]

  for ($ci = 0; $ci -lt $cellList.Count; $ci++) {
    $c = $cellList[$ci]
    $cl = [double]$c[0]; $ct = [double]$c[1]; $cr = [double]$c[2]; $cb = [double]$c[3]
    $dg = 0.0; if ($c.Count -ge 5) { $dg = [double]$c[4] }
    $mcx = ($cl + $cr) / 2.0; $mcy = ($ct + $cb) / 2.0
    $rad = [math]::PI * (-$dg) / 180.0
    $cosA = [math]::Cos($rad); $sinA = [math]::Sin($rad)
    $half = [math]::Sqrt([math]::Pow($cr - $cl, 2) + [math]::Pow($cb - $ct, 2)) / 2.0
    $x0 = [int][math]::Floor((($mcx - $half) * $AW / $LOGIC_W)); $x1 = [int][math]::Ceiling((($mcx + $half) * $AW / $LOGIC_W))
    $y0 = [int][math]::Floor((($mcy - $half) * $AH / $LOGIC_H)); $y1 = [int][math]::Ceiling((($mcy + $half) * $AH / $LOGIC_H))
    if ($x0 -lt 0) { $x0 = 0 }; if ($y0 -lt 0) { $y0 = 0 }
    if ($x1 -ge $AW) { $x1 = $AW - 1 }; if ($y1 -ge $AH) { $y1 = $AH - 1 }

    $inRect = 0; $inBoth = 0
    $perComp = @{}
    for ($py = $y0; $py -le $y1; $py++) {
      $ly = ($py + 0.5) * $LOGIC_H / $AH
      for ($px = $x0; $px -le $x1; $px++) {
        $lx = ($px + 0.5) * $LOGIC_W / $AW
        $ox = $lx - $mcx; $oy = $ly - $mcy
        $rx = $mcx + ($ox * $cosA - $oy * $sinA)
        $ry = $mcy + ($ox * $sinA + $oy * $cosA)
        if ($rx -lt $cl -or $rx -ge $cr -or $ry -lt $ct -or $ry -ge $cb) { continue }
        $inRect++
        $idx = $py * $AW + $px
        if ($diff[$idx]) {
          $inBoth++
          $lab = $label[$idx]
          $perComp[$lab] = 1 + [int]$perComp[$lab]
        }
      }
    }
    if ($inRect -eq 0) { continue }

    # slot khop nhat = thanh phan diff co nhieu pixel nhat trong rect
    $bestLab = -1; $bestN = 0
    foreach ($k in $perComp.Keys) { if ([int]$perComp[$k] -gt $bestN) { $bestN = [int]$perComp[$k]; $bestLab = $k } }
    $cov = 0.0
    if ($bestLab -ge 0 -and [int]$compArea[$bestLab] -gt 0) { $cov = $bestN / [double]$compArea[$bestLab] }
    $hit = $inBoth / [double]$inRect

    # rect tran ra ngoai canvas la co y (sp16) -> phan ngoai canvas khong tinh vao HIT
    $flag = ''
    if ($cov -lt $MinCov -and ($bestLab -lt 0 -or ([int]$compArea[$bestLab] * $PXAREA) -ge $MinComp)) { $flag = 'HUT' }
    if (-not $isMasked -and $hit -lt $MinHit) { if ($flag) { $flag += '+THUA' } else { $flag = 'THUA' } }
    if ($flag) {
      $report += [pscustomobject]@{
        Id = $id; Cell = ("o{0}" -f ($ci + 1)); Mask = $(if ($isMasked) { $MASKS[$id] } else { '-' })
        Hit = [math]::Round($hit, 2); Cov = [math]::Round($cov, 2); Flag = $flag
        Rect = ("({0},{1},{2},{3})" -f [int]$cl, [int]$ct, [int]$cr, [int]$cb)
      }
    }
  }
  Write-Host ("  cham {0} ({1} o, {2} vung diff)" -f $id, $cellList.Count, $nComp)
}

Write-Host ""
if ($report.Count -eq 0) {
  Write-Host "=== Moi o deu trung khop vung anh mau ==="
} else {
  Write-Host ("=== {0} o dang ngo (HUT = rect khong phu het anh; THUA = rect tran ra ngoai anh) ===" -f $report.Count)
  $report | Sort-Object -Property Hit | Format-Table -AutoSize
}
