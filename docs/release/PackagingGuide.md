# PackagingGuide — Đóng gói bản bàn giao

Tài liệu này mô tả **cách tạo ra gói bàn giao** và **gói đó gồm những gì**. Nó không lặp lại cơ chế
publish (đã có ở [`../android/Distribution.md`](../android/Distribution.md) và
[`../ios/Distribution.md`](../ios/Distribution.md)) — nó nói về *sản phẩm cuối cùng đưa cho đối tác*.

## Mục lục

<!-- toc -->
- [1. Nguyên tắc](#1-nguyên-tắc)
- [2. Lệnh tạo artifact](#2-lệnh-tạo-artifact)
- [3. Nội dung gói bàn giao](#3-nội-dung-gói-bàn-giao)
- [4. Sinh `MANIFEST.txt` và checksum](#4-sinh-manifesttxt-và-checksum)
- [5. dSYM của iOS — bắt buộc lưu](#5-dsym-của-ios--bắt-buộc-lưu)
- [6. Sinh tài liệu để phát hành](#6-sinh-tài-liệu-để-phát-hành)
  - [6.1. Bản `.docx` — hồ sơ nghiệm thu](#61-bản-docx--hồ-sơ-nghiệm-thu)
  - [6.2. Bản Confluence — wiki nội bộ / của đối tác](#62-bản-confluence--wiki-nội-bộ--của-đối-tác)
  - [6.3. Đẩy thẳng lên Confluence (thay cho dán tay)](#63-đẩy-thẳng-lên-confluence-thay-cho-dán-tay)
- [7. Điểm dễ sai (đã gặp)](#7-điểm-dễ-sai-đã-gặp)
<!-- /toc -->

---

## 1. Nguyên tắc

1. **Một số version cho cả gói.** `SDK_VERSION` trong `gradle.properties` (Android/KMP) và
   `MARKETING_VERSION` trong `Config/Version.xcconfig` (iOS) **phải trùng nhau**. Đây là điểm đồng bộ **thủ
   công** duy nhất còn lại — sai số là bàn giao lệch gói.
2. **Không gọi tay từng Gradle task.** Luôn dùng script; version và toạ độ Maven đọc từ
   `gradle.properties`.
3. **Không đè version đã phát hành.** Bản đã publish là thứ người khác đang build theo. Cả hai script
   đều chặn ghi đè và bắt phải `--force` tường minh.
4. **Gói bàn giao là bản đã build từ commit có tag.** Không đóng gói từ cây làm việc còn thay đổi
   chưa commit.

---

## 2. Lệnh tạo artifact

```bash
# Android — publish lên Artifactory (mặc định Viettelmoney)
./scripts/build-android.sh publish -v 1.0.0

# Android — thử vào ~/.m2 trước khi đẩy lên server
./scripts/build-android.sh publish --target local

# iOS — dựng Promotion.xcframework rồi đẩy lên Artifactory
./scripts/build-ios.sh publish -v 1.0.0

# iOS — dựng + kiểm tra, không gửi gì lên
./scripts/build-ios.sh publish --dry-run

# Test + coverage cho hồ sơ nghiệm thu
./scripts/test-report.sh --json
```

> `./scripts/build-android.sh -h` và `./scripts/build-ios.sh -h` in đầy đủ tuỳ chọn.

Chuỗi phụ thuộc bên iOS (script ép sẵn đúng thứ tự):

```
:promotionLogic (Kotlin) ──gradle──▶ PromotionLogic.xcframework  (lõi, static)
        └─ link tĩnh vào ──▶ Promotion.xcframework  (UI, Swift, dynamic)
                                     └─ host: Embed & Sign
```

---

## 3. Nội dung gói bàn giao

Đề xuất cấu trúc thư mục giao cho đối tác — `TTCN-PromotionSDK-1.0.0/`:

```
TTCN-PromotionSDK-1.0.0/
├── README.txt                      # 1 trang: gói này là gì, đọc file nào trước
├── android/
│   ├── COORDINATES.txt             # vn.viettelpay.library:promotion:1.0.0 + repo URL
│   └── (tuỳ chọn) promotion-1.0.0.aar + promotionLogic-1.0.0.* + POM/.module
├── ios/
│   ├── COORDINATES.txt             # URL zip trên vdo-ios-frameworks + sha256 + đoạn binaryTarget mẫu
│   ├── (tuỳ chọn) Promotion-<version>.xcframework.zip + metadata.json
│   └── dSYM/                       # BẮT BUỘC lưu lại — xem §5
├── docs/
│   ├── 01_TaiLieuThietKeChiTiet.docx        (docs/design/SDD.md)
│   ├── 02_HuongDanTichHop_Android.docx      (docs/AndroidIntegrationGuide.md)
│   ├── 03_HuongDanTichHop_iOS.docx          (docs/IosIntegrationGuide.md)
│   ├── 04_QuickStart.docx                   (docs/QuickStart.md)
│   ├── 05_BaoMat.docx                       (docs/common/Security.md)
│   ├── 06_ReleaseNotes.docx                   (docs/release/ReleaseNotes.md)
│   ├── 07_MaTranTuongThich.docx             (CompatibilityMatrix)
│   ├── 08_ChinhSachVersion.docx             (VersioningPolicy)
│   ├── 09_XuLySuCo.docx                     (Troubleshooting)
│   └── 10_BaoCaoKiemThu.docx                (TestReport)
├── legal/
│   ├── LICENSE.md
│   └── THIRD_PARTY_NOTICES.md
└── MANIFEST.txt                    # danh mục file + SHA-256 + version + commit + ngày build
```

> **Cả hai nền tảng thường KHÔNG cần kèm file binary.** Đối tác kéo qua Artifactory: Android bằng
> toạ độ Maven, iOS bằng `binaryTarget` (`url:` + `checksum:`). Chỉ kèm file rời khi đối tác chưa
> được cấp tài khoản đọc repo — iOS khi đó kèm **cả** `metadata.json` để họ có `sha256` đúng.
>
> **Android thường KHÔNG cần kèm file AAR.** Đối tác kéo qua Maven từ Artifactory nội bộ; đưa thêm
> file rời dễ dẫn đến việc họ tích hợp bản không khớp metadata. Chỉ kèm file khi đối tác chưa được
> cấp tài khoản đọc repo — và khi đó phải kèm **cả** `promotionLogic` cùng POM/`.module`, nếu không
> Gradle của họ sẽ không resolve được phụ thuộc.

---

## 4. Sinh `MANIFEST.txt` và checksum

```bash
cd TTCN-PromotionSDK-1.0.0
{
  echo "TTCN Promotion SDK 1.0.0"
  echo "Commit : $(git -C /path/to/repo rev-parse HEAD)"
  echo "Tag    : $(git -C /path/to/repo describe --tags --always)"
  echo "Ngày   : $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "Người  : <tên người đóng gói>"
  echo
  echo "SHA-256:"
  find . -type f ! -name MANIFEST.txt -exec shasum -a 256 {} \;
} > MANIFEST.txt
```

Bên nhận kiểm lại bằng `shasum -a 256 -c`.

---

## 5. dSYM của iOS — bắt buộc lưu

Mỗi bản phát hành iOS **phải lưu lại dSYM**. Không có nó thì crash report từ app đối tác chỉ còn địa
chỉ bộ nhớ, không symbolicate được — mà bản build đó thì không dựng lại y hệt được nữa.

Nơi lưu: kho nội bộ của đội SDK, đặt tên theo version. Chi tiết ở
[`../ios/Distribution.md`](../ios/Distribution.md) §1.

Android không cần bản đồ tương ứng: AAR **không obfuscate**, nên stacktrace đối tác gửi về đọc thẳng
được tên class/hàm, không cần `mapping.txt`.

---

## 6. Sinh tài liệu để phát hành

**Markdown trong repo là nguồn sự thật**; `.docx` và bản trên Confluence chỉ là bản in. Sửa bản in
rồi coi đó là bản mới thì lần sinh sau ghi đè mất — sửa Markdown rồi sinh lại.

Danh mục 17 tài liệu (và thứ tự) nằm ở `scripts/_docs_manifest.py`, dùng chung cho cả hai bộ sinh
để chúng không lệch nhau. Thêm/bớt tài liệu thì sửa **một** chỗ đó và §3 ở trên.

### 6.1. Bản `.docx` — hồ sơ nghiệm thu

```bash
python3 scripts/docs_toc.py                # 1. đánh số đầu mục + dựng mục lục
python3 scripts/docs_links.py              # 2. kiểm link nội bộ (đánh số làm đổi anchor)
python3 scripts/docs_to_docx.py            # 3. sinh .docx vào dist/docs-docx/
python3 scripts/docs_to_docx.py --list     # xem danh sách tài liệu sẽ sinh
```

**Đánh số đầu mục.** `docs_toc.py` đánh lại số phân cấp cho H2–H4 theo thứ tự xuất hiện
(`1.` → `1.1.` → `1.1.1.`), bóc số cũ rồi đánh lại — chèn/xoá/đổi chỗ một mục là cả tài liệu tự
dồn số. Ngoại lệ có chủ đích: mục đã đánh `0.` (phần mở đầu đứng trước mục 1, ví dụ `0. TL;DR`)
được **giữ nguyên** và không tính vào bộ đếm.

**Mục lục** nằm giữa hai mốc `<!-- toc -->` / `<!-- /toc -->` nên chạy lại chỉ thay phần bên trong.
`--check` chỉ báo file lệch và trả mã lỗi khác 0, dùng được cho CI. Bản `.docx` **bỏ qua** mục lục
Markdown này vì Word đã có mục lục riêng kèm số trang (nhấn `F9` để dựng).

⚠️ **Đánh số làm đổi anchor** của tiêu đề (`#1-tổng-quan` → `#2-tổng-quan`), mà link cũ thì vẫn
trông đúng. Vì vậy luôn chạy `docs_links.py` ngay sau — nó kiểm cả file đích lẫn `#anchor`, dùng
chung hàm sinh slug với bộ mục lục nên không có chuyện hai bên hiểu khác nhau.

Danh mục tài liệu bàn giao khai **một chỗ** ở `scripts/_docs_manifest.py`; mọi bộ sinh đọc chung
danh sách đó để không có hai bản lệch nhau.

Yêu cầu: `python-docx`. Cài trong virtualenv để không đụng Python hệ thống:

```bash
python3 -m venv .venv && ./.venv/bin/pip install python-docx
./.venv/bin/python scripts/docs_to_docx.py
```

### 6.2. Bản Confluence — wiki nội bộ / của đối tác

```bash
python3 scripts/docs_to_confluence.py      # sinh vào dist/docs-confluence/
```

Không cần thư viện ngoài. Mỗi tài liệu ra một file `.confluence` chứa **Confluence wiki markup**,
kèm `README.txt` hướng dẫn dán và cây trang gợi ý. Cách dán: tạo trang → đặt tiêu đề đúng như
`README.txt` ghi → **+** (Insert) → *Markup* → **Confluence wiki markup** → dán → Insert.

Tiêu đề trang phải đặt đúng vì **link chéo giữa các tài liệu được sinh theo tiêu đề**; đặt sai là
link gãy. Bản sinh cố tình chỉ dùng cú pháp có ở cả Cloud lẫn Server/DC, nên Kotlin/Swift/JSON/YAML
trong `{code}` bị hạ về ngôn ngữ gần nhất hoặc bỏ tô màu — nội dung code không đổi.

### 6.3. Đẩy thẳng lên Confluence (thay cho dán tay)

Dán tay 17 trang mỗi lần phát hành là việc dễ sót. Có script đẩy qua REST API:

```bash
cp .confluence.env.example .confluence.env     # rồi điền base URL / space / user / token
python3 scripts/docs_to_confluence.py          # 1. sinh markup
python3 scripts/confluence_publish.py --dry-run # 2. xem sẽ tạo/cập nhật trang nào
python3 scripts/confluence_publish.py           # 3. đẩy thật
```

Script **khớp trang theo tiêu đề**: lần đầu tạo mới, các lần sau cập nhật đúng trang cũ (tăng
version, không đẻ trang trùng). Tài liệu 01 làm trang cha, 16 trang còn lại làm con — thêm `--flat`
nếu muốn phẳng, `--only 01 04` để chỉ đẩy vài tài liệu.

`.confluence.env` chứa **token**, đã bị `.gitignore` chặn — đừng commit, đừng dán vào ticket. Token
Cloud lấy ở `id.atlassian.com/manage-profile/security/api-tokens`; Server/DC dùng Personal Access
Token (để trống `CONFLUENCE_USER`).

`dist/` **không** commit vào repo.

---

## 7. Điểm dễ sai (đã gặp)

| Sai | Hậu quả | Phòng |
|---|---|---|
| Bump Android mà quên iOS `MARKETING_VERSION` | Hai nền tảng khác số, đối tác báo lỗi "không rõ đang dùng bản nào" | Bước 1 của [ReleaseChecklist](./ReleaseChecklist.md) |
| Publish một trong hai module Android | Host resolve ra bản lõi không tồn tại | Script luôn publish **cả hai** cùng lượt |
| Đè zip iOS mà giữ nguyên URL | SPM lấy lại zip cũ từ cache tầng 2 → lỗi checksum sai lệch, log dễ lạc hướng | `build-ios.sh publish` **luôn** tự dọn cache SPM của version vừa đẩy |
| Bàn giao bản build từ cây còn thay đổi chưa commit | Không tái tạo được artifact khi cần điều tra | `MANIFEST.txt` ghi commit; kiểm `git status` sạch trước khi build |
| Quên tắt log debug | Rò `Authorization` trong log của app đối tác | Mục bắt buộc trong [ReleaseChecklist](./ReleaseChecklist.md) |
