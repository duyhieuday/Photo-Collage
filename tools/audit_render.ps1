# So ANH CHUP THAT cua app (DEBUG_FILL_CELLS to moi o mot MAU DAC) voi CHO DESIGNER DAT ANH,
# theo tung pixel. Tra ve tung CUM loi kem bbox:
#   HO   = designer co anh o day ma app KHONG co  -> hien nen/khung, thieu anh
#   TRAN = app co anh o day ma designer KHONG co  -> anh lan ra ngoai khung
#
# "Cho designer dat anh" = diff(Temp_, Thumb_). Khong can suy ra chu so huu slot hay thanh phan
# lien thong gi ca - nen khong bi cac bay cua template co NEN cung mau voi o (giay trang bd11,
# nen trang sp12) lam bao dong gia.
#
# Nhieu con lai (biet truoc, doc ket qua phai tru ra):
#   - Thumb_ co sticker/chu ma Temp_ khong co -> bao TRAN oan (thuong cum nho, o goc).
#   - Cho anh mau cua designer TRUNG mau template -> diff bo sot -> bao TRAN oan.
#   - Vien anti-alias quanh moi khung luon lech 1-2 pixel -> luon co cum HO/TRAN mong. Dat
#     -MinArea du lon (mac dinh 600) de bo qua.
#
# Usage: powershell -File tools\audit_render.ps1 -Id bd11 -Shot <png> [-Crop "156,359,762,1357"]
#   -Crop = vung canvas template trong anh chup. Emulator 1080x2280: 156,359,762,1357
#                                                Pixel 6a 1080x2400: 118,340,848,1500

param(
  [string]$Id = '',
  [string]$Shot = '',
  [string]$Crop = '156,359,762,1357',
  [int]$AW = 563,
  [int]$MinArea = 600,
  [int]$Thresh = 45,
  [int]$Tol = 60
)

Add-Type -AssemblyName System.Drawing
$SRC = "D:\EZTech\AppAssets\PhotoCollage\Category_Template"
$CATS = @{ 'bd' = 'Birthday'; 'cp' = 'Couple'; 'gs' = 'Glad season'; 'is' = 'IG Story'; 'sm' = 'Summer vibe'; 'sp' = 'Sports' }
$LOGIC_W = 1125; $LOGIC_H = 2000
$AH = [int][math]::Round($AW * $LOGIC_H / $LOGIC_W)
$PXAREA = ($LOGIC_W / [double]$AW) * ($LOGIC_H / [double]$AH)

# PHAI khop dbgColors trong TemplateEditorActivity.
$DBG = @(
  @(200, 230, 25), @(0, 160, 160), @(0, 90, 200), @(230, 30, 40), @(120, 60, 255),
  @(40, 200, 60), @(255, 0, 170), @(255, 140, 0), @(154, 107, 63)
)

if (-not $Id -or -not $Shot) { Write-Host "Thieu -Id / -Shot"; exit 1 }
$Id = $Id.Trim().ToLower()
$pre = $Id.Substring(0, 2); $nn = $Id.Substring(2)
$pTemp = Join-Path $SRC ("{0}\Temp_{1}{2}.png" -f $CATS[$pre], $pre.ToUpper(), $nn)
$pThumb = Join-Path $SRC ("{0}\Thumb_{1}{2}.png" -f $CATS[$pre], $pre.ToUpper(), $nn)
if (-not (Test-Path $pTemp) -or -not (Test-Path $pThumb)) { Write-Host "$Id : thieu Temp_/Thumb_"; exit 1 }

function LoadScaled([string]$p, [int]$gw, [int]$gh, [int[]]$src) {
  $img = [System.Drawing.Image]::FromFile($p)
  $bmp = New-Object System.Drawing.Bitmap($gw, $gh, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $gg = [System.Drawing.Graphics]::FromImage($bmp)
  $gg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
  $gg.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
  if ($src) {
    $gg.DrawImage($img, (New-Object System.Drawing.Rectangle(0, 0, $gw, $gh)),
      (New-Object System.Drawing.Rectangle($src[0], $src[1], $src[2], $src[3])), [System.Drawing.GraphicsUnit]::Pixel)
  } else {
    $gg.DrawImage($img, (New-Object System.Drawing.Rectangle(0, 0, $gw, $gh)))
  }
  $gg.Dispose(); $img.Dispose()
  $rc = New-Object System.Drawing.Rectangle(0, 0, $gw, $gh)
  $dt = $bmp.LockBits($rc, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $bytes = New-Object byte[] ($dt.Stride * $gh)
  [System.Runtime.InteropServices.Marshal]::Copy($dt.Scan0, $bytes, 0, $bytes.Length)
  $st = $dt.Stride
  $bmp.UnlockBits($dt); $bmp.Dispose()
  return @{ Buf = $bytes; Stride = $st }
}

$ta = LoadScaled $pTemp $AW $AH $null
$tb = LoadScaled $pThumb $AW $AH $null
$cropArr = @($Crop -split ',' | ForEach-Object { [int]$_ })
$sh = LoadScaled $Shot $AW $AH $cropArr
$total = $AW * $AH

$expected = New-Object bool[] $total   # designer co anh
$actual = New-Object bool[] $total     # app co anh
$which = New-Object int[] $total       # o nao (theo mau) - de bao o nao dinh loi
for ($py = 0; $py -lt $AH; $py++) {
  $ra = $py * $ta.Stride; $rs = $py * $sh.Stride
  for ($px = 0; $px -lt $AW; $px++) {
    $i = $py * $AW + $px
    $oa = $ra + $px * 4
    $d = [math]::Abs([int]$ta.Buf[$oa] - [int]$tb.Buf[$oa]) +
         [math]::Abs([int]$ta.Buf[$oa + 1] - [int]$tb.Buf[$oa + 1]) +
         [math]::Abs([int]$ta.Buf[$oa + 2] - [int]$tb.Buf[$oa + 2])
    $expected[$i] = (($d / 3) -ge $Thresh)
    $os = $rs + $px * 4
    $bb = $sh.Buf[$os]; $gg2 = $sh.Buf[$os + 1]; $rr = $sh.Buf[$os + 2]
    $which[$i] = -1
    for ($ci = 0; $ci -lt $DBG.Count; $ci++) {
      $c = $DBG[$ci]
      if ([math]::Abs($rr - $c[0]) + [math]::Abs($gg2 - $c[1]) + [math]::Abs($bb - $c[2]) -le $Tol) {
        $actual[$i] = $true; $which[$i] = $ci; break
      }
    }
  }
}

# --- cum loi ---
function Clusters([bool[]]$flag, [int]$aw, [int]$ah, [int]$minPx) {
  $tot = $aw * $ah
  $lab = New-Object int[] $tot
  for ($i = 0; $i -lt $tot; $i++) { $lab[$i] = -1 }
  $st = New-Object int[] $tot
  $res = @()
  $n = 0
  for ($seed = 0; $seed -lt $tot; $seed++) {
    if (-not $flag[$seed] -or $lab[$seed] -ge 0) { continue }
    $sp = 0; $st[$sp++] = $seed; $lab[$seed] = $n
    $cnt = 0; $mnX = $aw; $mxX = 0; $mnY = $ah; $mxY = 0
    while ($sp -gt 0) {
      $cur = $st[--$sp]; $cnt++
      $cy = [int][math]::Floor($cur / $aw); $cx = $cur - $cy * $aw
      if ($cx -lt $mnX) { $mnX = $cx }; if ($cx -gt $mxX) { $mxX = $cx }
      if ($cy -lt $mnY) { $mnY = $cy }; if ($cy -gt $mxY) { $mxY = $cy }
      for ($dy = -1; $dy -le 1; $dy++) {
        $ny = $cy + $dy; if ($ny -lt 0 -or $ny -ge $ah) { continue }
        for ($dx = -1; $dx -le 1; $dx++) {
          $nx = $cx + $dx; if ($nx -lt 0 -or $nx -ge $aw) { continue }
          $ni = $ny * $aw + $nx
          if ($flag[$ni] -and $lab[$ni] -lt 0) { $lab[$ni] = $n; $st[$sp++] = $ni }
        }
      }
    }
    $n++
    # Tra ve PSCustomObject chu KHONG phai mang: PowerShell tu duoi mang long nhau khi return,
    # lam $c[1..4] bien thanh so le -> bbox in ra sai het (da dinh: moi cum deu in "(0,0,2,2)").
    if ($cnt -ge $minPx) {
      $res += [pscustomobject]@{ N = $cnt; X0 = $mnX; Y0 = $mnY; X1 = $mxX; Y1 = $mxY }
    }
  }
  return $res
}

$gapFlag = New-Object bool[] $total
$ovFlag = New-Object bool[] $total
for ($i = 0; $i -lt $total; $i++) {
  if ($expected[$i] -and -not $actual[$i]) { $gapFlag[$i] = $true }
  if ($actual[$i] -and -not $expected[$i]) { $ovFlag[$i] = $true }
}
$minPx = [int]($MinArea / $PXAREA)
$sxl = $LOGIC_W / [double]$AW; $syl = $LOGIC_H / [double]$AH
$rows = @()
foreach ($c in (Clusters $gapFlag $AW $AH $minPx)) {
  $rows += [pscustomobject]@{ Loai = 'HO'; Area = [int]($c.N * $PXAREA)
    Bbox = ("({0},{1},{2},{3})" -f [int]($c.X0 * $sxl), [int]($c.Y0 * $syl), [int](($c.X1 + 1) * $sxl), [int](($c.Y1 + 1) * $syl))
    W = [int]((($c.X1 - $c.X0) + 1) * $sxl); H = [int]((($c.Y1 - $c.Y0) + 1) * $syl) }
}
foreach ($c in (Clusters $ovFlag $AW $AH $minPx)) {
  $rows += [pscustomobject]@{ Loai = 'TRAN'; Area = [int]($c.N * $PXAREA)
    Bbox = ("({0},{1},{2},{3})" -f [int]($c.X0 * $sxl), [int]($c.Y0 * $syl), [int](($c.X1 + 1) * $sxl), [int](($c.Y1 + 1) * $syl))
    W = [int]((($c.X1 - $c.X0) + 1) * $sxl); H = [int]((($c.Y1 - $c.Y0) + 1) * $syl) }
}
Write-Host ("==== {0} : {1} cum >= {2} don vi^2 ====" -f $Id, $rows.Count, $MinArea)
$rows | Sort-Object -Property Area -Descending | Select-Object -First 14 | Format-Table -AutoSize

