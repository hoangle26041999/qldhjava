# refactor-pages.ps1 - Tự động refactor 19 trang sang layout chung
$ErrorActionPreference = "Stop"
$root = "d:\LUMI-HOME\qldhjava\src\main\resources\templates"

# Bảng map: file -> (pageTitle, active key, optional contentTitle)
$map = @{
    "dashboard.html"              = @{ title="Dashboard";       active="dashboard";     contentTitle="Tổng quan hệ thống" }
    "admin\color-palette.html"    = @{ title="Bảng màu";        active="colors";        contentTitle="Quản lý bảng màu nhựa" }
    "admin\customers.html"        = @{ title="Khách hàng";       active="customers";     contentTitle="Danh sách khách hàng" }
    "admin\door-systems.html"     = @{ title="Hệ cửa";          active="door-systems";  contentTitle="Danh mục hệ cửa" }
    "admin\order-statuses.html"   = @{ title="Trạng thái đơn";   active="order-statuses"; contentTitle="Danh mục trạng thái" }
    "admin\processing-fees.html"  = @{ title="Phí gia công";     active="fees";          contentTitle="Phí gia công thêm" }
    "admin\product-specs.html"    = @{ title="Quy cách sản phẩm"; active="specs";        contentTitle="Quy cách cánh / khuôn / nẹp" }
    "orders\create-multi.html"    = @{ title="Tạo đơn nhiều cửa"; active="order-multi"; contentTitle="Nhập nhiều cửa một lúc" }
    "orders\create.html"          = @{ title="Tạo đơn mới";       active="order-new";    contentTitle="Tạo đơn hàng mới" }
    "orders\detail.html"          = @{ title="Chi tiết đơn hàng"; active="orders";       contentTitle="Thông tin chi tiết" }
    "orders\doors.html"           = @{ title="Danh sách cửa";     active="doors";        contentTitle="Các cánh cửa trong đơn" }
    "orders\list.html"            = @{ title="Đơn hàng";          active="orders";       contentTitle="Tất cả đơn hàng" }
    "orders\print-pattern.html"   = @{ title="Mẫu in";            active="print-pattern"; contentTitle="Mẫu in sản phẩm" }
    "production\material-summary.html" = @{ title="Tổng hợp NVL"; active="orders";     contentTitle="Tổng hợp nguyên vật liệu" }
    "production\orders-print.html"= @{ title="In đơn hàng";        active="orders";      contentTitle="Bảng in đơn hàng" }
    "production\orders.html"      = @{ title="Đơn sản xuất";       active="orders";      contentTitle="Đơn hàng sản xuất" }
    "production\summary.html"     = @{ title="Tổng hợp SX";        active="reports";     contentTitle="Tổng hợp sản xuất" }
    "reports\index.html"          = @{ title="Báo cáo";            active="reports";     contentTitle="Báo cáo thống kê" }
}

foreach ($rel in $map.Keys) {
    $src = Join-Path $root $rel
    $cfg = $map[$rel]
    if (-not (Test-Path $src)) { Write-Host "MISSING $rel"; continue }
    $raw = Get-Content $src -Raw -Encoding UTF8

    # Trích phần body
    if ($raw -match '(?s)<body[^>]*>(.*)</body>') {
        $body = $matches[1].Trim()
    } else {
        $body = $raw
    }

    # Loại bỏ các thẻ <style>...</style> và <script src CDN>...</script> (CDN đã có trong layout)
    $body = [regex]::Replace($body, '(?s)<style[^>]*>.*?</style>', '')
    # Loại bỏ <script src="https://..."></script> (Bootstrap, jQuery)
    $body = [regex]::Replace($body, '<script\s+src="https://[^"]+"[^>]*></script>', '')

    # Loại bỏ <link href="https://cdn..."> (Bootstrap CSS)
    $body = [regex]::Replace($body, '<link\s+href="https://cdn[^"]+"[^>]*/?>', '')

    # Loại bỏ wrapper có class main-content/alert nếu trùng với layout
    # (giữ nguyên, vì một số trang có alert riêng)

    $title = $cfg.title
    $active = $cfg.active
    $subtitle = $cfg.contentTitle

    $newContent = @"
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout" layout:decorate="~{layout}">
<head><title>$title</title></head>
<body>
<th:block layout:fragment="content">
<div class="page-header">
    <h2>$title</h2>
    <div class="subtitle">$subtitle</div>
</div>
$body
</th:block>
<th:block layout:fragment="scripts"></th:block>
</body>
</html>
"@

    Set-Content -Path $src -Value $newContent -Encoding UTF8
    Write-Host "OK $rel"
}
Write-Host "Done."