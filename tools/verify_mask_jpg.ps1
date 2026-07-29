# Sanity-check that JPEG compression did not wreck the runtime colour masks.
#
# The editor builds its cut-out mask at runtime by thresholding the template drawable's colours
# (TemplateEditorView.createMaskFromWhite / ...Gray / ...Gray2). If JPEG ringing pushes slot
# pixels out of the threshold band, the mask comes out speckled and photos show through wrong.
# This renders the mask exactly as the app would compute it, from the imported .jpg, so the
# result can be compared against the intended slot shape by eye.
#
# Transparent (cut-out) pixels render MAGENTA; kept pixels render as the template.
# A good result: solid magenta slots with clean edges, nothing magenta outside the slots.
#
# Usage: powershell -File tools\verify_mask_jpg.ps1 -Specs bd20:WHITE,cp12:GRAY,sm02:GRAY2

param([string[]]$Specs = @())

Add-Type -AssemblyName System.Drawing

$RES = "D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$OUT = "D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\maskcheck"
New-Item -ItemType Directory -Force -Path $OUT | Out-Null

$jpgEnc = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq 'image/jpeg' }

$Specs = @($Specs | ForEach-Object { $_ -split ',' } | Where-Object { $_ -match '\S' })

foreach ($spec in $Specs) {
  $parts = $spec.Trim() -split ':'
  $id = $parts[0].ToLower()
  $mode = if ($parts.Count -gt 1) { $parts[1].ToUpper() } else { 'WHITE' }

  $p = Get-ChildItem $RES -Filter "temp_$id.*" -ErrorAction SilentlyContinue | Select-Object -First 1
  if (-not $p) { Write-Host "$id : no drawable"; continue }
  # System.Drawing cannot decode .webp, so already-converted templates can't be checked here.
  if ($p.Extension -eq '.webp') { Write-Host "$id : .webp - System.Drawing can't read it, skipped"; continue }

  $img = [System.Drawing.Image]::FromFile($p.FullName)
  $W = $img.Width; $H = $img.Height
  $bmp = New-Object System.Drawing.Bitmap $W, $H, ([System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.DrawImage($img, 0, 0, $W, $H); $g.Dispose(); $img.Dispose()

  $rect = New-Object System.Drawing.Rectangle 0, 0, $W, $H
  $d = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadWrite, [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
  $stride = $d.Stride
  $buf = New-Object byte[] ($stride * $H)
  [System.Runtime.InteropServices.Marshal]::Copy($d.Scan0, $buf, 0, $buf.Length)

  $cut = 0
  for ($y = 0; $y -lt $H; $y++) {
    $row = $y * $stride
    for ($x = 0; $x -lt $W; $x++) {
      $o = $row + $x * 3
      $b = $buf[$o]; $gr = $buf[$o + 1]; $r = $buf[$o + 2]
      $transparent = $false
      switch ($mode) {
        # createMaskFromWhite: r,g,b all > 240
        'WHITE' { $transparent = ($r -gt 240 -and $gr -gt 240 -and $b -gt 240) }
        # createMaskFromGray: max in 231..243 and (max-min) <= 7
        'GRAY'  {
          $mx = $r; if ($gr -gt $mx) { $mx = $gr }; if ($b -gt $mx) { $mx = $b }
          $mn = $r; if ($gr -lt $mn) { $mn = $gr }; if ($b -lt $mn) { $mn = $b }
          $transparent = ($mx -ge 231 -and $mx -le 243 -and ($mx - $mn) -le 7)
        }
        # createMaskFromGray2: max in 208..228 and (max-min) <= 10
        'GRAY2' {
          $mx = $r; if ($gr -gt $mx) { $mx = $gr }; if ($b -gt $mx) { $mx = $b }
          $mn = $r; if ($gr -lt $mn) { $mn = $gr }; if ($b -lt $mn) { $mn = $b }
          $transparent = ($mx -ge 208 -and $mx -le 228 -and ($mx - $mn) -le 10)
        }
      }
      if ($transparent) { $buf[$o] = 255; $buf[$o + 1] = 0; $buf[$o + 2] = 255; $cut++ }
    }
  }
  [System.Runtime.InteropServices.Marshal]::Copy($buf, 0, $d.Scan0, $buf.Length)
  $bmp.UnlockBits($d)

  $OW = 360; $OH = [int]($H * $OW / $W)
  $small = New-Object System.Drawing.Bitmap $OW, $OH
  $g2 = [System.Drawing.Graphics]::FromImage($small)
  $g2.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
  $g2.DrawImage($bmp, 0, 0, $OW, $OH); $g2.Dispose(); $bmp.Dispose()

  $eps = New-Object System.Drawing.Imaging.EncoderParameters 1
  $eps.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality, [long]92)
  $small.Save((Join-Path $OUT ("{0}_{1}.jpg" -f $id, $mode)), $jpgEnc, $eps)
  $eps.Dispose(); $small.Dispose()

  Write-Host ("  {0,-6} {1,-6} src={2,-6} cut={3,6:P2} of canvas -> {0}_{1}.jpg" -f $id, $mode, $p.Extension, ($cut / [double]($W * $H)))
}
Write-Host "Masks -> $OUT"
