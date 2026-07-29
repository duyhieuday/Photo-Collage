# Simulate EXACTLY where a photo will appear in the editor, straight from the source PNG.
#
# TemplateEditorView.onDraw order: template -> cells (each clipped to its own rect, rotated by
# its angle) -> mask overlay (template with slot-coloured pixels punched transparent). So a
# photo is visible at pixel p only when BOTH hold:
#     p is inside some cellRect (after that cell's rotation), AND
#     the mask is transparent at p (i.e. p matches the MaskMode colour band)
#
# Renders those pixels MAGENTA over the template. Magenta outside the intended frame = the photo
# will bleed there. This is the failure mode a colour-fill screenshot hides: with a WHITE mask on
# a template whose BACKGROUND is also white, the mask punches through the background too, so the
# photo leaks out of the frame wherever the cellRect overhangs it.
#
# Uses the lossless source PNG, honours per-cell rotation (the old render_editor.ps1 did neither),
# and implements all four modes with the exact thresholds from TemplateEditorView.
#
# Usage: powershell -File tools\sim_editor.ps1 -Ids bd11,cp01 [-Mode WHITE]   (-Mode overrides config)

# NB on naming: PowerShell variables are case-insensitive, so a local named $mode would BE this
# $Mode parameter — after the first template its mask would leak into every later one and force
# them all to the same mode. Locals below are deliberately named $maskMode / $cellList.
param([string[]]$Ids = @(), [string]$ForceMode = '')

Add-Type -AssemblyName System.Drawing
. "D:\EZTech\EZTechApp\collage_pic_editor\tools\cells_config.ps1"

$SRC = "D:\EZTech\AppAssets\PhotoCollage\Category_Template"
$OUT = "D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\sim"
New-Item -ItemType Directory -Force -Path $OUT | Out-Null

$LOGIC_W = 1125; $LOGIC_H = 2000
$W = 1125; $H = 2000              # render in logic space = what the app composites
$CATS = @{
  'bd' = @('Birthday', 'BD'); 'cp' = @('Couple', 'CP'); 'gs' = @('Glad season', 'GS')
  'is' = @('IG Story', 'IS'); 'sm' = @('Summer vibe', 'SM'); 'sp' = @('Sports', 'SP')
}
$jpgEnc = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq 'image/jpeg' }

$Ids = @($Ids | ForEach-Object { $_ -split ',' } | Where-Object { $_ -match '\S' } | ForEach-Object { $_.Trim().ToLower() })
if ($Ids.Count -eq 0) { Write-Host "No -Ids."; exit 1 }

foreach ($id in $Ids) {
  $rr = $CELLS[$id]
  if ($null -eq $rr) { Write-Host "$id : khong co trong cells_config"; continue }
  if ($rr.Count -ge 1 -and ($rr[0] -is [int] -or $rr[0] -is [double])) { $rr = @(, $rr) }

  $pre = $id.Substring(0, 2); $nn = $id.Substring(2)
  $p = Join-Path $SRC ("{0}\Temp_{1}{2}.png" -f $CATS[$pre][0], $CATS[$pre][1], $nn)
  if (-not (Test-Path $p)) { Write-Host "$id : khong co Temp_ nguon"; continue }

  $maskMode = if ($ForceMode) { $ForceMode.ToUpper() } elseif ($MASKS.ContainsKey($id)) { $MASKS[$id] } else { 'NONE' }

  $img = [System.Drawing.Image]::FromFile($p)
  $bmp = New-Object System.Drawing.Bitmap $W, $H, ([System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.DrawImage($img, 0, 0, $W, $H); $g.Dispose(); $img.Dispose()

  # cells in pixel space + precomputed rotation
  $cellList = @()
  foreach ($c in $rr) {
    $deg = if ($c.Count -ge 5) { [double]$c[4] } else { 0.0 }
    $x0 = $c[0] * $W / $LOGIC_W; $y0 = $c[1] * $H / $LOGIC_H
    $x1 = $c[2] * $W / $LOGIC_W; $y1 = $c[3] * $H / $LOGIC_H
    $rad = -$deg * [Math]::PI / 180.0     # inverse rotation to test membership
    $cellList += ,@{
      x0 = $x0; y0 = $y0; x1 = $x1; y1 = $y1
      cx = ($x0 + $x1) / 2.0; cy = ($y0 + $y1) / 2.0
      cos = [Math]::Cos($rad); sin = [Math]::Sin($rad)
      rot = ([math]::Abs($deg) -ge 0.01)
    }
  }

  $rect = New-Object System.Drawing.Rectangle 0, 0, $W, $H
  $data = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadWrite, [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
  $stride = $data.Stride
  $buf = New-Object byte[] ($stride * $H)
  [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $buf, 0, $buf.Length)

  $inCellPx = 0; $shownPx = 0
  for ($y = 0; $y -lt $H; $y++) {
    $row = $y * $stride
    for ($x = 0; $x -lt $W; $x++) {
      $inCell = $false
      foreach ($c in $cellList) {
        if ($c.rot) {
          $dx = $x - $c.cx; $dy = $y - $c.cy
          $ux = $dx * $c.cos - $dy * $c.sin + $c.cx
          $uy = $dx * $c.sin + $dy * $c.cos + $c.cy
          if ($ux -ge $c.x0 -and $ux -lt $c.x1 -and $uy -ge $c.y0 -and $uy -lt $c.y1) { $inCell = $true; break }
        } else {
          if ($x -ge $c.x0 -and $x -lt $c.x1 -and $y -ge $c.y0 -and $y -lt $c.y1) { $inCell = $true; break }
        }
      }
      if (-not $inCell) { continue }
      $inCellPx++

      $o = $row + $x * 3
      $b = $buf[$o]; $gr = $buf[$o + 1]; $r = $buf[$o + 2]
      $mx = $r; if ($gr -gt $mx) { $mx = $gr }; if ($b -gt $mx) { $mx = $b }
      $mn = $r; if ($gr -lt $mn) { $mn = $gr }; if ($b -lt $mn) { $mn = $b }

      # thresholds mirror TemplateEditorView.createMaskFrom{White,Gray,Gray2}
      $show = switch ($maskMode) {
        'WHITE' { ($r -gt 240 -and $gr -gt 240 -and $b -gt 240) }
        'GRAY'  { ($mx -ge 231 -and $mx -le 243 -and ($mx - $mn) -le 7) }
        'GRAY2' { ($mx -ge 208 -and $mx -le 228 -and ($mx - $mn) -le 10) }
        default { $true }                                   # NONE: photo fills the whole rect
      }
      if ($show) { $buf[$o] = 255; $buf[$o + 1] = 0; $buf[$o + 2] = 255; $shownPx++ }
    }
  }
  [System.Runtime.InteropServices.Marshal]::Copy($buf, 0, $data.Scan0, $buf.Length)
  $bmp.UnlockBits($data)

  $OW = 420; $OH = [int]($H * $OW / $W)
  $small = New-Object System.Drawing.Bitmap $OW, $OH
  $g2 = [System.Drawing.Graphics]::FromImage($small)
  $g2.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g2.DrawImage($bmp, 0, 0, $OW, $OH); $g2.Dispose(); $bmp.Dispose()
  $eps = New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality, [long]92)
  $small.Save((Join-Path $OUT "$id.jpg"), $jpgEnc, $eps); $eps.Dispose(); $small.Dispose()

  $frac = if ($inCellPx -gt 0) { $shownPx / [double]$inCellPx } else { 0 }
  Write-Host ("  {0,-6} mask={1,-5} o={2}  anh hien {3,7:P1} dien tich cellRect  ({4:N0} px)" -f $id, $maskMode, @($rr).Count, $frac, $shownPx)
}
Write-Host ""
Write-Host "Magenta = noi ANH SE HIEN. Magenta tran ra ngoai khung = loi. -> $OUT"
