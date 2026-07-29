# Tim template BI THIEU O ANH: cho diff Temp_ vs Thumb_ ra vung designer co dat anh mau, roi
# xem vung do co o nao phu khong. Vung anh mau lon ma khong o nao phu = slot bi bo sot.
#
# Vi sao can: detector cu do theo DAI MAU cua slot. Template nao co slot mau TRANG tren nen
# TRANG (sp16: 3 panel trai trang, 2 panel phai xam) thi phan trang bi bo qua hoan toan, ma
# nhin Temp_ bang mat cung khong thay vi no chi la o trang tren nen trang. Diff voi Thumb_ thi
# lo ngay: cho nao designer dat anh la cho do phai co o.
#
# Nhieu bao dong gia se den tu: sticker/chu chi co trong Thumb_, va template Thumb_ khac design.
# Nen doc ket qua nhu goi y, mo anh ra xem roi moi sua.
#
# Usage: powershell -File tools\audit_missing.ps1 [-Ids sp16] [-AW 375] [-MinArea 20000]

param(
  [string[]]$Ids = @(),
  [int]$AW = 375,
  [int]$MinArea = 20000,     # dien tich logic^2 toi thieu de coi la mot o anh
  [int]$Thresh = 45,         # nguong khac biet mau giua Temp_ va Thumb_
  [double]$MaxCover = 0.5    # phu duoi muc nay thi bao thieu
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

function LoadGray([string]$p, [int]$gw, [int]$gh) {
  $img = [System.Drawing.Image]::FromFile($p)
  $bmp = New-Object System.Drawing.Bitmap($gw, $gh, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $gg = [System.Drawing.Graphics]::FromImage($bmp)
  $gg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $gg.DrawImage($img, (New-Object System.Drawing.Rectangle(0, 0, $gw, $gh)))
  $gg.Dispose(); $img.Dispose()
  $rc = New-Object System.Drawing.Rectangle(0, 0, $gw, $gh)
  $dt = $bmp.LockBits($rc, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $st = $dt.Stride
  $bytes = New-Object byte[] ($st * $gh)
  [System.Runtime.InteropServices.Marshal]::Copy($dt.Scan0, $bytes, 0, $bytes.Length)
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

  $ta = LoadGray $pTemp $AW $AH
  $tb = LoadGray $pThumb $AW $AH
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

  # thanh phan lien thong cua vung khac biet
  $label = New-Object int[] $total
  for ($i = 0; $i -lt $total; $i++) { $label[$i] = -1 }
  $stack = New-Object int[] $total
  $comps = @()
  $nComp = 0
  for ($seed = 0; $seed -lt $total; $seed++) {
    if (-not $diff[$seed] -or $label[$seed] -ge 0) { continue }
    $sp = 0; $stack[$sp++] = $seed; $label[$seed] = $nComp
    $px2 = New-Object System.Collections.ArrayList
    while ($sp -gt 0) {
      $cur = $stack[--$sp]; [void]$px2.Add($cur)
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
    $nComp++
    if (($px2.Count * $PXAREA) -ge $MinArea) { $comps += , $px2 }
  }

  foreach ($comp in $comps) {
    $inside = 0
    $minX = $AW; $maxX = 0; $minY = $AH; $maxY = 0
    foreach ($idx in $comp) {
      $py = [int][math]::Floor($idx / $AW); $px = $idx - $py * $AW
      if ($px -lt $minX) { $minX = $px }; if ($px -gt $maxX) { $maxX = $px }
      if ($py -lt $minY) { $minY = $py }; if ($py -gt $maxY) { $maxY = $py }
      $lx = ($px + 0.5) * $LOGIC_W / $AW; $ly = ($py + 0.5) * $LOGIC_H / $AH
      $hit = $false
      foreach ($c in $cellList) {
        $cl = [double]$c[0]; $ct = [double]$c[1]; $cr = [double]$c[2]; $cb = [double]$c[3]
        $dg = 0.0; if ($c.Count -ge 5) { $dg = [double]$c[4] }
        $rx = $lx; $ry = $ly
        if ($dg -ne 0.0) {
          $rad = [math]::PI * (-$dg) / 180.0
          $co = [math]::Cos($rad); $si = [math]::Sin($rad)
          $mx = ($cl + $cr) / 2.0; $my = ($ct + $cb) / 2.0
          $ox = $lx - $mx; $oy = $ly - $my
          $rx = $mx + ($ox * $co - $oy * $si); $ry = $my + ($ox * $si + $oy * $co)
        }
        if ($rx -ge $cl -and $rx -lt $cr -and $ry -ge $ct -and $ry -lt $cb) { $hit = $true; break }
      }
      if ($hit) { $inside++ }
    }
    $cover = $inside / [double]$comp.Count
    if ($cover -lt $MaxCover) {
      $lx0 = [int][math]::Round($minX * $LOGIC_W / $AW); $lx1 = [int][math]::Round(($maxX + 1) * $LOGIC_W / $AW)
      $ly0 = [int][math]::Round($minY * $LOGIC_H / $AH); $ly1 = [int][math]::Round(($maxY + 1) * $LOGIC_H / $AH)
      $report += [pscustomobject]@{ Id = $id; Area = [int]($comp.Count * $PXAREA); Cover = [math]::Round($cover, 2)
        Bbox = ("({0},{1},{2},{3})" -f $lx0, $ly0, $lx1, $ly1); Cells = $cellList.Count }
    }
  }
  Write-Host ("  quet {0} ({1} o, {2} vung anh lon)" -f $id, $cellList.Count, $comps.Count)
}

Write-Host ""
if ($report.Count -eq 0) {
  Write-Host "=== Khong template nao co vung anh mau bi bo sot ==="
} else {
  Write-Host ("=== {0} vung anh trong Thumb_ ma KHONG o nao phu (sap theo dien tich) ===" -f $report.Count)
  $report | Sort-Object -Property Area -Descending | Format-Table -AutoSize
}
