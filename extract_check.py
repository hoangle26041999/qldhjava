import sys
sys.stdout.reconfigure(encoding="utf-8", errors="replace")
import zipfile
path = "d:/LUMI-HOME/qldhjava/target/plastic-factory-manager-1.0.0.jar"
targets = [
    "BOOT-INF/classes/templates/orders/create-multi.html",
    "BOOT-INF/classes/templates/orders/detail.html",
    "BOOT-INF/classes/templates/admin/product-specs.html",
    "BOOT-INF/classes/templates/production/material-summary.html",
    "BOOT-INF/classes/templates/dashboard.html",
]
z = zipfile.ZipFile(path)
for name in targets:
    raw = z.read(name)
    text = raw.decode("utf-8", errors="replace")
    print(name)
    print("  size:", len(raw))
    print("  q:", text.count("?"))
    print("  moji:", text.count("Ä"))
    print("  Hình:", text.count("Hình"))
    print("  Đơn:", text.count("Đơn"))
    print("  Phụ:", text.count("Phụ"))
    print("  Tạo:", text.count("Tạo"))
    print()