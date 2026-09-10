# Release Notes — TTCN Promotion SDK

Tài liệu dành cho **đội tích hợp phía host**: mỗi bản có gì mới, cái gì gãy, và phải sửa gì để nâng
cấp. Bản đầy đủ cho người phát triển SDK nằm ở [`../../CHANGELOG.md`](../../CHANGELOG.md).

Toạ độ phát hành:

| Nền tảng | Toạ độ |
|---|---|
| Android | `vn.viettelpay.library:promotion:<version>` (Maven, Artifactory nội bộ) |
| iOS | `Promotion-<version>.xcframework.zip` trên Artifactory `vdo-ios-frameworks/Martech/Promotion/<version>/` — dùng qua SPM `binaryTarget`, `import PromotionKit` |

---

## 1. Bản kế tiếp — **2.0.0** (đang chuẩn bị, chưa phát hành)

> ⚠️ **Cảnh báo đánh số.** `gradle.properties` hiện vẫn ghi `SDK_VERSION=1.0.0`, trong khi mục
> `[Unreleased]` của CHANGELOG đã tích luỹ **nhiều thay đổi breaking** so với 1.0.0. Theo cam kết
> SemVer ghi ngay trong CHANGELOG ("từ 1.0.0 bề mặt public tuân theo SemVer"), bản kế tiếp **phải là
> 2.0.0**. Chốt số trước khi phát hành — xem [VersioningPolicy](./VersioningPolicy.md).

### 1.1. Breaking — bắt buộc sửa code host

| # | Thay đổi | Trước | Sau |
|---|---|---|---|
| B-1 | **Token vào SDK qua `PromotionTokenSource`** | `initialize(..., accessToken, ...)` + `PromotionSDK.updateToken(t)` | `initialize(..., tokenSource, ...)`; SDK tự đọc lại token mỗi request |
| B-2 | Bỏ `PromotionSDK.updateToken` / iOS `updateSession` / `applySession` | có | không còn — không cần nữa |
| B-3 | `updateContext` đổi tên thành **`updateOrderInfo`**, `orderItems` truyền phẳng | `updateContext(...)` | `updateOrderInfo(orderId, productId, ...)` |
| B-4 | `orderId` / `productId` trong `updateOrderInfo` **bắt buộc** | tuỳ chọn | không default, không nullable |
| B-5 | `skuId` đổi thành **`skuSourceId`**; để trống thì **không gửi field** lên server | `skuId` | `skuSourceId` |
| B-6 | Bỏ tham số `serviceCode` khỏi `updateOrderInfo` | có | không |
| B-7 | Đổi tên field của `PromotionAvailableService` / `PromotionServiceSelection` | — | xem CHANGELOG mục tương ứng |
| B-8 | **Chỉ `Entry` mới public** — mọi thứ khác `internal` (cả 2 nền tảng) | host chạm được widget/base/theme nội bộ | chỉ `com.ttcn.prm.entry.**` + theme + `offerwidget` |
| B-9 | Widget "Ưu đãi" **tự** mở màn "Chọn ưu đãi"; bỏ `onOpenVoucherSelection` | host tự wiring | không cần wiring |
| B-10 | Bỏ `PromotionIntegrateManager` | có | luồng thanh toán về lõi dùng chung |
| B-11 | Bỏ hoàn toàn `customerId` | `initialize(context, customerId, ...)` | BFF lấy định danh từ JWT `sub` |
| B-12 | `PromotionSDKCallback` còn **3 sự kiện** | 6–7 sự kiện | `onVoucherApplied` / `onServiceSelected` / `onExpireToken` |
| B-13 | Toạ độ Maven đổi sang `vn.viettelpay.library:promotion` | `com.ttcn.promotion:promotionSDK` | khai lại dependency |

**Cách nâng cấp B-1 (thay đổi lớn nhất):**

```kotlin
// Trước
PromotionSDK.initialize(ctx, auth.accessToken, BASE_URL, callback = cb)
// … và mỗi lần app refresh token:
PromotionSDK.updateToken(newToken)

// Sau — khai nguồn một lần, không phải đẩy gì nữa
object MyTokenSource : PromotionTokenSource {
    override fun currentToken(): String? = auth.accessToken          // gọi từ THREAD NỀN
    override fun refreshToken(onResult: (Boolean) -> Unit) {
        auth.refresh { ok -> onResult(ok) }                          // ghi token mới vào kho của host RỒI báo true
    }
}
PromotionSDK.initialize(ctx, MyTokenSource, BASE_URL, callback = cb)
```

Ba điều host phải biết về `PromotionTokenSource`:

1. `currentToken()` được gọi **từ thread nền** ⇒ phải thread-safe, **không** `@MainActor` ở Swift.
2. `refreshToken` trả `Boolean`, **không** trả token — host ghi token mới vào kho của mình *rồi* báo
   `true`. Token vào SDK chỉ qua đúng một đường là `currentToken()`.
3. Host không gọi callback trong **15 giây** thì SDK coi như thất bại (nếu không, mọi API của SDK sẽ
   treo). Gọi callback hai lần chỉ tính lần đầu.

### 1.2. Sửa lỗi đáng chú ý

- **Tầng data không còn chạy trên main thread (Android)** — mọi lời gọi mạng ép xuống IO dispatcher.
- **`onExpireToken()` không còn bắn nhầm khi lỗi 403** — chỉ 401 mới coi là hết hạn phiên.
- **SDK không còn ghi log chẩn đoán ra logcat của host ở bản release.**
- Màn "Ưu đãi của tôi": sửa lỗi tab nhảy ngược & danh sách bị xoá trắng (cả hai nền tảng).
- iOS: sửa skeleton hàng tab-chip bị skeleton danh sách đè trên màn hình nhỏ.

### 1.3. Thay đổi hành vi (không gãy chữ ký)

- **Bỏ hẳn toast** ở cả hai nền tảng. Lỗi giờ hoặc là **popup**, hoặc **không hiện gì** — chọn có chủ
  ý theo ngữ cảnh màn hình.
- Feature flag phơi 4 hàm cho host hỏi trước (`featureFlags`, `isFeatureEnabled`, `isSdkEnabled`,
  `refreshFeatureFlags`) — fail-open, không hàm nào ném lỗi.
- Android: mọi điều hướng nội bộ đi chung `PRMBaseFragment.addFragment()`.

---

## 2. — 2026-07-20

Bản phát hành ổn định đầu tiên. Từ đây bề mặt public tuân theo SemVer.

**Kiến trúc.** Kotlin Multiplatform: lõi Data + Domain dùng chung (`:promotionLogic`); UI native mỗi
nền tảng (Android XML/MVI, iOS UIKit/MVVM). Bề mặt SDK đối xứng 1:1 Android ↔ iOS.

**Tính năng.**

- Màn "Ưu đãi của tôi" + tìm kiếm.
- Màn chi tiết ưu đãi, có "Áp dụng" trả voucher về host.
- Màn "Chọn ưu đãi" cho đơn hàng + widget checkout (`PRMOfferWidget` / `createOfferWidget`).
- Headless API 5 hàm: `getVouchers`, `findEligible`, `getVoucherDetail`, `validateDiscounts`,
  `createRedemption`.
- Theming theo brand host qua `PromotionSDKTheme`.
- Feature flag / kill-switch — ở bản này **chưa** phơi ra host; mọi điểm vào tự gác.

**Phân phối.** Android: Maven. iOS: `PRM.xcframework`.

---

## 3. Quy ước đọc tài liệu này

| Ký hiệu | Nghĩa |
|---|---|
| **Breaking** | Host **phải** sửa code mới build/chạy được |
| **Thay đổi hành vi** | Chữ ký giữ nguyên nhưng kết quả khác — cần thử lại luồng liên quan |
| **Sửa lỗi** | Không cần làm gì, nhưng nên đọc để biết triệu chứng cũ đã hết |
