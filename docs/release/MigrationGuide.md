# MigrationGuide — Hướng dẫn nâng cấp giữa các phiên bản

Dành cho đội tích hợp phía host. Mỗi mục: **triệu chứng → sửa gì → kiểm lại thế nào.**

> Đọc kèm [ReleaseNotes](./ReleaseNotes.md) để biết bản mình đang nâng lên có gì.

---

## 1. → 2.0.0

Đây là bản nâng cấp **breaking diện rộng**. Ước lượng: nửa ngày cho một app host đã tích hợp đầy đủ.

### 1.1. Bước 1 — Đổi toạ độ dependency

```kotlin
// Android — TRƯỚC
implementation("com.ttcn.promotion:promotionSDK:1.0.0")
// SAU
implementation("vn.viettelpay.library:promotion:2.0.0")
```

Kèm khai repo và giới hạn theo group (repo nội bộ không nên tranh resolve androidx/kotlin với
`google()`/`mavenCentral()`):

```kotlin
maven {
    url = uri("https://mobile-data.viettelmoney.vn/artifactory/gradle-viettelmoney")
    credentials { /* đặt ở ~/.gradle/gradle.properties, KHÔNG commit */ }
    content { includeGroup("vn.viettelpay.library") }
}
```

iOS: sửa `url:` **và** `checksum:` của `binaryTarget` sang version mới (lấy `checksum` từ
`metadata.json` cạnh zip trên Artifactory), giữ **Embed & Sign**, rồi `⇧⌘K` (Clean Build Folder) —
cache Xcode có thể nói dối sau khi đổi chữ ký public. Đổi `url` mà quên `checksum` thì SPM báo
*"checksum does not match"*; đổi `checksum` mà quên `url` thì nó vẫn tải bản cũ.

**Kiểm lại:** `PromotionSDK.isInitialized()` biên dịch và chạy được (trả `false`).

### 1.2. Bước 2 — Chuyển sang `PromotionTokenSource`

Triệu chứng nếu bỏ qua: không biên dịch được (`initialize` không còn overload nhận `String`).

```kotlin
object AppPromotionTokenSource : PromotionTokenSource {
    override fun currentToken(): String? = tokenStore.accessToken      // THREAD NỀN, phải thread-safe
    override fun refreshToken(onResult: (Boolean) -> Unit) {
        authRepository.refresh { ok -> onResult(ok) }                  // ghi token mới vào tokenStore RỒI báo true
    }
}
```

```swift
final class AppPromotionTokenSource: PromotionTokenSource {          // KHÔNG @MainActor
    func currentToken() -> String? { tokenStore.accessToken }
    func refreshToken(onResult: @escaping (Bool) -> Void) {
        authRepository.refresh { ok in onResult(ok) }
    }
}
```

Xoá mọi lời gọi `PromotionSDK.updateToken(...)` — không còn cần và không còn tồn tại.

**Kiểm lại:** để token hết hạn giữa phiên → API của SDK tự hồi phục sau một lần refresh; nếu refresh
thất bại thì `onExpireToken()` bắn đúng một lần.

### 1.3. Bước 3 — `updateContext` → `updateOrderInfo`

```kotlin
// TRƯỚC
PromotionSDK.updateContext(orderId = id, serviceCode = "SVC", orderItems = listOf(PromotionOrderItem(skuId = sku, ...)))
// SAU — field phẳng, orderId/productId BẮT BUỘC, skuId → skuSourceId, KHÔNG còn serviceCode
PromotionSDK.updateOrderInfo(
    orderId = id,
    productId = productId,          // phải khớp applicableProducts.productId của voucher
    orderValue = amount,
    skuSourceId = sku,              // để trống ⇒ SDK không gửi field này lên server
    quantity = 1,
    unitPrice = price,
)
```

**Kiểm lại:** mở màn "Chọn ưu đãi" từ màn thanh toán → danh sách ưu đãi khớp đúng đơn hàng.

### 1.4. Bước 4 — Gỡ mọi import ngoài `Entry`

Từ bản này **chỉ `Entry` mới public**. Nếu host đang import widget, base class, theme nội bộ hay
Fragment của SDK, code sẽ không biên dịch.

Cần dùng thứ gì đó mà `Entry` chưa có → **báo đội SDK dời nó vào `entry`**, đừng tìm đường lách
(Java/reflection vẫn gọi được nhưng đó là hợp đồng đã gãy, bản sau sẽ vỡ tiếp).

Bề mặt hợp lệ: `com.ttcn.prm.entry.**` + `com.ttcn.prm.ui.theme.**` + `PRMEndowView`.

### 1.5. Bước 5 — Bỏ wiring thủ công cho widget

```kotlin
// TRƯỚC: host tự bắt sự kiện rồi mở màn chọn ưu đãi
endowView.onOpenVoucherSelection = { openVoucherSelection() }

// SAU: xoá dòng trên. Bấm widget → SDK tự mở màn "Chọn ưu đãi".
```

Muốn mở màn đó từ nút riêng của host: `PromotionSDK.openChoosePromotion(activity, endowView)`.

**Kiểm lại:** bấm widget mở đúng màn; chọn ưu đãi → áp dụng → widget cập nhật; bấm thanh toán →
`confirmRedemption` trả `sessionId`.

### 1.6. Bước 6 — Cập nhật `PromotionSDKCallback`

Callback còn **3 sự kiện**: `onVoucherApplied(voucherId)`, `onServiceSelected(...)`, `onExpireToken()`.
Xoá `onVoucherCleared` / `onVoucherCountChanged` / `onAvailabilityChanged` / `onClosed`.

Cần biết chi tiết giảm giá: Android nhận qua `PRMEndowView` (`AppliedDiscount`); iOS gọi
`api.validateDiscounts(...)`.

### 1.7. Bước 7 — Bỏ `customerId`

Xoá tham số này ở mọi lời gọi. BFF lấy định danh khách từ JWT `sub`.

### 1.8. Bước 8 — Kiểm tra lại phần hiển thị lỗi

SDK **không còn toast**. Nếu host từng dựa vào toast của SDK để báo lỗi cho user thì giờ một số lỗi
sẽ **im lặng theo chủ đích** (màn đã có empty-view/shimmer nói thay). Rà lại các màn có nhúng SDK.

### 1.9. Danh mục kiểm sau nâng cấp

- [ ] Build sạch cả Android lẫn iOS (iOS: đã Clean Build Folder).
- [ ] Bật R8/minify ở app host Android — mở đủ 4 màn + widget, không `NoClassDefFoundError`.
- [ ] Đăng nhập → mở "Ưu đãi của tôi" → tìm kiếm → chi tiết → áp dụng.
- [ ] Màn thanh toán: widget → chọn ưu đãi → áp dụng → xác nhận sử dụng.
- [ ] Token hết hạn giữa phiên → tự hồi phục hoặc `onExpireToken()`.
- [ ] Đăng xuất → gọi `PromotionSDK.release()`.
- [ ] Tắt `PROMOTION.ENABLE_ALL` phía server → mọi điểm vào bị chặn đúng cách.

---

## 2. Nguyên tắc chung khi nâng bất kỳ bản nào

1. **Đọc Release Notes trước, không nâng mù.** Số MAJOR đổi = chắc chắn phải sửa code.
2. **Nâng ở một branch riêng**, chạy hết luồng ưu đãi trên bản debug rồi mới merge.
3. **Luôn thử với bản minify/release**, không chỉ debug — lỗi giữ class chỉ lộ ở bản minify.
4. **iOS: Clean Build Folder** sau khi đổi framework.
5. Gặp lỗi lạ → xem [Troubleshooting](../Troubleshooting.md) trước khi báo đội SDK; nếu báo thì gửi
   kèm `X-Request-ID` và version SDK.
