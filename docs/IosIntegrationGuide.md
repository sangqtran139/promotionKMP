# iOS Integration Guide — Promotion SDK cho app host

> Hướng dẫn **tích hợp** dành cho đội app host (bên tiêu thụ SDK). Không phải tài liệu phát triển nội
> bộ SDK — cái đó xem [`ios/UIGuide.md`](./ios/UIGuide.md). Bề mặt API song ánh Android↔iOS: [`PublicApi.md`](./common/PublicApi.md).
> Tích hợp trực tiếp — gọi thẳng `PromotionSDK`, không cần wrapper (xem [`InitParity.md`](./common/InitParity.md) §6).

## Mục lục

<!-- toc -->
- [0. TL;DR](#0-tldr)
- [1. SDK đóng gói thế nào (vì sao host "sạch")](#1-sdk-đóng-gói-thế-nào-vì-sao-host-sạch)
- [2. Yêu cầu & phân phối](#2-yêu-cầu--phân-phối)
- [3. Thêm vào project](#3-thêm-vào-project)
  - [3.1. Swift Package Manager — kênh chính](#31-swift-package-manager--kênh-chính)
  - [3.2. Kéo tay (khi chưa được cấp tài khoản Artifactory)](#32-kéo-tay-khi-chưa-được-cấp-tài-khoản-artifactory)
  - [3.3. Kiểm tra nhanh](#33-kiểm-tra-nhanh)
- [4. Vòng đời SDK](#4-vòng-đời-sdk)
  - [4.1. Nguồn token — `PromotionTokenSource`](#41-nguồn-token--promotiontokensource)
  - [4.2. Khi SDK ăn 401 — `refreshToken(_:)`](#42-khi-sdk-ăn-401--refreshtoken_)
  - [4.3. Object này sống lâu hơn màn hình](#43-object-này-sống-lâu-hơn-màn-hình)
- [5. Bơm context đơn hàng](#5-bơm-context-đơn-hàng)
- [6. Màn hình UI có sẵn](#6-màn-hình-ui-có-sẵn)
- [7. Nhận sự kiện — `PromotionSDKCallback`](#7-nhận-sự-kiện--promotionsdkcallback)
  - [7.1. Cờ tính năng chặn điểm mở màn — `onFeatureDisabled`](#71-cờ-tính-năng-chặn-điểm-mở-màn--onfeaturedisabled)
- [8. Headless API (tự dựng UI) — `PromotionSDK.api`](#8-headless-api-tự-dựng-ui--promotionsdkapi)
- [9. Xử lý lỗi — `PromotionSDKError`](#9-xử-lý-lỗi--promotionsdkerror)
- [10. Theming](#10-theming)
- [11. Sai lầm thường gặp](#11-sai-lầm-thường-gặp)
- [12. Vòng đời gợi ý (khớp host thật)](#12-vòng-đời-gợi-ý-khớp-host-thật)
<!-- /toc -->

---

## 0. TL;DR

- Khai **một** `binaryTarget` Swift Package trỏ tới zip trên Artifactory nội bộ, đặt **Embed & Sign**.
  Xong. Không CocoaPods, không cài Kotlin hay thư viện nào khác — SDK là **một** xcframework khép kín.
- Mọi thứ host chạm đều bắt đầu bằng `Promotion*` (`PromotionSDK`, `PromotionSDKApi`, `PromotionSDKTheme`…).
- `import PRM` là import **duy nhất** host cần.
- Cấu hình một lần bằng `PromotionSDK.initialize(tokenSource:baseUrl:)`, bơm đơn hàng bằng `updateOrderInfo(...)`, nhận
  sự kiện qua `PromotionSDKCallback`.

---

## 1. SDK đóng gói thế nào (vì sao host "sạch")

SDK ship dạng **một dynamic framework** đóng trong `Promotion.xcframework` (binary là Mach-O
`DYLIB`). Kotlin (`PromotionLogic`) và các module UI nội bộ (`PRMPromotionUI`, `PRMDesignKit`,
`PRMKotlinBridge`, `PRMFoundation`) đều được **link tĩnh vào bên trong** framework động này và **giấu** sau
`@_implementationOnly`. Bằng chứng: file `.swiftinterface` công khai của framework **chỉ** import
`Foundation / UIKit / SwiftUI / Swift` — không một dòng nào lộ Kotlin hay module nội bộ.

Hệ quả cho host:

- ✅ Host **không** cần thêm bất kỳ dependency nào ngoài xcframework này.
- ✅ Host **không** thấy — và không build nhầm phải — type Kotlin của lõi.
- ✅ Public API chỉ dùng `String / Int / Bool / UIColor / UIView / UIViewController / Result` → không lo
  cross-module deserialization.

> ⚠️ Chính vì đóng gói kín, **đừng** cố `import PRMKotlinBridge` / `import PromotionLogic` ở host — chúng
> là module nội bộ, không có trong interface công khai; build sẽ fail `Unable to find module dependency`.

---

## 2. Yêu cầu & phân phối

| Mục | Giá trị |
|---|---|
| Artifact | `Promotion-<version>.xcframework.zip` — **một** xcframework khép kín |
| Kênh phát hành | Artifactory nội bộ, repo generic **`vdo-ios-frameworks`**:<br>`https://mobile-data.viettelmoney.vn/artifactory/vdo-ios-frameworks/Martech/Promotion/<version>/` |
| Cách tiêu thụ | **SPM `binaryTarget`** (`url:` + `checksum:`) — xem §3.1. Kéo tay vẫn dùng được, xem §3.2 |
| Xác thực | Basic auth qua `~/.netrc` — SwiftPM **không bao giờ hỏi mật khẩu** |
| Module import | `import PRM` |
| iOS tối thiểu | **iOS 13.0** |
| Slice | `ios-arm64` (thiết bị) + `ios-arm64_x86_64-simulator` (simulator) |
| UI | UIKit — API trả `UIViewController` / `UIView` |
| Linking | **Dynamic framework** → bắt buộc **Embed & Sign** (dylib phải copy vào app bundle; lõi Kotlin và module nội bộ link tĩnh sẵn bên trong) |
| Version | `SDK_VERSION` (mặc định `1.0.0`) → stamp vào `MARKETING_VERSION` = `CFBundleShortVersionString` trong Info.plist của framework |

> **Version nằm ở hai chỗ, đừng lẫn.** Bản dựng ra ở máy build mang tên **cố định**
> `Promotion.xcframework.zip`; bản **đã phát hành** trên Artifactory mang tên **có version**
> `Promotion-<version>.xcframework.zip`, mỗi version một thư mục riêng và **không cho ghi đè**. Ngoài
> ra version còn nằm trong `Info.plist` (`CFBundleShortVersionString`) nên host đọc lại được lúc runtime
> qua `Bundle`. Đối xứng `SDK_VERSION` bên Android (ở đó version nằm trong toạ độ Maven
> `vn.viettelpay.library:promotion:<version>`). Chi tiết: [`Distribution.md`](./ios/Distribution.md).
>
> Cạnh mỗi zip có `metadata.json` chứa `download_url` + `sha256` — **lấy `checksum` từ đó**, không tự
> tính lại.

---

## 3. Thêm vào project

### 3.1. Swift Package Manager — kênh chính

`binaryTarget` chỉ nhận `url:` khi nằm trong một Swift package, **không** khai thẳng vào
`.xcodeproj` được. Nên tạo một package mỏng không chứa code, chỉ để trỏ tới zip:

```swift
// swift-tools-version: 5.10
import PackageDescription

let package = Package(
    name: "PromotionRemote",
    platforms: [.iOS(.v13)],
    products: [
        .library(name: "PromotionRemote", targets: ["Promotion"]),
    ],
    targets: [
        // Tên target BẮT BUỘC trùng tên xcframework bên trong zip (`Promotion.xcframework`) —
        // KHÔNG phải tên module host import (`PRM`).
        .binaryTarget(
            name: "Promotion",
            url: "https://mobile-data.viettelmoney.vn/artifactory/vdo-ios-frameworks/Martech/Promotion/1.0.0/Promotion-1.0.0.xcframework.zip",
            checksum: "9da231216a057b570df3164e06ec51d5087a7fd64a74f2466f4a2da557caf065"
        ),
    ]
)
```

Rồi thêm package đó vào project host và đặt sản phẩm của nó = **Embed & Sign**.

**Xác thực — bước hay bị quên.** Repo Artifactory cần Basic auth, mà SwiftPM **không bao giờ hỏi mật
khẩu**: nó đọc `~/.netrc`.

```
machine mobile-data.viettelmoney.vn login <user> password <identity token>
```

Thiếu dòng này thì Xcode chỉ báo *"failed downloading"* cụt lủn, **không** nói là 401. Máy dev mới và
CI runner đều phải tự khai.

**Nâng version** = sửa `url:` + `checksum:` (lấy từ `metadata.json` cạnh zip). Không có "resolve tự
động lên bản mới" — `binaryTarget` ghim đúng một file.

> ⚠️ **Bẫy cache.** SPM cache theo **URL**, không theo checksum, ở hai tầng: `<DerivedData>/SourcePackages/artifacts/`
> và `~/Library/Caches/org.swift.swiftpm/artifacts/<url>` (tầng sau dùng chung mọi project trên máy).
> Vì repo phát hành **không cho ghi đè version** nên bình thường không gặp; nếu gặp báo lỗi checksum
> mà chắc chắn số đã đúng, xoá cả hai tầng rồi resolve lại.

### 3.2. Kéo tay (khi chưa được cấp tài khoản Artifactory)

1. Kéo `Promotion.xcframework` (giải nén từ zip) vào project navigator.
2. Chọn target host → tab **General** → **Frameworks, Libraries, and Embedded Content**.
3. Đặt `Promotion.xcframework` = **Embed & Sign**.
   > Bắt buộc "Embed" vì đây là **dynamic framework**: dylib phải được copy vào `.app/Frameworks` thì mới
   > load được lúc runtime. Ngoài ra framework còn kèm resource bundle (`PRMDesignKit`, `PRMPromotionUI`,
   > `PRMFoundation`) và các `.nib`. Để "Do Not Embed" → crash `dyld: Library not loaded` khi mở app.
4. Build. Không cần cấu hình `OTHER_LDFLAGS` hay search path thủ công.

### 3.3. Kiểm tra nhanh

```swift
import PRM

print(PromotionSDK.isInitialized()) // false — link OK là được
```

---

## 4. Vòng đời SDK

`PromotionSDK` là **singleton tĩnh** — mọi điểm vào là `static`. SDK giữ **một** phiên sống tại một thời điểm.

```swift
import PRM

// Cách tối giản — đủ cho phần lớn host, chỉ 2 tham số bắt buộc:
PromotionSDK.initialize(
    tokenSource: myTokenSource,                     // xem §4.1 — nguồn token, không phải chuỗi
    baseUrl: "http://125.235.38.229:8080/",
    // tuỳ chọn:
    environment: .prod,                             // mặc định .prod
    availableServices: [                            // cho bottom sheet "Chọn dịch vụ"
        PromotionAvailableService(productId: "TOPUP", productName: "Nạp tiền", iconUrl: iconUrl)
    ],
    callback: myCallback                            // conform PromotionSDKCallback (xem §7)
)
```

Cần cấu hình sâu hơn (theme, …) thì dùng overload nhận `PromotionSDKOptions`:

```swift
PromotionSDK.initialize(options: PromotionSDKOptions(
    session: PromotionSessionConfig(tokenSource: myTokenSource, baseUrl: baseUrl, environment: .prod),
    availableServices: services, theme: myTheme, callback: myCallback
))
```

| Việc | API |
|---|---|
| Khởi tạo (tối giản) | `PromotionSDK.initialize(tokenSource:baseUrl:)` |
| Khởi tạo (đầy đủ) | `PromotionSDK.initialize(options:)` |
| Nguồn token | `PromotionTokenSource` — xem §4.1 |
| Kiểm tra đã init | `PromotionSDK.isInitialized() -> Bool` |
| Giải phóng (logout) | `PromotionSDK.release()` |
| Lấy callback đã set | `PromotionSDK.getCallback() -> PromotionSDKCallback?` |

**Lúc nào vào app cũng gọi `initialize(...)`.** `updateSession` đã bị bỏ — chỉ còn một lối vào.

SDK tách hai loại field:

| Tĩnh (đặt **một lần**, dùng lại) | Theo phiên (đưa mỗi lần init) |
|---|---|
| `baseUrl`, `environment`, `language`, `theme` | `tokenSource`, `availableServices` |

Field **tĩnh** chỉ cần truyền ở lần `initialize` đầu (hoặc lần đầu sau `release()`). Các lần sau SDK
**dùng lại giá trị đã có, không khởi tạo lần nữa** — host chỉ cần đưa `tokenSource`:

```swift
// Lần đầu — khai đủ
PromotionSDK.initialize(tokenSource: myTokenSource, baseUrl: baseUrl,
                        availableServices: services, callback: cb)

// Mỗi lần vào app / login lại — gọi y hệt; SDK dùng lại baseUrl/environment/language/theme đã có.
PromotionSDK.initialize(tokenSource: myTokenSource, baseUrl: baseUrl, availableServices: services)
```

- **Đổi field tĩnh thật** (vd chuyển environment): `release()` rồi `initialize(...)` lại.
- **`release()` xoá dữ liệu phiên** (session, context đơn hàng, callback) nhưng **giữ** cấu hình tĩnh
  và theme đã lưu — đó là cấu hình tích hợp của host, không phải dữ liệu người dùng.

- **Token hết hạn giữa phiên:** không cần gọi gì cả. SDK đọc lại `tokenSource.currentToken()` ở mỗi
  request — xem §4.1.

> `release()` khi chưa init là vô hại; không xoá theme đã lưu.

### 4.1. Nguồn token — `PromotionTokenSource`

Token vào SDK qua **một** đường duy nhất: `tokenSource`. Không có tham số `accessToken`, vì token là
thứ đổi theo thời gian chứ không phải cấu hình chụp một lần.

```swift
final class MyTokenSource: PromotionTokenSource {
    func currentToken() -> String? { auth.accessToken }   // SDK đọc lại ở MỖI request
}

PromotionSDK.initialize(
    tokenSource: MyTokenSource.shared,
    baseUrl: baseUrl,
    availableServices: services,
    callback: myCallback
)
```

SDK không giữ bản sao token nào. App đổi token lúc nào, ở đâu, bằng cơ chế gì — SDK không cần biết
và không cần được báo; lượt gọi API kế tiếp tự dùng giá trị mới.

**Yêu cầu duy nhất với ô nhớ nguồn:** nó được đọc từ **thread nền** (lõi bọc mọi lời gọi API trong
`withContext(ioDispatcher)`) trong khi cơ chế refresh của bạn ghi từ thread khác — nên nó phải
thread-safe (`NSLock`, serial queue, `os_unfair_lock`…) và **không** được `@MainActor`.

`currentToken()` nằm trên đường dựng header của **mọi** request nên phải **non-blocking**:

```swift
func currentToken() -> String? { auth.accessToken }           // ✅ đọc biến đã đồng bộ
func currentToken() -> String? { try? keychain.readToken() }  // ❌ chặn mỗi request
```

Trả `nil`/rỗng → SDK gửi request **không kèm** header `Authorization`. Không có fallback nào: lỗi
hiện ra ngay thay vì gửi một token cũ trong im lặng.

### 4.2. Khi SDK ăn 401 — `refreshToken(_:)`

Mặc định SDK **hỏng luôn**: bắn `PromotionSDKCallback.onExpireToken()`, host đưa user về màn đăng
nhập. Đúng cho app không có cách lấy token mới theo yêu cầu.

App **có** hàm lấy token mới gọi được thì cài đặt thêm — SDK sẽ xin một lần rồi **tự chạy lại
request hỏng**, user không thấy màn lỗi:

```swift
final class MyTokenSource: PromotionTokenSource {
    func currentToken() -> String? { auth.accessToken }

    func refreshToken(_ onResult: @escaping (Bool) -> Void) {
        auth.refresh { newToken in
            if let newToken { auth.accessToken = newToken }   // ghi vào kho TRƯỚC
            onResult(newToken != nil)
        }
    }
}
```

**`Bool` chứ không phải token mới** là cố ý: token vào SDK theo đúng một đường là `currentToken()`.
Bạn ghi vào kho của mình rồi báo `true` — không có đường thứ hai để nhầm.

**Ràng buộc:**

- Gọi `onResult` **đúng một lần**, kể cả khi hỏng. Không gọi thì SDK chờ tối đa **15 giây** rồi coi
  như `false`.
- Được gọi từ **thread nền**.
- **Thử lại đúng một lần, không phải vòng lặp.** Refresh xong mà server vẫn 401 = phiên chết thật →
  `onExpireToken()`.
- SDK đã gộp mọi request 401 cùng lúc thành **một** lần gọi. Nhưng cơ chế của app vẫn nên
  single-flight: hai màn mở cách nhau vài giây vẫn có thể chạm vào hai lần.

### 4.3. Object này sống lâu hơn màn hình

`PromotionSDK` là **singleton**: nó giữ `tokenSource` từ lúc `initialize()` tới `release()`, **không**
theo vòng đời màn hình nào. Gọi `initialize()` trong một view controller rồi push sang màn khác thì
object đó **vẫn sống** — vấn đề ngược với lo lắng thường gặp.

Cho một `UIViewController` conform `PromotionTokenSource` rồi truyền `self` là **giữ màn hình đó vĩnh
viễn**, và token đóng băng ở giá trị cuối — đúng cái bệnh mà cơ chế này sinh ra để chữa. Trỏ vào kho
token **cấp app** (singleton), đừng trỏ vào màn hình.

---

## 5. Bơm context đơn hàng

Giữ **một** phiên từ lúc login, tới màn có voucher mới bơm đơn hàng — **không** re-init:

```swift
PromotionSDK.updateOrderInfo(
    // orderId/productId bắt buộc.
    orderId: order.id,
    productId: "P-FOOD-001",
    orderValue: "500000",   // chuỗi số nguyên VNĐ
    metaData: nil,
    // Đơn chỉ hỗ trợ MỘT dòng sản phẩm → truyền phẳng field của PromotionOrderItem thay vì mảng.
    skuSourceId: "SKU1",
    quantity: 1,
    unitPrice: "500000"
)
```

SDK đọc lại các giá trị này ở **mỗi** request, nên chỉ cần gọi trước khi mở màn / gọi API. Đọc ngược lại
qua `PromotionSDK.currentOrderId / currentOrderValue / currentServiceCode / currentMetaData` và `PromotionSDK.session`.

> ⚠️ Gọi trước `initialize`: `updateOrderInfo` / `openMyPromotion` / `openPromotionDetail` /
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
// Mặc định returnVoucherOnApply = true → nút "Áp dụng", trả object về đúng lời gọi này.
PromotionSDK.openPromotionDetail(voucherId: "V123", from: self) { detail in
    // detail: PromotionVoucherDetail — SDK đã tự pop màn chi tiết
    applyToMyScreen(detail)
}

// Muốn hành vi cũ (nút "Sử dụng ngay" → SDK tự mở sheet chọn dịch vụ):
PromotionSDK.openPromotionDetail(voucherId: "V123", from: self, returnVoucherOnApply: false)

// Muốn TỰ đóng màn chi tiết (hỏi xác nhận / animation riêng / đi thẳng sang màn khác):
PromotionSDK.openPromotionDetail(
    voucherId: "V123", from: self,
    hostHandlesDismiss: true            // SDK báo xong ĐỂ NGUYÊN màn
) { [weak self] detail in
    self?.navigationController?.popViewController(animated: true)   // host tự đóng
    self?.goToCheckout(detail)
}

// Widget checkout — gắn vào layout của bạn, tự load dữ liệu
let widget = PromotionSDK.createEndowView(from: self, orderId: order.id, orderValue: "500000")
container.addSubview(widget)

// Widget checkout kèm dòng sản phẩm (lấy campaign theo SKU)
let widget2 = PromotionSDK.createEndowView(
    from: self, orderId: order.id, orderValue: "500000",
    orderItems: [PromotionOrderItem(skuSourceId: "SKU1", quantity: 1, unitPrice: "500000")]
)
```

`onVoucherApplied` là **kênh trả gắn với lời gọi**, nên màn nào của host mở cũng nhận đúng chỗ —
khác `PromotionSDKCallback` (singleton, set một lần lúc `initialize`, không biết màn nào đã gọi).
Màn gọi **không cần** là màn thanh toán. Object trả về là `PromotionVoucherDetail`, đúng thứ
`api.getVoucherDetail` trả, nên không phải gọi API lần nữa để lấy tên/mô tả/HSD/ảnh/mã code.

**Feature flag — SDK tự gác, host hỏi thêm được.** Nếu cờ tương ứng TẮT, `openMyPromotion` /
`openPromotionDetail` / `confirmRedemption` tự hiện **popup** `PRM_MOB_021` (`PRMConfirmationDialog`,
1 nút "Đóng"), hoặc gọi `onFeatureDisabled` nếu host có truyền.
**Không làm gì thêm thì kill-switch vẫn chạy đủ.**

`confirmRedemption(onError:)` trả thẳng `PromotionSDKError` (không phải mã thô):

```swift
PromotionSDK.confirmRedemption(onSuccess: { proceedPayment() }, onError: { error in
    switch error {
    // SDK đã tự hiện popup — host chỉ cần dừng luồng, đừng hiện thêm thông báo của mình.
    case .featureDisabled: stopCheckout()
    default:               showMyOwnError(error.errorDescription)
    }
})
```

Đối ứng `PRMEndowView.onError` / `confirmRedemption(onError:)` bên Android — cùng kiểu lỗi.

Muốn mượt hơn — ẩn hẳn nút trước khi user kịp bấm — thì hỏi SDK:

```swift
// Đọc cache, đồng bộ, không gọi mạng
myVoucherButton.isHidden = !PromotionSDK.isFeatureEnabled(.voucherList)

// Hoặc nạp lại từ server rồi dựng UI (completion chạy trên main thread)
PromotionSDK.refreshFeatureFlags { flags in
    self.promotionSection.isHidden = !flags.all
    self.myVoucherButton.isHidden = !flags.voucherList
}
```

| Hàm | Trả gì |
|---|---|
| `PromotionSDK.featureFlags()` | `PromotionFeatureFlagsSnapshot` — toàn bộ cờ, đọc cache |
| `PromotionSDK.isFeatureEnabled(_:)` | `Bool` cho một `PromotionFeature` |
| `PromotionSDK.isSdkEnabled()` | Công tắc tổng — `false` thì ẩn **toàn bộ** điểm vào ưu đãi |
| `PromotionSDK.refreshFeatureFlags { … }` | Nạp lại từ server, trả snapshot mới trên main thread |

Ba điều cần nhớ:

- **Snapshot đã áp sẵn công tắc tổng**: `flags.all == false` → mọi field còn lại đều `false`.
- **Fail-open**: chưa `initialize()` hoặc chưa gọi được API lần nào → trả bật hết. Không hàm nào dừng chương trình.
- Cờ có thể đổi giữa phiên → **đừng cache lại** snapshot, hỏi lại mỗi khi dựng UI.

Không hỏi chủ động cũng được: cờ TẮT thì SDK tự chặn ở điểm mở màn (xem `onFeatureDisabled` dưới đây).

---

## 7. Nhận sự kiện — `PromotionSDKCallback`

Tất cả method đều có default (protocol extension) → chỉ implement cái cần.

```swift
final class MyPromotionCallback: PromotionSDKCallback {
    func onVoucherApplied(voucherId: String) { /* user áp voucher thành công */ }
    func onServiceSelected(selection: PromotionServiceSelection) { /* điều hướng tới dịch vụ đã chọn */ }
}
```

| Sự kiện | Khi nào bắn |
|---|---|
| `onVoucherApplied(voucherId:)` | User bấm "Áp dụng" thành công |
| `onServiceSelected(selection:)` | User chọn dịch vụ trong bottom sheet |

> **Đã bỏ:** `onVoucherCleared`, `onVoucherCountChanged`, `onAvailabilityChanged`, `onClosed`.
> Host không cần biết mấy thứ này — widget tự quản trạng thái của nó, việc bật/tắt theo cờ do SDK
> tự xử lý.

### 7.1. Cờ tính năng chặn điểm mở màn — `onFeatureDisabled`

Hai hàm mở màn nhận thêm tham số **tuỳ chọn**:

```swift
PromotionSDK.openMyPromotion(from: self)                                  // SDK tự hiện popup
PromotionSDK.openMyPromotion(from: self, onFeatureDisabled: { hideEntry() })  // host tự lo
```

Có truyền → host xử lý. **Không truyền → SDK tự hiện popup** `PRM_MOB_021`. Màn không mở trong cả hai
trường hợp. Áp cho `openMyPromotion` và `openPromotionDetail`.

Nhận closure ngay ở hàm `open…` chứ không qua `PromotionSDKCallback` là có lý do: protocol có default
implementation nên SDK **không phân biệt được** "host có implement" với "host mặc kệ" — không thể dựa
vào đó để quyết định có tự hiện popup hay không. Đối ứng `onFeatureDisabled` bên Android.

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
| Gọi `api` / `updateOrderInfo` trước `initialize` | Luôn `initialize` sau login trước tiên |
| Đổi token bằng cách sửa field | Gọi lại `initialize(options:)` với session mới |
| Đặt xcframework "Do Not Embed" | **Embed & Sign** (framework có resource bundle) |
| `import PromotionLogic` / `PRMKotlinBridge` | Chỉ `import PRM` |
| Tự hỏi feature flag để ẩn UI | Dùng `isSdkEnabled()` / `isFeatureEnabled(_:)` khi dựng UI |
| Tưởng phải tự viết wrapper `PromotionManager` | Gọi thẳng `PromotionSDK` — SDK đã tự lo token/context/callback |

---

## 12. Vòng đời gợi ý (khớp host thật)

```
login thành công        → PromotionSDK.initialize(tokenSource:baseUrl:)
vào màn có voucher       → PromotionSDK.updateOrderInfo(orderId:productId:orderValue:...)
mở UI                    → openMyPromotion / openPromotionDetail / createEndowView
login lại (phiên mới)    → PromotionSDK.initialize(...)   (SDK khoá field cố định)
logout                   → PromotionSDK.release()
```
