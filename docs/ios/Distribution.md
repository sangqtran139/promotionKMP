# Distribution (iOS) — XCFramework

**Không đi qua Maven.** Host iOS nhận thẳng **một** `PRM.xcframework`; kênh phát hành là
xcframework rời (SPM/CocoaPods đóng gói quanh nó). Hai nền tảng có hai kênh phát hành khác nhau — bình
thường, không phải thiếu sót.

> Phát hành **Android** (Maven) là kênh riêng — xem [../android/Distribution.md](../android/Distribution.md).

Một lệnh, đối xứng với Android:

```bash
./scripts/build-ios.sh              # dựng XCFramework → build app demo (simulator)
./scripts/build-ios.sh --skip-app   # chỉ dựng PRM.xcframework
./scripts/build-ios.sh --run        # build xong cài + mở trên simulator
```

Chuỗi phụ thuộc mà script ép đúng thứ tự:

```
:promotionLogic (Kotlin) ──gradle──▶ PromotionLogic.xcframework   (lõi, static)
             └─ link tĩnh vào ─────▶ PRM.xcframework   (UI, Swift)
                                              └─ iosApp link + embed
```

Bước 1 do `iosPromotionSDK/scripts/build-xcframework.sh` lo (nó tự gọi Gradle dựng lõi rồi copy vào
`iosPromotionSDK/Frameworks/` **trước khi** archive Swift — dùng header cũ thì lỗi hiện ra tận
`SwiftCompile` với "cannot find … in scope", rất khó lần).

> **Scheme `iosApp` phải là shared scheme** (`xcshareddata/xcschemes`, đã commit). Để trong
> `xcuserdata` thì chỉ máy của người tạo mới thấy — máy khác clone về, `xcodebuild -scheme iosApp`
> không tìm ra. Và `-derivedDataPath` bắt buộc đi kèm `-scheme`, không dùng được với `-target`.

## 1. Mỗi bản phát hành phải **lưu dSYM lại**

Target framework đặt `STRIP_STYLE = non-global` (Release), nên binary giao cho host **không còn
local symbol**. Đây là chủ ý: 30.500 local symbol — 8.680 trong đó là `kfun:`/`ktype:` của
Kotlin/Native link tĩnh vào — làm `__LINKEDIT` phình 2,6MB trên tổng 9,5MB của slice device. Strip
xong slice device còn 7,3MB, và các global symbol host cần để link vẫn nguyên vẹn (đã kiểm bằng cách
link thật một host giả lập vào framework đã strip).

Cái giá: **crash report chỉ symbolicate được bằng dSYM.** Không có nó thì stack trace chỉ còn địa
chỉ trần — kể cả phần Kotlin.

`build-xcframework.sh` đặt dSYM **cạnh** xcframework:

```
iosPromotionSDK/build/
├── PRM.xcframework          ← giao cho host
└── PromotionSDKUI.framework.dSYM       ← GIỮ LẠI, đừng để rơi
```

> ⚠️ Thư mục `build/` bị `rm -rf` ở **đầu** mỗi lần chạy script. dSYM không được archive đi nơi khác
> thì lần build sau là mất vĩnh viễn, và mọi crash report của bản đã phát hành thành vô dụng —
> không dựng lại được, vì UUID của bản build mới sẽ khác.

Vì sao để cạnh chứ không nhét vào trong bằng `create-xcframework -debug-symbols` (cách "đúng sách"):
riêng dSYM device đã **26MB**, cộng bản simulator thì gói phân phối phồng từ ~35MB lên hơn 100MB.
Host không cần dSYM để build — chỉ người giữ bản phát hành cần, lúc đọc crash report. Nên nó là
**artifact của quy trình release**, không phải thứ gửi cho host.

Kiểm tra dSYM đúng với binary đang phát hành (hai UUID phải trùng):

```bash
dwarfdump --uuid iosPromotionSDK/build/PromotionSDKUI.framework.dSYM/Contents/Resources/DWARF/PromotionSDKUI
dwarfdump --uuid iosPromotionSDK/build/PRM.xcframework/ios-arm64/PromotionSDKUI.framework/PromotionSDKUI
```

## 2. Slice — host không phải khai gì

XCFramework có hai slice, cùng cách phân phối như bản `VDSPromotionSDK.xcframework` cũ:

| Slice | Dùng khi nào |
|---|---|
| `ios-arm64` | Bản lên App Store — **chỉ slice này ship** |
| `ios-arm64_x86_64-simulator` | Dev của app host chạy simulator (cả Apple Silicon lẫn Mac Intel) |

Slice simulator **không** ship, nhưng vẫn bắt buộc phải có: thiếu nó thì dev bên host bấm Run với
simulator là ăn lỗi link *"building for iOS Simulator, but linking in object file built for iOS"*.

> **Đã cân nhắc bỏ `x86_64`** (simulator trên Mac Intel) để cắt ~9,5MB slice simulator. Không làm,
> vì một xcframework **không thể tiêm build setting vào project host**: bỏ `x86_64` thì app host
> **buộc** phải tự khai `EXCLUDED_ARCHS[sdk=iphonesimulator*] = x86_64` — nếu không, CI build kiểu
> `xcodebuild -sdk iphonesimulator` (không kèm `-destination`) sẽ fail *"Unable to find module
> dependency"*. Đẩy một dòng cấu hình bắt buộc sang phía host không đáng đổi lấy 9,5MB, nhất là khi
> ba tối ưu còn lại (§6, asset/strip/abi.json) đã giảm gói mà host không phải làm gì.

## 3. Đóng gói: host **không phải cài thêm gì** — cơ chế và ràng buộc

Bất biến của SDK iOS: host kéo **đúng một** `PRM.xcframework`, `import PRM`,
xong. Không khai RxSwift, không thêm SPM package, không chép resource bundle. Ba cơ chế giữ bất biến
này (khác Android — nơi androidx **vẫn** rò ra public API, xem §5.1):

1. **Mọi dependency link tĩnh vào trong framework — và nay gần như không còn dependency ngoài.**
   Các local package (`PRMFoundation`, `PRMDesignKit`, `PRMPromotionUI`, `PRMKotlinBridge`) là static
   library; khi archive target `PromotionSDKUI` (một framework, `BUILD_LIBRARY_FOR_DISTRIBUTION = YES`)
   chúng bị nhồi thẳng vào binary. Lõi Kotlin `PromotionLogic.xcframework` cũng gộp vào qua
   `binaryTarget`. **RxSwift đã được gỡ hoàn toàn** (tầng UI dùng Combine + async/await của iOS 13+),
   nên không còn thư viện reactive bên thứ ba nào trong gói — đã kiểm bằng `nm`: 0 symbol RxSwift trong
   binary. → Không có framework động nào phải nhúng kèm. `build-xcframework.sh` in danh sách framework
   động ở cuối; kỳ vọng là *"(không có — mọi dependency đã link tĩnh)"*.

2. **Public interface chỉ chạm UIKit/Foundation.** `Entry/PromotionSDK.swift` không phơi bất kỳ type
   nào của RxSwift/PromotionLogic; `_impl` cất sau `NSObject`, còn `PromotionSDKImpl`/`PRMBaseViewModel`
   dùng `@_implementationOnly import RxSwift`. → Compiler của host **không** phải nạp RxSwift hay
   PromotionLogic để suy ra layout khi build. Đây là điều kiện để "cài xong dùng luôn".

3. **Resource bundle được embed vào framework** (`scripts/embed-spm-bundles.sh`, chạy như build phase).
   Package khai `resources:` (`PRMDesignKit`, `PRMPromotionUI`) sinh bundle riêng; link tĩnh thì Xcode
   **không** tự copy bundle vào framework, đến runtime `Bundle.module` gọi `fatalError`. Script quét cả
   `BUILT_PRODUCTS_DIR` lẫn `UninstalledProducts` (bundle mang `SKIP_INSTALL=YES`), dùng `cp -RL` vì ở
   `UninstalledProducts` bundle là **symlink** — copy nguyên symlink thì runtime vẫn crash.

**Ràng buộc còn lại (đừng kỳ vọng sai):**

- ✅ **Nguy cơ trùng symbol RxSwift đã được loại bỏ.** Trước đây RxSwift link tĩnh vào SDK → nếu host
  cũng dùng RxSwift thì có hai bản trong cùng process (trùng symbol / lẫn version). Nay tầng UI đã
  chuyển sang **Combine + async/await** (thành phần của iOS 13+, không link thư viện) và RxSwift bị gỡ
  hẳn → SDK **không** mang theo thư viện reactive nào, host dùng RxSwift/Combine tùy ý đều vô can.
- Bất biến chỉ đúng khi phát hành **qua xcframework**. Nếu ai đó tích hợp bằng cách thêm trực tiếp SPM
  package của SDK vào project host thì tính đóng gói mất — **kênh phát hành hỗ trợ duy nhất là xcframework rời**.
- Host **vẫn** tự ký framework lúc embed (`CODE_SIGNING_ALLOWED=NO` khi build SDK) — đây là thao tác
  chuẩn của Xcode, không phải "cài thêm".
- **Sàn iOS 13 giữ nguyên.** Combine yêu cầu iOS 13; async/await back-deploy về iOS 13 (Xcode tự nhúng
  runtime concurrency vào app host, host không cấu hình gì). RxSwift 5.1.1 vốn đã bắt iOS 13 nên không
  có host nào bị loại. Nên **smoke-test một lần trên thiết bị iOS 13/14 thật** (back-deploy concurrency).

> Muốn CI chặn hồi quy bất biến này: cho `build-xcframework.sh` **fail** khi danh sách framework động
> cuối cùng khác rỗng, thay vì chỉ in cảnh báo như hiện tại.


---

## 4. Liên quan

- [../common/PublicApi.md](../common/PublicApi.md) — bề mặt SDK cho host; đổi là breaking, ảnh hưởng chính sách version.
- [../common/ProjectStructure.md](../common/ProjectStructure.md) — vai trò `:promotionLogic` / `PromotionSDKUI`.
- [UIGuide.md](./UIGuide.md) — pattern UI iOS, base class `PRM*`, Combine + async/await.
- [../android/Distribution.md](../android/Distribution.md) — kênh phát hành Android (Maven).
- [../AI_AGENT_RULES.md](../AI_AGENT_RULES.md) — điều 6 (thêm thư viện phải có lý do), điều 8 (đổi API thì cập nhật docs).
