# HandoverChecklist — Danh mục bàn giao

Dùng khi giao một bản SDK cho đối tác tích hợp. Mỗi dòng phải có người chịu trách nhiệm và ngày hoàn
thành.

| Mục | Giá trị |
|---|---|
| Bản bàn giao | `______` |
| Đối tác nhận | `______` |
| Ngày bàn giao | `______` |
| Đầu mối phía SDK | `______` |
| Đầu mối phía đối tác | `______` |

## Mục lục

<!-- toc -->
- [1. Artifact](#1-artifact)
- [2. Tài liệu](#2-tài-liệu)
- [3. Nghiệm thu kỹ thuật cùng đối tác](#3-nghiệm-thu-kỹ-thuật-cùng-đối-tác)
- [4. Vận hành & hỗ trợ](#4-vận-hành--hỗ-trợ)
- [5. Điểm cần nêu rõ khi bàn giao (đừng để đối tác tự phát hiện)](#5-điểm-cần-nêu-rõ-khi-bàn-giao-đừng-để-đối-tác-tự-phát-hiện)
<!-- /toc -->

---

## 1. Artifact

| # | Hạng mục | Xong | Người | Ngày |
|---|---|---|---|---|
| A-1 | Android: `vn.viettelpay.library:promotion:<version>` đã có trên Artifactory, đối tác **resolve thử thành công** | ☐ | | |
| A-2 | Android: `promotionLogic` cùng version đã lên **cùng lượt** | ☐ | | |
| A-3 | iOS: `Promotion-<version>.xcframework.zip` + `metadata.json` (`download_url` + `sha256`) đã lên `vdo-ios-frameworks/Martech/Promotion/<version>/` | ☐ | | |
| A-4 | iOS: dSYM đã lưu vào kho nội bộ theo version | ☐ | | |
| A-5 | `MANIFEST.txt` (danh mục + checksum + commit + ngày build) | ☐ | | |
| A-6 | Đối tác đã được cấp tài khoản đọc Artifactory — **cả hai repo**: `gradle-viettelmoney` (Android) và `vdo-ios-frameworks` (iOS), kèm hướng dẫn khai `~/.netrc` | ☐ | | |

## 2. Tài liệu

| # | Tài liệu | Xong | Ghi chú |
|---|---|---|---|
| D-1 | [Tài liệu thiết kế chi tiết](../design/SDD.md) | ☐ | |
| D-2 | [Hướng dẫn tích hợp Android](../AndroidIntegrationGuide.md) | ☐ | |
| D-3 | [Hướng dẫn tích hợp iOS](../IosIntegrationGuide.md) | ☐ | |
| D-4 | [QuickStart](../QuickStart.md) | ☐ | |
| D-5 | [Bề mặt public](../common/PublicApi.md) | ☐ | |
| D-6 | [Release Notes](./ReleaseNotes.md) | ☐ | |
| D-7 | [Ma trận tương thích](./CompatibilityMatrix.md) | ☐ | |
| D-8 | [Chính sách phiên bản & hỗ trợ](./VersioningPolicy.md) | ☐ | |
| D-9 | [Hướng dẫn nâng cấp](./MigrationGuide.md) | ☐ | chỉ khi không phải bản đầu |
| D-10 | [Xử lý sự cố](../Troubleshooting.md) | ☐ | |
| D-11 | [Bảo mật & dữ liệu](../common/Security.md) | ☐ | |
| D-12 | [Báo cáo kiểm thử](./TestReport.md) đã điền kết quả | ☐ | |
| D-13 | [Giấy phép](../../LICENSE.md) + [THIRD_PARTY_NOTICES](../../THIRD_PARTY_NOTICES.md) | ☐ | |
| D-14 | Bản `.docx` (và bản Confluence, nếu đối tác dùng wiki) đã sinh lại từ Markdown mới nhất | ☐ | |

## 3. Nghiệm thu kỹ thuật cùng đối tác

| # | Hạng mục | Xong | Ghi chú |
|---|---|---|---|
| T-1 | Đối tác build thành công app của họ với SDK mới (Android + iOS) | ☐ | |
| T-2 | Chạy đủ luồng: danh sách → tìm kiếm → chi tiết → áp dụng → widget → xác nhận sử dụng | ☐ | |
| T-3 | Thử với bản minify/release của app host | ☐ | |
| T-4 | Thử token hết hạn giữa phiên | ☐ | |
| T-5 | Thử kill-switch `PROMOTION.ENABLE_ALL` | ☐ | |
| T-6 | Xác nhận bản release không in log nhạy cảm | ☐ | |
| T-7 | Đối tác xác nhận danh sách phụ thuộc transitive không xung đột với app của họ (Android) | ☐ | |

## 4. Vận hành & hỗ trợ

| # | Hạng mục | Xong | Ghi chú |
|---|---|---|---|
| O-1 | Thống nhất kênh hỗ trợ và thời gian phản hồi | ☐ | |
| O-2 | Thống nhất cách báo lỗi: kèm **version SDK** + **`X-Request-ID`** + log tái hiện | ☐ | |
| O-3 | Đối tác biết chính sách vòng đời hỗ trợ (Current / Maintenance 6 tháng / EOL) | ☐ | |
| O-4 | Đã bàn giao cách bật/tắt feature flag phía server và ai có quyền | ☐ | |
| O-5 | Đã thống nhất quy trình hotfix (ai gọi ai, trong bao lâu) | ☐ | |

## 5. Điểm cần nêu rõ khi bàn giao (đừng để đối tác tự phát hiện)

- **Android host thấy** Ktor, coroutines, AppCompat, Glide, Gson trên compile classpath — cần rà
  trùng version. iOS thì không (mọi thứ link tĩnh, giấu trong framework).
- **AAR không obfuscate** — hàng rào là hợp đồng API (`internal` + không sources.jar + resource
  private), không phải chống dịch ngược.
- **Không có certificate pinning trong SDK** — nếu bên an ninh yêu cầu, phải bàn trước vì đó là thay
  đổi hợp đồng cấu hình.
- **Host phải gọi `release()` khi đăng xuất.**
- **`currentToken()` bị gọi từ thread nền** — phải thread-safe, không `@MainActor`.
- **`onExpireToken()` hiện chưa áp dụng cho headless API** — host dùng headless phải tự bắt
  `PromotionSDKError.SessionExpired`.
- Các điểm **lệch Android ↔ iOS (N1)** đã biết — xem [PublicApi](../common/PublicApi.md) và
  [InitParity](../common/InitParity.md).

---

**Bên giao:** ____________ **Ngày:** ________
**Bên nhận:** ____________ **Ngày:** ________
