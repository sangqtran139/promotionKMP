# XMLViewGuide — Hướng dẫn XML View

Đây là **hướng dẫn UI chính thức** của TTCN Promotion SDK. Toàn bộ giao diện làm bằng **XML View**
kết hợp **Data Binding** và **View Binding** (project **không** dùng Compose — xem `ComposeGuide.md`).

---

## 1. Cấu hình

Trong `vds-promotion/build.gradle.kts`:

```kotlin
buildFeatures {
    dataBinding = true
    viewBinding = true
}
```

- **View Binding**: dùng cho đa số layout — truy cập view an toàn null, không cần `findViewById`.
- **Data Binding**: dùng khi cần binding biểu thức/observable trực tiếp trong XML (`<layout>` wrapper).

---

## 2. Resource & đặt tên

Layout đặt trong `vds-promotion/src/main/res/layout/`.

| Loại | Quy ước tên | Ví dụ |
|------|-------------|-------|
| Layout Fragment | `prm_fragment_<feature>.xml` | `prm_fragment_my_promotion.xml` |
| Layout Activity | `prm_activity_<name>.xml` | |
| Item RecyclerView | `prm_item_<name>.xml` | `prm_item_voucher.xml` |
| Layout con/include | `prm_view_<name>.xml` | |
| id của view | camelCase hoặc snake_case nhất quán theo file hiện có | `rvVouchers`, `tvTitle` |

- Dùng tiền tố **`prm_`** cho resource của SDK để tránh trùng với host app.
- String đặt trong `res/values/strings.xml`, **không** hardcode chuỗi hiển thị trong code/XML.
- Kích thước responsive dùng `sdp-android`: `@dimen/_12sdp`, `@dimen/_8sdp`… thay vì hardcode `dp`.
- Font tùy biến trong `res/font/`; màu/brand tham chiếu theme (`ui/theme/`).

---

## 3. Sử dụng View Binding trong Fragment

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
        _binding = null   // tránh leak view sau khi Fragment view bị huỷ
    }
}
```

**Bắt buộc** gán `_binding = null` trong `onDestroyView()` để tránh memory leak.
Ưu tiên kế thừa `PRMBaseFragment` và tuân theo mẫu binding đã có trong các feature hiện tại.

---

## 4. RecyclerView & Adapter

- Adapter đặt trong `ui/feature/.../adapter/`.
- Dùng `ListAdapter` + `DiffUtil.ItemCallback` cho danh sách động (so sánh theo id) thay vì `notifyDataSetChanged()`.
- Item model là các UI item trong contract (vd `MyVoucherListItem`), **không** dùng trực tiếp DTO/domain trong adapter.
- Sự kiện click chuyển lên ViewModel qua callback → ViewModel phát `Action`.

---

## 5. Liên kết UI ↔ MVI

UI **chỉ** render state và gửi action:

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

- `render(state)`: cập nhật visibility, text, list… từ một `State` duy nhất.
- `handleEffect(effect)`: điều hướng, hiển thị lỗi/toast — sự kiện một lần.
- Không đặt business logic trong Fragment.

---

## 6. Thư viện UI đang dùng (tái sử dụng, không thêm mới)

| Thư viện | Mục đích |
|----------|----------|
| `material` | Component Material |
| `constraintlayout` | Layout chính |
| `recyclerview` | Danh sách |
| `swiperefreshlayout` | Kéo làm mới |
| `glide` | Tải ảnh |
| `shimmer` (facebook) | Hiệu ứng loading skeleton |
| `shapeofview` | View bo góc/shape đặc biệt |
| `sdp-android` | Kích thước responsive |

> Cần UI mới → ưu tiên dùng các thư viện trên + custom view trong `ui/utils/view/` (vd `PRMEditText`).
> Không thêm thư viện UI mới nếu chưa được yêu cầu (AI_AGENT_RULES điều 4).

---

## 7. Custom View

- Đặt trong `ui/utils/view/`, tiền tố `PRM` (vd `PRMEditText`).
- Hỗ trợ attribute tùy biến qua `res/values/attrs.xml` nếu cần.
- Tái sử dụng trước khi tạo mới (AI_AGENT_RULES điều 6).
