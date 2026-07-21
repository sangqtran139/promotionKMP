# iOS Integration Guide — Promotion SDK cho app host

> Hướng dẫn **tích hợp** dành cho đội app host (bên tiêu thụ SDK). Không phải tài liệu phát triển nội
> bộ SDK — cái đó xem [`IosUIGuide.md`](./IosUIGuide.md). Bề mặt API song ánh Android↔iOS: [`PublicApi.md`](./PublicApi.md).
> Mẫu wrapper khuyến nghị ở host: [`InitParity.md`](./InitParity.md) §6 + `iosApp/iosApp/PromotionManager.swift`.

---

## 0. TL;DR

- Kéo **một** file `PromotionSDKUI.xcframework` vào project, đặt **Embed & Sign**. Xong. Không CocoaPods,
  không SPM, không cài Kotlin/RxSwift.
- Mọi thứ host chạm đều bắt đầu bằng `Promotion*` (`PromotionSDK`, `PromotionSDKApi`, `PromotionSDKTheme`…).
- `import PromotionSDKUI` là import **duy nhất** host cần.
- Cấu hình một lần bằng `PromotionSDK.initialize(options:)`, bơm đơn hàng bằng `updateContext(...)`, nhận
  sự kiện qua `PromotionSDKCallback`.

---

## 1. SDK đóng gói thế nào (vì sao host "sạch")

SDK ship dạng **một dynamic framework** đóng trong `PromotionSDKUI.xcframework` (binary là Mach-O
`DYLIB`). Kotlin (`PromotionLogic`), RxSwift, và các module UI nội bộ (`PRMPromotionUI`, `PRMDesignKit`,
`PRMKotlinBridge`, `PRMFoundation`) đều được **link tĩnh vào bên trong** framework động này và **giấu** sau
`@_implementationOnly`. Bằng chứng: file `.swiftinterface` công khai của framework **chỉ** import
`Foundation / UIKit / SwiftUI / Swift` — không một dòng nào lộ Kotlin hay RxSwift.

Hệ quả cho host:

- ✅ Host **không** cần thêm bất kỳ dependency nào ngoài xcframework này.
- ✅ Host **không** thấy — và không build nhầm phải — type Kotlin/RxSwift.
- ✅ Public API chỉ dùng `String / Int / Bool / UIColor / UIView / UIViewController / Result` → không lo
  cross-module deserialization.

> ⚠️ Chính vì đóng gói kín, **đừng** cố `import PRMKotlinBridge` / `import PromotionLogic` ở host — chúng
> là module nội bộ, không có trong interface công khai; build sẽ fail `Unable to find module dependency`.

---

## 2. Yêu cầu & phân phối

| Mục | Giá trị |
|---|---|
| Artifact | `PromotionSDKUI.xcframework` (một file duy nhất) |
| Module import | `import PromotionSDKUI` |
| iOS tối thiểu | **iOS 13.0** |
| Slice | `ios-arm64` (thiết bị) + `ios-arm64_x86_64-simulator` (simulator) |
| UI | UIKit — API trả `UIViewController` / `UIView` |
| Linking | **Dynamic framework** → bắt buộc **Embed & Sign** (dylib phải copy vào app bundle; deps Kotlin/RxSwift link tĩnh sẵn bên trong) |

---

## 3. Thêm vào project

### 3.1. Kéo tay (Xcode)

1. Kéo `PromotionSDKUI.xcframework` vào project navigator.
2. Chọn target host → tab **General** → **Frameworks, Libraries, and Embedded Content**.
3. Đặt `PromotionSDKUI.xcframework` = **Embed & Sign**.
   > Bắt buộc "Embed" vì đây là **dynamic framework**: dylib phải được copy vào `.app/Frameworks` thì mới
   > load được lúc runtime. Ngoài ra framework còn kèm resource bundle (`PRMDesignKit`, `PRMPromotionUI`,
   > `PRMFoundation`) và các `.nib`. Để "Do Not Embed" → crash `dyld: Library not loaded` khi mở app.
4. Build. Không cần cấu hình `OTHER_LDFLAGS` hay search path thủ công.

### 3.2. Kiểm tra nhanh

```swift
import PromotionSDKUI

print(PromotionSDK.isInitialized()) // false — link OK là được
```

---

## 4. Vòng đời SDK

`PromotionSDK` là **singleton tĩnh** — mọi điểm vào là `static`. SDK giữ **một** phiên sống tại một thời điểm.

```swift
import PromotionSDKUI

// Sau khi login thành công:
PromotionSDK.initialize(
    options: PromotionSDKOptions(
        session: PromotionSessionConfig(
            customerId: user.id,
            accessToken: auth.accessToken,
            baseUrl: "http://125.235.38.229:8080/",
            language: "vi-VN",          // mặc định "vi-VN"
            environment: .prod          // .prod | .staging, mặc định .prod
        ),
        availableServices: [            // cho bottom sheet "Chọn dịch vụ" (có thể để rỗng)
            PromotionAvailableService(serviceCode: "TOPUP", serviceName: "Nạp tiền", iconUrl: iconUrl)
        ],
        theme: nil,                     // nil = SDK tự khôi phục theme đã lưu (xem §9)
        callback: myCallback            // đối tượng conform PromotionSDKCallback (xem §7)
    )
)
```

| Việc | API |
|---|---|
| Khởi tạo | `PromotionSDK.initialize(options:)` |
| Kiểm tra đã init | `PromotionSDK.isInitialized() -> Bool` |
| Giải phóng (logout) | `PromotionSDK.release()` |
| Lấy callback đã set | `PromotionSDK.getCallback() -> PromotionSDKCallback?` |

**Refresh access token:** token bị "chụp" lúc init. Muốn đổi token = **gọi lại** `initialize(options:)` với
`session` mới (SDK tự dựng lại đồ thị nội bộ). `release()` khi chưa init là vô hại; **không** xoá theme đã lưu.

---

## 5. Bơm context đơn hàng

Giữ **một** phiên từ lúc login, tới màn có voucher mới bơm đơn hàng — **không** re-init:

```swift
PromotionSDK.updateContext(
    orderId: order.id,
    orderValue: "500000",   // chuỗi số nguyên VNĐ
    serviceCode: "TOPUP",
    metaData: nil
)
```

SDK đọc lại các giá trị này ở **mỗi** request, nên chỉ cần gọi trước khi mở màn / gọi API. Đọc ngược lại
qua `PromotionSDK.currentOrderId / currentOrderValue / currentServiceCode / currentMetaData` và `PromotionSDK.session`.

> ⚠️ `updateContext` / `api` gọi trước `initialize` sẽ **preconditionFailure** (crash) — luôn init trước.

---

## 6. Màn hình UI có sẵn

SDK tự lo navigation: nếu `viewController` có `navigationController` → **push**, ngược lại → **present modal fullScreen**.

```swift
// "Ưu đãi của tôi"
PromotionSDK.openMyPromotion(from: self)

// Chi tiết một ưu đãi (đã biết voucherId, vd từ push notification / deeplink)
PromotionSDK.openPromotionDetail(voucherId: "V123", from: self)

// Widget checkout — gắn vào layout của bạn, tự load dữ liệu
let widget = PromotionSDK.createEndowView(from: self, orderId: order.id, orderValue: "500000")
container.addSubview(widget)

// Widget checkout kèm dòng sản phẩm (lấy campaign theo SKU)
let widget2 = PromotionSDK.createEndowView(
    from: self, orderId: order.id, orderValue: "500000",
    orderItems: [PromotionOrderItem(skuId: "SKU1", quantity: 1, unitPrice: "500000")]
)
```

**Feature flag tự gác:** nếu cờ tương ứng TẮT, `openMyPromotion` / `openPromotionDetail` tự hiện popup lỗi
`PRM_MOB_021` trên `viewController` rồi báo host qua `onAvailabilityChanged(enabled: false)`. Host **không**
cần hỏi cờ — chỉ cần lắng nghe callback để ẩn điểm vào (xem §8).

---

## 7. Nhận sự kiện — `PromotionSDKCallback`

Tất cả method đều có default (protocol extension) → chỉ implement cái cần.

```swift
final class MyPromotionCallback: PromotionSDKCallback {
    func onVoucherApplied(voucherId: String) { /* user áp voucher thành công */ }
    func onVoucherCleared() { /* user bỏ chọn voucher */ }
    func onVoucherCountChanged(count: Int) { /* widget load xong, biết số voucher khả dụng */ }
    func onServiceSelected(selection: PromotionServiceSelection) { /* điều hướng tới dịch vụ đã chọn */ }
    func onAvailabilityChanged(enabled: Bool) { /* enabled == false → ẩn điểm vào ưu đãi */ }
    func onClosed() { /* màn SDK đóng (user back) */ }
}
```

| Sự kiện | Khi nào bắn |
|---|---|
| `onVoucherApplied(voucherId:)` | User bấm "Áp dụng" thành công |
| `onVoucherCleared()` | User bỏ chọn voucher trên widget |
| `onVoucherCountChanged(count:)` | Widget load xong, biết tổng voucher khả dụng |
| `onServiceSelected(selection:)` | User chọn dịch vụ trong bottom sheet |
| `onAvailabilityChanged(enabled:)` | Feature flag báo bật/tắt SDK |
| `onClosed()` | Màn SDK bị đóng |

> Callback của SDK là kênh **1-1**. Muốn nhiều nơi trong app cùng nghe → dùng wrapper fan-out
> (`PromotionManager` mẫu ở `iosApp/`, xem [`InitParity.md`](./InitParity.md) §6).

---

## 8. Headless API (tự dựng UI) — `PromotionSDK.api`

Không dùng UI có sẵn thì gọi trực tiếp `PromotionSDK.api` (kiểu `PromotionSDKApi`). Closure-based, chạy về
main thread. **Phải `initialize` trước.**

```swift
PromotionSDK.api.getVouchers(keyword: "grab", page: 0) { result in
    switch result {
    case .success(let page):  // PromotionVoucherPage(vouchers: [PromotionVoucher], isLastPage)
        render(page.vouchers)
    case .failure(let error): // PromotionSDKError
        show(error.errorDescription)
    }
}
```

Năm hàm (song ánh Android):

| Hàm | Dùng cho |
|---|---|
| `getVouchers(keyword:serviceCode:tab:page:size:completion:)` | Voucher **của khách** (đã sở hữu) |
| `findEligible(orderId:orderValue:items:...:completion:)` | Ưu đãi đủ điều kiện cho đơn — 2 nhóm "của tôi"/"khác", phân trang độc lập |
| `getVoucherDetail(voucherId:serviceCode:completion:)` | Chi tiết một voucher |
| `validateDiscounts(orderId:orderValue:voucherIds:objectType:completion:)` | Validate voucher với đơn trước khi áp |
| `createRedemption(orderId:orderValue:voucherIds:objectType:completion:)` | Tạo redemption session để thanh toán |

---

## 9. Xử lý lỗi — `PromotionSDKError`

API **không ném lỗi nghiệp vụ**; mọi thất bại về `.failure(PromotionSDKError)`:

```swift
switch error {
case .networkFailure(let code, let message): // code = HTTP status nếu có; token hết hạn về đây với code == 401
case .timeout:
case .parseFailed:                            // server trả data:null nơi bắt buộc có
case .featureDisabled:                        // tính năng TẮT qua feature flag (PRM_MOB_021)
case .unknown(let underlying):
case .sessionExpired:                         // hiện SDK KHÔNG tự phát case này (xem ghi chú)
}
```

Mỗi case có sẵn `errorDescription` tiếng Việt để hiển thị. `.networkFailure` còn có `.serverCode`.

> ⚠️ **Token hết hạn:** SDK hiện **không** map ra `.sessionExpired` — lỗi 401 về dưới dạng
> `.networkFailure(code: 401, …)`. Muốn bắt phiên hết hạn, host kiểm `error.serverCode == 401` rồi refresh
> token và `initialize` lại. (`.sessionExpired` là case dành sẵn cho tương lai, đối xứng 2 nền tảng.)

---

## 10. Theming

```swift
PromotionSDK.configure(theme: PromotionSDKTheme(
    buttonToken: ButtonToken(/* ... */),
    // 6 token: buttonToken, searchBarToken, listItemToken, tabChipToken, tabUnderlineToken, discountBadgeToken
))
```

- `configure(theme:)` áp **và lưu lại** → sống qua các lần mở app. Truyền `nil` = xoá, về mặc định SDK.
- Truyền `theme` trong `PromotionSDKOptions` lúc init cũng được; để `nil` = SDK tự khôi phục theme đã lưu.
- Nhóm token để `nil` = giữ mặc định SDK cho nhóm đó.
- Nên cấu hình **một lần** lúc khởi tạo — view đã render chỉ đổi khi được dựng lại.
- Đọc theme đang áp: `PromotionSDK.currentTheme() -> PromotionSDKTheme?`.

---

## 11. Sai lầm thường gặp

| ❌ Sai | ✅ Đúng |
|---|---|
| Gọi `api` / `updateContext` trước `initialize` | Luôn `initialize` sau login trước tiên |
| Đổi token bằng cách sửa field | Gọi lại `initialize(options:)` với session mới |
| Đặt xcframework "Do Not Embed" | **Embed & Sign** (framework có resource bundle) |
| `import PromotionLogic` / `PRMKotlinBridge` | Chỉ `import PromotionSDKUI` |
| Tự hỏi feature flag để ẩn UI | Lắng nghe `onAvailabilityChanged(enabled:)` |
| Rải `PromotionSDK.*` khắp app | Gom qua 1 wrapper (`PromotionManager`, [`InitParity.md`](./InitParity.md) §6) |

---

## 12. Vòng đời gợi ý (khớp host thật)

```
login thành công        → PromotionSDK.initialize(options:)
vào màn có voucher       → PromotionSDK.updateContext(orderId:orderValue:...)
mở UI                    → openMyPromotion / openPromotionDetail / createEndowView
refresh access token     → PromotionSDK.initialize(options:) lại (session mới)
logout                   → PromotionSDK.release()
```
