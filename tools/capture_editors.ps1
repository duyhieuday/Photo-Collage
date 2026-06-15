# Launch each template editor directly via adb and capture device screenshot. ASCII only.
$adb="C:\Users\CHATTT\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$pkg="com.ezt.photo.collage.maker"
$act="$pkg/com.example.piceditor.templates_editor.TemplateEditorActivity"
$outDir="D:\EZTech\EZTechApp\collage_pic_editor\tools\_out\dev"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$ids=@()
foreach($p in @(@("bd",10),@("cp",9),@("gs",10),@("is",15),@("sm",9))){ for($i=1;$i -le $p[1];$i++){ $ids+=("{0}{1:D2}" -f $p[0],$i) } }
$ids+="sp01"

foreach($id in $ids){
  & $adb shell am start -n $act --es extra_template_id $id --activity-clear-top | Out-Null
  Start-Sleep -Milliseconds 2300
  & $adb shell screencap -p /sdcard/e.png
  & $adb pull /sdcard/e.png (Join-Path $outDir "$id.png") | Out-Null
  Write-Host "captured $id"
}
Write-Host "DONE"
