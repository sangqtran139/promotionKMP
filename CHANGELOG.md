# Changelog

Toàn bộ thay đổi đáng chú ý của **TTCN Promotion SDK** ghi ở đây. Định dạng theo
[Keep a Changelog](https://keepachangelog.com/), version theo [SemVer](https://semver.org/).

Nguồn version tập trung: `gradle.properties` (`SDK_VERSION`) cho Android/KMP; `MARKETING_VERSION`
trong `PRM.xcodeproj` cho iOS — **giữ trùng số**.

## [Unreleased]

### BREAKING (iOS) — bề mặt public là `@MainActor`

`PromotionSDK` bên iOS — và cả cây UI dưới nó (`PromotionSDKImpl`, `PRMStoreViewModel`,
`PRMBaseBuilder`, `PRMBaseRouter`) — nay đánh `@MainActor`.

Hợp đồng không đổi: đây vốn là API UI (`openMyPromotion(from:)`, `createEndowView(from:)`,
`configure(theme:)`), luôn phải gọi trên main thread. Cái đổi là **ai bắt lỗi**: trước đây chỉ có
doc comment, host gọi từ thread nền thì crash lúc chạy ở tầng UIKit; nay compiler chỉ đúng dòng sai.
Nó cũng đóng phần global mutable state (bốn `private static var`) mà Swift 6 sẽ chặn.

**Cách nâng cấp:** gọi từ ngữ cảnh không phải main thì bọc `await MainActor.run { … }`.

> Thêm `@MainActor` **sau** go-live là source-breaking với mọi host, nên phải làm bây giờ.

Bên Android, cùng hợp đồng đó nay được đánh `@MainThread` (lint) trên 6 điểm UI:
`configure`, `openMyPromotion`, `openPromotionDetail`, `openChoosePromotion`,
`closePromotionDetail`, `closeMyPromotion`. Không breaking — annotation chỉ bật cảnh báo lint.

### BREAKING — `PromotionSDKError` thêm hai nhánh, và `PromotionSDK.api` không còn dừng chương trình

Sửa ba lỗi hành vi do bản rà soát SDK chỉ ra (§3), cộng phần "chốt danh sách case" của API-2.

**1. `PromotionSDK.api` gọi trước `initialize` không còn làm crash / ném.** iOS trước đây
`preconditionFailure` — SDK làm **crash app của host** vì lỗi thứ tự khởi tạo của *host*, loại lỗi
rất dễ xảy ra khi vào màn bằng deep link. Android ném `IllegalStateException`. Nay cả hai trả một bề
mặt mà **mọi hàm** cho `PromotionSDKError.NotInitialized`; chữ ký `api` vẫn non-optional.

**2. Lỗi mạng không còn trả message rỗng.** `NETWORK_ERROR` từng map thành
`NetworkFailure(code = null, message = "")` — host hiện `error.message` thì ra **popup trắng không
chữ**, ở đúng case hay xảy ra nhất. Nay lùi về câu của SDK khi server không kèm gì (trùng
`R.string.prm_error_network`).

**3. Mã nghiệp vụ của server không còn giả dạng lỗi mạng.** Nhánh `default` từng nhét mọi mã
(`VOUCHER_EXPIRED`…) vào `NetworkFailure(code = null, message = errorCode)` — mã lỗi thô nằm đúng chỗ
đáng lẽ là câu hiển thị cho người dùng. Nay là nhánh riêng `BusinessRule(code, serverMessage)`.

**Gộp hai đường map lỗi về một.** `PromotionSDKError.from(errorCode, serverMessage, httpStatus)` là
nơi duy nhất; `PromotionSDKApi.toSdkError` (cả hai nền tảng) uỷ thẳng cho nó. Trước đây mỗi bề mặt
map một bản, và hai bản không khớp nhau.

| Bỏ | Thay bằng |
|---|---|
| `api` ném / crash khi chưa init | `PromotionSDKError.NotInitialized` từ mọi hàm |
| `NetworkFailure(message = "")` | `NetworkFailure` với câu lùi mặc định |
| `NetworkFailure(message = errorCode)` cho mã nghiệp vụ | `BusinessRule(code, serverMessage)` |

**Cách nâng cấp:** `when`/`switch` trên `PromotionSDKError` phải thêm hai nhánh `BusinessRule` và
`NotInitialized`. Chỗ nào đang đọc `NetworkFailure.message` để lấy mã lỗi nghiệp vụ thì đọc
`BusinessRule.code`.

### Changed — dependency Android: `api` cho hai artifact public, bỏ alpha, gỡ `sdp-android`, gỡ Timber

| | Trước | Sau |
|---|---|---|
| `androidx.fragment` / `androidx.constraintlayout` | `implementation` — host không thấy lúc biên dịch, phải tự khai lại | **`api`** (chúng nằm trong chữ ký public) |
| `androidx.constraintlayout` | `2.2.0-alpha10` | `2.2.1` |
| `androidx.swiperefreshlayout` | `1.1.0-alpha02` | `1.1.0` |
| `com.intuit.sdp:sdp-android` | dependency — đổ ~600 dimens × 29 bucket vào `R` của host | **gỡ**; 58 giá trị SDK dùng nội bộ hoá thành `prm_Xsdp` |
| `com.jakewharton.timber` | dependency, 12 chỗ gọi, 0 `plant()` | **gỡ** |
| Data Binding | bật, 20/24 layout bọc `<layout>`, 0 layout dùng | **tắt** |

`prm_Xsdp` giữ nguyên giá trị **theo từng bucket** của sdp (đã đối chiếu 1624 giá trị với AAR gốc:
khớp 100%) — không quy về dp cố định, vì sdp là "scalable dp" và số cứng sẽ đổi layout trên tablet.

> **Host có thể đang "ăn ké" resource của SDK.** App demo trong repo build hỏng ngay sau khi gỡ
> `sdp-android`: layout của **chính nó** dùng `@dimen/_16sdp` mà chưa bao giờ khai dependency đó.
> Host thật cũng có thể như vậy — nếu build gãy vì thiếu `_Xsdp`, hãy tự khai `sdp-android`.

> **Glide 5.0.5 vẫn đi vào runtime classpath của host** và Gradle luôn chọn version cao nhất. Host
> đang ở Glide 4.x cần chốt lại — xem `docs/AndroidIntegrationGuide.md` §3.1. Không gỡ được vì luật
> GIF động, mà `AnimatedImageDrawable` chỉ có từ API 28 còn `minSdk` là 24.

### Changed — tầng ảnh iOS có cache đĩa và gộp request trùng

`RemoteImageCache` trước đây chỉ có `NSCache` (RAM). Hai hệ quả trong luồng thật: mỗi lần mở lại app
là tải lại toàn bộ logo voucher (và `NSCache` còn bị hệ điều hành xoá bất cứ lúc nào), và cùng một
logo ở 5 cell là **5 lượt tải song song cho cùng một URL**.

Nay: RAM → đĩa (`Library/Caches`, tên file là SHA256 của URL, cap 50 MB, xoá LRU) → mạng, và các
request trùng URL được **gộp** thành một. `URLSessionDataTask` gắn theo image view bị bỏ cùng lúc —
với request đã gộp thì cancel theo một view sẽ cắt luôn của các view còn lại; chống ảnh nhảy khi cell
tái sử dụng vẫn nguyên vẹn nhờ đối chiếu URL lúc gán.

> Tên file dùng SHA256 chứ **không** phải `hashValue` của Swift: `hashValue` có seed ngẫu nhiên mỗi
> lần chạy app, nên file ghi phiên này không bao giờ tìm thấy ở phiên sau — cache đĩa thành vô dụng
> theo cách rất khó nhận ra.

### Removed — Timber khỏi SDK Android

12 chỗ gọi `Timber.x()`, **0** chỗ `Timber.plant()`. Timber không có cây nào thì mọi lời gọi im lặng
rơi vào hư không — 12 chỗ log đó chưa từng in ra gì. Và nếu SDK *có* plant, nó ghi vào cây **toàn
cục** dùng chung với app host: log của SDK trộn vào hệ thống log của host.

Thay bằng `PRMLog` nội bộ (`android.util.Log`, gác `BuildConfig.DEBUG` của **module SDK**, một tag
`PromotionSDK` cho cả SDK). Bỏ luôn `implementation(libs.timber)` — host không còn bị SDK kéo theo
Timber. Kèm theo: plugin lint của Timber đang **crash** trên repo này
(`timber.lint.WrongTimberUsageDetector`) làm `lintRelease` fail vì lỗi của chính detector; gỡ Timber
là hết.

### Added — `PromotionSDK.sdkVersion`

Host đọc được version SDK đang chạy bằng một dòng, **trước** `initialize`. Với support và telemetry,
"app đang chạy SDK bản nào" là câu hỏi đầu tiên. Trước đây iOS không có API nào — host phải tự biết
class nào để `Bundle(for:)`, tức kiến thức nội bộ của SDK; đây cũng là **lệch parity** vì Android đã
có `BuildConfig.SDK_VERSION`. Nay cả hai nền tảng cùng một tên.

Nguồn giữ nguyên: `SDK_VERSION` (`gradle.properties`) và `MARKETING_VERSION` (`PRM.xcodeproj`) —
hai số vẫn phải giữ trùng nhau, xem `docs/release/VersioningPolicy.md`.

### Fixed — bỏ cơ chế "báo host khi kill-switch tắt" chưa từng chạy

`PromotionSDK.swift` hứa với host ở ba chỗ rằng SDK báo trạng thái cờ qua
`PromotionSDKCallback.onAvailabilityChanged(enabled:)` — **method đó chưa bao giờ tồn tại**.
`PromotionSDKImpl.onAvailabilityUpdate` có khai và có chỗ gọi, nhưng `wireCallbacks` không gán nó;
bên Android `notifyAvailability()` có thân hàm chỉ đọc `callback` rồi không làm gì.

Không thêm callback: `docs/common/InitParity.md` §3 ghi rõ `onAvailabilityChanged` bị **loại có chủ
đích** — host đọc cờ qua `refreshFeatureFlags`, còn cờ chặn một điểm mở màn thì báo qua
`onFeatureDisabled` của chính hàm `open…`. Nên phần bị xoá là xác chết và doc nói sai:

- 4 closure không ai gán trong `PromotionSDKImpl` (`onAvailabilityUpdate`, `onClearVoucher`,
  `onUpdateWidgetCount`, `onClose`) + `typealias OnClearVoucher`.
- `notifyAvailabilityAfterInitialLoad()` (iOS) và `notifyAvailability()` (Android).
- 3 doc comment mồ côi trong `PromotionSDKCallback.swift`, 3 lời hứa trong `PromotionSDK.swift`.

Không đổi hành vi nào đang chạy — thứ bị xoá chưa từng chạy.

### Changed — màn "Chọn ưu đãi": luôn nạp lại dữ liệu, và ưu đãi bị từ chối thì disable tại chỗ

Ba thay đổi hành vi thấy được trên UI, **không** đụng public API của host.

**1. Mở màn là luôn gọi lại `findEligible`.** Trước đây màn nhận danh sách widget `PRMEndowView` đã
nạp và dùng thẳng, không gọi mạng — tiết kiệm một request nhưng user nhìn thấy dữ liệu của *thời điểm
widget nạp*: ngân sách campaign có thể đã hết, voucher có thể vừa bị dùng ở thiết bị khác, và không có
gì sửa lại cho tới khi user tự kéo-để-tải-lại. Nay mở màn là hiện shimmer một nhịp rồi ra danh sách
mới. Cờ phân trang cũng lấy từ chính response đó.

**2. `validateStackableDiscounts` trả `valid = false` → ưu đãi bị disable tại chỗ, màn ở lại.** Card
mờ đi, mất ô tick, **bỏ chọn** — và **không** đeo thêm nhãn/dải trạng thái nào; lý do hiện ở một popup
mang **câu nguyên văn của server** (`discountDetails[].validationMessages`, lùi về
`businessRuleViolations`). Widget ở màn thanh toán **giữ nguyên** bộ discount cũ. Trạng thái disable
sống qua cả kéo-để-tải-lại — `findEligible` vẫn đánh `usable = true` cho ưu đãi mà `validateDiscounts`
vừa từ chối.

> Trước đây nhánh này bị gộp với thành công: màn chọn đóng lại như đã áp xong, còn widget lặng lẽ
> chuyển sang `UNAVAILABLE`. User chọn voucher rồi thấy nó gạch ngang mà không ai nói vì sao. Hệ quả:
> **`EndowWidgetState.UNAVAILABLE` nay chỉ còn đến từ** ưu đãi đang áp hỏng giữa chừng (hết ngân sách
> lúc `createRedemption`) và host tự đưa vào (`setDiscountDetails` / `markAppliedVoucherUnavailable`).

**3. Gọi API hỏng → popup + ở lại màn** (không đổi so với trước, nay đi qua nhánh riêng
`EndowApplyOutcome.Failed`).

Hiện áp dụng cho **chế độ chọn đơn** (mặc định); chế độ chọn nhiều sẽ làm sau.

**Nội bộ SDK** (`internal`, host không chạm tới): `EndowStore.validateAndApply` trả
`EndowApplyOutcome` (`Applied` / `Rejected(items)` / `Failed(errorCode)`) thay cho `EndowState`;
`ChoosePromotionIntent.SeedOnce` rút còn `SeedOnce(preSelectedIds)`; thêm intent `ApplyRejected` /
`ConsumeApplyMessage`, state `rejectedIds` / `applyMessage` và `ChooseOffer.isRejected`; xoá đường preload
(`PRMEndowView.myVouchers`/`otherVouchers`/`myIsLastPage`/`otherIsLastPage`,
`ChoosePromotionFragment.initial*`, `ChoosePromotionBuilder.DataModel.preloaded*`).

Chi tiết: [`docs/features/ChoosePromotion.md`](docs/features/ChoosePromotion.md),
[`docs/features/EndowView.md`](docs/features/EndowView.md),
[`docs/common/InitParity.md`](docs/common/InitParity.md) B12.

### BREAKING — token vào SDK qua `PromotionTokenSource`, bỏ `accessToken` và `updateToken`

Cả bề mặt token được thay bằng **một** khái niệm:

```kotlin
interface PromotionTokenSource {
    fun currentToken(): String?                              // SDK đọc lại ở MỖI request
    fun refreshToken(onResult: (Boolean) -> Unit) = onResult(false)   // 401 → xin 1 lần
}
```

**Vấn đề nó chữa.** Token của host sống ~15 phút và host tự lấy lại theo cơ chế riêng. SDK thì giữ
chuỗi token nhận lúc `initialize`, nên ngay sau lần refresh đầu tiên của host, SDK cầm một token đã
chết trong khi phiên đăng nhập vẫn sống — mọi API của SDK trả 401 và màn ưu đãi báo lỗi dù người
dùng chưa hề đăng xuất. Gốc rễ là **SDK giữ bản sao token**; thiết kế mới không giữ gì cả.

**Ba hành vi, ánh xạ 1-1 với hai hàm trên:**

1. SDK luôn dùng token mới nhất của host — `currentToken()` được gọi ở mỗi request
   (`defaultRequest { }`), không cache, không fallback.
2. Ăn 401 → xin token mới **đúng một lần** rồi chạy lại request — `refreshToken`.
3. Không lấy được → hỏng luôn: `TOKEN_EXPIRED` + `PromotionSDKCallback.onExpireToken()`. Đây cũng là
   hành vi mặc định khi host không cài đặt `refreshToken`.

`refreshToken` trả `Boolean` chứ không phải token mới là cố ý: token vào SDK theo đúng một đường là
`currentToken()`. Host ghi vào kho của mình **rồi** báo `true` — không có đường thứ hai để nhầm.

**Đã xoá:**

| Bỏ | Thay bằng |
|---|---|
| `PromotionSessionConfig.accessToken` | `tokenSource` |
| `initialize(..., accessToken: String, ...)` | `initialize(..., tokenSource: PromotionTokenSource, ...)` |
| `PromotionSDK.updateToken(...)` | không cần — SDK tự đọc lại mỗi request |
| iOS `PromotionSDKImpl.updateSession` / `applySession` | (đã không còn call-site) |

`PromotionSDK.session` vẫn còn nhưng không mang token nữa — nó là cấu hình phiên (`baseUrl` /
`language` / `environment` / `tokenSource`).

**Cách nâng cấp:**

```kotlin
// Trước
PromotionSDK.initialize(ctx, auth.accessToken, BASE_URL, callback = cb)
// … và mỗi lần app refresh token:
PromotionSDK.updateToken(newToken)

// Sau — khai nguồn một lần, không phải đẩy gì nữa
PromotionSDK.initialize(
    ctx,
    tokenSource = object : PromotionTokenSource {
        override fun currentToken() = auth.accessToken     // field @Volatile của app
    },
    baseUrl = BASE_URL,
    callback = cb,
)
```

Field nguồn phải `@Volatile` / `AtomicReference` / `StateFlow.value`: SDK đọc nó từ **thread nền**.
Object `tokenSource` bị SDK giữ tới `release()` — trỏ vào kho token **cấp app**, không phải vào
Fragment/ViewController đang gọi `initialize`.

Chi tiết + phần iOS: `docs/AndroidIntegrationGuide.md` §4.1 / `docs/IosIntegrationGuide.md` §4.1.

### Fixed — tầng data không còn chạy trên main thread (Android)

`PromotionRemoteDataSource.apiCall` và `FeatureFlagRemoteDataSource.apiCall` bọc
`withContext(ioDispatcher)` (`expect`/`actual` mới ở `promotionLogic/common/IoDispatcher.kt`).

Ktor chạy pipeline **phía client** trong context của coroutine gọi nó — chỉ engine mới tự nhảy sang
thread nền. Store dùng chung nhận `scope` từ nền tảng, và `PRMStoreViewModel` bên Android truyền
thẳng `viewModelScope` (`Dispatchers.Main.immediate`). Nghĩa là toàn bộ `defaultRequest { }` — dựng
header, đọc token/ngôn ngữ/context đơn hàng từ host — đang chạy trên main thread. Nay không còn, và
đó là điều kiện để `PromotionTokenSource.currentToken()` của host chạy ngoài main thread.

### Changed — Android: mọi điều hướng nội bộ đi chung `PRMBaseFragment.addFragment()`

`MyPromotionFragment.openSearchMyPromotion()` tự dựng `FragmentTransaction` riêng, chép lại đúng
phần chọn FM (`parentFragmentManager`) + lấy container từ view cha + dedup theo tag mà
[addFragment] đã làm — hai bản song song, và bản trong base thì thiếu mất hai chốt bảo vệ mà bản
chép tay có. Nay màn Tìm kiếm gọi thẳng `addFragment()`, còn base nhận lại hai chốt đó cho **mọi**
màn:

- **Dedup theo tag** — bấm nhanh hai lần vào nút mở màn không còn chồng hai instance (trước đây
  `openPromotionDetail()` dính lỗi này; `PromotionSDK` ở bề mặt host thì đã gác từ trước).
- **`setReorderingAllowed(true)`** — animation và vòng đời chạy đúng khi add chồng màn.

`addFragment()` thêm tham số `tag` (mặc định tên class) để giữ được các tag `prm_*` cố định. Không
đổi chữ ký public nào — `PRMBaseFragment` là `internal`.

### Fixed — Android: SDK không còn ghi log chẩn đoán ra logcat của host ở bản release

`PromotionSDK` (Android) có 7 lệnh `Log.d(TAG = "PRMSystemBack", …)` **không gác cờ nào** — một khối
còn ghi rõ `// TEMP DEBUG` — nằm đúng trên các entry point `openPromotionDetail()`, `popSdkScreen()`,
`resolveFragmentManager()`, `addOrHideThenAdd()`. Hệ quả với host: logcat bị spam mỗi lần mở màn SDK
**kể cả build release**, và nội dung log phơi cả tên class fragment/view của host cùng
`identityHashCode`. Một lệnh `findViewById` cũng đang chạy chỉ để dựng chuỗi log.

Nay: các dòng thuần debug bị xoá; hai dòng có giá trị chẩn đoán thật (giải thích vì sao
`closePromotionDetail()` / `closeMyPromotion()` trả `false`) chuyển sang `debugLog()` — gác sau
`PromotionSDKConfig.isDebug`, **cùng cờ** đã gác cURL/`LogLevel.BODY` ở `PromotionHttpClient` và log
ảnh ở `PRMImageExt`. `Log.e` của `requireInitialized()` giữ nguyên hành vi luôn-in: nó báo host gọi
sai API, không phải log chẩn đoán.

Không đổi chữ ký public nào.

### Fixed — `onExpireToken()` không còn bắn nhầm khi lỗi 403

`Throwable.toErrorCode()` (`domain/exception/ErrorCodeExtensions.kt`) trước đây map cả HTTP 401 **và**
403 sang `TOKEN_EXPIRED`, khiến host nhận `onExpireToken()` cả khi lỗi thực ra là **403 — không đủ
quyền** (token vẫn hợp lệ), không phải hết hạn/không hợp lệ. Nay chỉ 401 mới map sang `TOKEN_EXPIRED`;
403 rơi về nhánh `errorCode`/`GENERAL` như các lỗi HTTP khác. Sửa ở tầng `promotionLogic` (dùng chung
2 nền tảng) nên áp dụng cho cả 5 store (`MyPromotion`/`SearchMyPromotion`/`ChoosePromotion`/
`PromotionDetail`/`Endow`) mà không cần sửa native.

### Changed — **BREAKING**: `PromotionOrderItem`/`updateOrderInfo` đổi `skuId` thành `skuSourceId`; bỏ gửi chuỗi rỗng lên server

Đồng nhất tên với field thật trên wire (`EligibleOrderItemDto.skuSourceId`) — trước đây model/tham số
gọi là `skuId` còn request gửi lên `findEligible` lại là `skuSourceId`, phải tự nhớ quy đổi. Đổi cả 2
nền tảng, cả 2 điểm chạm: `PromotionOrderItem.skuId` và tham số `skuId` của `updateOrderInfo`.

Đi kèm sửa lỗi: trước đây bỏ trống `skuId` thì `updateOrderInfo` vẫn dựng `PromotionOrderItem` với
`skuId = ""`, server nhận `"skuSourceId":""` (chuỗi rỗng) thay vì không thấy field này. Nay `skuSourceId`
rỗng/không truyền → **field bị bỏ hẳn khỏi JSON** gửi lên `findEligible` (đúng ngữ nghĩa "không có SKU"),
không ảnh hưởng các field khác của item (`productId`/`productName`/`quantity`/`unitPrice` vẫn gửi bình
thường).

**Host phải sửa:**

| Cũ | Mới |
|---|---|
| `PromotionOrderItem(skuId = "SKU1", ...)` | `PromotionOrderItem(skuSourceId = "SKU1", ...)` |
| `PromotionSDK.updateOrderInfo(orderId, productId, skuId = "SKU1", ...)` | `PromotionSDK.updateOrderInfo(orderId, productId, skuSourceId = "SKU1", ...)` |

### Changed — **BREAKING**: đổi tên field của `PromotionAvailableService` và `PromotionServiceSelection`

Đồng nhất thuật ngữ với `PromotionOrderItem`/`ApplicableProductDto` — cả 2 nền tảng, cả 2 type public:

| Cũ | Mới |
|---|---|
| `serviceCode` | `productId` |
| `serviceName` | `productName` |
| `serviceType` | `skuSourceId` |

`iconUrl`/`voucherId` không đổi. Host đang dựng `PromotionAvailableService` hoặc đọc field của
`PromotionServiceSelection` (payload `onServiceSelected`) bằng tham số/thuộc tính có tên phải sửa
tên; dựng `PromotionAvailableService` theo vị trí (positional) thì không cần sửa vì thứ tự field
giữ nguyên.

Đổi kèm nội bộ (không phải public API nhưng cùng đường đi dữ liệu): `AvailableService` (core,
`promotionLogic`), `ServiceSelectorUiItem` (Android), `ServiceSelectorItem` (iOS).

### Changed — **BREAKING**: `updateContext` đổi tên thành `updateOrderInfo`, `orderItems` truyền phẳng

Đơn hàng hiện chỉ hỗ trợ **một** dòng sản phẩm, nên `orderItems: List<PromotionOrderItem>` (Android) /
`[PromotionOrderItem]` (iOS) bị bỏ khỏi chữ ký. Thay vào đó `updateOrderInfo` nhận trực tiếp các field
của `PromotionOrderItem` (`skuId`, `productId`, `productName`, `productCategory`, `quantity`,
`unitPrice` — đều tuỳ chọn); SDK tự bọc lại thành danh sách 1 phần tử (nếu có `skuId`) hoặc rỗng (nếu
không) trước khi ghi vào context nội bộ.

**Host phải sửa:**

| Cũ | Mới |
|---|---|
| `PromotionSDK.updateContext(orderId, orderValue, serviceCode, metaData, orderItems = listOf(PromotionOrderItem(skuId = "SKU1", quantity = 1, unitPrice = "500000")))` | `PromotionSDK.updateOrderInfo(orderId, orderValue, serviceCode, metaData, skuId = "SKU1", quantity = 1, unitPrice = "500000")` |
| `PromotionSDK.updateContext(orderId, orderValue)` (không có dòng sản phẩm) | `PromotionSDK.updateOrderInfo(orderId, orderValue)` — chỉ đổi tên hàm |

### Changed — **BREAKING**: bỏ tham số `serviceCode` khỏi `updateOrderInfo`

`updateOrderInfo` không còn nhận `serviceCode` ở cả 2 nền tảng. Host đang truyền `serviceCode = ...`
(Android) / `serviceCode: ...` (iOS) vào lời gọi này phải **xoá** tham số đó — không có thay thế.

Không ảnh hưởng `PromotionAvailableService.productId` (danh mục dịch vụ cho bottom sheet "Chọn dịch
vụ") hay `getVouchers`/`getVoucherDetail(serviceCode:)` (lọc voucher theo dịch vụ) — hai API khác,
vẫn giữ nguyên.

### Changed — **BREAKING**: `updateOrderInfo` dựng dòng sản phẩm theo `productId` thay vì `skuId`

Trước đây truyền `skuId` là điều kiện để SDK dựng `PromotionOrderItem` (bỏ trống → `orderItems` rỗng).
Nay điều kiện đó chuyển sang `productId`: có `productId` → dựng 1 phần tử (`skuId` bỏ trống thì rơi về
chuỗi rỗng thay vì làm mất cả item); không có `productId` → `orderItems` rỗng dù có truyền `skuId`.

### Changed — **BREAKING**: `orderId`/`productId` trong `updateOrderInfo` nay bắt buộc

Cả 2 nền tảng: `orderId` và `productId` đổi từ tuỳ chọn (`String?`/`String?`, default `nil`/`null`)
sang **bắt buộc** (`String`, không default, không nhận `null`). Thứ tự tham số đổi theo — `productId`
dời lên ngay sau `orderId` (vị trí thứ 2) để hai tham số bắt buộc đứng đầu. Vì `productId` giờ luôn có
giá trị, `updateOrderInfo` luôn dựng đúng **1** `PromotionOrderItem`; nhánh `orderItems` rỗng khi thiếu
`productId` không còn tồn tại.

**Host phải sửa:**

| Cũ | Mới |
|---|---|
| `PromotionSDK.updateOrderInfo(orderId, orderValue)` (không có dòng sản phẩm) | Phải thêm `productId`: `PromotionSDK.updateOrderInfo(orderId, productId, orderValue)` |
| `PromotionSDK.updateOrderInfo(orderId = ..., skuId = "SKU1", productId = "P1", ...)` | Đổi thành `PromotionSDK.updateOrderInfo(orderId = ..., productId = "P1", skuId = "SKU1", ...)` (Kotlin dùng named-arg thì thứ tự không bắt buộc; Swift/Java positional phải theo đúng thứ tự mới) |

**Host phải sửa:** nếu trước đây chỉ truyền `skuId` mà không truyền `productId` để lấy campaign theo
SKU, giờ phải truyền thêm `productId` — thiếu nó thì `skuId`/`productName`/`productCategory`/
`quantity`/`unitPrice` bị bỏ qua hoàn toàn, request chỉ còn nhận campaign cấp đơn.

### Changed — **BREAKING**: widget "Ưu đãi" tự mở màn "Chọn ưu đãi", bỏ `onOpenVoucherSelection`

Trước đây `PRMEndowView` chỉ bắn callback `onOpenVoucherSelection` khi user bấm — host phải tự dựng
`FragmentTransaction` để add `PromotionSDK.createChoosePromotionFragment(endowView)` vào container
của mình. Bước này lặp lại y hệt ở mọi host, và là chỗ dễ quên/làm sai (container id, back stack,
dedup double-tap).

Nay widget tự điều hướng: bấm vào widget → `PRMEndowView` tự resolve `FragmentActivity` từ `context`
rồi gọi `PromotionSDK.openChoosePromotion(activity, endowView)` nội bộ — cùng khuôn
`openMyPromotion`/`openPromotionDetail` (tự chọn `FragmentManager` qua `resolveFragmentManager`,
dedup theo tag, `addToBackStack`). Host không cần dòng nào cho việc này nữa; chỉ cần nhúng
`PRMEndowView` vào layout XML.

**Host phải sửa:**

| Cũ | Mới |
|---|---|
| `binding.endowView.onOpenVoucherSelection = { addFragment(PromotionSDK.createChoosePromotionFragment(binding.endowView)) }` | Xoá hẳn — widget tự mở màn, không cần wiring |
| `PromotionSDK.createChoosePromotionFragment(endowView): Fragment` | `PromotionSDK.openChoosePromotion(activity, endowView, containerViewId = null)` — chỉ cần gọi thẳng khi host muốn tự kích hoạt màn này từ nơi khác ngoài cú bấm mặc định của widget |

### Fixed — iOS: skeleton hàng tab-chip bị skeleton danh sách đè trên màn hình nhỏ

Hai phần skeleton của màn "Ưu đãi của tôi" đều neo vào **vùng tab thật** chứ không neo vào nhau: chip
cao 32 canh giữa vùng tab, còn skeleton danh sách bắt đầu ở `tabArea.bottom + 8`. Vùng tab lại chỉ còn
được giữ bởi constraint 50pt ở **priority 250** trong XIB — cặp constraint từng khoá cứng chiều cao nó
chết theo hai `PromotionTabView` mà `configTabViews()` gỡ ra để thay bằng scroll view ngang. Vùng tab
bị bóp xuống dưới 16pt là chip tràn xuống dưới mốc của danh sách, và danh sách nằm trên trong z-order
nên che mất chip.

Nay skeleton danh sách có **sàn cứng** `>= chipRow.bottom + 8` (ràng buộc bám vùng tab hạ xuống
`.defaultHigh` để giữ nguyên khoảng cách trên máy layout bình thường), hàng chip bám đỉnh overlay thay
vì canh giữa vùng tab, và vùng tab thật có sàn chiều cao required. Cách xếp dọc này khớp Android
(`prm_shimmer_my_promotion.xml`).

### Changed — **BREAKING**: bỏ `PromotionIntegrateManager`, luồng thanh toán về lõi dùng chung

Luồng `createRedemption → gặp INSUFFICIENT_BUDGET → validate lại → cập nhật widget` là **nghiệp vụ
đụng tiền**, nhưng nó nằm trong một class public **chỉ có ở Android**. iOS không hề có
`confirmRedemption` nào — host iOS phải tự gọi `PromotionSDKApi.createRedemption`. Kèm theo là hai
mapper thân giống hệt nhau (`appliedDiscountFor` bên Android ↔ `toEndowAppliedDiscount` ở lõi), một
CoroutineScope thứ ba cho một widget, và nghĩa vụ `clear()` mà host quên là rò.

Nay luồng này ở `EndowStore.confirmRedemption()` (`promotionLogic`), hai nền tảng chạy một đường và
có **6 test** ở `EndowStoreTest` (body-error, HTTP 422, lỗi khác, revalidate rỗng, đơn không voucher,
request mang đúng `expectedDiscount`).

**Host phải sửa:**

| Cũ | Mới |
|---|---|
| `PromotionIntegrateManager.create(endowView)` + `confirmRedemption(...)` + `clear()` | `endowView.confirmRedemption(onSuccess, onError)` — không còn class, không còn `clear()` |
| (iOS: không có) | `PromotionSDK.confirmRedemption(onSuccess:onError:)` |
| `com.ttcn.prm.ui.feature.endowview.EndowViewState` | `com.ttcn.promotionsdk.presentation.endow.EndowWidgetState` |
| `PRMEndowView.onVoucherItemClick` | Bỏ — nó khai báo rồi nhưng **SDK chưa từng gọi** (đã ghi "Đang hỏng" trong docs) |

`PRMEndowView.getCurrentState()` giữ nguyên, chỉ đổi kiểu trả về sang `EndowWidgetState`.


### Fixed — màn "Ưu đãi của tôi": tab nhảy ngược & danh sách bị xoá trắng (cả 2 nền tảng)

Hai lỗi trong `MyPromotionStore` (`promotionLogic`, dùng chung Android + iOS), cùng đến từ commit
thêm cache tab. Cả hai đều có test bắt đúng từ đầu nhưng bị commit trong tình trạng đỏ.

- **Tab sáng nhảy ngược dưới ngón tay user.** Store truyền `requestTabCode` vào
  `resolveActiveTab()`, mà rule đó xếp `selectedTab` của response **trên** tab client yêu cầu. Server
  echo lệch — nhận `tab=used` nhưng trả `selectedTab=all` — là tab sáng nhảy về "Tất cả" trong khi
  danh sách hiện ra lại là của "Đã dùng". Nay tab user vừa bấm thắng thẳng; `resolveActiveTab()` chỉ
  còn được hỏi ở lần load đầu, đúng vai trò "đáp xuống tab nào". Rule domain giữ nguyên, không đổi.
- **Kéo làm mới rớt mạng là mất sạch danh sách.** Nhánh `onFailure` dùng "tab này có cache không" để
  quyết định giữ hay xoá list. Nhưng cache chỉ được ghi khi có tab code, nên với host mà server
  **không trả `tabs[]`**, `tabCaches` rỗng suốt vòng đời màn → mọi lần load lại thất bại đều rơi vào
  nhánh xoá. Nay điều kiện là `keepCurrentListWhileLoading` — chính cái cờ gây ra chuyện "list đang
  hiện thuộc về tab khác", tức đúng câu hỏi cần trả lời.

Hành vi cố ý vẫn giữ: đổi sang tab **chưa có cache** mà API hỏng thì vẫn xoá list, để user không thấy
danh sách tab bên cạnh nằm dưới tab vừa bấm.

### Changed — **BREAKING**: chỉ `Entry` mới public, mọi thứ khác `internal` (cả 2 nền tảng)

Trước đây bề mặt public rò ra ngoài `entry` lúc nào không hay: 93 khai báo top-level của
`AndroidPromotionSDK` nằm ngoài package `entry` vẫn `public` — toàn bộ `ui/widget/**`
(`PRMButton`, `PRMSearchField`, `PRMTextView`…), `ui/base/**` (`PRMBaseFragment`, `PRMBaseActivity`,
`PRMBaseViewModel`), ~40 extension trong `ui/utils/**`, cả năm Fragment nghiệp vụ, và nhóm theme.
Host thấy hết trong autocomplete, dùng nhầm rồi vỡ ở bản sau. Bên iOS là 10 file `PromotionSDKUI/Theme/`.

Từ nay **`com.ttcn.prm.entry.**` (Android) / `iosPromotionSDK/Entry/**` (iOS) là bề mặt public duy
nhất**; mọi khai báo khác là `internal` / không `public`. Xem [PublicApi.md](./docs/common/PublicApi.md)
(có sẵn lệnh `grep` để CI hoặc người review kiểm lại).

**Host phải sửa:**

| Cũ | Mới |
|---|---|
| `com.ttcn.prm.ui.feature.endowview.PRMEndowView` (kể cả tag trong layout XML) | `com.ttcn.prm.ui.feature.endowview.PRMEndowView` |
| `com.ttcn.prm.ui.feature.PromotionIntegrateManager` | **Đã bỏ hẳn** — xem mục BREAKING ở trên (`endowView.confirmRedemption`) |
| `ChoosePromotionFragment.forEndowView(endowView)` | `PromotionSDK.createChoosePromotionFragment(endowView)` — trả `Fragment` trần |
| `com.ttcn.prm.ui.theme.PromotionSDKTheme` / `PromotionThemeJson` / `PromotionThemeDisplay` | `com.ttcn.prm.ui.theme.*` |
| `com.ttcn.prm.ui.theme.token.*` (6 token) | `com.ttcn.prm.ui.theme.token.*` |
| `com.ttcn.prm.entry.AppliedDiscount` | `com.ttcn.prm.ui.feature.endowview.AppliedDiscount` (về ở cạnh widget dùng nó) |
| Kế thừa `PRMBaseFragment` / `PRMBaseActivity` | Không còn — host tự viết base của mình (xem `androidApp/.../base/`) |
| `PromotionThemeDefaults` (iOS) | Không còn public — dùng `PromotionThemeDisplay.load()` |

Widget/extension/base/applier nội bộ **không có đường thay thế** và đó là chủ đích: chúng chưa bao
giờ là hợp đồng, chỉ tình cờ với tới được.

Kèm theo:
- `consumer-rules.pro` giữ `com.ttcn.prm.entry.**` — trước đó nó vẫn trỏ `com.ttcn.promotionsdk.ui.entry.**`,
  một package **không còn tồn tại**, nên bề mặt public thật ra đang **không** được R8 giữ.
- Layout demo `prm_fragment_payment_demo.xml` bị **gỡ khỏi SDK** (nó chỉ phục vụ app demo) và chuyển
  sang `androidApp` thành `fragment_payment_demo.xml`.
- App demo `androidApp` được sửa để chỉ dùng bề mặt host: có base class riêng
  (`app/base/AppBaseFragment` + `AppBaseActivity`), codec hex riêng (`DemoHex`), và màn "Theme
  preview" bỏ phần preview widget nội bộ (chỉ còn `PRMEndowView`).
- Hai chỗ **không khoá được** trên Android: `com.ttcn.prm.R` và `com.ttcn.prm.databinding.*` là class
  Java do AGP sinh nên luôn public. Chúng không nằm trong hợp đồng.

### Added — feature flag phơi ra host (cả 2 nền tảng)
- `PromotionSDK` có thêm 4 hàm để host **hỏi trước** trạng thái cờ, thay vì để user bấm rồi ăn toast
  PRM_MOB_021: `featureFlags()`, `isFeatureEnabled(feature)`, `isSdkEnabled()`,
  `refreshFeatureFlags(onComplete/completion:)`. Cùng tên và **cùng thứ tự** ở Android ↔ iOS.
  - Ba hàm đầu đọc **cache đồng bộ** (không gọi mạng); `refreshFeatureFlags` gọi server rồi trả
    snapshot mới trên **main thread**.
  - **Fail-open, không ném lỗi**: chưa `initialize()` hoặc chưa có cache → trả bật hết.
  - DTO public mới: `PromotionFeature` (enum) + `PromotionFeatureFlagsSnapshot` —
    `entry/api/PromotionFeatureModels.kt` ↔ `Entry/API/PromotionFeatureModels.swift`. Hằng chuỗi
    `PromotionFeatureFlag` và data class `PromotionFeatureFlags` của lõi **không** ra tới host
    (cùng lý do đã có `PromotionApiModels`).
  - **Tầng gác của SDK không đổi.** Đây là tầng tuỳ chọn; host bỏ qua thì kill-switch vẫn chạy đủ.
  - Xem [features/FeatureFlag.md §3](./docs/features/FeatureFlag.md) và
    [common/PublicApi.md §2](./docs/common/PublicApi.md).

### Changed — `onAvailabilityChanged` nay bắn cả `true`
- Trước đây callback này **chỉ** bắn `false`, và chỉ khi user đã bấm vào một điểm vào bị chặn — host
  ẩn entry point rồi thì không có đường hiện lại. Nay nó bắn ở 4 thời điểm với cả hai giá trị: nạp cờ
  xong sau `initialize`/login lại, mỗi lần `refreshFeatureFlags`, widget checkout đổi trạng thái, và
  khi user bấm mà bị chặn.
- Android còn thiếu so với iOS ở luồng widget — `PRMEndowView.applyFeatureFlag()` nay cũng báo host
  mỗi lần đổi trạng thái, đối xứng `PromotionSDKImpl.applyFlag`.
- **Tương thích:** thuần bổ sung, không đổi chữ ký. Host nào đang coi mọi lần gọi là "tắt SDK" thì
  phải đọc tham số `enabled` — hành vi cũ tương đương `if (!enabled)`.

### Changed — phát hành (Android)
- Thêm đích publish **JFrog Artifactory** bên cạnh `~/.m2`: repo khai một lần ở `build.gradle.kts`
  gốc cho cả `:promotionLogic` và `:AndroidPromotionSDK`, tự chọn repo release/snapshot theo hậu tố
  `SDK_VERSION`. Chạy `./scripts/build-android.sh --remote`. Xem
  [docs/android/Distribution.md](./docs/android/Distribution.md) §3.4.
  - Cấu hình đặt ở `~/.gradle/gradle.properties` (`artifactoryUrl` / `artifactoryUser` /
    `artifactoryPassword`) hoặc env `ARTIFACTORY_*` — thiếu thì repo không đăng ký, build vẫn chạy.
  - `groupId` tách thành property tập trung `SDK_GROUP` trong `gradle.properties` (giá trị **không
    đổi**: `com.ttcn.promotion`). Đổi giá trị này là breaking với host.
  - Host giờ phải khai repo Artifactory + credentials — [AndroidIntegrationGuide](./docs/AndroidIntegrationGuide.md) §3.1.

### Removed — ⚠️ BREAKING (bề mặt public)
- Bỏ hoàn toàn `customerId` khỏi SDK. Định danh khách BFF đã lấy từ JWT `sub`, không API nào
  còn gửi tham số này; consumer cuối cùng là feature flag (`userId` → Unleash) cũng đã gỡ.
  - `PromotionSessionConfig(customerId, …)` → `PromotionSessionConfig(accessToken, baseUrl, …)`
  - `initialize(context, customerId, accessToken, baseUrl, …)` → `initialize(context, accessToken, baseUrl, …)`
  - `initialize(customerId:accessToken:baseUrl:…)` → `initialize(accessToken:baseUrl:…)` (iOS)
  - `updateSession(customerId, accessToken, …)` → `updateSession(accessToken, …)` (cả 2 nền tảng)
  - `PromotionRequestContextProvider.getCustomerId()` bị xoá (còn 7 getter).
  - `FeatureFlagRequest` bỏ cả `userId` lẫn `sessionId` (hai trường luôn được gửi rỗng);
    `FeatureFlagRemoteDataSource.getFeatureFlags()` không còn tham số nào.
    ⚠️ **Đổi wire format** — body `POST feature-flag/list` giờ chỉ còn `{"properties":{}}`.
- **Hệ quả:** request feature flag không mang định danh khách → Unleash không rollout theo % user
  được, chỉ bật/tắt toàn bộ. Cần nối lại nguồn định danh khi vertical FeatureFlag triển khai.

## [1.0.0] — 2026-07-20

Bản phát hành ổn định đầu tiên. Từ đây bề mặt public tuân theo SemVer (thay đổi breaking → tăng major).

### Kiến trúc
- Kotlin Multiplatform: lõi Data + Domain dùng chung (`:promotionLogic`); UI native mỗi nền tảng
  (Android XML/MVI, iOS UIKit/MVVM).
- Bề mặt SDK đối xứng 1:1 Android ↔ iOS — xem [docs/common/InitParity.md](./docs/common/InitParity.md).

### Bề mặt công khai
- Entry `PromotionSDK`: `initialize` / `release` / `isInitialized` / `updateContext` /
  `configure(theme)` / `currentTheme` / `getCallback`.
- Màn hình: `openMyPromotion`, `openPromotionDetail`, widget checkout (`PRMEndowView` /
  `createEndowView`).
- Headless `PromotionSDKApi` (5 hàm): `getVouchers`, `findEligible`, `getVoucherDetail`,
  `validateDiscounts`, `createRedemption` — trả DTO + `PromotionApiResult`.
- Callback thống nhất 6 sự kiện (`PromotionSDKCallback`); theming qua `PromotionSDKTheme`.
- Feature flag **không** phơi ra host — mọi điểm vào tự gác qua `PromotionFeatureGate`.

### Phân phối
- Android: Maven (`com.ttcn.promotion:promotionSDK:1.0.0`) — xem [docs/android/Distribution.md](./docs/android/Distribution.md).
- iOS: `PRM.xcframework`.
