"""Danh mục tài liệu bàn giao — nguồn dùng chung cho các bộ sinh (`docs_to_*.py`).

Để ở một chỗ vì hai bộ sinh (.docx cho hồ sơ nghiệm thu, wiki markup cho Confluence) phải giao
**cùng một danh sách, cùng thứ tự**; tách hai bản là tự tạo chỗ để chúng lệch nhau.
Thứ tự này khớp §3 của docs/release/PackagingGuide.md.
"""
from __future__ import annotations

import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PRODUCT = "TTCN Promotion SDK"

# Space Confluence đánh số tên trang theo cây: "3. Tài liệu kỹ thuật" → "3.3 Thiết kế chi tiết"
# → "3.3.14 Tài liệu thiết kế Mobile". Bộ tài liệu này là con của mục đó nên tên trang phải nối
# tiếp: "3.3.14.1", "3.3.14.2"… Chỉ áp cho TÊN TRANG trên wiki — bản .docx giữ tiêu đề trơn.
CONFLUENCE_PREFIX = "3.3.14"
CONFLUENCE_PARENT_ID = "207750898"          # trang "3.3.14 Tài liệu thiết kế Mobile"


def page_title(name: str, title: str) -> str:
    """Tên trang Confluence: "3.3.14.<số thứ tự> <tiêu đề>". `name` là "01_TaiLieu…"."""
    return f"{CONFLUENCE_PREFIX}.{int(name[:2])} {title}"

# (đường dẫn nguồn, tên file sinh ra — KHÔNG đuôi, tiêu đề tài liệu)
DOCS: list[tuple[str, str, str]] = [
    ("docs/design/SDD.md",                    "01_TaiLieuThietKeChiTiet",   "Tài liệu thiết kế chi tiết"),
    ("docs/AndroidIntegrationGuide.md",       "02_HuongDanTichHop_Android", "Hướng dẫn tích hợp — Android"),
    ("docs/IosIntegrationGuide.md",           "03_HuongDanTichHop_iOS",     "Hướng dẫn tích hợp — iOS"),
    ("docs/QuickStart.md",                    "04_QuickStart",              "Bắt đầu nhanh"),
    ("docs/common/Security.md",               "05_BaoMat",                  "Bảo mật, dữ liệu & tuân thủ"),
    ("docs/release/ReleaseNotes.md",          "06_ReleaseNotes",            "Ghi chú phát hành"),
    ("docs/release/CompatibilityMatrix.md",   "07_MaTranTuongThich",        "Ma trận tương thích"),
    ("docs/release/VersioningPolicy.md",      "08_ChinhSachVersion",        "Chính sách phiên bản & hỗ trợ"),
    ("docs/Troubleshooting.md",               "09_XuLySuCo",                "Xử lý sự cố & FAQ"),
    ("docs/release/TestReport.md",            "10_BaoCaoKiemThu",           "Báo cáo kiểm thử"),
    ("docs/release/MigrationGuide.md",        "11_HuongDanNangCap",         "Hướng dẫn nâng cấp"),
    ("docs/release/PackagingGuide.md",        "12_HuongDanDongGoi",         "Đóng gói bản bàn giao"),
    ("docs/release/ReleaseChecklist.md",      "13_ChecklistPhatHanh",       "Danh mục kiểm trước phát hành"),
    ("docs/release/HandoverChecklist.md",     "14_ChecklistBanGiao",        "Danh mục bàn giao"),
    ("docs/common/PublicApi.md",              "15_BeMatPublicAPI",          "Bề mặt public cho app host"),
    ("THIRD_PARTY_NOTICES.md",                "16_ThirdPartyNotices",       "Thành phần bên thứ ba"),
    ("LICENSE.md",                            "17_GiayPhep",                "Giấy phép sử dụng"),
]


def git(*args: str) -> str:
    try:
        return subprocess.run(["git", "-C", str(ROOT), *args],
                              capture_output=True, text=True, check=True).stdout.strip()
    except Exception:
        return "(không xác định)"


def sdk_version() -> str:
    props = ROOT / "gradle.properties"
    if props.exists():
        m = re.search(r"^SDK_VERSION=(.+)$", props.read_text(encoding="utf-8"), re.M)
        if m:
            return m.group(1).strip()
    return "(không xác định)"
