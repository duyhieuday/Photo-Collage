# So RECT dang dung voi VUNG ANH THAT cua designer, tung canh mot.
#
# Bat loai loi ma cac tool khac khong thay: rect DUNG HINH, DUNG GOC, nhung THUA hoac HUT vai
# chuc don vi -> anh an vao vien khung / de len chu, nut bam, sticker cua thiet ke. Chinh loai
# nay lam cp20 (player nhac) bi anh trum len vien do cua khung.
#
# Vung anh that = thanh phan lien thong cua diff(Temp_, Thumb_) -> min-area rotated rect.
# CHI so sanh nhung vung "sach": fill >= 0.88 (tuc dung la hinh chu nhat, khong phai blob/tron
# hay hai o dinh nhau). Vung fill thap thi BO QUA - khong ket luan gi.
#
# Voi template CO MASK thi rect co y bao trum rong hon hinh -> chi bao khi rect HUT, bo qua thua.
#
# Usage: powershell -File tools\audit_rectfit.ps1 [-Ids cp20] [-Tol 10]

param(
  [string[]]$Ids = @(),
  [int]$AW = 375,
  [int]$Thresh = 45,
  [int]$MinArea = 30000,
  [double]$MinFill = 0.88,
  [int]$Tol = 10
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
  if (-not (Test-Path $pTemp) -or -not (Test-Path $pThumb)) { continue }
  $cellList = @($CELLS[$id] | Where-Object { $_ })
  if ($cellList.Count -eq 0) { continue }
  $masked = [bool]$MASKS[$id]

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
      $diff[$py * $AW + $px] = (($d / 3) -ge $Thresh)
    }
  }
  $label = New-Object int[] $total
  for ($i = 0; $i -lt $total; $i++) { $label[$i] = -1 }
  $stack = New-Object int[] $total
  $nComp = 0
  for ($seed = 0; $seed -lt $total; $seed++) {
    if (-not $diff[$seed] -or $label[$seed] -ge 0) { continue }
    $sp = 0; $stack[$sp++] = $seed; $label[$seed] = $nComp
    $pts = New-Object System.Collections.ArrayList
    while ($sp -gt 0) {
      $cur = $stack[--$sp]
      $cy = [int][math]::Floor($cur / $AW); $cx = $cur - $cy * $AW
      [void]$pts.Add(@($cx, $cy))
      for ($dy = -1; $dy -le 1; $dy++) {
        $ny = $cy + $dy; if ($ny -lt 0 -or $ny -ge $AH) { continue }
        for ($dx = -1; $dx -le 1; $dx++) {
          $nx = $cx + $dx; if ($nx -lt 0 -or $nx -ge $AW) { continue }
          $ni = $ny * $AW + $nx
          if ($diff[$ni] -and $label[$ni] -lt 0) { $label[$ni] = $nComp; $stack[$sp++] = $ni }
        }
      }
    }
    $nComp++
    if (($pts.Count * $PXAREA) -lt $MinArea) { continue }

    $bestA = [double]::MaxValue; $bestDeg = 0.0; $bl = 0.0; $bt = 0.0; $brr = 0.0; $bb = 0.0
    for ($deg = -25.0; $deg -le 25.0; $deg += 0.5) {
      $rad = [math]::PI * $deg / 180.0
      $co = [math]::Cos($rad); $si = [math]::Sin($rad)
      $mnx = [double]::MaxValue; $mxx = [double]::MinValue; $mny = [double]::MaxValue; $mxy = [double]::MinValue
      foreach ($p in $pts) {
        $rx = $p[0] * $co + $p[1] * $si; $ry = -$p[0] * $si + $p[1] * $co
        if ($rx -lt $mnx) { $mnx = $rx }; if ($rx -gt $mxx) { $mxx = $rx }
        if ($ry -lt $mny) { $mny = $ry }; if ($ry -gt $mxy) { $mxy = $ry }
      }
      $a = ($mxx - $mnx) * ($mxy - $mny)
      if ($a -lt $bestA) { $bestA = $a; $bestDeg = $deg; $bl = $mnx; $bt = $mny; $brr = $mxx; $bb = $mxy }
    }
    $fill = $pts.Count / $bestA
    if ($fill -lt $MinFill) { continue }
    $rad = [math]::PI * $bestDeg / 180.0
    $co = [math]::Cos($rad); $si = [math]::Sin($rad)
    $ccx = ($bl + $brr) / 2.0; $ccy = ($bt + $bb) / 2.0
    $sxl = $LOGIC_W / [double]$AW; $syl = $LOGIC_H / [double]$AH
    $cxL = ($ccx * $co - $ccy * $si) * $sxl
    $cyL = ($ccx * $si + $ccy * $co) * $syl
    $wL = ($brr - $bl) * $sxl; $hL = ($bb - $bt) * $syl

    # ghep voi cell gan nhat theo tam
    $bi = -1; $bd = [double]::MaxValue
    for ($ci = 0; $ci -lt $cellList.Count; $ci++) {
      $c = $cellList[$ci]
      $mx = ([double]$c[0] + [double]$c[2]) / 2.0; $my = ([double]$c[1] + [double]$c[3]) / 2.0
      $dd = [math]::Sqrt([math]::Pow($mx - $cxL, 2) + [math]::Pow($my - $cyL, 2))
      if ($dd -lt $bd) { $bd = $dd; $bi = $ci }
    }
    if ($bi -lt 0 -or $bd -gt 250) { continue }
    $c = $cellList[$bi]
    $cw = [double]$c[2] - [double]$c[0]; $ch = [double]$c[3] - [double]$c[1]
    $dW = [int]($cw - $wL); $dH = [int]($ch - $hL)
    $dCx = [int](([double]$c[0] + [double]$c[2]) / 2.0 - $cxL); $dCy = [int](([double]$c[1] + [double]$c[3]) / 2.0 - $cyL)
    $cdeg = 0.0; if ($c.Count -ge 5) { $cdeg = [double]$c[4] }
    $dDeg = [math]::Round($cdeg - $bestDeg, 1)
    $bad = ([math]::Abs($dW) -gt $Tol) -or ([math]::Abs($dH) -gt $Tol) -or
           ([math]::Abs($dCx) -gt $Tol) -or ([math]::Abs($dCy) -gt $Tol) -or ([math]::Abs($dDeg) -gt 2)
    if ($masked -and $dW -ge 0 -and $dH -ge 0) { $bad = $false }   # co mask + rect bao trum = OK
    if ($bad) {
      $report += [pscustomobject]@{
        Id = $id; O = "o$($bi+1)"; Mask = $(if ($masked) { $MASKS[$id] } else { '-' })
        dW = $dW; dH = $dH; dCx = $dCx; dCy = $dCy; dDeg = $dDeg
        AnhThat = ("({0},{1},{2},{3},{4})" -f [int]($cxL - $wL / 2), [int]($cyL - $hL / 2), [int]($cxL + $wL / 2), [int]($cyL + $hL / 2), $bestDeg)
      }
    }
  }
  Write-Host ("  cham {0}" -f $id)
}
Write-Host ""
Write-Host ("=== {0} o lech so voi vung anh designer (nguong {1} don vi) ===" -f $report.Count, $Tol)
Write-Host "dW/dH > 0 = rect THUA (anh an ra vien/chu); < 0 = rect HUT (con vien khung khong co anh)"
$report | Sort-Object -Property { [math]::Abs($_.dW) + [math]::Abs($_.dH) + [math]::Abs($_.dCx) + [math]::Abs($_.dCy) } -Descending | Format-Table -AutoSize
