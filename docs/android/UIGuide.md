# UIGuide (Android) — UI Android

Hướng dẫn UI cho `:AndroidPromotionSDK`. Giao diện làm bằng **XML View + Data Binding + View Binding**,
kiến trúc **MVI**. Không dùng Compose — xem [ComposeGuide.md](../common/ComposeGuide.md).

> Nguồn code: `AndroidPromotionSDK/` (kéo từ `ttcn-promotion-android-sdk`).

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

**Khác với SDK cũ:** `:promotionSDK` **không** cần `kotlin-kapt` (Room đã bị loại bỏ), và **không**
chứa networking — nó gọi `:promotionLogic`.

---

## 2. Khởi tạo SDK

```kotlin
PromotionSDK.initialize(context, options)
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
và SDK tự map. Bề mặt đầy đủ: [PublicApi.md](../common/PublicApi.md).

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
> `:promotionLogic`, **không** dành cho host: host chỉ tích hợp `AndroidPromotionSDK` nên không có
> `core.*` trên compile classpath. Host tự dựng UI thì gọi `PromotionSDK.api` — xem
> [PublicApi.md](../common/PublicApi.md). Trong module này, UI dựng thẳng use case đơn lẻ và tự `runCatching`.

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

Host **hỏi trước được** để ẩn entry point của chính mình — `PromotionSDK.featureFlags()` /
`isFeatureEnabled(feature)` / `isSdkEnabled()` / `refreshFeatureFlags(onComplete)`. Đó là tầng
**tuỳ chọn**, không thay cho việc gác ở trên; xem [features/FeatureFlag.md §3](../features/FeatureFlag.md).

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
- Font tùy biến trong `res/font/`; màu/brand tham chiếu theme (xem [Theming.md](../common/Theming.md)).

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

---

## 11. Ảnh từ mạng & GIF

Mọi ảnh remote đi qua **một cửa**: `ui/utils/PRMImageExt.kt`
(`loadPromotionVoucherLogo` / `loadPromotionVoucherBanner`) → Glide. Không gọi `Glide.with(...)` rải rác
ở fragment/adapter.

> **LUẬT: mọi chỗ có ảnh phải hiển thị được GIF động — cả Android lẫn iOS.**
> Thêm một ô ảnh mới thì phải test bằng một URL GIF động, không chỉ PNG/JPEG.

Bên Android GIF động là **mặc định** của Glide (`GifDrawable`), nên thường không cần làm gì. Nhưng có hai
cái bẫy đã cắn thật:

1. **Đừng nhét ảnh Glide vào view tự snapshot drawable thành `Bitmap`.**
   `PRMCircleImageView.getBitmapFromDrawable()` vẽ drawable **một lần** vào một `Bitmap`, rồi `onDraw`
   chỉ vẽ lại snapshot đó bằng `BitmapShader` và **không gọi `super.onDraw()`**. `GifDrawable` tự
   `invalidateSelf()` mỗi frame nhưng snapshot không bao giờ được cập nhật → **GIF đứng ở frame 1**.
   Logo màn chi tiết từng bị đúng lỗi này. Cách đúng: `PRMImageView` (ImageView thường) + để Glide
   `CircleCrop` lo hình tròn — nó áp transformation cho **từng frame** qua `GifDrawableTransformation`,
   nên không cần mask ở tầng view. `PRMCircleImageView` giờ chỉ còn dùng cho **shimmer** (không có Glide).
2. **`.dontAnimate()` = tắt GIF**, không phải "tắt crossfade". Trong Glide 4/5 nó chỉ set
   `GifOptions.DISABLE_ANIMATION` (kiểm bằng `javap` trên `glide-5.0.5.aar`); crossfade do
   `.transition()` quyết định. Gọi nó ở nhánh cache là GIF đã cache hiện tĩnh còn GIF tải mới lại chạy.

Các ô ảnh hiện có và tình trạng GIF:

| Ô ảnh | View | GIF |
|---|---|---|
| `imgBanner` (chi tiết) | `PRMImageView` | ✅ |
| `circleLogo` (chi tiết) | `PRMImageView` + `CircleCrop` | ✅ |
| `imgVoucher` (card danh sách / chọn ưu đãi) | `PRMImageView` trong `CircleView` (shapeofview) | ✅ |
| `imgServiceIcon` (bottom sheet chọn dịch vụ) | `AppCompatImageView` | ✅ |

Phần đối chiếu với iOS (kích thước, placeholder, hạn mức RAM khi decode ảnh động, quy ước ảnh rỗng 1×1
của BFF) ở [ios/UIGuide.md §7.5](../ios/UIGuide.md).

---

## 12. Shimmer (skeleton loading)

> **LUẬT: skeleton phải khớp UI thật sẽ thay thế nó** — cùng kích thước, cùng mốc, cùng widget.
> Lệch số là màn hình "nhảy" một nấc lúc dữ liệu về. Và hai nền tảng phải khớp nhau, vì cả hai đều
> đang soi cùng một UI thật.

Cách kiểm: mở layout thật và layout shimmer cạnh nhau, đối chiếu **từng** con số. Những chỗ đã cắn:

| Chỗ | Shimmer từng khai | UI thật |
|---|---|---|
| Logo card chi tiết | `view_size_24` | **`view_size_44`** (`circleLogo`) |
| Hàng tab chi tiết | cao 36dp, không thụt lề | cao **48dp** (`TabLayout` tab-chữ) + thụt thêm `_10sdp` |
| Nội dung tab chi tiết | mấy dòng trần trên nền màn | **`PRMCardView` bo 16** căng tới nút (`fragment_content_detail_endow_prm.xml`) |
| Nút áp dụng | `tokenSizing56` | **`tokenSizing48`** (= `PRMCoreButtonSize.LARGE`) |
| Card list ưu đãi | `PRMCardView` (**có đổ bóng** — kế thừa `PRMShadowView`, mặc định `TokenShadowsCard`) | `androidx.cardview.widget.CardView` + **`cardElevation="0dp"`** (phẳng) |

Hai bài học rút ra:

1. **Dùng đúng widget của bản thật, đừng dùng widget "tương đương".** `PRMCardView` và `CardView` cùng
   bo góc nhưng khác đổ bóng.
2. **Đừng ghim chiều cao khi bản thật co theo nội dung.** Bên iOS
   `PromotionDetailShimmerView` từng ghim `card.height = 128` trong khi `VoucherCardView` thật cao theo
   nội dung (`expiryLabel.bottom = card.bottom - 20`) → nay để `dateBar` quyết định đáy card, y như thật.
   Tốt nhất là **lấy số từ chính bản thật** như `PRMPromotionCardShimmerCell` bên iOS đang làm
   (`PromotionCardView.estimatedHeight` + `PromotionCardView.Metrics`) — sửa card là shimmer tự theo.

Số chuẩn của shimmer màn chi tiết (đã đồng bộ 2 nền tảng, lấy từ UI thật):

**Số của iOS là chuẩn** cho hình học màn chi tiết (XIB + `VoucherCardView` dựng theo Figma); Android đã
được sửa theo. Dùng **dp cố định**, không dùng `sdp` — xem luật 1 ở §11.

| Mốc | Giá trị (cả UI thật lẫn shimmer, 2 nền tảng) |
|---|---|
| Banner | cao **173** |
| Card: lề ngang / đè đáy banner | **20** / **-32** |
| Logo | **44**, cách mép card 16, cách đỉnh card 20 |
| Tên brand ↔ logo | cách **12** (iOS `headerStack.spacing`; Android `view_size_8` — chưa gộp) |
| Tên ưu đãi | 2 dòng, dòng 1 cách logo **8**, dòng 2 cách dòng 1 **6** và ngắn hơn 60 |
| HSD | cách tên ưu đãi **8**, đáy cách đáy card **20** |
| Hàng tab | cách card **24**, lề **20** (không thụt thêm), cao 50 (iOS) / 48dp (Android) |
| Khe card nội dung ↔ nút | **16** |
| Nút | cao **48**, lề ngang **16** |

> Riêng **lề đáy của nút** cố ý KHÔNG đồng bộ: iOS dùng hằng số 34 = vùng home indicator, còn Android
> để `tokenSpacing12` và nhường phần đáy cho system inset (nav bar / gesture bar). Copy 34dp sang Android
> là cộng hai lần khoảng trống.
