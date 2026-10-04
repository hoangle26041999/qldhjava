# -*- coding: utf-8 -*-
"""Fix mojibake (? thay cho chữ có dấu) trong tất cả templates.
Cách làm: thay thế `?` bằng ký tự tiếng Việt tương ứng dựa vào từ điển chuỗi đầy đủ.
Nếu một từ chứa `?` chưa có trong dict, ghi log để xử lý thủ công.
"""
import sys, os, re, json
sys.stdout.reconfigure(encoding="utf-8", errors="replace")

# Từ điển đầy đủ các từ tiếng Việt bị corrupt (có chứa ?) -> dạng đúng
WORD_MAP = {
    # Đơn giá / đơn vị / đvt
    "Don gi?": "Đơn giá",
    "don gi?": "đơn giá",
    "�on giá": "Đơn giá",
    "�on gi?": "Đơn giá",
    "�VT": "ĐVT",
    "?VT": "ĐVT",
    # Hình / ảnh
    "?nh": "Hình",
    "H�nh": "Hình",
    "?nh s?n ph?m": "ảnh sản phẩm",
    # Đơn hàng
    "T?o �on h�ng": "Tạo đơn hàng",
    "T?o don h�ng": "Tạo đơn hàng",
    "T?o don ": "Tạo đơn ",
    "�on h�ng": "đơn hàng",
    "�on hàng": "đơn hàng",
    "?n hàng": "đơn hàng",
    "S?a �on": "Sửa đơn",
    "Chi ti?t �on": "Chi tiết đơn",
    # Bảng cửa / phụ kiện / dịch vụ
    "B?ng c?a": "Bảng cửa",
    "B?ng ph? ki?n & d?ch v?": "Bảng phụ kiện & dịch vụ",
    "B?ng ph? ki?n": "Bảng phụ kiện",
    "ph? ki?n": "phụ kiện",
    "ph? ki?n / d?ch v?": "phụ kiện / dịch vụ",
    "Ph? ki?n": "Phụ kiện",
    "Ph? ki?n & d?ch v?": "Phụ kiện & dịch vụ",
    "d?ch v?": "dịch vụ",
    "D?ch v?": "Dịch vụ",
    # Loại / Tên / Sản phẩm
    "Lo?i / T�n s?n ph?m": "Loại / Tên sản phẩm",
    "Lo?i s?n ph?m": "Loại sản phẩm",
    "T�n s?n ph?m": "Tên sản phẩm",
    "T�n s?n ph?m / d?ch v?": "Tên sản phẩm / dịch vụ",
    # Kích thước / ô chờ
    "K.thu?c � ch? (R�C�D)": "K.thước ô chờ (RxCxD)",
    "K�ch thu?c": "Kích thước",
    "thu?c": "thước",
    "� ch?": "ô chờ",
    "R?ng": "Rộng",
    "R�C�D": "RxCxD",
    "Cao": "Cao",
    # Quy cách / Mô tả / Thông số
    "Quy c�ch / M� t?": "Quy cách / Mô tả",
    "Quy c�ch": "Quy cách",
    "M� t?": "Mô tả",
    "Th�ng s?": "Thông số",
    # Số lượng / SL
    "S? lu?ng": "Số lượng",
    "SL": "SL",
    # Trạng thái / Ghi chú
    "Tr?ng th�i & Ghi ch�": "Trạng thái & Ghi chú",
    "Tr?ng th�i": "Trạng thái",
    "Ghi ch� th�m": "Ghi chú thêm",
    "Ghi ch�": "Ghi chú",
    # Thông tin khách hàng
    "Th�ng tin kh�ch h�ng": "Thông tin khách hàng",
    "Th�ng tin": "Thông tin",
    "kh�ch h�ng": "khách hàng",
    "kh�ch": "khách",
    "Kh�ch h�ng": "Khách hàng",
    "Kh�ch:": "Khách:",
    # Tên / Họ tên / Mã
    "H? t�n": "Họ tên",
    "M� KH": "Mã KH",
    "M� s?n ph?m": "Mã sản phẩm",
    # Số điện thoại / Địa chỉ
    "S? di?n tho?i": "Số điện thoại",
    "�?a ch?": "Địa chỉ",
    "S�T": "SĐT",
    # Tự sinh / Tự động
    "T? sinh": "Tự sinh",
    "T? d?ng": "Tự động",
    "T? d?ng luu": "Tự động lưu",
    # Dòng cửa
    "D�ng c�nh": "Dòng cánh",
    "d�ng c?a": "dòng cửa",
    "d�ng c�nh": "dòng cánh",
    "Th�m d�ng c?a": "Thêm dòng cửa",
    "X�a d�ng c?a": "Xóa dòng cửa",
    "X�a d�ng": "Xóa dòng",
    # Phát sinh gia công
    "Ph�t sinh gia c�ng": "Phát sinh gia công",
    "Ch?n ph�t sinh": "Chọn phát sinh",
    "Ch?n ph�t sinh...": "Chọn phát sinh...",
    "Ch?n ph�t sinh gia c�ng": "Chọn phát sinh gia công",
    "Kh�ng c� ph�t sinh": "Không có phát sinh",
    "ph� h?p": "phù hợp",
    "Th�m ph�t sinh": "Thêm phát sinh",
    "Th�m ngay": "Thêm ngay",
    # Thành tiền / Tổng
    "Th�nh ti?n": "Thành tiền",
    "T?ng c?ng": "Tổng cộng",
    "T?ng ti?n": "Tổng tiền",
    "T?ng SL": "Tổng SL",
    # Khóa / Kính / Bản lề / Lắp đặt / Vận chuyển
    "Kh�a": "Khóa",
    "K�nh": "Kính",
    "B?n l?": "Bản lề",
    "L?p d?t": "Lắp đặt",
    "V?n chuy?n": "Vận chuyển",
    "Kh�c": "Khác",
    # Đơn vị
    "c�i": "cái",
    "b?": "bộ",
    "chi?c": "chiếc",
    "l?n": "lần",
    "m�": "mét",
    "Chi?c": "Chiếc",
    # Trợ giúp / hướng dẫn
    "Chua c�": "Chưa có",
    "Chua c� d�ng c?a": "Chưa có dòng cửa",
    "Chua c� ph? ki?n": "Chưa có phụ kiện",
    "C� th?": "Có thể",
    "b? qua": "bỏ qua",
    "Vui l�ng": "Vui lòng",
    "Vui l�ng nh?p": "Vui lòng nhập",
    "Vui l�ng th�m": "Vui lòng thêm",
    "nh?p t�n": "nhập tên",
    "�t nh?t": "ít nhất",
    # Hành động
    "T?o": "Tạo",
    "S?a": "Sửa",
    "Xem": "Xem",
    "X�a": "Xóa",
    "L?u": "Lưu",
    "Th�m": "Thêm",
    "Thay d?i": "Thay đổi",
    "Thay d?i tr?ng th�i": "Thay đổi trạng thái",
    "H?y": "Hủy",
    "Quay l?i": "Quay lại",
    # Tiêu đề
    "T?o don h�ng": "Tạo đơn hàng",
    "T?o don": "Tạo đơn",
    "Chi ti?t": "Chi tiết",
    "Chi ti?t don": "Chi tiết đơn",
    "Danh s�ch": "Danh sách",
    "B�ng c�a": "Bảng cửa",
    "Báo c�o": "Báo cáo",
    "T?ng quan": "Tổng quan",
    "Trang ch?": "Trang chủ",
    # Thông báo
    "th�nh c�ng": "thành công",
    "Upload l?i": "Upload lỗi",
    "L?i:": "Lỗi:",
    "�� ch?n": "Đã chọn",
    # Autocomplete
    "G� t�n": "Gõ tên",
    "t�m kh�ch cu": "tìm khách cũ",
    "N?u chua c�": "Nếu chưa có",
    "d? tr?ng": "để trống",
    "h? th?ng": "hệ thống",
    "m?i khi": "mỗi khi",
    "S? t? di?n": "Sẽ tự điền",
    "t? th�ng tin": "từ thông tin",
    "T�m theo": "Tìm theo",
    # Đặt / Hủy / Cập nhật
    "C?p nh?t": "Cập nhật",
    "C�n": "Còn",
    # Tổng hợp / Báo cáo
    "T?ng h?p": "Tổng hợp",
    "T?ng h?p v?t li?u": "Tổng hợp vật liệu",
    "v?t li?u": "vật liệu",
    "V?t li?u": "Vật liệu",
    "B�o c�o": "Báo cáo",
    "Th�ng k�": "Thống kê",
    # Quản lý
    "Qu?n l�": "Quản lý",
    "Qu?n l� don": "Quản lý đơn",
    "Qu?n l� kh�ch": "Quản lý khách",
    "Qu?n tr?": "Quản trị",
    "H? th?ng": "Hệ thống",
    "C?u h�nh": "Cấu hình",
    "Ng�n ng?": "Ngôn ngữ",
    # Khác
    "kh�ng th?": "không thể",
    "C� th?": "Có thể",
    "C? th?": "Có thể",
    "M?t kh?u": "Mật khẩu",
    "T�i kho?n": "Tài khoản",
    "��ng xu?t": "Đăng xuất",
    "��ng nh?p": "Đăng nhập",
    "Giao di?n": "Giao diện",
    "Thi?t k?": "Thiết kế",
    "M?u": "Mẫu",
    "C�": "Có",
    "Kh�ng": "Không",
    "C?a": "Cửa",
    "Nh�": "Nhà",
    "Nhà m�y": "Nhà máy",
    "Nh� m�y": "Nhà máy",
    "Nh?p": "Nhập",
    "xu?t": "xuất",
    "Xu?t": "Xuất",
    "In": "In",
    "T�i": "Tải",
    "Tải xu?ng": "Tải xuống",
    "Tải l�n": "Tải lên",
    "T?i xu?ng": "Tải xuống",
    "T?i l�n": "Tải lên",
    # các từ đơn lẻ có ?
    "Hình": "Hình",
    "thành": "thành",
    "tiền": "tiền",
    "Đang": "Đang",
    "theo": "theo",
    "thái": "thái",
    "hàng": "hàng",
    "hoặc": "hoặc",
    "cộng": "cộng",
    "Tổng": "Tổng",
    "nhất": "nhất",
    "Đơn": "Đơn",
    "hợp": "hợp",
    "đặt": "đặt",
    "chú": "chú",
    "đơn": "đơn",
    "cửa": "cửa",
    "Tạo": "Tạo",
    "Mã": "Mã",
    "để": "để",
    "có": "có",
    "hệ": "hệ",
    "sẽ": "sẽ",
    "Tự": "Tự",
    "ít": "ít",
    "ô": "ô",
    "Số": "Số",
    "Đã": "Đã",
    "lề": "lề",
    "vụ": "vụ",
    "tự": "tự",
    "Họ": "Họ",
    "Gõ": "Gõ",
    "tìm": "tìm",
    "Nếu": "Nếu",
    "lưu": "lưu",
    "mỗi": "mỗi",
    "khi": "khi",
    "tạo": "tạo",
    "lần": "lần",
    "sau": "sau",
    "cánh": "cánh",
    "ảnh": "ảnh",
    "chờ": "chờ",
    "công": "công",
    "cách": "cách",
    "Địa": "Địa",
    "chỉ": "chỉ",
    "SĐT": "SĐT",
    "thêm": "thêm",
    "thống": "thống",
    "thước": "thước",
    "tên": "tên",
    "dòng": "dòng",
    "Lắp": "Lắp",
    "Vận": "Vận",
    "chuyển": "chuyển",
    "Bản": "Bản",
    "Loại": "Loại",
    "Tên": "Tên",
    "Quy": "Quy",
    "Khóa": "Khóa",
    "Kính": "Kính",
    "tên": "tên",
    "Xóa": "Xóa",
    "Ghi": "Ghi",
    "lại": "lại",
    "tin": "tin",
    "Tìm": "Tìm",
    "kiện": "kiện",
    "Dịch": "Dịch",
    "Phụ": "Phụ",
    "Trạng": "Trạng",
    "Bảng": "Bảng",
    "Quay": "Quay",
    "chưa": "chưa",
    "động": "động",
    "dùng": "dùng",
    "kích": "kích",
    "Trạng": "Trạng",
    "Hủy": "Hủy",
    "Lỗi": "Lỗi",
    "Phát": "Phát",
    "sinh": "sinh",
    "Thông": "Thông",
    "khách": "khách",
    "thoại": "thoại",
    "trống": "trống",
    "Hình": "Hình",
    "Upload": "Upload",
    "trạng": "trạng",
    "thành": "thành",
    "tiền": "tiền",
    "Đang": "Đang",
    "theo": "theo",
    "thái": "thái",
    "hàng": "hàng",
    "hoặc": "hoặc",
    "cộng": "cộng",
    "Tổng": "Tổng",
    "nhất": "nhất",
    "Đơn": "Đơn",
    "hợp": "hợp",
    "đặt": "đặt",
    "chú": "chú",
    "đơn": "đơn",
    "cửa": "cửa",
    "Tạo": "Tạo",
    "Mã": "Mã",
    "để": "để",
    "có": "có",
    "hệ": "hệ",
    "sẽ": "sẽ",
    "Tự": "Tự",
    "ít": "ít",
    "ô": "ô",
    "Số": "Số",
    "Đã": "Đã",
    "lề": "lề",
    "vụ": "vụ",
    "tự": "tự",
    "Họ": "Họ",
    "Gõ": "Gõ",
    "tìm": "tìm",
    "Nếu": "Nếu",
    "lưu": "lưu",
    "mỗi": "mỗi",
    "khi": "khi",
    "tạo": "tạo",
    "lần": "lần",
    "sau": "sau",
    "cánh": "cánh",
    "ảnh": "ảnh",
    "chờ": "chờ",
    "công": "công",
    "cách": "cách",
    "Địa": "Địa",
    "chỉ": "chỉ",
    "SĐT": "SĐT",
    "thêm": "thêm",
    "thống": "thống",
    "thước": "thước",
    "tên": "tên",
    "dòng": "dòng",
    "Lắp": "Lắp",
    "Vận": "Vận",
    "chuyển": "chuyển",
    "Bản": "Bản",
    "Loại": "Loại",
    "Tên": "Tên",
    "Quy": "Quy",
    "Khóa": "Khóa",
    "Kính": "Kính",
    "Xóa": "Xóa",
    "Ghi": "Ghi",
    "lại": "lại",
    "tin": "tin",
    "Tìm": "Tìm",
    "kiện": "kiện",
    "Dịch": "Dịch",
    "Phụ": "Phụ",
    "Trạng": "Trạng",
    "Bảng": "Bảng",
    "Quay": "Quay",
    "chưa": "chưa",
    "động": "động",
    "dùng": "dùng",
    "kích": "kích",
    "Hủy": "Hủy",
    "Lỗi": "Lỗi",
    "Phát": "Phát",
    "sinh": "sinh",
    "Thông": "Thông",
    "khách": "khách",
    "thoại": "thoại",
    "trống": "trống",
    "Đăng": "Đăng",
    "Đặt": "Đặt",
    "Đổi": "Đổi",
    "Tắt": "Tắt",
    "Bật": "Bật",
    "Hoàn": "Hoàn",
    "tác": "tác",
    "Dừng": "Dừng",
    "Tiếp": "Tiếp",
    "Nhận": "Nhận",
    "Gửi": "Gửi",
    "Duyệt": "Duyệt",
    "Từ": "Từ",
    "Đến": "Đến",
    "Trước": "Trước",
    "Sau": "Sau",
    "Trên": "Trên",
    "Dưới": "Dưới",
    "Trong": "Trong",
    "Ngoài": "Ngoài",
    "Đầu": "Đầu",
    "Cuối": "Cuối",
    "Lỗi": "Lỗi",
    "đặt": "đặt",
    "Lỗi": "Lỗi",
    "Cao": "Cao",
    "Rộng": "Rộng",
}

# Loại bỏ trùng và sắp xếp theo độ dài giảm dần
seen = set()
unique = []
for src, dst in WORD_MAP.items():
    if src not in seen and src != dst and "?" in src or any(c in src for c in ["�", "�", "ó"]):
        seen.add(src)
        unique.append((src, dst))

# Sort by length descending (longer phrases first)
unique.sort(key=lambda x: (-len(x[0]), x[0]))

base = "d:/LUMI-HOME/qldhjava/src/main/resources/templates"
files = []
for root, dirs, fs in os.walk(base):
    for f in fs:
        if f.endswith(".html"):
            files.append(os.path.join(root, f))

results = {}
for f in files:
    raw = open(f, "rb").read()
    text = raw.decode("utf-8", errors="replace")
    orig = text
    counts = {}
    for src, dst in unique:
        if src in text:
            n = text.count(src)
            text = text.replace(src, dst)
            counts[dst] = counts.get(dst, 0) + n
    if text != orig:
        open(f, "w", encoding="utf-8", newline="").write(text)
        results[f] = counts

# In kết quả
for f, counts in sorted(results.items()):
    total = sum(counts.values())
    rel = f.replace(base + "\\", "")
    print(f"=== {rel} ({total} fixes) ===")
    for word, n in sorted(counts.items(), key=lambda kv: -kv[1])[:5]:
        print(f"  {n}x {word!r}")

# In các từ còn ? chưa fix
print("\n\n=== REMAINING '?' ===")
for f in files:
    raw = open(f, "rb").read()
    text = raw.decode("utf-8", errors="replace")
    # Tìm các từ có ? kèm space xung quanh
    matches = re.findall(r"\S*\?\S*", text)
    if matches:
        uniq = set(m for m in matches if "?" in m and len(m) > 1)
        if uniq:
            rel = f.replace(base + "\\", "")
            print(f"\n{rel}:")
            for m in sorted(uniq)[:30]:
                print(f"  {m!r}")