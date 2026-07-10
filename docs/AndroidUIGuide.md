# AndroidUIGuide — UI Android

Hướng dẫn UI cho `:promotionUI` (Android). Giao diện làm bằng **XML View + Data Binding + View Binding**,
kiến trúc **MVI**. Không dùng Compose — xem [ComposeGuide.md](./ComposeGuide.md).

> Gộp từ `AndroidGuide.md` + `XMLViewGuide.md` của SDK Android gốc.
> Nguồn code: `ttcn-promotion-android-sdk/vds-promotion/src/main/java/com/ttcn/promotionsdk/ui/`.

---

## 1. Cấu hình nền tảng

| Mục | Giá trị |
|-----|---------|
| Loại module | `com.android.library` |
| Namespace | `com.ttcn.promotionsdk` |
| `minSdk` | 24 |
| Build features | `dataBinding = true`, `viewBinding = true` |
| Plugin | android-library, kotlin-android, kotlin-parcelize |

> `minSdk = 24` — không dùng API yêu cầu min cao hơn nếu không guard bằng `Build.VERSION.SDK_INT`.

**Khác với SDK cũ:** `:promotionUI` **không** cần `kotlin-kapt` (Room đã bị loại bỏ), và **không**
chứa networking — nó gọi `:promotionLogic`.

---

## 2. Khởi tạo SDK

```kotlin
PromotionSDK.init(context, options)
   └─ options.config.toCoreConfig()               // PromotionConfig (public) → PromotionSDKConfig (lõi)
   └─ PromotionContainer.init(context, config)    // androidMain của :promotionLogic
        ├─ AndroidContextHolder.set(applicationContext)
        ├─ isDebug ← ApplicationInfo.FLAG_DEBUGGABLE
        └─ dựng DI
   └─ ensureUiDiLoaded()                          // nạp ViewModelModule một lần
```

- Luôn dùng `applicationContext`; **không** giữ tham chiếu Activity tĩnh.
- Use case chỉ **gọi** được sau `init()`; dựng trước thì không ném, lỗi nổi lên ở `invoke()`.
- Hai chế độ: **UI mode** (host mở Fragment của SDK) và **headless mode** (host tự dựng UI, gọi
  `PromotionSDK.api`).

Host **không** thấy `PromotionSDKConfig` / `PromotionContainer` của lõi — nó truyền `PromotionConfig`
và SDK tự map. Bề mặt đầy đủ: [PublicApi.md](./PublicApi.md).

---

## 3. Activity / Fragment

- Kế thừa base có sẵn: `PRMBaseActivity`, `PRMBaseFragment`. Không tạo base mới khi base hiện tại đủ dùng.
- ViewModel lấy qua `PromotionViewModelFactory` / `ViewModelModule` (DI), không `new` thủ công.
- Quan sát state/effect trong vòng đời an toàn: `viewLifecycleOwner` + `repeatOnLifecycle(STARTED)`.
- Fragment **chỉ** render state và gửi `Action`; không chứa business logic.

---

## 4. MVI — State / Action / Effect

`PRMBaseViewModel<S, A, E>`:

- `uiState: StateFlow<S>` — nguồn sự thật duy nhất cho UI; cập nhật qua `setState { copy(...) }`.
- `uiEffect: SharedFlow<E>` — sự kiện một lần (điều hướng, toast, lỗi); phát qua `sendEffect(...)`.
- `handleAction(action: A)` — điểm vào duy nhất. UI **không** gọi business logic trực tiếp.
- `launch { }` — coroutine có sẵn `CoroutineExceptionHandler` → gọi `onError(throwable)`.

```kotlin
// Quan sát state
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state -> render(state) }
    }
}

// Quan sát effect (one-shot)
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiEffect.collect { effect -> handleEffect(effect) }
    }
}

// Gửi action
binding.swipeRefresh.setOnRefreshListener {
    viewModel.handleAction(MyPromotionAction.Refresh)
}
```

### ViewModel gọi lõi KMP

ViewModel dựng **thẳng use case đơn lẻ** (giống ViewModel bên iOS), không đi qua facade
`PromotionUseCases`. Use case **ném** `PromotionException` / `NetworkException`, nên bắt bằng
`runCatching` rồi map sang `errorCode`:

```kotlin
fun loadVouchers() = launch {
    setState { copy(isLoading = true) }
    runCatching { searchCustomerVouchersUseCase(request) }
        .onSuccess { r -> setState { copy(isLoading = false, vouchers = r?.content.orEmpty()) } }
        .onFailure { e ->
            setState { copy(isLoading = false) }
            sendEffect(MyPromotionEffect.ShowError(e.toErrorCode()))
        }
}
```

Use case được cấp qua `PromotionViewModelFactory` (xem `ui/di/`), lấy repository từ đồ thị mà
`PromotionContainer.initialize(...)` đã dựng.

> Chỉ facade `PromotionUseCases()` mới trả `PromotionResult` và **không ném**. Nó là type của
> `:promotionLogic`, **không** dành cho host: host chỉ tích hợp `AndroidPromotionUI` nên không có
> `core.*` trên compile classpath. Host tự dựng UI thì gọi `PromotionSDK.api` — xem
> [PublicApi.md](./PublicApi.md). Trong module này, UI dựng thẳng use case đơn lẻ và tự `runCatching`.

### Luồng checkout dùng `findEligible`, không phải `searchVouchers`

`ChoosePromotionViewModel` và `PRMEndowViewModel` gọi `FindEligibleCampaignsUseCase` — trả hai nhóm
`myOffers` (đã sở hữu) + `otherOffers` (campaign công khai). `searchVouchers` chỉ có nhóm đầu, dùng
cho màn "Ưu đãi của tôi" và màn Tìm kiếm.

Hai hệ quả:

- `findEligible` **chưa trả `isAutoApplied`** → voucher tự-áp-dụng không chạy ở checkout.
  Xem `TODO(auto-apply)` trong `PromotionUiMapper.kt`.
- `findEligible` **không nhận `keyword`** → ô tìm kiếm màn "Chọn ưu đãi" **chưa chạy**.

Khung tìm kiếm đã dựng theo đúng khuôn `SearchMyPromotionViewModel` (gõ mỗi ký tự → `QueryChanged` →
debounce 400ms → `search()`; `Search` chạy ngay; `ClearKeyword` reset), nhưng `search()` còn để trống:
xem `TODO(search)`. Cố tình **không** lọc trong bộ nhớ — lọc client chỉ đúng trên trang đầu (10 mục),
nên nó im lặng trả sai kết quả khi danh sách dài hơn một trang. Khi backend chốt trường `keyword`,
điền vào `search()` là xong; phần còn lại đã sẵn.

### Feature flag

Điều hướng phải đi qua `PromotionFeatureGate` — **object Kotlin trong `promotionLogic`**, dùng chung
với iOS, không phải một bản riêng của Android:

- `PromotionSDK.openMyPromotion()` gác bởi `canOpenVoucherList()`.
- `PRMBaseFragment.openPromotionDetail(voucherId)` gác bởi `canOpenVoucherDetail()` — dùng hàm này
  thay cho `addFragment(PromotionDetailFragment.newInstance(...))` để không màn nào quên gác.
- `PRMEndowView` tự ẩn nếu `canShowVoucherSelection()` trả `false`.

Cờ TẮT → hiện `R.string.prm_feature_disabled` (PRM_MOB_021) và không điều hướng.
**Không** gọi thẳng `PromotionFeatureFlagUseCases()` từ tầng UI; thêm màn mới thì thêm một hàm
`canOpen…` vào gate.

---

## 5. Resource & đặt tên

Layout đặt trong `res/layout/`.

| Loại | Quy ước tên | Ví dụ |
|------|-------------|-------|
| Layout Fragment | `prm_fragment_<feature>.xml` | `prm_fragment_my_promotion.xml` |
| Layout Activity | `prm_activity_<name>.xml` | |
| Item RecyclerView | `prm_item_<name>.xml` | `prm_item_voucher.xml` |
| Layout con/include | `prm_view_<name>.xml` | |

- Tiền tố **`prm_`** cho mọi resource của SDK, tránh trùng với host app.
- String đặt trong `res/values/strings.xml`, **không** hardcode chuỗi hiển thị.
- Kích thước responsive dùng `sdp-android` (`@dimen/_12sdp`), không hardcode `dp`.
- Font tùy biến trong `res/font/`; màu/brand tham chiếu theme (xem [Theming.md](./Theming.md)).

---

## 6. View Binding

```kotlin
class MyPromotionFragment : PRMBaseFragment(...) {
    private var _binding: PrmFragmentMyPromotionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(...): View {
        _binding = PrmFragmentMyPromotionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null   // BẮT BUỘC — tránh leak view
    }
}
```

---

## 7. RecyclerView & Adapter

- Adapter đặt trong `feature/.../adapter/`.
- Dùng `ListAdapter` + `DiffUtil.ItemCallback` (so sánh theo id), **không** `notifyDataSetChanged()`.
- Item model là UI item trong contract (vd `MyVoucherListItem`), **không** dùng thẳng DTO/domain model.
- Click chuyển lên ViewModel qua callback → ViewModel phát `Action`.

---

## 8. Thư viện UI (tái sử dụng, không thêm mới)

| Thư viện | Mục đích |
|----------|----------|
| `material` | Component Material |
| `constraintlayout` | Layout chính |
| `recyclerview` | Danh sách |
| `swiperefreshlayout` | Kéo làm mới |
| `glide` | Tải ảnh |
| `shimmer` (facebook) | Skeleton loading |
| `shapeofview` | View bo góc/shape đặc biệt |
| `sdp-android` | Kích thước responsive |

Không thêm thư viện UI mới nếu chưa được yêu cầu (AI_AGENT_RULES điều 6).

---

## 9. Custom View

- Đặt trong `utils/view/`, tiền tố `PRM` (vd `PRMEditText`, `PRMButton`, `PRMEndowView`).
- Attribute tùy biến qua `res/values/attrs.xml`.
- Tái sử dụng trước khi tạo mới.

---

## 10. Quy tắc riêng cho SDK

- **Không giữ tham chiếu Activity/Context tĩnh** gây leak.
- **Public API tối thiểu**, chỉ expose qua `entry/`. Thay đổi = breaking, phải cập nhật docs.
- **Logging**: dùng Timber. Không `Log.d` rải rác, không log token hay thông tin khách hàng.
  Log HTTP do `:promotionLogic` xử lý và chỉ bật ở `isDebug`.
- **ProGuard**: SDK có `consumer-rules.pro`. Class cần giữ qua reflection → bổ sung consumer rules.
  Model của kotlinx.serialization cần giữ `@Serializable` metadata.
- **Đa luồng**: coroutines cho IO; cập nhật UI trên main thread.
- **Permission**: hạn chế tối đa trong manifest SDK; chỉ khai báo thứ thật sự cần (INTERNET).
