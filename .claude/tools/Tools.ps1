# Shared helpers. Dot-source:  . .\Tools.ps1
Add-Type -AssemblyName PresentationCore
Add-Type -AssemblyName System.Drawing

$global:DRAW = "D:\EZTech\EZTechApp\collage_pic_editor\app\src\main\res\drawable"

function Find-TempPath([string]$id) {
    foreach($e in @('webp','png','jpg')){ $p="$global:DRAW\temp_$id.$e"; if(Test-Path $p){ return $p } }
    return $null
}
function Find-ThumbPath([string]$id) {
    foreach($e in @('webp','png','jpg')){ $p="$global:DRAW\thumb_$id.$e"; if(Test-Path $p){ return $p } }
    return $null
}

# Load image scaled to logic space (1125 x H, H keeps aspect). Returns bitmap.
function Load-Logic([string]$Image, [int]$W=1125) {
    $uri = New-Object System.Uri ((Resolve-Path $Image).Path)
    $bi = New-Object System.Windows.Media.Imaging.BitmapImage
    $bi.BeginInit(); $bi.UriSource=$uri; $bi.CacheOption=[System.Windows.Media.Imaging.BitmapCacheOption]::OnLoad; $bi.EndInit(); $bi.Freeze()
    $H = [int]([math]::Round($W * $bi.PixelHeight / $bi.PixelWidth))
    $st0=$bi.PixelWidth*4; $px0=New-Object byte[] ($st0*$bi.PixelHeight)
    $cvt=New-Object System.Windows.Media.Imaging.FormatConvertedBitmap; $cvt.BeginInit(); $cvt.Source=$bi; $cvt.DestinationFormat=[System.Windows.Media.PixelFormats]::Bgra32; $cvt.EndInit(); $cvt.Freeze(); $cvt.CopyPixels($px0,$st0,0)
    $sb=New-Object System.Drawing.Bitmap $bi.PixelWidth,$bi.PixelHeight,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $rr=New-Object System.Drawing.Rectangle 0,0,$bi.PixelWidth,$bi.PixelHeight
    $dd=$sb.LockBits($rr,[System.Drawing.Imaging.ImageLockMode]::WriteOnly,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    [System.Runtime.InteropServices.Marshal]::Copy($px0,0,$dd.Scan0,$px0.Length); $sb.UnlockBits($dd)
    $tpl=New-Object System.Drawing.Bitmap $W,$H,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $gt=[System.Drawing.Graphics]::FromImage($tpl); $gt.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic; $gt.DrawImage($sb,0,0,$W,$H); $gt.Dispose(); $sb.Dispose()
    return $tpl
}

# Returns @{ W; H; bytes; stride }
function Get-Pixels($bmp) {
    $W=$bmp.Width; $H=$bmp.Height
    $d=$bmp.LockBits((New-Object System.Drawing.Rectangle 0,0,$W,$H),[System.Drawing.Imaging.ImageLockMode]::ReadOnly,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $b=New-Object byte[] ($d.Stride*$H); [System.Runtime.InteropServices.Marshal]::Copy($d.Scan0,$b,0,$b.Length); $bmp.UnlockBits($d)
    return @{ W=$W; H=$H; bytes=$b; stride=$d.Stride }
}

function Add-Grid($g, [int]$W, [int]$H) {
    $pen=New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(110,255,255,0)),1
    $f=New-Object System.Drawing.Font 'Arial',17,([System.Drawing.FontStyle]::Bold); $br=New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)
    for($x=0;$x -le $W;$x+=100){ $g.DrawLine($pen,$x,0,$x,$H); $g.DrawString("$x",$f,$br,$x-16,2) }
    for($y=0;$y -le $H;$y+=100){ $g.DrawLine($pen,0,$y,$W,$y); $g.DrawString("$y",$f,$br,2,$y-10) }
}

# Connected pure-white(>240) components. Returns list of rects + fill ratio.
function Get-WhiteRegions([string]$Image, [int]$MinArea=18000) {
    $bmp = Load-Logic $Image
    $P = Get-Pixels $bmp
    $W=$P.W; $H=$P.H; $bytes=$P.bytes; $stride=$P.stride
    $bmp.Dispose()
    $white = New-Object 'bool[]' ($W*$H)
    for($y=0;$y -lt $H;$y++){ $row=$y*$stride; $wr=$y*$W
      for($x=0;$x -lt $W;$x++){ $i=$row+$x*4
        if($bytes[$i] -gt 240 -and $bytes[$i+1] -gt 240 -and $bytes[$i+2] -gt 240){ $white[$wr+$x]=$true } } }
    $visited = New-Object 'bool[]' ($W*$H)
    $sx=New-Object System.Collections.Generic.Stack[int]; $sy=New-Object System.Collections.Generic.Stack[int]
    $res=@()
    for($y=0;$y -lt $H;$y++){ for($x=0;$x -lt $W;$x++){
        $k=$y*$W+$x
        if(-not $white[$k] -or $visited[$k]){ continue }
        $minX=$x;$maxX=$x;$minY=$y;$maxY=$y;$cnt=0
        $sx.Push($x); $sy.Push($y); $visited[$k]=$true
        while($sx.Count -gt 0){
          $cx=$sx.Pop(); $cy=$sy.Pop(); $cnt++
          if($cx -lt $minX){$minX=$cx}; if($cx -gt $maxX){$maxX=$cx}; if($cy -lt $minY){$minY=$cy}; if($cy -gt $maxY){$maxY=$cy}
          $nb=@(@($cx-1,$cy),@($cx+1,$cy),@($cx,$cy-1),@($cx,$cy+1))
          foreach($nn in $nb){ $nx=$nn[0]; $ny=$nn[1]
            if($nx -ge 0 -and $ny -ge 0 -and $nx -lt $W -and $ny -lt $H){ $nk=$ny*$W+$nx
              if($white[$nk] -and -not $visited[$nk]){ $visited[$nk]=$true; $sx.Push($nx); $sy.Push($ny) } } }
        }
        $area=($maxX-$minX+1)*($maxY-$minY+1)
        if($cnt -ge $MinArea){ $res += [PSCustomObject]@{ x1=$minX;y1=$minY;x2=$maxX;y2=$maxY;px=$cnt;fill=[math]::Round($cnt/$area,2);H=$H } }
    } }
    return ($res | Sort-Object y1, x1)
}

# Faithful render sim: template -> colored cells -> mask(white>240 OR black<50 transparent). Optional grid.
function Render-Sim([string]$Image, [string]$Out, [string]$Rects, [switch]$Grid, [switch]$Black) {
    $tpl = Load-Logic $Image
    $W=$tpl.Width; $H=$tpl.Height
    $P = Get-Pixels $tpl
    $mb = $P.bytes.Clone()
    if($Black){
      for($i=0;$i -lt $mb.Length;$i+=4){ if($mb[$i] -lt 50 -and $mb[$i+1] -lt 50 -and $mb[$i+2] -lt 50){ $mb[$i+3]=0 } }
    } else {
      for($i=0;$i -lt $mb.Length;$i+=4){ if($mb[$i] -gt 240 -and $mb[$i+1] -gt 240 -and $mb[$i+2] -gt 240){ $mb[$i+3]=0 } }
    }
    $mask=New-Object System.Drawing.Bitmap $W,$H,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $kd=$mask.LockBits((New-Object System.Drawing.Rectangle 0,0,$W,$H),[System.Drawing.Imaging.ImageLockMode]::WriteOnly,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    [System.Runtime.InteropServices.Marshal]::Copy($mb,0,$kd.Scan0,$mb.Length); $mask.UnlockBits($kd)
    $res=New-Object System.Drawing.Bitmap $W,$H,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g=[System.Drawing.Graphics]::FromImage($res); $g.DrawImage($tpl,0,0)
    $cols=@([System.Drawing.Color]::FromArgb(255,235,80,80),[System.Drawing.Color]::FromArgb(255,80,200,90),[System.Drawing.Color]::FromArgb(255,80,140,235),[System.Drawing.Color]::FromArgb(255,240,170,55),[System.Drawing.Color]::FromArgb(255,210,80,210),[System.Drawing.Color]::FromArgb(255,80,210,210),[System.Drawing.Color]::FromArgb(255,200,200,60),[System.Drawing.Color]::FromArgb(255,150,90,220),[System.Drawing.Color]::FromArgb(255,235,140,60))
    $i=0
    foreach($r in ($Rects -split ';')){ if(-not $r){continue}; $p=$r -split ','; $b=New-Object System.Drawing.SolidBrush $cols[$i%$cols.Length]; $g.FillRectangle($b,[int]$p[0],[int]$p[1],([int]$p[2]-[int]$p[0]),([int]$p[3]-[int]$p[1])); $b.Dispose(); $i++ }
    $g.DrawImage($mask,0,0)
    if($Grid){ Add-Grid $g $W $H }
    $g.Dispose(); $res.Save($Out,[System.Drawing.Imaging.ImageFormat]::Png); $res.Dispose(); $tpl.Dispose(); $mask.Dispose()
    Write-Output "Sim -> $Out  (${W}x${H})"
}

# Diagnostic: uncovered white=RED, covered=GREEN, else dim. + grid
function Render-Uncovered([string]$Image, [string]$Out, [string]$Rects) {
    $tpl = Load-Logic $Image
    $W=$tpl.Width; $H=$tpl.Height
    $data=$tpl.LockBits((New-Object System.Drawing.Rectangle 0,0,$W,$H),[System.Drawing.Imaging.ImageLockMode]::ReadWrite,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $stride=$data.Stride; $b=New-Object byte[] ($stride*$H); [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0,$b,0,$b.Length)
    $cells=@(); foreach($r in ($Rects -split ';')){ if(-not $r){continue}; $p=$r -split ','; $cells += ,@([int]$p[0],[int]$p[1],[int]$p[2],[int]$p[3]) }
    for($y=0;$y -lt $H;$y++){ $row=$y*$stride
      for($x=0;$x -lt $W;$x++){ $idx=$row+$x*4
        if($b[$idx] -gt 240 -and $b[$idx+1] -gt 240 -and $b[$idx+2] -gt 240){
          $cov=$false; foreach($c in $cells){ if($x -ge $c[0] -and $x -lt $c[2] -and $y -ge $c[1] -and $y -lt $c[3]){ $cov=$true; break } }
          if($cov){ $b[$idx]=0;$b[$idx+1]=180;$b[$idx+2]=0 } else { $b[$idx]=0;$b[$idx+1]=0;$b[$idx+2]=255 }
        } else { $b[$idx]=[byte]($b[$idx]*0.35);$b[$idx+1]=[byte]($b[$idx+1]*0.35);$b[$idx+2]=[byte]($b[$idx+2]*0.35) }
      } }
    [System.Runtime.InteropServices.Marshal]::Copy($b,0,$data.Scan0,$b.Length); $tpl.UnlockBits($data)
    $g=[System.Drawing.Graphics]::FromImage($tpl); Add-Grid $g $W $H; $g.Dispose()
    $tpl.Save($Out,[System.Drawing.Imaging.ImageFormat]::Png); $tpl.Dispose()
    Write-Output "Uncovered -> $Out"
}

function Render-Outline([string]$Image, [string]$Out, [string]$Rects, [switch]$Grid) {
    $tpl = Load-Logic $Image
    $W=$tpl.Width; $H=$tpl.Height
    $g=[System.Drawing.Graphics]::FromImage($tpl); $g.SmoothingMode=[System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    if($Grid){ Add-Grid $g $W $H }
    $cols=@([System.Drawing.Color]::Red,[System.Drawing.Color]::Lime,[System.Drawing.Color]::Blue,[System.Drawing.Color]::Orange,[System.Drawing.Color]::Magenta,[System.Drawing.Color]::Cyan,[System.Drawing.Color]::Yellow,[System.Drawing.Color]::BlueViolet,[System.Drawing.Color]::Coral)
    $f=New-Object System.Drawing.Font 'Arial',46,([System.Drawing.FontStyle]::Bold); $i=0
    foreach($r in ($Rects -split ';')){ if(-not $r){continue}; $p=$r -split ','; $pen=New-Object System.Drawing.Pen $cols[$i%$cols.Length],9; $g.DrawRectangle($pen,[int]$p[0],[int]$p[1],([int]$p[2]-[int]$p[0]),([int]$p[3]-[int]$p[1])); $br=New-Object System.Drawing.SolidBrush $cols[$i%$cols.Length]; $g.DrawString("$($i+1)",$f,$br,([int]$p[0]+10),([int]$p[1]+10)); $i++ }
    $g.Dispose(); $tpl.Save($Out,[System.Drawing.Imaging.ImageFormat]::Png); $tpl.Dispose()
    Write-Output "Outline -> $Out"
}
