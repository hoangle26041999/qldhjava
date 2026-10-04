# build-templates.ps1
# Copy tất cả file .html từ src/main/resources/templates/ sang
# target/classes/templates/ và runtime-lib/templates/ để áp dụng ngay khi dev
# (chạy java -jar mà không cần mvn package lại).

$src = Join-Path $PSScriptRoot "src\main\resources\templates"
$dst1 = Join-Path $PSScriptRoot "target\classes\templates"
$dst2 = Join-Path $PSScriptRoot "runtime-lib\templates"

if (!(Test-Path $src)) { Write-Error "Không tìm thấy $src"; exit 1 }

$count += Copy-Item -Path "$src\*" -Destination $dst1 -Recurse -Force -PassThru | Measure-Object | ForEach-Object { $_.Count }
$count += Copy-Item -Path "$src\*" -Destination $dst2 -Recurse -Force -PassThru | Measure-Object | ForEach-Object { $_.Count }

Write-Host "[OK] Đã copy $count file templates từ src -> target/classes và runtime-lib" -ForegroundColor Green