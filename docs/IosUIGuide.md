# IosUIGuide — UI iOS

Hướng dẫn UI cho `promotionUI` (iOS). Giao diện làm bằng **UIKit (XIB)**, kiến trúc
**MVVM + Builder + Router**, reactive bằng **RxSwift**.

> Gộp từ `UIKitGuide.md` + `StateManagement.md` + `Architecture.md` của SDK iOS gốc.
> Nguồn code: `ttcn-promotion-ios-sdk/VDSPromotion`.

---

## 1. Cấu hình nền tảng

| Mục | Giá trị |
|-----|---------|
| Ngôn ngữ | Swift 5.x |
| iOS tối thiểu | 13.0 |
| UI framework | UIKit (XIB-based) |
| Reactive | RxSwift / RxCocoa / RxRelay `5.1.1` |
| Tổ chức | Modular SPM (local packages) |
| Linter | SwiftLint (`.swiftlint.yml`) |

**Khác với SDK cũ:** các package `Repository`, `PromotionLogic`, và phần networking của `CoreNetwork`
được thay bằng `PromotionLogic.framework` (Kotlin Multiplatform). `CoreUI`, `PromotionUI`, `Utility`
giữ nguyên.

---

## 2. Pattern: MVVM + Builder + Router

Mỗi màn hình gồm bốn thành phần, kế thừa base trong `VDSPromotion/Base/MVVM/`:

| Thành phần | Base class | Trách nhiệm |
|-----------|-----------|-------------|
| **Builder** | `BaseBuilder<VC, VM, R, Dependency>` | Lắp ráp VC + VM + Router, inject dependency |
| **Router** | `BaseRouter<VC>` | Điều hướng: push/pop/present; giữ `viewController` + `navigator` (weak) |
| **ViewModel** | `BaseViewModel<R>` + `ViewModelType` | Logic, biến đổi `Input → Output` |
| **ViewController** | `BaseViewController<VM>` | Load XIB, `setupUI()` + `bindViewModel()` |

```swift
class BaseViewController<VM>: UIViewController {
    let viewModel: VM
    let disposeBag = DisposeBag()

    override func viewDidLoad() {
        super.viewDidLoad()
        _ = VDSBundleSetup.once          // đăng ký SDK bundle (cho UIImage.sdk)
        navigationController?.navigationBar.isHidden = true
        setupUI()
        bindViewModel()
    }

    func setupUI() {}        // override để cấu hình view
    func bindViewModel() {}  // override để bind Rx
}
```

> XIB phải đặt **cùng tên class** (vd `SelectPromotionViewController.xib`).
> `init?(coder:)` bị đánh dấu `unavailable` — luôn khởi tạo qua Builder.

---

## 3. State management — RxSwift

| Loại | Dùng khi | Đặc tính |
|------|----------|----------|
| `Single<T>` | Một lần request mạng/use case | Phát đúng 1 success **hoặc** 1 error |
| `Observable<T>` | Stream sự kiện nội bộ ViewModel | Có thể lỗi; cần quản lý thread |
| `Driver<T>` | Bind ra UI | Không lỗi, main thread, share replay |
| `BehaviorRelay<T>` | Giữ **state hiện tại** | Đọc `.value`, ghi `.accept()` |
| `PublishRelay<T>` | Sự kiện UI (tap, scroll) | Không giữ giá trị, không lỗi |

```swift
protocol ViewModelType {
    associatedtype Input
    associatedtype Output
    func transform(input: Input) -> Output
}
```

- `Input` = nguồn sự kiện do **View** sở hữu (text ô search, tap chọn, scroll đáy).
- `Output` = **`Driver`** để View bind an toàn.
- Mọi subscription đặt trong `transform(...)` và `.disposed(by: disposeBag)`.

Không dùng Combine.

---

## 4. Gọi lõi Kotlin từ Swift

`:promotionLogic` xuất ra `PromotionLogic.framework`. Hàm `suspend` của Kotlin được Kotlin/Native
export sang Swift dưới dạng `async` (hoặc completion handler).

```swift
Task {
    let result = try await PromotionContainer.shared.useCases.searchVouchers(request: request)
    // result là PromotionResult — Success hoặc Failure
}
```

### Cầu nối sang RxSwift

ViewModel hiện tại chờ `Single<T>`. Bọc lời gọi async thành `Single`:

```swift
func searchVouchers(_ request: SearchCustomerVouchersRequest) -> Single<SearchCustomerVouchersResult> {
    Single.create { single in
        let task = Task {
            do {
                let result = try await PromotionContainer.shared.useCases.searchVouchers(request: request)
                switch onEnum(of: result) {
                case .success(let s): single(.success(s.data))
                case .failure(let f): single(.error(VDSPromotionError.from(f)))
                }
            } catch {
                single(.error(error))
            }
        }
        return Disposables.create { task.cancel() }
    }
}
```

Đặt lớp cầu nối này ở một nơi duy nhất — **không** rải `Task { }` khắp ViewModel.

> `PromotionUseCases` không ném lỗi nghiệp vụ; lỗi nằm trong `PromotionResult.Failure`.
> `try` chỉ để bắt `CancellationException`.

---

## 5. Public API & "NSObject box"

Class công khai `VDSPromotion` **không** giữ trực tiếp type nội bộ:

```swift
public final class VDSPromotion {
    private let _impl: NSObject
    private var impl: VDSPromotionImpl { _impl as! VDSPromotionImpl }
}
```

Lý do: ngăn compiler của app host phải nạp module `PromotionLogic`/`RxSwift` để suy ra class layout —
tránh crash đệ quy `deserializeClass`. Xem `ADR/0002-nsobject-box-public-facade.md` ở repo iOS gốc.

Chữ ký public **chỉ** dùng type Foundation/UIKit. Không lộ RxSwift, không lộ type Kotlin.

---

## 6. Module SPM (`iosPromotionUI/Packages/`)

| Module | Tầng | Trách nhiệm |
|--------|------|-------------|
| `Utility` | Foundation | Extension, Logger, `SDKBundle`/`VDSAsset`, re-export RxSwift |
| `CoreUI` | Design system | Token (`Colors`, `Typography`, `Spacing`…) + component (`VDSButton`, `Shimmer`…) |
| `PromotionUI` | Feature UI | View nghiệp vụ: `PromotionCardView`, `CouponViews`, `PRMEndowView` |
| `PromotionKit` | Keo | Adapter `async → Single`, ánh xạ lỗi Kotlin → `PromotionError`. **Không nghiệp vụ.** |
| `PromotionSDKUI` | Facade | Public API + màn hình (MVVM), phụ thuộc mọi module qua `@_implementationOnly` |

Đã **xoá** khỏi bản KMP: `PromotionLogic` (Swift), `Repository`, `CoreNetwork`, `CoreDatabase` —
kéo theo Realm, Alamofire, SwiftyJSON, KeychainSwift. Dependency ngoài duy nhất còn lại: **RxSwift**.

Nguyên tắc: phụ thuộc **một chiều**, tầng trên biết tầng dưới.

### Nơi đặt code mới

| Bạn muốn thêm... | Đặt ở |
|------------------|-------|
| Business rule / endpoint | **`:promotionLogic` (Kotlin)** — không phải Swift |
| Component UI tái dùng, generic | `CoreUI` |
| Component UI gắn nghiệp vụ ưu đãi | `PromotionUI` |
| Màn hình mới | `PromotionSDKUI` — bộ Builder/Router/ViewModel/ViewController |
| Public method cho đối tác | `PromotionSDK` / `PromotionSDKApi` — chỉ Foundation/UIKit type ở chữ ký |
| Extension/helper chung | `Utility` |

### Tên class đồng nhất với Android

`PromotionSDK`, `PromotionSDKCallback`, `PromotionSDKTheme`, `MyPromotionViewController`,
`ChoosePromotionViewController`, `PromotionDetailViewController`, `SearchMyPromotionViewController`,
`PRMEndowView`. Tên hàm cũng vậy: `openMyPromotion`, `openPromotionDetail`, `createEndowView`.

Riêng token theme của `CoreUI` (`VDSPromotionButtonToken`…) giữ prefix `VDS` — đó là design system
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
`promotionLogic`, và báo host qua `vdsPromotion(_:didUpdateAvailability:)` khi bị chặn.

> **Ràng buộc, đã kiểm chứng bằng compiler.** Kể cả khi muốn phơi ra, type Kotlin không thể xuất hiện
> trong API public: nó bị ghi vào `.swiftinterface` của framework, kéo theo `import PromotionKit`.
> App host chỉ có `PromotionSDKUI.xcframework`, không có module đó, nên build hỏng ngay:
>
> ```
> error: Unable to find module dependency: 'PromotionKit'
> ```
>
> `PromotionSDKImpl` thì được phép nhắc tới type Kotlin, vì nó import `PromotionKit` dạng
> `@_implementationOnly`. Đây cũng chính là lý do `PromotionSDKApi` phải map model Kotlin → DTO Swift
> chứ không trả thẳng — xem mục dưới.

### `PromotionSDKApi` — ranh giới, không phải use case

Tên cũ là `PromotionSDKUseCases`, gây hiểu lầm: nó **không** chứa nghiệp vụ. Gác cờ, bắt lỗi, chuẩn
hoá `errorCode` đều nằm trong `PromotionUseCases` của lõi (dùng chung với Android). Lớp này chỉ uỷ
quyền, rồi đổi model Kotlin sang DTO Swift và `PromotionResult` sang `PromotionApiResult`.

Nó tồn tại **vì ranh giới phân phối**, không phải vì khẩu vị. Cả hai nền tảng nay đều có mapper, và
hai file là song ánh — xem [PublicApi.md](./PublicApi.md):

| | Android | iOS |
|---|---|---|
| Khai báo | `implementation(projects.promotionLogic)` | `@_implementationOnly import PromotionKit` |
| Host thấy type lõi? | Không | Không |
| Cần mapper? | **Có** | **Có** |
| Nếu vẫn phơi type lõi | Host: `Unresolved reference` | Host: `Unable to find module dependency: 'PromotionKit'` |

Ràng buộc bên iOS **cứng hơn**: type Kotlin lọt vào chữ ký public bị ghi thẳng vào `.swiftinterface`,
nên hỏng ngay cả khi host chưa dùng tới nó. Android chỉ hỏng ở đúng chỗ host chạm vào.

Ba file API bên iOS đặt ở `PromotionSDK/Entry/API/`, đối ứng `ui/entry/api/` bên Android:
`PromotionSDKApi.swift`, `PromotionApiModels.swift`, `PromotionApiResult.swift`.

> **Cạm bẫy đã mất một buổi.** Đổi chữ ký public rồi dựng lại xcframework, app host **vẫn** compile
> theo chữ ký cũ: Xcode cache module nhị phân ở `SwiftExplicitPrecompiledModules/` và không tự dọn.
> Compiler báo lỗi kèm `note:` trỏ vào một chữ ký không còn tồn tại trong file interface bên cạnh.
> `build-xcframework.sh` nay tự xoá cache đó; build tay thì `⇧⌘K`.

---

## 8. Quy tắc

1. **Business logic không được viết bằng Swift.** Mọi nghiệp vụ mới thuộc `:promotionLogic`.
   `VDSPromotionUseCases` chỉ là lớp mỏng chuyển `PromotionResult` → `Result`/`Single`.
2. ViewModel **không** gọi thẳng repository; đi qua use case của lõi KMP.
3. `Output` luôn là `Driver` — an toàn thread, không lỗi.
4. Mọi subscription `.disposed(by: disposeBag)`.
5. Router giữ `viewController`/`navigator` bằng `weak` — tránh retain cycle.
6. Chữ ký public không lộ RxSwift/Kotlin type.
7. Chuỗi hiển thị thuộc iOS, **không** thuộc `:promotionLogic`. Ví dụ: lõi trả `minOrderValue` và
   `unmatchedRules`; câu "Đơn tối thiểu 1.000.000đ để áp dụng" do đây dựng.
8. Đổi kiến trúc/luồng dữ liệu → cập nhật file này + `Architecture.md`.
