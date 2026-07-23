# UIGuide (iOS) — UI iOS

Hướng dẫn UI cho `promotionSDK` (iOS). Giao diện làm bằng **UIKit (XIB)**, kiến trúc
**MVVM + Builder + Router**, ràng buộc View↔ViewModel bằng **callback thuần** (không framework reactive).

> Gộp từ `UIKitGuide.md` + `StateManagement.md` + `Architecture.md` của SDK iOS gốc.
> Nguồn code: `ttcn-promotion-ios-sdk/VDSPromotion`.
>
> **Cập nhật (2026-07-23) — đã gỡ Combine khỏi tầng UI.** Trước đó: RxSwift → Combine → nay là
> **callback thuần**. Lý do: sau khi nghiệp-vụ-trình-bày dồn về store dùng chung ở `promotionLogic`
> (đã tự lo debounce/paging/latest-wins), Combine ở tầng UI chỉ còn làm **đường ống** giữa callback
> của store (`watchState`) và UIKit — đếm thực tế: 0 `combineLatest`, 0 `debounce`, 0 `flatMap`;
> chỉ có `map`/`sink`/`eraseToAnyPublisher`. Bỏ đi thì ViewModel iOS **soi gương được Android**
> (`onState`↔`uiState`, `onEffect`↔`uiEffect`, `handleAction`↔`handleAction`).
>
> SDK vẫn **không link thư viện ngoài** nào (RxSwift đã gỡ từ trước — xem [Distribution (iOS) §3](./Distribution.md)).

---

## 1. Cấu hình nền tảng

| Mục | Giá trị |
|-----|---------|
| Ngôn ngữ | Swift 5.x |
| iOS tối thiểu | 13.0 |
| UI framework | UIKit (XIB-based) |
| Ràng buộc View↔VM | **Callback thuần** (`onState` / `onEffect` / `handleAction`) — không Combine, không Rx |
| Bất đồng bộ | **async/await** (`Task`) khi gọi thẳng use case; còn lại state đến từ store dùng chung |
| Bất đồng bộ lõi | `Task` gọi thẳng `suspend` Kotlin (async/await) |
| Tổ chức | Modular SPM (local packages) — **không** dependency ngoài |
| Linter | SwiftLint (`.swiftlint.yml`) |

**Khác với SDK cũ:** các package `Repository`, `PromotionLogic`, và phần networking của `CoreNetwork`
được thay bằng `PromotionLogic.framework` (Kotlin Multiplatform). `PRMDesignKit`, `PRMPromotionUI`, `PRMFoundation`
giữ nguyên. **RxSwift/RxCocoa/RxRelay đã bị gỡ** — tầng UI dùng Combine + async/await (có sẵn trong OS).

---

## 2. Pattern: MVVM + Builder + Router

Mỗi màn hình gồm bốn thành phần, kế thừa base trong `PromotionSDKUI/Base/MVVM/`:

| Thành phần | Base class (thật, trong code) | Trách nhiệm |
|-----------|-----------|-------------|
| **Builder** | `PRMBaseBuilder<VC, VM, R, Dependency>` | Lắp ráp VC + VM + Router, inject dependency |
| **Router** | `PRMBaseRouter<VC: UIViewController>` (adopt `PRMBaseRouterProtocol`) | Điều hướng: push/pop/present; giữ `viewController` + `navigator` (weak) |
| **ViewModel** | `PRMBaseViewModel<R: PRMBaseRouterProtocol>` | Bọc store dùng chung; phơi `onState`/`onEffect`/`handleAction` |
| **ViewController** | `PRMBaseViewController<VM>` | Load XIB, `setupUI()` + `bindViewModel()` |

> Toàn bộ base class mang tiền tố `PRM` (điều 3 [CodingStandards.md](../common/CodingStandards.md)) — đây là tên thật
> trong `PromotionSDKUI/Base/MVVM/`. Bản iOS cũ (`BaseViewController`, `SelectPromotionViewController`) đã được
> đổi tên khi kéo về repo này; đừng dùng tên cũ.

```swift
class PRMBaseViewController<VM>: UIViewController {
    let viewModel: VM

    // Nib mặc định = tên class, bundle = bundle chứa framework SDK.
    init(viewModel: VM, nibName: String? = nil, bundle: Bundle? = nil) { ... }

    override func viewDidLoad() {
        super.viewDidLoad()
        _ = PRMBundleSetup.once          // đăng ký SDK bundle (cho UIImage.sdk)
        navigationController?.navigationBar.isHidden = true
        setupUI()
        bindViewModel()
    }

    func setupUI() {}        // override để cấu hình view
    func bindViewModel() {}  // override để gán onState/onEffect
}
```

> XIB phải đặt **cùng tên class** (vd `ChoosePromotionViewController.xib`, `MyPromotionViewController.xib`).
> `init?(coder:)` bị đánh dấu `unavailable` — luôn khởi tạo qua Builder.

---

## 3. State management — callback thuần, soi gương Android

Mỗi ViewModel bọc một store ở `promotionLogic` và phơi đúng 3 thứ:

| iOS | Android | Ghi chú |
|---|---|---|
| `var onState: ((UiState) -> Void)?` | `uiState: StateFlow<UiState>` | Gán `onState` là **nhận ngay** state hiện tại (mô phỏng replay của StateFlow bằng `didSet`) |
| `var onEffect: ((Effect) -> Void)?` | `uiEffect: Flow<Effect>` | Một-lần, **không** replay (lỗi hiện rồi thôi) |
| `func handleAction(_:)` | `fun handleAction(action)` | Enum `Action` khai báo đúng những gì màn thật sự phát |

```swift
final class SearchMyPromotionViewModel: PRMBaseViewModel<SearchMyPromotionRouter> {
    struct UiState { var promotions: [MyPromotionCellViewModel] = []; var isLoading = false /* … */ }
    enum Action { case queryChanged(String), search, loadMore, selectPromotion(String) }
    enum Effect { case showError(String) }

    private(set) var uiState = UiState() { didSet { onState?(uiState) } }
    var onState: ((UiState) -> Void)? { didSet { onState?(uiState) } }   // replay khi gán
    var onEffect: ((Effect) -> Void)?

    private func bindStore() {                       // đối ứng Android bindStore()
        storeCancellable = observeStore(watch: { [store] in store.watchState(onEach: $0) }) { [weak self] state in
            self?.render(state); self?.handleError(state)
        }
    }
}
```

ViewController chỉ còn **một** `render(state)` cho cả màn — đối ứng `collectFlow(viewModel.uiState)` bên Android:

```swift
override func bindViewModel() {
    viewModel.onState  = { [weak self] in self?.render($0) }
    viewModel.onEffect = { [weak self] in self?.handle($0) }
    viewModel.start()          // màn nào cần kích load lần đầu
}
```

**Quy đổi những thứ trước đây làm bằng Combine:**

| Trước (Combine) | Nay | Ghi chú |
|---|---|---|
| `Input` (publisher/relay) | gọi thẳng `viewModel.handleAction(.x)` | bỏ hẳn lớp `Input`/`transform` |
| `Output` (struct publisher) | `UiState` (struct giá trị) + `onState` | bỏ `eraseToAnyPublisher` |
| `.sink { }.store(in:&cancellables)` | gán closure | không cần `Set<AnyCancellable>` |
| `UITextField.textPublisher` | `addTarget(_:action:for:.editingChanged)` | **khác biệt**: không phát giá trị đầu lúc bind như `.prepend` cũ |
| `PRMRefreshTableView.refreshPublisher` / `loadMorePublisher` | `onRefresh` / `onLoadMore` (closure, thêm mới ở PRMDesignKit) | publisher cũ vẫn còn để không phá API package |
| `.receive(on: DispatchQueue.main)` | không cần | `observeStore` đã hop main sẵn |
| `debounce` / `latest-wins` | **nằm ở store Kotlin** | dùng chung 2 nền tảng, không làm lại ở UI |

> Kỷ luật bắt buộc: closure `onState`/`onEffect` luôn `[weak self]` (y như `.sink` trước đây).

---

## 4. Gọi lõi Kotlin từ Swift

`:promotionLogic` xuất ra `PromotionLogic.framework`. Hàm `suspend` của Kotlin được Kotlin/Native
export sang Swift dưới dạng `async` (hoặc completion handler).

```swift
Task { @MainActor in
    do {
        let result = try await useCase.invoke(request: request)   // suspend Kotlin
        // cập nhật state trên main thread (đã @MainActor)
    } catch {
        // use case @Throws(PromotionException, NetworkException, CancellationException)
    }
}
```

### Gọi use case: async/await trực tiếp (không còn adapter Rx)

Trước đây ViewModel chờ `Single<T>`, nên có adapter `singleFromKotlin` (trong `PRMKotlinBridge`) bọc
`async` thành `Single`. **Adapter đó đã bị xoá.** ViewModel/Impl nay gọi thẳng trong `Task`:

```swift
private func performSearch(...) {
    let useCase = searchVouchersUseCase
    Task { @MainActor [weak self] in
        do {
            let result = try await useCase.invoke(request: request)
            guard let self, token == self.latestSearchToken else { return }   // "latest wins"
            // … cập nhật CurrentValueSubject …
        } catch {
            // lỗi → tắt loading, giữ state
        }
    }
}
```

Quy tắc:
- `Task { @MainActor }` thay `observeOn(MainScheduler.instance)` — callback về main thread.
- Cần "hủy request cũ khi có request mới" (thay `flatMapLatest`): giữ `Task` và `.cancel()` khi fetch
  mới, **kèm** một token/generation guard — vì Kotlin/Native **không** hủy coroutine khi hủy `Task`,
  nên vẫn phải bỏ response cũ bằng cách so token.
- `PRMKotlinBridge` giờ chỉ còn `boxed(_:)` (bọc `Int?`→`KotlinInt?`) và `toPromotionError(_:)`
  (bóc exception Kotlin trong `NSError`).

> Use case ném `@Throws(PromotionException, NetworkException, CancellationException)`; nhánh `catch`
> chuẩn hoá qua `toPromotionError(_:)` khi cần hiển thị.

---

## 5. Public API & "NSObject box"

Class công khai `PromotionSDK` **không** giữ trực tiếp type nội bộ:

```swift
public final class PromotionSDK {
    private let _impl: NSObject
    private var impl: PromotionSDKImpl { _impl as! PromotionSDKImpl }
}
```

Lý do: ngăn compiler của app host phải nạp module `PromotionLogic` (và các module SPM nội bộ) để suy
ra class layout — tránh crash đệ quy `deserializeClass`. Xem `ADR/0002-nsobject-box-public-facade.md`
ở repo iOS gốc.

Chữ ký public **chỉ** dùng type Foundation/UIKit. Không lộ type Kotlin, không lộ Combine (là chi tiết
nội bộ). RxSwift thì đã gỡ hẳn khỏi SDK.

---

## 6. Module SPM (`iosPromotionSDK/Packages/`)

| Module | Tầng | Trách nhiệm |
|--------|------|-------------|
| `PRMFoundation` | Foundation | Extension (gồm `UITextField.textPublisher` cho Combine), Logger, `SDKBundle`/`PRMAsset` |
| `PRMDesignKit` | Design system | Token (`Colors`, `Typography`, `Spacing`…) + component (`PRMButton`, `Shimmer`, `PRMRefreshTableView`…) |
| `PRMPromotionUI` | Feature UI | View nghiệp vụ: `PromotionCardView`, `CouponViews`, `PRMEndowView` |
| `PRMKotlinBridge` | Keo | `boxed(_:)` (`Int?`→`KotlinInt?`) + `toPromotionError(_:)` (bóc exception Kotlin). **Không nghiệp vụ.** |
| `PromotionSDKUI` | Facade | Public API + màn hình (MVVM), phụ thuộc mọi module qua `@_implementationOnly` |

Đã **xoá** khỏi bản KMP: `PromotionLogic` (Swift), `Repository`, `CoreNetwork`, `CoreDatabase` —
kéo theo Realm, Alamofire, SwiftyJSON, KeychainSwift. Và nay **cả RxSwift cũng đã bị gỡ**:
tầng UI dùng Combine + async/await (thành phần của iOS 13+). → **Không còn dependency ngoài nào** —
mọi thứ trong `PRM.xcframework` là code của SDK + hệ điều hành.

Nguyên tắc: phụ thuộc **một chiều**, tầng trên biết tầng dưới.

### Nơi đặt code mới

| Bạn muốn thêm... | Đặt ở |
|------------------|-------|
| Business rule / endpoint | **`:promotionLogic` (Kotlin)** — không phải Swift |
| Component UI tái dùng, generic | `PRMDesignKit` |
| Component UI gắn nghiệp vụ ưu đãi | `PRMPromotionUI` |
| Màn hình mới | `PromotionSDKUI` — bộ Builder/Router/ViewModel/ViewController |
| Public method cho đối tác | `PromotionSDK` / `PromotionSDKApi` — chỉ Foundation/UIKit type ở chữ ký |
| Extension/helper chung | `PRMFoundation` |

### Tên class đồng nhất với Android

`PromotionSDK`, `PromotionSDKCallback`, `PromotionSDKTheme`, `MyPromotionViewController`,
`ChoosePromotionViewController`, `PromotionDetailViewController`, `SearchMyPromotionViewController`,
`PRMEndowView`. Tên hàm cũng vậy: `openMyPromotion`, `openPromotionDetail`, `createEndowView`.

Riêng token theme của `PRMDesignKit` (`PRMButtonThemeToken`…) dùng prefix `PRM` — đó là design system
dùng chung, không thuộc bề mặt SDK.

---

## 7. Feature flag

Gate là **object Kotlin dùng chung với Android**, không phải một bản Swift riêng:

```swift
guard PromotionFeatureGate.shared.canOpenVoucherDetail() else { showDialog(); return }
```

| Việc | Gọi ở đâu |
|---|---|
| Mở màn "Chi tiết ưu đãi" | `BaseRouter.canOpenVoucherDetail()` — gate + popup PRM_MOB_021 |
| Mở màn "Ưu đãi của tôi" | `PromotionSDK.openMyPromotion` → `impl.canOpenVoucherList` |
| Hiện widget checkout | `PromotionSDKImpl.applyFlag()` |
| 5 hàm headless | Đã gác sẵn bên trong `PromotionUseCases` của lõi — Swift **không** gác lại |
| Nạp cờ lúc init | `PromotionSDKImpl.init` → `gate.refresh()` |
| Hỏi cờ từ host | *(không có — SDK không phơi API này)* |

`refresh()` là `suspend` bên Kotlin → Swift thấy `async throws`. Nó không ném lỗi nghiệp vụ (hỏng thì
giữ cache), nên `try? await gate.refresh()` là đúng.

**Không** gọi thẳng `PromotionFeatureFlagUseCases()` từ tầng UI. Thêm màn mới thì thêm một hàm
`canOpen…` vào `PromotionFeatureGate` bên Kotlin — cả hai nền tảng cùng được.

### Không phơi API hỏi cờ ra host

`PromotionSDKFeature` đã bị xoá, và **không có** enum thay thế. Host không cần biết cờ nào đang bật:
`openMyPromotion` / `openPromotionDetail` / widget đều tự gác qua `PromotionFeatureGate` của
`promotionLogic`, và báo host qua `onAvailabilityChanged(enabled:)` khi bị chặn.

> **Ràng buộc, đã kiểm chứng bằng compiler.** Kể cả khi muốn phơi ra, type Kotlin không thể xuất hiện
> trong API public: nó bị ghi vào `.swiftinterface` của framework, kéo theo `import PRMKotlinBridge`.
> App host chỉ có `PRM.xcframework`, không có module đó, nên build hỏng ngay:
>
> ```
> error: Unable to find module dependency: 'PRMKotlinBridge'
> ```
>
> `PromotionSDKImpl` thì được phép nhắc tới type Kotlin, vì nó import `PRMKotlinBridge` dạng
> `@_implementationOnly`. Đây cũng chính là lý do `PromotionSDKApi` phải map model Kotlin → DTO Swift
> chứ không trả thẳng — xem mục dưới.

### `PromotionSDKApi` — ranh giới, không phải use case

Tên cũ là `PromotionSDKUseCases`, gây hiểu lầm: nó **không** chứa nghiệp vụ. Gác cờ, bắt lỗi, chuẩn
hoá `errorCode` đều nằm trong `PromotionUseCases` của lõi (dùng chung với Android). Lớp này chỉ uỷ
quyền, rồi đổi model Kotlin sang DTO Swift và `PromotionResult` sang `PromotionApiResult`.

Nó tồn tại **vì ranh giới phân phối**, không phải vì khẩu vị. Cả hai nền tảng nay đều có mapper, và
hai file là song ánh — xem [PublicApi.md](../common/PublicApi.md):

| | Android | iOS |
|---|---|---|
| Khai báo | `implementation(projects.promotionLogic)` | `@_implementationOnly import PRMKotlinBridge` |
| Host thấy type lõi? | Không | Không |
| Cần mapper? | **Có** | **Có** |
| Nếu vẫn phơi type lõi | Host: `Unresolved reference` | Host: `Unable to find module dependency: 'PRMKotlinBridge'` |

Ràng buộc bên iOS **cứng hơn**: type Kotlin lọt vào chữ ký public bị ghi thẳng vào `.swiftinterface`,
nên hỏng ngay cả khi host chưa dùng tới nó. Android chỉ hỏng ở đúng chỗ host chạm vào.

Ba file API bên iOS đặt ở `PromotionSDKUI/Entry/API/`, đối ứng `ui/entry/api/` bên Android:
`PromotionSDKApi.swift`, `PromotionApiModels.swift`, `PromotionApiResult.swift`.

> **Cạm bẫy đã mất một buổi.** Đổi chữ ký public rồi dựng lại xcframework, app host **vẫn** compile
> theo chữ ký cũ: Xcode cache module nhị phân ở `SwiftExplicitPrecompiledModules/` và không tự dọn.
> Compiler báo lỗi kèm `note:` trỏ vào một chữ ký không còn tồn tại trong file interface bên cạnh.
> `build-xcframework.sh` nay tự xoá cache đó; build tay thì `⇧⌘K`.

---

## 8. Quy tắc

1. **Business logic không được viết bằng Swift.** Mọi nghiệp vụ mới thuộc `:promotionLogic`.
   `PromotionSDKApi` chỉ là lớp mỏng chuyển `PromotionResult` → `PromotionApiResult`.
2. ViewModel **không** gọi thẳng repository; đi qua use case của lõi KMP (`try await useCase.invoke`).
3. `Output` luôn là `AnyPublisher<_, Never>` đã `.receive(on: DispatchQueue.main)` — an toàn thread, không lỗi.
4. Mọi subscription `.store(in: &cancellables)`; use case one-shot gọi trong `Task { @MainActor }`.
5. Router giữ `viewController`/`navigator` bằng `weak` — tránh retain cycle.
6. Chữ ký public không lộ Kotlin/Combine type (và RxSwift đã gỡ hẳn khỏi SDK).
7. Chuỗi hiển thị thuộc iOS, **không** thuộc `:promotionLogic`. Ví dụ: lõi trả `minOrderValue` và
   `unmatchedRules`; câu "Đơn tối thiểu 1.000.000đ để áp dụng" do đây dựng.
8. Đổi kiến trúc/luồng dữ liệu → cập nhật file này + `Architecture.md`.
