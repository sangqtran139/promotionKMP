# iOS Integration Guide — Promotion SDK cho app host

> Hướng dẫn **tích hợp** dành cho đội app host (bên tiêu thụ SDK). Không phải tài liệu phát triển nội
> bộ SDK — cái đó xem [`IosUIGuide.md`](./IosUIGuide.md). Bề mặt API song ánh Android↔iOS: [`PublicApi.md`](./PublicApi.md).
> Tích hợp trực tiếp — gọi thẳng `PromotionSDK`, không cần wrapper (xem [`InitParity.md`](./InitParity.md) §6).

---

## 0. TL;DR

- Kéo **một** file `PRM.xcframework` vào project, đặt **Embed & Sign**. Xong. Không CocoaPods,
  không SPM, không cài Kotlin/RxSwift.
- Mọi thứ host chạm đều bắt đầu bằng `Promotion*` (`PromotionSDK`, `PromotionSDKApi`, `PromotionSDKTheme`…).
- `import PRM` là import **duy nhất** host cần.
- Cấu hình một lần bằng `PromotionSDK.initialize(customerId:accessToken:baseUrl:)`, bơm đơn hàng bằng `updateContext(...)`, nhận
  sự kiện qua `PromotionSDKCallback`.

---

## 1. SDK đóng gói thế nào (vì sao host "sạch")

SDK ship dạng **một dynamic framework** đóng trong `PRM.xcframework` (binary là Mach-O
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
| Artifact | `PRM.xcframework` (một file duy nhất) |
| Module import | `import PRM` |
| iOS tối thiểu | **iOS 13.0** |
| Slice | `ios-arm64` (thiết bị) + `ios-arm64_x86_64-simulator` (simulator) |
| UI | UIKit — API trả `UIViewController` / `UIView` |
| Linking | **Dynamic framework** → bắt buộc **Embed & Sign** (dylib phải copy vào app bundle; deps Kotlin/RxSwift link tĩnh sẵn bên trong) |

---

## 3. Thêm vào project

### 3.1. Kéo tay (Xcode)

1. Kéo `PRM.xcframework` vào project navigator.
2. Chọn target host → tab **General** → **Frameworks, Libraries, and Embedded Content**.
3. Đặt `PRM.xcframework` = **Embed & Sign**.
   > Bắt buộc "Embed" vì đây là **dynamic framework**: dylib phải được copy vào `.app/Frameworks` thì mới
   > load được lúc runtime. Ngoài ra framework còn kèm resource bundle (`PRMDesignKit`, `PRMPromotionUI`,
   > `PRMFoundation`) và các `.nib`. Để "Do Not Embed" → crash `dyld: Library not loaded` khi mở app.
4. Build. Không cần cấu hình `OTHER_LDFLAGS` hay search path thủ công.

### 3.2. Kiểm tra nhanh

```swift
import PRM

print(PromotionSDK.isInitialized()) // false — link OK là được
```

---

## 4. Vòng đời SDK

`PromotionSDK` là **singleton tĩnh** — mọi điểm vào là `static`. SDK giữ **một** phiên sống tại một thời điểm.

```swift
import PRM

// Cách tối giản — đủ cho phần lớn host, chỉ 3 tham số bắt buộc:
PromotionSDK.initialize(
    customerId: user.id,
    accessToken: auth.accessToken,
    baseUrl: "http://125.235.38.229:8080/",
    // tuỳ chọn:
    environment: .prod,                             // mặc định .prod
    availableServices: [                            // cho bottom sheet "Chọn dịch vụ"
        PromotionAvailableService(serviceCode: "TOPUP", serviceName: "Nạp tiền", iconUrl: iconUrl)
    ],
    callback: myCallback                            // conform PromotionSDKCallback (xem §7)
)
```

Cần cấu hình sâu hơn (theme, …) thì dùng overload nhận `PromotionSDKOptions`:

```swift
PromotionSDK.initialize(options: PromotionSDKOptions(
    session: PromotionSessionConfig(customerId: user.id, accessToken: auth.accessToken, baseUrl: baseUrl, environment: .prod),
    availableServices: services, theme: myTheme, callback: myCallback
))
```

| Việc | API |
|---|---|
| Khởi tạo (tối giản) | `PromotionSDK.initialize(customerId:accessToken:baseUrl:)` |
| Khởi tạo (đầy đủ) | `PromotionSDK.initialize(options:)` |
| **Login lại** (session mới) | `PromotionSDK.updateSession(customerId:accessToken:availableServices:)` |
| Refresh token giữa phiên | `PromotionSDK.updateToken(newToken)` (tuỳ chọn) |
| Kiểm tra đã init | `PromotionSDK.isInitialized() -> Bool` |
| Giải phóng (logout) | `PromotionSDK.release()` |
| Lấy callback đã set | `PromotionSDK.getCallback() -> PromotionSDKCallback?` |

**Host gọi `initialize` MỘT LẦN, mỗi login sau chỉ gọi `updateSession`.** SDK tách hai loại field:

| Cố định (khoá ở lần init **đầu**) | Đặc trưng session (đổi mỗi login) |
|---|---|
| `baseUrl`, `environment`, `language`, `theme` | `customerId`, `accessToken`, `availableServices` |

- **Login lần đầu (mở app):** `initialize(...)` với đầy đủ config → SDK **chốt** field cố định.
- **Login lại (user khác / phiên mới):** `PromotionSDK.updateSession(customerId:accessToken:availableServices:)`
  — chỉ field động; SDK **giữ** field cố định đã khoá + callback. `availableServices` bỏ qua (`nil`) = giữ
  danh mục hiện tại. Context đơn hàng reset về rỗng vì là phiên mới.

  ```swift
  if PromotionSDK.isInitialized() {
      PromotionSDK.updateSession(customerId: user.id, accessToken: token, availableServices: services)
  } else {
      PromotionSDK.initialize(customerId: user.id, accessToken: token, baseUrl: baseUrl, availableServices: services, callback: cb)
  }
  ```

- **Gọi lại `initialize(...)` cũng an toàn** (guard): SDK khoá field cố định, chỉ áp field động; host lỡ
  truyền field cố định khác đi thì **bỏ qua** kèm cảnh báo log.
- **Đổi field cố định thật** (vd chuyển environment): `release()` rồi `initialize(...)` lại.
- **Refresh token giữa phiên (cùng customer, đang checkout):** `PromotionSDK.updateToken(newToken)` —
  tuỳ chọn, nhẹ hơn; **giữ nguyên** cả context đơn hàng đang ghi.

> `release()` khi chưa init là vô hại; không xoá theme đã lưu.

---

## 5. Bơm context đơn hàng

Giữ **một** phiên từ lúc login, tới màn có voucher mới bơm đơn hàng — **không** re-init:

```swift
PromotionSDK.updateContext(
    orderId: order.id,
    orderValue: "500000",   // chuỗi số nguyên VNĐ
    serviceCode: "TOPUP",
    metaData: nil,
    // Có dòng sản phẩm → lấy được campaign theo SKU; bỏ trống thì chỉ campaign cấp đơn.
    orderItems: [PromotionOrderItem(skuId: "SKU1", quantity: 1, unitPrice: "500000")]
)
```

SDK đọc lại các giá trị này ở **mỗi** request, nên chỉ cần gọi trước khi mở màn / gọi API. Đọc ngược lại
qua `PromotionSDK.currentOrderId / currentOrderValue / currentServiceCode / currentMetaData` và `PromotionSDK.session`.

> ⚠️ Gọi trước `initialize`: `updateContext` / `openMyPromotion` / `openPromotionDetail` /
> `createEndowView` **không crash** — chúng bỏ qua lệnh và ghi một dòng cảnh báo qua `NSLog`
> (`[PromotionSDK] … bị gọi trước initialize()`). Riêng **`api`** vẫn `preconditionFailure` vì kiểu trả
> về không optional. SDK khởi tạo thường là **bất đồng bộ** (chờ login), nên hãy gác điểm vào bằng
> `PromotionSDK.isInitialized()` — nếu không, nút bấm sẽ như "không ăn" và widget sẽ trống.

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

**Feature flag tự gác:** nếu cờ tương ứng TẮT, `openMyPromotion` / `openPromotionDetail` tự hiện toast lỗi
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

> Callback của SDK là kênh **1-1** (một object nhận sự kiện, truyền qua `initialize(callback: ...)`).
> Muốn nhiều nơi cùng nghe → host tự bọc một object fan-out nhỏ (tuỳ chọn; demo có `DemoPromotionCallback` ~30 dòng).

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
| `import PromotionLogic` / `PRMKotlinBridge` | Chỉ `import PRM` |
| Tự hỏi feature flag để ẩn UI | Lắng nghe `onAvailabilityChanged(enabled:)` |
| Tưởng phải tự viết wrapper `PromotionManager` | Gọi thẳng `PromotionSDK` — SDK đã tự lo token/context/callback |

---

## 12. Vòng đời gợi ý (khớp host thật)

```
login thành công        → PromotionSDK.initialize(customerId:accessToken:baseUrl:)
vào màn có voucher       → PromotionSDK.updateContext(orderId:orderValue:...)
mở UI                    → openMyPromotion / openPromotionDetail / createEndowView
login lại (phiên mới)    → PromotionSDK.initialize(...)   (SDK khoá field cố định)
refresh token giữa phiên → PromotionSDK.updateToken(newToken)
logout                   → PromotionSDK.release()
```
