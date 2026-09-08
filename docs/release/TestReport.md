# TestReport — Báo cáo kiểm thử (mẫu + hiện trạng)

| Mục | Giá trị |
|---|---|
| Sản phẩm | TTCN Promotion SDK |
| Version kiểm thử | `______` |
| Commit | `______` |
| Ngày chạy | `______` |
| Người chạy | `______` |
| Môi trường | macOS `___` · JDK `___` · Xcode `___` · thiết bị/simulator `___` |

> **Cách dùng.** §1–§3 mô tả bộ test **có sẵn trong repo** (cập nhật khi bộ test đổi). §4–§7 là phần
> **điền kết quả cho từng bản phát hành** — copy file này vào hồ sơ nghiệm thu rồi điền, đừng ghi đè
> bản gốc.

## Mục lục

<!-- toc -->
- [1. Chiến lược kiểm thử](#1-chiến-lược-kiểm-thử)
- [2. Bộ test hiện có — `commonTest` (43 file)](#2-bộ-test-hiện-có--commontest-43-file)
- [3. Lệnh chạy](#3-lệnh-chạy)
- [4. Kết quả tự động — điền cho bản phát hành](#4-kết-quả-tự-động--điền-cho-bản-phát-hành)
- [5. Coverage — điền cho bản phát hành](#5-coverage--điền-cho-bản-phát-hành)
- [6. Kiểm thử thủ công — điền cho bản phát hành](#6-kiểm-thử-thủ-công--điền-cho-bản-phát-hành)
- [7. Lỗi còn tồn — điền cho bản phát hành](#7-lỗi-còn-tồn--điền-cho-bản-phát-hành)
<!-- /toc -->

---

## 1. Chiến lược kiểm thử

| Tầng | Nơi test | Chạy trên | Vì sao đặt ở đó |
|---|---|---|---|
| Nghiệp vụ + logic hiển thị | `promotionLogic/src/commonTest` | **Cả JVM lẫn Kotlin/Native** | Logic dùng chung thì test dùng chung. Một test chỉ pass trên JVM không chứng minh được gì về iOS |
| UI Android | `AndroidPromotionSDK` unit test | JVM | Phần chỉ Android mới có |
| Tích hợp đầu-cuối | `androidApp` / `iosApp` đóng vai host | Thiết bị/simulator | Kiểm đúng artifact đối tác sẽ nhận |

Nguyên tắc bắt buộc:

1. **Kiểm cả hai chiều** ở mọi test API — *payload gửi lên* lẫn *kết quả map xuống*. Bỏ
   `encodeDefaults = true` thì code vẫn biên dịch, test map-xuống vẫn xanh, mà **server nhận thiếu
   field**; chỉ test payload bắt được.
2. **Chạy cả hai target trước khi phát hành.** Kotlin/Native khác JVM ở freeze, thread và khởi tạo lazy.
3. **Không tin "BUILD SUCCESSFUL".** Gradle báo thành công cả khi không có test nào chạy.
4. Không test qua network thật (`MockEngine`), không test phụ thuộc thời gian thực.

---

## 2. Bộ test hiện có — `commonTest` (43 file)

| Nhóm | File | Bao gì |
|---|---|---|
| **Đường ống API** | `PromotionPipelineTest`, `ApiMappingVerifyTest`, `MalformedResponseTest`, `NullResponseBranchTest`, `HttpErrorBranchTest` | JSON → DTO → domain; header; lỗi HTTP; lỗi nghiệp vụ ẩn trong HTTP 200; payload `createRedemption` |
| **Tìm ưu đãi đủ điều kiện** | `EligibleCampaignsTest`, `EligibleMappingBranchTest`, `EligibleOrderItemsServiceTest`, `ForSectionPageTest` | Map hai nhóm `myOffers`/`otherOffers`, sort tab, payload pagination/section, dòng sản phẩm |
| **Voucher & mapping** | `VoucherMappingBranchTest`, `VoucherDetailFieldBranchTest`, `VoucherStatusTest`, `NumericAmountTest`, `PromotionHtmlContentTest`, `DiscountMapperTest` | Mapper DTO→domain, trạng thái voucher, số tiền, nội dung HTML |
| **Store màn hình** | `MyPromotionStoreTest`, `MyPromotionBranchTest`, `MyPromotionLastBranchTest`, `SearchMyPromotionStoreTest`, `PromotionDetailStoreTest`, `ChoosePromotionStoreTest`, `ChoosePromotionStoreBranchTest`, `EndowStoreTest`, `StoreEdgeBranchTest`, `PresentationSharedTest`, `ResolveActiveTabTest` | State/Intent, latest-wins, load-more, tab |
| **Đồng thời** | `CanApplyDuringRefreshTest`, `RequestAndStaleBranchTest` | Response về muộn, thao tác khi đang refresh |
| **Token & phiên** | `TokenPullPerRequestTest`, `TokenRefreshGateTest`, `TokenRefreshRetryTest` | Token là pull; 401 thử lại **một lần**; single-flight |
| **Feature flag** | `FeatureFlagTest`, `FeatureFlagGateTest` | Cache, `ENABLE_ALL` là công tắc tổng, `refresh()` không ném khi API lỗi |
| **Hạ tầng** | `PromotionContainerTest`, `ComponentRegistryTest`, `PromotionPreferencesTest`, `PromotionClockTest`, `PromotionCurlLoggingTest`, `NoContextProviderTest`, `EndowHostNotifierTest`, `BusinessRuleViolationNullTest`, `RemainingBranchTest`, `ValidateDiscountsOutcomeTest` | DI, storage, clock, logging, thông báo host |

---

## 3. Lệnh chạy

```bash
./gradlew :promotionLogic:testAndroidHostTest        # JVM
./gradlew :promotionLogic:iosSimulatorArm64Test      # Kotlin/Native (cần macOS)
./gradlew :AndroidPromotionSDK:testDebugUnitTest     # tầng UI Android
./gradlew :promotionLogic:koverVerify                # gác ngưỡng coverage
./scripts/test-report.sh --json                      # cả 2 target + coverage, kèm bản .json cho CI
```

Đọc số test thật (không tin dòng "BUILD SUCCESSFUL"):

```bash
grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' \
  promotionLogic/build/test-results/testAndroidHostTest/*.xml
```

---

## 4. Kết quả tự động — điền cho bản phát hành

| Bộ test | Lệnh | Tổng | Pass | Fail | Skip | Thời gian |
|---|---|---|---|---|---|---|
| Lõi — JVM | `:promotionLogic:testAndroidHostTest` | | | | | |
| Lõi — iOS simulator | `:promotionLogic:iosSimulatorArm64Test` | | | | | |
| UI Android | `:AndroidPromotionSDK:testDebugUnitTest` | | | | | |

**Kết luận:** ☐ Đạt ☐ Không đạt — ghi chú: `______`

## 5. Coverage — điền cho bản phát hành

| Chỉ số | Ngưỡng | Thực tế | Đạt? |
|---|---|---|---|
| LINE | ≥ 93% | | ☐ |
| INSTRUCTION | ≥ 92% | | ☐ |
| BRANCH | ≥ 90% | | ☐ |

Đo trên target `android` (JVM) vì Kover cần bytecode JVM; test nằm ở `commonTest` và chạy trên cả hai
target nên số liệu vẫn phản ánh đúng `commonMain`.

Loại trừ khỏi phép đo: DTO thuần dữ liệu (`*Dto`, `*Request`, `*Response`, `$serializer`) và cầu nền
tảng `SdkLockKt`.

**Nhánh chết đã biết (không phải thiếu test):** `ChoosePromotionStore` có nhánh
`it.isMultiSelection -> cur + id` mà không Intent nào bật được `isMultiSelection`.

## 6. Kiểm thử thủ công — điền cho bản phát hành

| # | Kịch bản | Android | iOS | Ghi chú |
|---|---|---|---|---|
| M-1 | Khởi tạo SDK, `isInitialized()` | ☐ | ☐ | |
| M-2 | Mở "Ưu đãi của tôi", cuộn tải thêm, kéo-để-tải-lại | ☐ | ☐ | |
| M-3 | Chuyển tab, giữ đúng tab sau khi tải lại | ☐ | ☐ | |
| M-4 | Tìm kiếm theo từ khoá, trạng thái rỗng | ☐ | ☐ | |
| M-5 | Mở chi tiết ưu đãi, hiển thị đủ trường | ☐ | ☐ | |
| M-6 | "Áp dụng" → host nhận đủ object voucher | ☐ | ☐ | |
| M-7 | "Dùng ngay" → bottom sheet chọn dịch vụ | ☐ | ☐ | |
| M-8 | Widget checkout hiển thị đúng ưu đãi của đơn | ☐ | ☐ | |
| M-9 | Bấm widget → màn "Chọn ưu đãi", **không** gọi lại API tìm ưu đãi | ☐ | ☐ | |
| M-10 | Đối soát hỏng → popup lỗi, **không** đóng màn | ☐ | ☐ | |
| M-11 | `confirmRedemption` → trả `sessionId` cho host | ☐ | ☐ | |
| M-12 | Ảnh động (GIF) chạy được ở mọi ô ảnh | ☐ | ☐ | |
| M-13 | Theme của host áp đúng | ☐ | ☐ | |
| M-14 | Mất mạng giữa chừng → báo lỗi đúng, không treo | ☐ | ☐ | |
| M-15 | Token hết hạn → tự hồi phục; refresh hỏng → `onExpireToken()` **một lần** | ☐ | ☐ | |
| M-16 | Đăng xuất → `release()` → API sau không dùng token cũ | ☐ | ☐ | |
| M-17 | Tắt `PROMOTION.ENABLE_ALL` → mọi điểm vào bị chặn + popup `PRM_MOB_021` | ☐ | ☐ | |
| M-18 | Tắt từng cờ con → đúng màn tương ứng bị chặn | ☐ | ☐ | |
| M-19 | Bật R8/minify ở host Android → không `NoClassDefFoundError`/`AbstractMethodError` | ☐ | — | |
| M-20 | Bản release **không** in log body/cURL | ☐ | ☐ | |

## 7. Lỗi còn tồn — điền cho bản phát hành

| # | Mô tả | Mức | Nền tảng | Xử lý |
|---|---|---|---|---|
| | | | | |

**Kết luận nghiệm thu:** ☐ Đạt ☐ Đạt có điều kiện ☐ Không đạt

**Người kiểm thử:** ____________ **Ngày:** ________
**Người phê duyệt:** ____________ **Ngày:** ________
