# Distribution (iOS) — XCFramework

**Không đi qua Maven.** Host iOS nhận thẳng **một** `Promotion.xcframework`; kênh phát hành là
xcframework rời (SPM/CocoaPods đóng gói quanh nó). Hai nền tảng có hai kênh phát hành khác nhau — bình
thường, không phải thiếu sót.

> Phát hành **Android** (Maven) là kênh riêng — xem [../android/Distribution.md](../android/Distribution.md).

Một lệnh, đối xứng với Android — **hai chế độ, cùng tên cờ với `build-android.sh`**:

```bash
./scripts/build-ios.sh                    # hỏi chọn chế độ
./scripts/build-ios.sh local              # dựng XCFramework → build app demo (simulator)
./scripts/build-ios.sh local --skip-app   # chỉ dựng Promotion.xcframework
./scripts/build-ios.sh local --run        # build xong cài + mở trên simulator
./scripts/build-ios.sh publish            # hỏi version → dựng → build app demo → đẩy Artifactory
./scripts/build-ios.sh publish -v 1.2.0 --yes    # cho CI, không hỏi gì
./scripts/build-ios.sh publish --dry-run  # dựng + kiểm tra, không gửi gì lên
```

> Chế độ `publish` **build cả app demo trước khi đẩy**, và hỏng thì dừng. App demo link xcframework
> theo đường dẫn tương đối `../iosPromotionSDK/build/Promotion.xcframework`, nên nó là thứ rẻ nhất
> chứng minh gói vừa dựng thật sự link + embed được. Đẩy một bản không ai link nổi thì version đó coi
> như bỏ — repo không cho ghi đè. Bỏ cổng này bằng `--skip-app` khi thật sự cần.

Chuỗi phụ thuộc mà script ép đúng thứ tự:

```
:promotionLogic (Kotlin) ──gradle──▶ PromotionLogic.xcframework   (lõi, static)
             └─ link tĩnh vào ─────▶ Promotion.xcframework   (UI, Swift)
                                              └─ iosApp link + embed
```

Bước 1 do `iosPromotionSDK/scripts/build-xcframework.sh` lo (nó tự gọi Gradle dựng lõi rồi copy vào
`iosPromotionSDK/Frameworks/` **trước khi** archive Swift — dùng header cũ thì lỗi hiện ra tận
`SwiftCompile` với "cannot find … in scope", rất khó lần).

### Đánh version (đối xứng Android `SDK_VERSION`)

Truyền `SDK_VERSION` cho script (mặc định `1.0.0`, cùng mặc định với property `SDK_VERSION` bên
`AndroidPromotionSDK/build.gradle.kts`):

```bash
SDK_VERSION=1.2.3 ./scripts/build-xcframework.sh
```

Nó nhồi vào `MARKETING_VERSION` khi `xcodebuild archive` → thành `CFBundleShortVersionString` trong
`Info.plist` của `PRM.framework` (host đọc lại lúc runtime qua `Bundle`). Không truyền thì lấy mặc
định trong pbxproj (`MARKETING_VERSION = 1.0.0`).

Mỗi lần build, script tự đóng gói `build/Promotion.xcframework.zip` (**tên cố định**, không kèm version —
mang đi tích hợp ngay; version đã nằm trong Info.plist). Khác Android đặt version vào tên file
(`AndroidPromotionSDK-<version>.aar`): iOS tích hợp theo tên framework cố định nên giữ tên zip ổn định.

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
├── Promotion.xcframework          ← giao cho host
└── PRM.framework.dSYM             ← GIỮ LẠI, đừng để rơi
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
dwarfdump --uuid iosPromotionSDK/build/PRM.framework.dSYM/Contents/Resources/DWARF/PRM
dwarfdump --uuid iosPromotionSDK/build/Promotion.xcframework/ios-arm64/PRM.framework/PRM
```

> Tên binary là `PRM` (`PRODUCT_NAME`), không phải `PromotionSDKUI` — vỏ xcframework tên
> `Promotion.xcframework` nhưng framework bên trong là `PRM.framework`, host `import PRM`.

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
> các tối ưu còn lại (asset/strip/abi.json, xem §2.1) đã giảm gói mà host không phải làm gì.

### 2.1 Những thứ bị loại khỏi gói phát hành

`build-xcframework.sh` dọn sẵn, host nhận gói đã sạch:

| Loại bỏ | Cỡ | Vì sao bỏ được |
|---|---|---|
| `*.abi.json` | ~2 MB | Chỉ phục vụ `swift-api-digester` (so ABI giữa hai bản). Host compile theo `.swiftinterface` cạnh đó. |
| `*.private.swiftinterface` | 23 KB × 3 | Dành cho client dùng `@_spi`. SDK **không** khai `@_spi` nào nên nó ra giống hệt từng byte bản công khai. Thiếu thì Swift tự dùng bản công khai. |
| `__MACOSX/` trong zip | 86 entry | Resource fork + metadata HFS do `ditto --sequesterRsrc` gói vào. Framework iOS **phẳng, 0 symlink** (chỉ macOS mới có `Versions/A`), xattr duy nhất là `com.apple.provenance` do hệ tự dán → cờ đó bảo vệ một thứ không tồn tại. Nay dùng `--norsrc --noextattr`. |
| dSYM | 21 MB | Artifact của quy trình release, không phải thứ gửi host — xem §1. |

> ⚠️ Nếu sau này SDK bắt đầu phơi API qua `@_spi`, **phải bỏ dòng xoá `*.private.swiftinterface`**
> trong `build-xcframework.sh`, không thì client SPI mất lối.

**Giữ lại `*.swiftdoc`** (47 KB × 3) dù nó không cần để compile: đó là nguồn doc comment hiện trong
quick-help của Xcode bên app host. 143 KB đổi lấy tài liệu hiện ngay lúc gõ code là đáng.

## 3. Đóng gói: host **không phải cài thêm gì** — cơ chế và ràng buộc

Bất biến của SDK iOS: host kéo **đúng một** `Promotion.xcframework`, `import PRM`,
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

- ✅ **Không có nguy cơ trùng symbol thư viện reactive.** Tầng UI dùng Combine + async/await (thành
  phần của iOS 13+, không link thư viện), SDK **không** mang theo thư viện reactive nào — host dùng
  RxSwift/Combine tuỳ ý đều vô can.
- Bất biến chỉ đúng khi phát hành **qua xcframework**. Nếu ai đó tích hợp bằng cách thêm trực tiếp SPM
  package của SDK vào project host thì tính đóng gói mất — **kênh phát hành hỗ trợ duy nhất là xcframework rời**.
- Host **vẫn** tự ký framework lúc embed (`CODE_SIGNING_ALLOWED=NO` khi build SDK) — đây là thao tác
  chuẩn của Xcode, không phải "cài thêm".
- **Sàn iOS 13 giữ nguyên.** Combine yêu cầu iOS 13; async/await back-deploy về iOS 13 (Xcode tự nhúng
  runtime concurrency vào app host, host không cấu hình gì). RxSwift 5.1.1 vốn đã bắt iOS 13 nên không
  có host nào bị loại. Nên **smoke-test một lần trên thiết bị iOS 13/14 thật** (back-deploy concurrency).

> Muốn CI chặn hồi quy bất biến này: cho `build-xcframework.sh` **fail** khi danh sách framework động
> cuối cùng khác rỗng, thay vì chỉ in cảnh báo như hiện tại.


## 4. Đẩy lên Artifactory

Gói zip dựng xong nằm ở máy; kênh giao cho host là repo generic **`vdo-ios-frameworks`** trên
Artifactory nội bộ (cùng host với repo Maven của Android, khác repo key).

Đường dùng thường ngày là qua `build-ios.sh` (dựng + kiểm tra + đẩy trong một lệnh):

```bash
./scripts/build-ios.sh publish              # hỏi version → dựng → build app demo → đẩy
./scripts/build-ios.sh publish --dry-run    # dựng + kiểm tra, dừng trước khi gửi
```

Nó gọi xuống script upload dưới đây; gọi thẳng khi gói đã dựng sẵn và chỉ muốn đẩy lại:

```bash
./iosPromotionSDK/scripts/publish-xcframework.sh --dry-run   # xem sẽ đẩy gì, không gửi
./iosPromotionSDK/scripts/publish-xcframework.sh             # đẩy thật
```

> `publish-xcframework.sh` **cố ý không tự build**: publish là hành động một chiều, phải đẩy đúng
> thứ vừa kiểm tra bằng tay, không phải thứ vừa được dựng lại sau lưng. Muốn build kèm thì dùng
> `build-ios.sh publish`.

Bố cục bám đúng convention các SDK khác trong cùng repo (`Martech/CEP`, `VDONetwork`, `VDOUtils`):

```
vdo-ios-frameworks/Martech/Promotion/<version>/
├── Promotion-<version>.xcframework.zip
└── metadata.json          ← download_url + sha256
```

> Ở đây tên file **có kèm version** — khác tên cố định `Promotion.xcframework.zip` lúc build (§ đầu
> file). Trên Artifactory mỗi version là một thư mục riêng nên tên phải phân biệt được; còn bản
> nằm ở `build/` thì giữ tên ổn định để mang đi tích hợp tay.

**Thứ tự bắt buộc:** tạo thư mục version trước (PUT vào path có `/` cuối), xác nhận nó tồn tại, rồi
mới upload zip vào trong. Artifactory có tạo folder ngầm khi upload, nhưng làm tường minh thì lỗi
phân quyền lộ ra ngay ở bước tạo folder — không phải sau khi đã đẩy xong 12MB.

`metadata.json` **sinh tự động** từ zip + git (checksum, branch, build time), không viết tay:
checksum lệch là thứ sai lặng lẽ nhất — SPM chỉ báo *"checksum mismatch"* mà không nói bên nào sai.

Script upload kèm header `X-Checksum-Sha256` để Artifactory tự đối chiếu; hỏng đường truyền thì nó
từ chối thay vì âm thầm nhận file lỗi.

### Credentials

Không commit, đọc theo thứ tự ưu tiên:

1. env `ARTIFACTORY_USER` / `ARTIFACTORY_PASSWORD`
2. `local.properties`: `maven.username` / `maven.password` — **cùng tài khoản** Android đang dùng để
   resolve `gradle-viettelmoney`, không phải khai thêm

### Không ghi đè version đã phát hành

Script **fail** nếu thư mục version đã tồn tại. Bản đã phát hành là thứ người khác đang build theo —
cùng một version mà nhận hai binary khác nhau là loại lỗi tốn cả ngày để lần. Muốn ghi đè thì phải
cố ý: `--force`.

#### Build đè và bẫy cache SPM

SPM có **hai tầng cache, cả hai đánh key theo URL** — không có checksum trong key:

| Tầng | Chỗ nằm | Nội dung |
|---|---|---|
| 1 | `<DerivedData>/SourcePackages/artifacts/…` | xcframework đã giải nén |
| 2 | `~/Library/Caches/org.swift.swiftpm/artifacts/<url>` | file zip, **dùng chung mọi project trên máy** |

Tầng 2 là cái bẫy: đè zip mà giữ nguyên URL thì xoá DerivedData **cũng vô ích** — SPM lấy lại đúng
zip cũ từ tầng 2. Hai kịch bản, không cái nào dễ chịu:

- **Có sửa `checksum`** → lỗi cứng, mà thông báo chỉ sai hướng:
  `error: checksum of downloaded artifact ... does not match` — chữ *"downloaded"* sai, dòng log ngay
  trên ghi rõ *"Fetching binary artifact … **from cache**"*.
- **Không sửa `checksum`** → máy bạn im lặng chạy binary **cũ**; CI và máy đồng nghiệp tải bản mới về
  thì **fail** vì checksum không khớp. Chạy ngon chỗ bạn, vỡ chỗ khác.

Nên `./scripts/build-ios.sh publish --force` **tự dọn cả hai tầng** cho đúng version vừa đè. Muốn ép
app demo kéo lại mà không publish gì: `./scripts/build-ios.sh local --force`.

> Chỉ dọn được máy chạy lệnh. Máy nào đã kéo bản cũ về thì vẫn giữ nó cho tới khi tự dọn.
> **Cách lành nhất vẫn là tăng version** — URL đổi thì cả hai tầng đều là key mới, không phải nhớ gì.

Version lấy từ `CFBundleShortVersionString` của chính binary sắp đẩy, không phải từ biến rời — tránh
cảnh tên file ghi `1.0.1` mà `Info.plist` bên trong vẫn `1.0.0`. Truyền `SDK_VERSION` khác với bản
đã build thì script dừng và bảo dựng lại.

## 5. App demo lấy SDK từ đâu

**Từ Artifactory, không phải từ `build/` cục bộ.** `iosApp` phụ thuộc vào local package
`iosApp/PromotionRemote`, package này khai `binaryTarget(url:checksum:)` trỏ vào zip đã phát hành.

```
iosApp ──▶ PromotionRemote (local package)
              └── binaryTarget(url: ".../Martech/Promotion/1.0.0/Promotion-1.0.0.xcframework.zip",
                               checksum: "9da2…f065")
```

Cấu hình cũ (link thẳng `../iosPromotionSDK/build/Promotion.xcframework`) **vẫn còn trong
`project.pbxproj` dưới dạng comment**, đủ để đổi ngược.

> ⚠️ **Comment trong `project.pbxproj` không sống sót qua Xcode.** Xcode ghi lại nguyên file mỗi khi
> project bị sửa qua UI (đổi build setting, kéo file vào…) và **xoá sạch** mọi comment do người viết.
> Muốn giữ vĩnh viễn thì phải chép ra chỗ khác — mục này chính là chỗ đó.
>
> Và pbxproj **không cho comment lồng nhau**: các dòng gốc đều mang sẵn annotation `/* … */`, nên khi
> comment vào phải gỡ annotation bên trong, không thì dấu đóng đầu tiên kết thúc khối sớm và Xcode
> báo *"project is damaged"*.

### Cái giá phải trả (biết trước, đừng ngạc nhiên)

1. **Sửa SDK xong, app demo KHÔNG thấy gì** cho tới khi publish một version mới rồi cập nhật
   `url:` + `checksum:` trong `PromotionRemote/Package.swift`. Không có cảnh báo nào — app vẫn chạy
   ngoan bản cũ trên server. Đây là hệ quả của thiết kế, không phải lỗi cấu hình.
2. **Mỗi lần thử một thay đổi là đốt một version** trên repo phát hành, vì repo không cho ghi đè.
3. **Cần `~/.netrc`** trên mọi máy dev và CI runner:
   ```
   machine mobile-data.viettelmoney.vn login <user> password <identity token>
   ```
   Thiếu thì Xcode chỉ báo *"failed downloading"*, không nói là 401.

`checksum` của SPM = **sha256 thường** của file zip (đã kiểm: `swift package compute-checksum` ra
đúng số mà `shasum -a 256` cho), nên chép thẳng từ `metadata.json` cạnh zip.

### Đổi ngược về link cục bộ

Bỏ comment các khối `CŨ` trong `project.pbxproj` (5 chỗ: `PBXBuildFile`, `PBXFileReference`,
`PBXFrameworksBuildPhase`, `PBXCopyFilesBuildPhase`, `FRAMEWORK_SEARCH_PATHS` ×2), rồi gỡ
`packageReferences` / `packageProductDependencies` / target `PromotionRemote`.

---

## 6. Liên quan

- [../common/PublicApi.md](../common/PublicApi.md) — bề mặt SDK cho host; đổi là breaking, ảnh hưởng chính sách version.
- [../common/ProjectStructure.md](../common/ProjectStructure.md) — vai trò `:promotionLogic` / `PromotionSDKUI`.
- [UIGuide.md](./UIGuide.md) — pattern UI iOS, base class `PRM*`, Combine + async/await.
- [../android/Distribution.md](../android/Distribution.md) — kênh phát hành Android (Maven).
- [../AI_AGENT_RULES.md](../AI_AGENT_RULES.md) — điều 6 (thêm thư viện phải có lý do), điều 8 (đổi API thì cập nhật docs).
