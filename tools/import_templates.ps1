# Import Category_Template PNGs -> downscaled JPEG into res/drawable,
# and auto-detect white photo-frame rects -> cellRects JSON (in 1125x2000 logic space).
# Windows PowerShell 5.1 + System.Drawing only (no cwebp/ImageMagick available).

Add-Type -AssemblyName System.Drawing

$src     = "D:\EZTech\AppAssets\PhotoCollage\Category_Template"
$resDir  = "D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"
$outDir  = "D:\EZTech\EZTechApp\collage_pic_editor\tools\_out"
$ovDir   = Join-Path $outDir "overlay"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
New-Item -ItemType Directory -Force -Path $ovDir  | Out-Null

# Logic space (khớp với TemplateEditor: TEMPLATE_W=1125, 9:16 -> 2000)
$LOGIC_W = 1125
$LOGIC_H = 2000
# Convert sizes
$TEMP_W = 1125; $TEMP_H = 2000     # ảnh template cho editor
$THUMB_W = 540; $THUMB_H = 960     # thumbnail cho picker
# Analysis size (dò khung) - nhỏ cho nhanh
$AW = 225; $AH = 400

# ----- danh sách template: id, file Temp, file Thumb -----
$items = @()
function Add-Range($folder, $prefix, $from, $to, $idprefix) {
    for ($i=$from; $i -le $to; $i++) {
        $nn = "{0:D2}" -f $i
        $script:items += [pscustomobject]@{
            id    = "$idprefix$nn"
            temp  = Join-Path $src "$folder\Temp_$prefix$nn.png"
            thumb = Join-Path $src "$folder\Thumb_$prefix$nn.png"
        }
    }
}
Add-Range "Birthday"    "BD" 1 10 "bd"
Add-Range "Couple"      "CP" 1 9  "cp"
Add-Range "Glad season" "GS" 1 10 "gs"
Add-Range "IG Story"    "IS" 1 15 "is"
Add-Range "Summer vibe" "SM" 1 9  "sm"
# Sports folder rỗng -> SP01 lấy từ Popular
$items += [pscustomobject]@{ id="sp01"; temp=Join-Path $src "Popular\Temp_SP01.png"; thumb=Join-Path $src "Popular\Thumb_SP01.png" }

# ----- helpers -----
$jpgEnc = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq 'image/jpeg' }
function Save-Jpeg($bmp, $path, $quality) {
    $eps = New-Object System.Drawing.Imaging.EncoderParameters 1
    $eps.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality, [long]$quality)
    $bmp.Save($path, $jpgEnc, $eps)
    $eps.Dispose()
}
function Resize-Save($srcPath, $dstPath, $w, $h, $q) {
    $img = [System.Drawing.Image]::FromFile($srcPath)
    $bmp = New-Object System.Drawing.Bitmap $w, $h
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode   = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.DrawImage($img, 0, 0, $w, $h)
    $g.Dispose()
    Save-Jpeg $bmp $dstPath $q
    $bmp.Dispose(); $img.Dispose()
}

# Dò khung trắng: trả về list rect [l,t,r,b] trong logic space 1125x2000
function Detect-Cells($srcPath) {
    $img = [System.Drawing.Image]::FromFile($srcPath)
    $small = New-Object System.Drawing.Bitmap $AW, $AH
    $g = [System.Drawing.Graphics]::FromImage($small)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.DrawImage($img, 0, 0, $AW, $AH)
    $g.Dispose(); $img.Dispose()

    $rect = New-Object System.Drawing.Rectangle 0, 0, $AW, $AH
    $data = $small.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
    $stride = $data.Stride
    $buf = New-Object byte[] ($stride * $AH)
    [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $buf, 0, $buf.Length)
    $small.UnlockBits($data); $small.Dispose()

    $N = $AW * $AH
    $white = New-Object 'bool[]' $N
    for ($y=0; $y -lt $AH; $y++) {
        $row = $y * $stride
        for ($x=0; $x -lt $AW; $x++) {
            $o = $row + $x*3
            $b = $buf[$o]; $gr = $buf[$o+1]; $r = $buf[$o+2]
            if ($r -gt 244 -and $gr -gt 244 -and $b -gt 244) { $white[$y*$AW + $x] = $true }
        }
    }

    # connected components (4-neighbour) bằng stack
    $visited = New-Object 'bool[]' $N
    $cells = @()
    $stack = New-Object System.Collections.Generic.Stack[int]
    for ($i=0; $i -lt $N; $i++) {
        if ($white[$i] -and -not $visited[$i]) {
            $stack.Clear(); $stack.Push($i); $visited[$i] = $true
            $minX=$AW; $minY=$AH; $maxX=0; $maxY=0; $cnt=0
            while ($stack.Count -gt 0) {
                $p = $stack.Pop(); $cnt++
                $px = $p % $AW; $py = [int]($p / $AW)
                if ($px -lt $minX){$minX=$px}; if ($px -gt $maxX){$maxX=$px}
                if ($py -lt $minY){$minY=$py}; if ($py -gt $maxY){$maxY=$py}
                # neighbours
                if ($px -gt 0)     { $q=$p-1;   if ($white[$q] -and -not $visited[$q]){$visited[$q]=$true;$stack.Push($q)} }
                if ($px -lt $AW-1) { $q=$p+1;   if ($white[$q] -and -not $visited[$q]){$visited[$q]=$true;$stack.Push($q)} }
                if ($py -gt 0)     { $q=$p-$AW; if ($white[$q] -and -not $visited[$q]){$visited[$q]=$true;$stack.Push($q)} }
                if ($py -lt $AH-1) { $q=$p+$AW; if ($white[$q] -and -not $visited[$q]){$visited[$q]=$true;$stack.Push($q)} }
            }
            $bw = $maxX - $minX + 1; $bh = $maxY - $minY + 1
            $bboxArea = $bw * $bh
            $fill = $cnt / [double]$bboxArea
            $areaFrac = $bboxArea / [double]$N
            # lọc: đủ lớn, hình chữ nhật đặc, không phải toàn ảnh (nền), không quá thuôn
            $aspect = $bw / [double]$bh
            if ($areaFrac -gt 0.012 -and $areaFrac -lt 0.92 -and $fill -gt 0.6 -and `
                $aspect -gt 0.12 -and $aspect -lt 8 -and `
                -not ($bw -gt $AW*0.95 -and $bh -gt $AH*0.95)) {
                $cells += [pscustomobject]@{
                    l = [math]::Round($minX * $LOGIC_W / $AW)
                    t = [math]::Round($minY * $LOGIC_H / $AH)
                    r = [math]::Round(($maxX+1) * $LOGIC_W / $AW)
                    b = [math]::Round(($maxY+1) * $LOGIC_H / $AH)
                    fill = [math]::Round($fill,2)
                }
            }
        }
    }
    # sort top->bottom, left->right
    $cells = $cells | Sort-Object t, l
    return ,$cells
}

# Overlay debug: vẽ rect đã dò lên ảnh template (downscale) để mắt kiểm tra
function Save-Overlay($srcPath, $cells, $dstPath) {
    $img = [System.Drawing.Image]::FromFile($srcPath)
    $W=360; $H=640
    $bmp = New-Object System.Drawing.Bitmap $W,$H
    $g=[System.Drawing.Graphics]::FromImage($bmp)
    $g.DrawImage($img,0,0,$W,$H)
    $pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::Red), 3
    foreach ($c in $cells) {
        $x=$c.l*$W/$LOGIC_W; $y=$c.t*$H/$LOGIC_H
        $w=($c.r-$c.l)*$W/$LOGIC_W; $h=($c.b-$c.t)*$H/$LOGIC_H
        $g.DrawRectangle($pen,[float]$x,[float]$y,[float]$w,[float]$h)
    }
    $pen.Dispose(); $g.Dispose()
    Save-Jpeg $bmp $dstPath 85
    $bmp.Dispose(); $img.Dispose()
}

# ----- main -----
$result = [ordered]@{}
$idx=0
foreach ($it in $items) {
    $idx++
    if (-not (Test-Path $it.temp))  { Write-Host "MISSING temp:  $($it.temp)";  continue }
    if (-not (Test-Path $it.thumb)) { Write-Host "MISSING thumb: $($it.thumb)"; continue }

    Resize-Save $it.temp  (Join-Path $resDir "temp_$($it.id).jpg")  $TEMP_W  $TEMP_H  88
    Resize-Save $it.thumb (Join-Path $resDir "thumb_$($it.id).jpg") $THUMB_W $THUMB_H 86

    $cells = Detect-Cells $it.temp
    Save-Overlay $it.temp $cells (Join-Path $ovDir "$($it.id).jpg")
    $result[$it.id] = $cells
    Write-Host ("[{0,2}/{1}] {2}  cells={3}" -f $idx, $items.Count, $it.id, @($cells).Count)
}

$result | ConvertTo-Json -Depth 5 | Out-File (Join-Path $outDir "cells.json") -Encoding utf8
Write-Host "DONE. JSON -> $outDir\cells.json ; overlays -> $ovDir"
