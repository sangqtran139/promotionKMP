# HostCapabilities — gói năng lực do host cấp

Tài liệu này trả lời một câu hỏi hay bị trả lời sai ở repo này:

> *"Phần native của host lo DB / tracker. Làm sao `promotionLogic` xử lý riêng cho từng nền tảng?"*

Câu trả lời ngắn: **không dùng `expect/actual`.** Đó là cơ chế cho loại việc khác. Thứ cần ở đây là
một **port**: interface ở `commonMain`, hiện thực do **host** cấp vào lúc `initialize`.

Và tất cả các port đó được đóng gói thành **một** kiểu duy nhất — `PromotionHostServices` — chứ
không phải mỗi năng lực một tham số. Đó là điểm quan trọng nhất của thiết kế: thêm năng lực thứ ba
**không đụng một chữ ký public nào**.

## Mục lục

<!-- toc -->
- [1. Hai loại "phụ thuộc nền tảng" — đừng trộn](#1-hai-loại-phụ-thuộc-nền-tảng--đừng-trộn)
- [2. Gói `PromotionHostServices`](#2-gói-promotionhostservices)
  - [2.1. Đường đi — 4 tầng](#21-đường-đi--4-tầng)
  - [2.2. Vị trí file](#22-vị-trí-file)
- [3. Quy tắc thiết kế chữ ký (KMP + Swift interop)](#3-quy-tắc-thiết-kế-chữ-ký-kmp--swift-interop)
- [4. `tracker` — SDK bắn, host nhận](#4-tracker--sdk-bắn-host-nhận)
  - [4.1. Danh mục event](#41-danh-mục-event)
- [5. `storage` — dùng lại kho sẵn có của app (DB cũ)](#5-storage--dùng-lại-kho-sẵn-có-của-app-db-cũ)
- [6. Cổng chưa có: đọc dữ liệu nghiệp vụ của host](#6-cổng-chưa-có-đọc-dữ-liệu-nghiệp-vụ-của-host)
- [7. Checklist khi thêm một năng lực mới](#7-checklist-khi-thêm-một-năng-lực-mới)
- [8. Khi feature tách thành module Gradle](#8-khi-feature-tách-thành-module-gradle)
  - [8.1. Đồ thị module đích](#81-đồ-thị-module-đích)
  - [8.2. Ba chỗ phải sửa trước khi tách được](#82-ba-chỗ-phải-sửa-trước-khi-tách-được)
  - [8.3. Cái giá — đọc trước khi quyết](#83-cái-giá--đọc-trước-khi-quyết)
  - [8.4. Làm gì ngay bây giờ](#84-làm-gì-ngay-bây-giờ)
<!-- /toc -->

---

## 1. Hai loại "phụ thuộc nền tảng" — đừng trộn

| | **Loại A** — SDK tự làm, API OS khác nhau | **Loại B** — host **đã có sẵn** ở tầng native |
|---|---|---|
| Ví dụ trong repo | `SdkLock`, `ioDispatcher`, `createPreferences()`, `clearPlatformState()` | tracker của app, kho dữ liệu của app |
| Cơ chế | `expect/actual` | **port/adapter** (tài liệu này) |
| Ai viết hiện thực | SDK, hai bản | **Host**, một bản mỗi nền tảng |
| Số hiện thực có thể có | đúng 1 mỗi target | bao nhiêu app tích hợp thì bấy nhiêu |

Vì sao loại B **không** dùng được `expect/actual`:

1. `actual` nằm **trong** SDK → `:promotionLogic` phải biết Room/CoreData/Firebase. Kéo theo KSP,
   đúng thứ [StorageGuide §5.1](./StorageGuide.md) cấm vì nó ảnh hưởng target iOS và thời gian build.
2. Mỗi target chỉ có **một** `actual`. Hai app host dùng hai hệ tracking khác nhau là hết đường.
3. `actual` không thay thế được trong `commonTest` → mất khả năng test store.
4. Đảo chiều phụ thuộc: domain đi biết hạ tầng của nền tảng.

---

## 2. Gói `PromotionHostServices`

```kotlin
public data class PromotionHostServices(
    val tracker: PromotionTracker? = null,       // §4
    val storage: PromotionPreferences? = null,   // §5
)
```

Host bật đúng thứ mình cần, phần còn lại SDK tự lo:

```kotlin
// Android
PromotionSDK.initialize(
    context, tokenSource = AppTokenSource, baseUrl = BASE_URL,
    hostServices = PromotionHostServices(tracker = AppPromotionTracker),
)
```

```swift
// iOS
PromotionSDK.initialize(
    tokenSource: AppTokenSource.shared, baseUrl: baseUrl,
    hostServices: PromotionHostServices(tracker: AppPromotionTracker.shared)
)
```

**Không thuộc gói này:** `PromotionTokenSource` / `PromotionRequestContextProvider`. Chúng cũng là
cổng do host cấp, nhưng đứng ở `PromotionSessionConfig` vì token là **thông tin phiên** — gắn với
lần đăng nhập, đổi theo user. Gói này là **năng lực hạ tầng**, gắn với vòng đời app. Ranh giới đó
quyết định thứ gì đi vào đâu khi bạn thêm cổng mới.

### 2.1. Đường đi — 4 tầng

Khuôn này **đã chạy production** từ trước, ở `PromotionRequestContextProvider`. Thêm cổng mới thì
chép lại đúng nó, đừng phát minh cơ chế thứ hai.

```
┌─ Host native ────────────────────────────────────────────────────────────┐
│  Android: object của app : com.ttcn.prm.entry.PromotionTracker           │
│  iOS:     class của app  : PromotionKit.PromotionTracker                 │
└───────────────┬──────────────────────────────────────────────────────────┘
                │ PromotionHostServices(...)  →  PromotionSDKOptions.hostServices
┌───────────────▼──────────────────────────────────────────────────────────┐
│  Lớp entry SDK — kiểu public riêng của từng nền tảng + adapter           │
│  PromotionHostServices.toCore()  (Kotlin / Swift, cùng tên)              │
│  → để kiểu của promotionLogic KHÔNG lọt vào public API (PublicApi.md)    │
└───────────────┬──────────────────────────────────────────────────────────┘
                │ PromotionSDKConfig.hostServices
┌───────────────▼──────────────────────────────────────────────────────────┐
│  commonMain — host/PromotionHostServices.kt (gói) + HostModule (DI)      │
│  không cấu hình → hiện thực mặc định của SDK (no-op / kho nền tảng)      │
└───────────────┬──────────────────────────────────────────────────────────┘
                │ PromotionAnalytics (tracker) · LocalModule (storage)
┌───────────────▼──────────────────────────────────────────────────────────┐
│  Store / UseCase dùng chung — nơi thật sự gọi                            │
└──────────────────────────────────────────────────────────────────────────┘
```

### 2.2. Vị trí file

| Vai trò | File |
|---|---|
| Gói + các port + no-op | `promotionLogic/…/host/PromotionHostServices.kt`, `host/PromotionTracker.kt` |
| Đăng ký DI | `promotionLogic/…/host/HostModule.kt` — nạp **đầu tiên** trong `PromotionContainer.initialize` |
| Chọn kho theo `storage` | `promotionLogic/…/data/local/LocalModule.kt` |
| Facade + tiền tố + tham số dùng chung | `promotionLogic/…/common/PromotionAnalytics.kt` |
| Tên event của từng feature | cạnh store của feature — `presentation/<feature>/…Contract.kt` |
| Public Android + adapter | `AndroidPromotionSDK/…/entry/PromotionHostServices.kt` |
| Public iOS + adapter | `iosPromotionSDK/Entry/PromotionHostServices.swift` |
| Test | `promotionLogic/src/commonTest/…/PromotionHostServicesTest.kt` |

---

## 3. Quy tắc thiết kế chữ ký (KMP + Swift interop)

1. **Không `suspend`, không `Flow`** trên interface mà host phải hiện thực — dùng **callback**. Lý do
   đã ghi kỹ ở `PromotionRequestContextProvider.refreshAccessToken`: cơ chế của host là bất đồng bộ
   và nằm ở tầng native, chữ ký phải implement được bằng closure thường từ **cả Swift lẫn Java**.
2. Chỉ dùng primitive, `Map<String, String>`, hoặc data class **public** của SDK. Type `internal`
   lọt ra là vi phạm [PublicApi.md](./PublicApi.md); `Map<String, Any>` thì mỗi nền tảng ép kiểu một
   kiểu.
3. **Mặc định là hiện thực thật (no-op / kho nền tảng), không phải `null`.** Nhánh
   `if (tracker != null)` rải ở call-site là cách một trong hai nền tảng lặng lẽ quên bắn event.
4. Ghi rõ **hợp đồng thread**: lõi gọi từ thread nền, host phải non-blocking và thread-safe, và ở
   Swift thì **không** được `@MainActor`.
5. Ghi rõ **vòng đời**: SDK giữ object tới `release()`. Host trỏ vào singleton cấp app, không trỏ vào
   `Activity`/`Fragment`/`UIViewController` — nếu không là rò rỉ vĩnh viễn.
6. **Tên và thứ tự hàm trùng nhau tuyệt đối** giữa Kotlin và Swift, và ghi vào
   [InitParity.md](./InitParity.md) §2.

---

## 4. `tracker` — SDK bắn, host nhận

```kotlin
public interface PromotionTracker {
    public fun track(event: PromotionEvent)
}

public data class PromotionEvent(val name: String, val params: Map<String, String> = emptyMap())
```

Chiều đi **ngược** với `PromotionTokenSource`: ở đó SDK *hỏi* host, ở đây SDK *báo* host.

Điều quan trọng nhất: **điểm bắn nằm trong Store ở `promotionLogic`**, không nằm ở
Fragment/ViewController. Nhờ vậy Android và iOS phát ra cùng một tên event mà không ai phải đồng bộ
tay — nếu để mỗi tầng UI tự gọi tracker của mình thì hai nền tảng sẽ lệch tên và BI không gộp được
phễu.

Không cấp → `NoOpPromotionTracker`. Host ném → lõi nuốt kèm `promotionWarn`, **không** kéo đổ màn
hình (`PromotionAnalytics`). Chưa `initialize()` → bỏ qua im lặng.

### 4.1. Danh mục event

**Tên event thuộc về feature bắn nó**, nằm cạnh store của feature (`ChoosePromotionEvents`,
`PromotionDetailEvents`), **không** gom vào `common/`. Gom hết vào tầng nền thì tầng nền phải biết
danh sách feature — chiều phụ thuộc ngược, và là chỗ đầu tiên vỡ khi mỗi feature tách thành module
Gradle riêng. `PromotionAnalytics` chỉ nhận một chuỗi + một map nên nó **không biết feature nào tồn
tại**; feature thêm sau dùng được ngay mà không sửa gì ở tầng nền.

Cái thật sự dùng chung nằm ở `PromotionEvents` (`common/PromotionAnalytics.kt`): tiền tố
`PromotionEvents.PREFIX` = `prm_`, và tên tham số lặp ở nhiều feature (`voucher_id`…). Quy ước tên:
`prm_<màn>_<hành_động>`, snake_case. Tên event là **hợp đồng với BI** — thêm thì tự do, đổi/xoá thì
ghi `CHANGELOG.md`. Bảng dưới là danh mục đầy đủ (gom lại để đọc, không phải nơi khai báo).

| Event | Bắn khi | Params |
|---|---|---|
| `prm_choose_promotion_view` | Màn "Chọn ưu đãi" có dữ liệu để vẽ lần đầu — **một lần** cho mỗi vòng đời store (kéo-để-tải-lại và tìm kiếm **không** tính) | `my_count`, `other_count` |
| `prm_choose_promotion_search` | Một lượt tìm kiếm server-side trả về | `has_keyword`, `my_count`, `other_count` |
| `prm_choose_promotion_select` | Người dùng tick/bỏ tick một ưu đãi | `voucher_id`, `selected` |
| `prm_choose_promotion_apply_rejected` | Server từ chối ở lượt "Áp dụng" | `rejected_count` |
| `prm_promotion_detail_view` | Màn "Chi tiết ưu đãi" nạp xong dữ liệu (API hỏng thì **không** bắn) | `voucher_id`, `status` |

**Không event nào mang PII.** Cụ thể: không gửi từ khoá người dùng gõ (chỉ `has_keyword`), không gửi
`message` mà server soạn cho người dùng (chỉ `rejected_count`).

```kotlin
// Android — singleton CẤP APP, không phải Fragment
object AppPromotionTracker : PromotionTracker {
    override fun track(event: PromotionEvent) {
        firebaseAnalytics.logEvent(event.name, event.params.toBundle())
    }
}
```

```swift
// iOS — singleton CẤP APP, không phải ViewController
final class AppPromotionTracker: PromotionTracker {
    static let shared = AppPromotionTracker()
    func track(event: PromotionEvent) {
        Analytics.logEvent(event.name, parameters: event.params)
    }
}
```

---

## 5. `storage` — dùng lại kho sẵn có của app (DB cũ)

SDK có kho khoá–giá trị của riêng nó (`SharedPreferences` / `NSUserDefaults`, xem
[StorageGuide](./StorageGuide.md)). `hostServices.storage` cho host **thay** kho đó bằng kho của
mình — điển hình là **DB của bản SDK native cũ**, để dữ liệu không nằm rải ở hai nơi sau khi nâng
cấp.

Quyết định nằm ở **một** chỗ duy nhất, `LocalModule`:

```kotlin
single<PromotionPreferences> {
    get<PromotionHostServices>().storage ?: createPreferences()
}
```

Nhờ vậy cache cờ tính năng (`FeatureFlagLocalDataSource`) và theme đã lưu (tầng UI đọc qua
`PromotionContainer.preferences`) **không thể** lệch nhau về kho.

Hợp đồng — khác tracker ở chỗ **đồng bộ**:

- `getString` phải trả giá trị ngay; `putString` phải thấy được ở lượt đọc kế tiếp.
- Có thể bị gọi từ **thread nền**, và nằm trên đường dựng màn → phải thread-safe và **nhanh**. Bọc
  một truy vấn Room/CoreData đồng bộ ở đây là tự cắm một lần chặn vào mỗi lượt mở màn.
- `clear()` xoá **kho**. Nếu kho dùng chung với dữ liệu khác của app thì thu hẹp phạm vi xoá về đúng
  phần của SDK, đừng xoá cả bảng.
- SDK **không** ghi token hay dữ liệu nhạy cảm vào đây → kho không cần mã hoá.

> Đây là kịch bản "SDK lưu dữ liệu **của nó**, nhờ kho của host". Khác hẳn §6.

---

## 6. Cổng chưa có: đọc dữ liệu nghiệp vụ của host

Khi SDK cần **dữ liệu của app** (lịch sử đơn, profile khách đã nằm trong DB của host), đó là một cổng
khác — và quy tắc quan trọng nhất là **đặt tên hàm theo nghiệp vụ SDK cần, không theo cơ chế lưu trữ
của host**:

```kotlin
// ĐÚNG — hẹp, nói về nghiệp vụ
public interface PromotionHostDataSource {
    public fun loadRecentOrders(limit: Int, onResult: (List<PromotionHostOrder>) -> Unit)
}

// SAI — SDK phụ thuộc schema của host, host đổi bảng là SDK vỡ
public interface PromotionHostDataSource {
    public fun query(sql: String, onResult: (List<Map<String, String>>) -> Unit)
}
```

Cổng đó phải được **bọc lại** thành repository trước khi use case chạm vào, để domain không biết tới
khái niệm "host":

```
host/PromotionHostDataSource   (host hiện thực)
        ▲ adapter
data/repository/HostOrderRepositoryImpl  ──implements──▶  domain/repository/HostOrderRepository
                                                                  ▲
                                                         domain/usecase → Store
```

> **Trạng thái:** thiết kế đã chốt, **chưa có code**. Đừng đọc §6 thành API đang tồn tại. Khi làm,
> nó vào `PromotionHostServices` như một field thứ ba — không đổi chữ ký public nào.

---

## 7. Checklist khi thêm một năng lực mới

1. Xác định đúng loại (§1). Loại A → `expect/actual`, dừng ở đây.
2. Xác định đúng chỗ (§2): năng lực hạ tầng → `PromotionHostServices`; thông tin phiên →
   `PromotionSessionConfig`.
3. Interface + kiểu dữ liệu + hiện thực mặc định ở `promotionLogic/…/host/`.
4. Thêm **một field** (default `null`) vào `PromotionHostServices`, và đăng ký vào `HostModule` với
   fallback là hiện thực mặc định.
5. Thêm field tương ứng vào `PromotionHostServices` public + adapter ở **cả hai** lớp entry. Chữ ký
   `initialize` / `PromotionSDKOptions` **không đổi**.
6. Test ở `commonTest`: host cấp / host không cấp / chưa `initialize` / host ném / dựng lại sau
   `clear()`.
7. Cập nhật [InitParity.md](./InitParity.md) §2, file này, và `CHANGELOG.md`.
8. Chạy `scripts/check-public-api.sh`.

---

## 8. Khi feature tách thành module Gradle

Câu hỏi thực tế: *"sau này nhiều feature, mỗi feature một module — thì `PromotionHostServices` đặt ở
đâu để tái sử dụng được cho tất cả?"*

Trả lời: **ở module thấp nhất, module không phụ thuộc vào gì cả.**

### 8.1. Đồ thị module đích

```
:prm-core-api        ← PromotionHostServices, PromotionTracker, PromotionPreferences,
                       PRMStore, PromotionCancellable, PromotionException, config types.
                       KHÔNG phụ thuộc gì (kể cả Ktor).
        ▲
:prm-core            ← SdkDi/PromotionContainer, Ktor client, SettingsPreferences,
                       PromotionAnalytics, PromotionLog.        api(:prm-core-api)
        ▲
:prm-feature-choose  :prm-feature-detail  :prm-feature-widget  …
                       data + domain + presentation của ĐÚNG MỘT feature.
                       implementation(:prm-core). KHÔNG feature nào biết feature nào.
        ▲
:promotionLogic      ← aggregator: gom feature, đăng ký DI, export symbol cho iOS.
                       Giữ nguyên tên để toạ độ maven và XCFramework của host không đổi.
```

**`PromotionHostServices` phải ở `:prm-core-api`, không phải `:prm-core`.** Lý do cụ thể: `:prm-core`
kéo Ktor. Một feature chỉ cần bắn tracking mà phải kéo cả HTTP client là sai — và đó chính là kiểu
phụ thuộc thừa khiến việc tách module không mang lại lợi ích gì.

### 8.2. Ba chỗ phải sửa trước khi tách được

**1. Danh sách DI module đang hardcode ở nền.** `PromotionContainer.initialize` liệt kê tay
`HostModule, NetworkModule, LocalModule, RepositoryModule, UseCaseModule`. Feature ở module riêng
thì `:prm-core` không được phép nhắc tên chúng. Đảo lại: nền định nghĩa khuôn, aggregator nạp danh
sách.

```kotlin
// :prm-core
public interface PRMFeatureModule { public fun register() }

public fun PromotionContainer.initialize(
    config: PromotionSDKConfig,
    features: List<PRMFeatureModule> = emptyList(),
)

// :prm-feature-choose
public object ChoosePromotionFeature : PRMFeatureModule { override fun register() { … } }

// :promotionLogic (aggregator) — CHỖ DUY NHẤT biết danh sách feature
initialize(config, features = listOf(ChoosePromotionFeature, PromotionDetailFeature, …))
```

**2. Tên event.** Đã xong (§4.1): mỗi feature giữ danh mục của mình, `PromotionAnalytics` chỉ nhận
`String` + `Map`. Đây đúng là chỗ đầu tiên từng vi phạm — xem `scripts/check-core-feature-boundary.sh`.

**3. Bố cục thư mục đang là *layer-first*.** Hiện tại một feature nằm rải ở `data/dto/`,
`data/repository/`, `domain/usecase/`, `presentation/<feature>/`. Muốn tách module thì phải gom về
*feature-first* (`feature/choosepromotion/{data,domain,presentation}/`). **Đây là phần tốn công
nhất** — và phải làm trước khi viết dòng Gradle nào.

### 8.3. Cái giá — đọc trước khi quyết

SDK này phát hành **một AAR + một XCFramework**. Nhiều module Gradle không tự động thành nhiều
artifact, nên:

| | Chi phí |
|---|---|
| **iOS** | XCFramework dựng từ **một** framework binary. Mỗi feature module phải được `export(projects.prmFeatureX)` trong khối `binaries.framework`. Quên một dòng `export` = Swift mất sạch type của feature đó, **build vẫn xanh ở Kotlin**. |
| **Android** | Gradle **không** gộp nhiều module thành một AAR. Hoặc publish N artifact (host phải kéo đủ, cần BOM), hoặc giữ một AAR và chấp nhận module chỉ là ranh giới compile-time. |
| **Build time** | Nhiều module KMP = nhiều lần link Kotlin/Native. Thường **chậm hơn**, không nhanh hơn, cho tới khi đủ lớn. |

Vì vậy khuyến nghị: **tách module khi có lý do thật** — nhịp phát hành riêng, đội sở hữu riêng, hoặc
feature phải bật/tắt được lúc build. Không tách chỉ vì "cho gọn".

### 8.4. Làm gì ngay bây giờ

Giữ một module, nhưng **giữ đúng các đường nối** để lúc tách chỉ là việc cơ học:

1. `scripts/check-core-feature-boundary.sh` — gác chiều nền → feature, chạy cùng các gate khác.
   Vi phạm kiểu này **không làm build đỏ**, nên không gác là sẽ tích tụ.
2. Feature mới: danh mục event, contract, store, use case riêng của nó nằm cùng chỗ với nhau. Thứ
   dùng chung mới được đi xuống `common/`/`host/`.
3. Feature **không** import feature khác. Cần dùng chung → đẩy xuống nền.
4. Năng lực host mới luôn vào `PromotionHostServices` (§7), không thành tham số riêng — đó là thứ
   giữ cho chữ ký public không phình theo số feature.
