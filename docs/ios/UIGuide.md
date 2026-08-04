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
| `PRMDesignKit` | Design system | Token (`Colors`, `Typography`, `Spacing`…) + component (`PRMButton`, `Shimmer`, `PRMRefreshTableView`, `PRMMarqueeLabel`…) |
| `PRMPromotionUI` | Feature UI | View nghiệp vụ: `PromotionCardView`, `CouponViews`, `PRMEndowView` |
| `PRMKotlinBridge` | Keo | `boxed(_:)` (`Int?`→`KotlinInt?`) + `toPromotionError(_:)` (bóc exception Kotlin). **Không nghiệp vụ.** |
| `PromotionSDKUI` | Facade | Public API + màn hình (MVVM), phụ thuộc mọi module qua `@_implementationOnly` |

Đã **xoá** khỏi bản KMP: `PromotionLogic` (Swift), `Repository`, `CoreNetwork`, `CoreDatabase` —
kéo theo Realm, Alamofire, SwiftyJSON, KeychainSwift. Và nay **cả RxSwift cũng đã bị gỡ**:
tầng UI dùng Combine + async/await (thành phần của iOS 13+). → **Không còn dependency ngoài nào** —
mọi thứ trong `Promotion.xcframework` là code của SDK + hệ điều hành.

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
| Mở màn "Chi tiết ưu đãi" | `BaseRouter.canOpenVoucherDetail()` — gate + toast PRM_MOB_021 |
| Mở màn "Ưu đãi của tôi" | `PromotionSDK.openMyPromotion` → `impl.canOpenVoucherList` |
| Hiện widget checkout | `PromotionSDKImpl.applyFlag()` |
| 5 hàm headless | Đã gác sẵn bên trong `PromotionUseCases` của lõi — Swift **không** gác lại |
| Nạp cờ lúc init | `PromotionSDKImpl.init` → `gate.refresh()` |
| Hỏi cờ từ host | `PromotionSDK.featureFlags()` / `isFeatureEnabled(_:)` / `isSdkEnabled()` / `refreshFeatureFlags(completion:)` — tầng **tuỳ chọn**, xem [features/FeatureFlag.md §3](../features/FeatureFlag.md) |

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
> App host chỉ có `Promotion.xcframework`, không có module đó, nên build hỏng ngay:
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

## 7.5. Ảnh từ mạng (`UIImageView.setImage`)

> **LUẬT: mọi chỗ có ảnh phải hiển thị được GIF động — cả iOS lẫn Android.** Thêm ô ảnh mới thì test
> bằng URL GIF động, không chỉ PNG/JPEG. Bẫy phía Android xem
> [android/UIGuide.md §11](../android/UIGuide.md) (view snapshot drawable thành `Bitmap` là chết GIF).

Toàn bộ ảnh remote của SDK (logo card, banner chi tiết, icon service selector) đi qua một cửa duy nhất:
`PRMFoundation/Extension/UIImageView+Remote.swift` — `URLSession` + cache RAM (`RemoteImageCache`),
tự cancel request cũ khi cell tái sử dụng. **Không** dùng thư viện ngoài (Kingfisher/SDWebImage) —
xem §1.

| Hạng mục | iOS | Android (đối chiếu) |
|---|---|---|
| Loader | `URLSession` + `NSCache` | Glide |
| **GIF/APNG động** | ✅ `UIImage.prmDecoded(from:)` (ImageIO) | ✅ Glide sẵn có |
| Cache đĩa | phó mặc `URLCache.shared` + header `Cache-Control` của server | ép `DiskCacheStrategy.ALL` |
| Giảm kích thước khi decode | chỉ ảnh động, theo hạn mức RAM (dưới) | `.override()` mọi ảnh |

> Cache đĩa **cố ý** không làm giống nhau: Android ép `DiskCacheStrategy.ALL` (còn ảnh khi offline),
> iOS tôn trọng header của server qua `URLCache.shared`. SDK không được cấu hình lại `URLCache.shared`
> — đó là cache dùng chung của app host. Hệ quả: offline, Android còn ảnh cũ, iOS về nền xám.
>
> Bên Android **không** dùng `.dontAnimate()` ở nhánh cache-probe. Trong Glide 4/5 hàm đó chỉ set
> `GifOptions.DISABLE_ANIMATION` (đã kiểm bằng `javap` trên `glide-5.0.5.aar`), **không** liên quan
> crossfade — crossfade do `.transition()` quyết định. Gọi nó thì GIF đã cache hiện tĩnh 1 frame còn
> GIF tải mới lại chạy: cùng một logo mà lần đầu chạy, vào lại thì đứng.

### Kích thước & cách fill — phải trùng số giữa hai nền tảng

| Ảnh | Kích thước (cả 2 bên) | iOS | Android |
|---|---|---|---|
| Logo card danh sách (My/Choose) | **48** | `iconContainer` 48×48, radius 24, `scaleAspectFill` | `avatar_container`/`imgVoucher` `@dimen/view_size_48` trong `CircleView` + Glide `.circleCrop()` |
| Banner màn chi tiết | **173** | XIB `bannerImageView` height 173, `scaleAspectFill` | `@dimen/prm_detail_banner_height` = 173dp, `centerCrop` |
| Logo màn chi tiết | **44** | `logoImageView` radius 22, `scaleAspectFill` | `@dimen/view_size_44` + `.circleCrop()` |
| Icon service selector | **48**, **tròn** | 48×48, `scaleAspectFill`, `cornerRadius = 24` | `@dimen/view_size_48` + `.circleCrop()` |
| Nền placeholder | **#E9E9E9** (`tokenDark10`) | `UIImageView.remotePlaceholderColor` | `prm_bg_image_placeholder` / `prm_bg_image_placeholder_circle` |
| Transition khi ảnh lên | **không có** | gán thẳng `image` | **không** `.transition(withCrossFade())` |

Hai luật rút ra từ lần lệch trước:

1. **Ảnh dùng dp/pt cố định, KHÔNG dùng `sdp`.** `sdp` scale theo bề rộng màn hình nên chỉ trùng số
   của iOS ở đúng một cỡ máy: `_147sdp` = 147dp @sw300, 176 @sw360, 191 @sw390, **294 @sw600 (tablet)**;
   `_37sdp` = 37/44.4/48.1/74. iOS không có cơ chế tương ứng → dùng `sdp` là bảo đảm lệch.
2. **Fill phải là crop, không phải fit.** `.circleCrop()`/`centerCrop` bên Android ⇔ `scaleAspectFill`
   bên iOS. `scaleAspectFit` làm logo tỉ lệ ngang bị letterbox, lọt giữa vòng tròn trong khi Android
   crop kín.
3. **Bo tròn phải khai ở CẢ hai bên.** Bên Android cái tròn đến "miễn phí" từ `.circleCrop()` của
   `loadPromotionVoucherLogo`, còn iOS phải tự set `cornerRadius` — quên là Android tròn, iOS vuông.
4. **Placeholder phải cùng HÌNH với ảnh load xong.** Glide **không** áp transformation lên
   `placeholder`/`error`, nên ô tròn phải dùng `prm_bg_image_placeholder_circle`; dùng bản chữ nhật thì
   hiện ô xám vuông rồi nhảy thành tròn (rõ nhất ở service selector — view phẳng, không có `CircleView`
   che giúp). Bên iOS `backgroundColor` tự bị `cornerRadius` bo nên không cần drawable riêng.
5. **Không transition.** Fade 300 ms của `withCrossFade()` cộng vào thời gian tải khiến Android "lên
   chậm" hơn iOS thấy rõ. Muốn có fade thì thêm ở cả hai bên (`UIView.transition`), đừng bật một bên.

### Ảnh động decode 2 chặng

Dựng ảnh động phải giải nén **mọi** frame trước khi vẽ được gì. Đo trên Mac (máy thật chậm hơn 2–4 lần):

| GIF | frames | dung lượng | frame đầu | toàn bộ frame |
|---|---|---|---|---|
| 200×200 | 51 | 92 KB | 0,2 ms | 4,6 ms |
| 764×781 | 90 | 287 KB | 7,4 ms | **247 ms** |
| 295×274 | 249 | **7,3 MB** | 5,5 ms | **190 ms** |

Gộp một chặng thì iOS hiện ảnh muộn hơn Android đúng bằng cột cuối, vì Glide vẽ frame đầu rồi decode
dần các frame sau theo nhịp animation. Nên loader chia 2 chặng:

1. `UIImage.prmQuickDecoded(from:)` → frame đầu, gán ngay.
2. `UIImage.prmDecoded(from:)` ở background → thay bằng ảnh động, cache lại.

Frame đầu decode với **cùng hạn mức kích thước** như chặng 2 để lúc thay không bị "nét rồi mờ". Chặng 1
của ảnh động **không** ghi cache (ghi thì cache giữ ảnh tĩnh, view sau mở ra sẽ không bao giờ chạy).

Cột "dung lượng" mới là phần lớn thời gian chờ lần đầu: ảnh động **không thể vẽ trước khi tải xong cả
file**, 7,3 MB cho một cái logo thì mạng yếu là chờ vài giây — chỗ này chỉ backend giảm được.

> **Mọi lần gán ảnh phải đối chiếu `currentImageURL`.** Cancel task không đủ: chặng 2 chạy **sau khi**
> request đã xong (`cancel()` lúc đó là no-op) và mất thêm vài trăm ms — đủ để cell được gán URL khác,
> rồi chặng 2 ghi ảnh của URL cũ lên. Đây là lý do có `applyLoadedImage(_:if:)`.

**Ảnh động giữ mọi frame trong RAM** (khác Glide: stream frame theo nhịp vẽ), nên
`UIImage+Animated.swift` áp hạn mức `kAnimatedBitmapBudget` = 32MB/ảnh: vượt hạn mức thì mỗi frame
được decode nhỏ lại qua `CGImageSourceCreateThumbnailAtIndex`. Đo thực tế: GIF 90 frame @764×781
(204MB nguyên bản) → 31MB; GIF 200×200 nhỏ thì decode nguyên bản. Sửa hằng số này là sửa đúng một chỗ.

**Ảnh 1×1 = ảnh rỗng.** BFF ảnh promotion trả **HTTP 200 + PNG 1×1 trong suốt** khi record không có
ảnh thật (không phải 404), ví dụ:

```
GET http://…/promotion-vtm-bff/images/<uuid>.png/preview
→ 200, Content-Type: image/png, Content-Length: 70, PNG 1×1 RGBA(0,0,0,0)
```

Decode "thành công" nhưng vẽ ra thì trong suốt → `prmIsRenderable` chặn lại để giữ nền xám
placeholder, và **không** cache (để ảnh thật lên là hiện ngay).

Android phải chặn **cùng luật đó**, bằng `PRMEmptyImageTransformation` (`ui/utils/`) chèn trước
`CircleCrop`. Không chặn thì Glide vẫn vẽ ô trong suốt, và trong `CircleView` (shapeofview mask bằng
`PorterDuff.DST_IN`/`DST_OUT`) vùng trong suốt hiện ra **màu ĐEN** — đúng lỗi "Android đen, iOS xám".
Phải chèn *trước* `CircleCrop` vì sau khi crop thì ảnh 1×1 đã bị phóng lên bằng khung, không còn nhận
ra được. Kèm theo đó `circleCrop()` phải viết tay lại thành
`downsample(CENTER_INSIDE).transform(guard, CircleCrop())` — đúng những gì `circleCrop()` làm bên trong.
Khác biệt còn lại (chấp nhận): Android cache ảnh rỗng đó theo `DiskCacheStrategy.ALL`, iOS thì không.

**Mọi nhánh lỗi đều ra cùng một ô xám**, nên loader bắt buộc log `[PRMRemoteImage]` kèm lý do
(network error / HTTP status / decode fail / ảnh rỗng) — chỉ ở bản DEBUG. Khi "ảnh không hiện", đọc log
này trước, đừng đoán. Riêng ảnh trên host HTTP: app host phải tự khai ngoại lệ ATS trong `Info.plist`
(app demo khai sẵn cho `125.235.38.229`), Android tương ứng cần `usesCleartextTraffic`.

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
