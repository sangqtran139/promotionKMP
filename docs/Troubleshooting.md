# Troubleshooting — Xử lý sự cố & FAQ

Tra theo **triệu chứng**. Mỗi mục: triệu chứng → nguyên nhân → cách sửa.

Khi báo lỗi cho đội SDK, luôn kèm: **version SDK**, **nền tảng**, **`X-Request-ID`** của request lỗi
(có trong log/HAR), và các bước tái hiện.

## Mục lục

<!-- toc -->
- [1. Tích hợp & build](#1-tích-hợp--build)
  - [1.1. `Could not find vn.viettelpay.library:promotion:<version>`](#11-could-not-find-vnviettelpaylibrarypromotionversion)
  - [1.2. `Could not find vn.viettelpay.library:promotionLogic:<version>`](#12-could-not-find-vnviettelpaylibrarypromotionlogicversion)
  - [1.3. `Unresolved reference 'PromotionUseCases'` / `PromotionSDKConfig`](#13-unresolved-reference-promotionusecases--promotionsdkconfig)
  - [1.4. Xung đột version thư viện (Android)](#14-xung-đột-version-thư-viện-android)
  - [1.5. iOS: `dyld: Library not loaded … Promotion.framework`](#15-ios-dyld-library-not-loaded--promotionframework)
  - [1.6. iOS: `Unable to find module dependency: 'PRMKotlinBridge'`](#16-ios-unable-to-find-module-dependency-prmkotlinbridge)
  - [1.7. iOS: `checksum of downloaded artifact does not match`](#17-ios-checksum-of-downloaded-artifact-does-not-match)
  - [1.8. iOS: đổi chữ ký public rồi mà Xcode vẫn báo lỗi cũ](#18-ios-đổi-chữ-ký-public-rồi-mà-xcode-vẫn-báo-lỗi-cũ)
  - [1.9. Android: `NoClassDefFoundError` / `AbstractMethodError` chỉ ở bản minify](#19-android-noclassdeffounderror--abstractmethoderror-chỉ-ở-bản-minify)
  - [1.10. Android: lint `PrivateResource` khi tham chiếu resource của SDK](#110-android-lint-privateresource-khi-tham-chiếu-resource-của-sdk)
- [2. Phiên & token](#2-phiên--token)
  - [2.1. Mọi API trả 401 dù user vừa đăng nhập](#21-mọi-api-trả-401-dù-user-vừa-đăng-nhập)
  - [2.2. `onExpireToken()` bắn liên tục](#22-onexpiretoken-bắn-liên-tục)
  - [2.3. `onExpireToken()` bắn khi user không đủ quyền](#23-onexpiretoken-bắn-khi-user-không-đủ-quyền)
  - [2.4. Mọi API của SDK treo sau khi token hết hạn](#24-mọi-api-của-sdk-treo-sau-khi-token-hết-hạn)
  - [2.5. iOS: app treo/đơ khi gọi API của SDK](#25-ios-app-treođơ-khi-gọi-api-của-sdk)
  - [2.6. Đổi user mà vẫn thấy dữ liệu của user cũ](#26-đổi-user-mà-vẫn-thấy-dữ-liệu-của-user-cũ)
- [3. Hiển thị & nghiệp vụ](#3-hiển-thị--nghiệp-vụ)
  - [3.1. Widget/màn "Chọn ưu đãi" rỗng dù tài khoản có voucher](#31-widgetmàn-chọn-ưu-đãi-rỗng-dù-tài-khoản-có-voucher)
  - [3.2. Dịch vụ không hiện ở bottom sheet "Chọn dịch vụ"](#32-dịch-vụ-không-hiện-ở-bottom-sheet-chọn-dịch-vụ)
  - [3.3. Bấm vào ưu đãi/màn nào đó thì hiện popup "tính năng tạm ngưng"](#33-bấm-vào-ưu-đãimàn-nào-đó-thì-hiện-popup-tính-năng-tạm-ngưng)
  - [3.4. Tính năng bị tắt dù server bật hết](#34-tính-năng-bị-tắt-dù-server-bật-hết)
  - [3.5. Có lỗi nhưng không thấy thông báo nào](#35-có-lỗi-nhưng-không-thấy-thông-báo-nào)
  - [3.6. Ảnh GIF không chạy](#36-ảnh-gif-không-chạy)
  - [3.7. Màu sắc/giao diện không đúng brand](#37-màu-sắcgiao-diện-không-đúng-brand)
  - [3.8. Màn SDK hiển thị tối trong khi SDK "ép light"](#38-màn-sdk-hiển-thị-tối-trong-khi-sdk-ép-light)
- [4. Headless API](#4-headless-api)
  - [4.1. Gọi `PromotionSDK.api` ném `IllegalStateException`](#41-gọi-promotionsdkapi-ném-illegalstateexception)
  - [4.2. Không nhận được `onExpireToken()` khi dùng headless](#42-không-nhận-được-onexpiretoken-khi-dùng-headless)
  - [4.3. `getVouchers` trả rỗng nhưng `findEligible` có dữ liệu (hoặc ngược lại)](#43-getvouchers-trả-rỗng-nhưng-findeligible-có-dữ-liệu-hoặc-ngược-lại)
  - [4.4. Ngày tháng trả về khó parse](#44-ngày-tháng-trả-về-khó-parse)
- [5. Câu hỏi thường gặp](#5-câu-hỏi-thường-gặp)
- [6. Cần thêm dữ liệu để chẩn đoán](#6-cần-thêm-dữ-liệu-để-chẩn-đoán)
<!-- /toc -->

---

## 1. Tích hợp & build

### 1.1. `Could not find vn.viettelpay.library:promotion:<version>`

- Chưa khai repo Artifactory, hoặc thiếu credentials → đặt `ttcnArtifactoryUser`/`ttcnArtifactoryToken`
  ở `~/.gradle/gradle.properties` (**không** commit).
- Khai `content { includeGroup("vn.viettelpay.library") }` để repo nội bộ không tranh resolve với
  `google()`/`mavenCentral()`.
- Version chưa được publish, hoặc publish nhầm repo snapshot (hậu tố `-SNAPSHOT` đẩy sang repo khác).

### 1.2. `Could not find vn.viettelpay.library:promotionLogic:<version>`

Lõi chưa lên cùng lượt với `promotion`. Hai artifact **luôn** phải publish cùng nhau và cùng số
version — báo đội SDK, host không tự khắc phục được.

### 1.3. `Unresolved reference 'PromotionUseCases'` / `PromotionSDKConfig`

Đang import `com.ttcn.promotionsdk.*`. Đó là **lõi nội bộ**, không nằm trên compile classpath của
host — đúng thiết kế. Chỉ dùng `com.ttcn.prm.entry.*` (+ `ui.theme.*`, `PRMEndowView`).

Cần một thứ mà `entry` chưa có → **báo đội SDK dời nó vào `entry`**, đừng lách bằng Java/reflection.

### 1.4. Xung đột version thư viện (Android)

Host **thấy** Ktor, coroutines, AppCompat, Glide, Gson qua metadata của SDK. Gradle chọn version cao
hơn theo luật thường; nếu vỡ thì ghim bằng `resolutionStrategy` phía host. Xem
[CompatibilityMatrix](./release/CompatibilityMatrix.md) để biết SDK đang compile với version nào.

### 1.5. iOS: `dyld: Library not loaded … Promotion.framework`

Framework đang để **Do Not Embed**. `Promotion.xcframework` là **dynamic framework** — phải
**Embed & Sign** để dylib được copy vào `.app/Frameworks`.

### 1.6. iOS: `Unable to find module dependency: 'PRMKotlinBridge'`

Host đang `import PRMKotlinBridge` hoặc `import PromotionLogic`. Đó là module nội bộ, giấu sau
`@_implementationOnly`. Chỉ `import PRM`.

Nếu **không** import mà vẫn gặp: một type nội bộ đã lọt vào chữ ký public của SDK — lỗi phía SDK,
báo đội SDK.

### 1.7. iOS: `checksum of downloaded artifact does not match`

Cache SPM hai tầng, cả hai đánh key theo URL: `<DerivedData>/SourcePackages/artifacts/…` và
`~/Library/Caches/org.swift.swiftpm/artifacts/<url>`. Tầng hai dùng chung mọi project trên máy và
**không** có checksum trong tên thư mục — đè zip mà giữ nguyên URL thì xoá DerivedData cũng vô ích.

Chữ "downloaded" trong thông báo dễ gây lạc hướng: dòng log ngay trên thường ghi *"Fetching binary
artifact … from cache"*.

**Sửa:** lấy version mới (đúng cách), hoặc dọn cache tầng hai. `build-ios.sh publish` luôn tự dọn
cache của version vừa đẩy trên **máy chạy lệnh** — máy khác đã kéo bản cũ thì vẫn giữ nó.

### 1.8. iOS: đổi chữ ký public rồi mà Xcode vẫn báo lỗi cũ

`⇧⌘K` (Clean Build Folder). Cache module của Xcode có thể nói dối sau khi framework đổi interface.

### 1.9. Android: `NoClassDefFoundError` / `AbstractMethodError` chỉ ở bản minify

Bản debug xanh nhưng release chết là dấu hiệu R8 cắt nhầm. `consumer-rules.pro` đóng gói sẵn trong
AAR đã giữ phần cần giữ — nếu vẫn gặp, báo đội SDK kèm class bị thiếu (rất có thể một `-keep` đã bị
bỏ sót). **Đừng** tự thêm keep-rule rộng ở app rồi coi là xong.

### 1.10. Android: lint `PrivateResource` khi tham chiếu resource của SDK

Đúng thiết kế — toàn bộ resource của SDK là private (`public.xml` rỗng). Host phải tự khai resource
của mình, đừng mượn của SDK.

---

## 2. Phiên & token

### 2.1. Mọi API trả 401 dù user vừa đăng nhập

Theo thứ tự kiểm:

1. `currentToken()` có trả token **mới nhất** không? SDK gọi lại nó ở **mỗi** request — nếu host cache
   token vào biến rồi không cập nhật, SDK sẽ dùng chuỗi đã chết.
2. Host có ghi token mới vào kho của mình **trước khi** gọi `onResult(true)` không? Trả `true` sớm là
   SDK thử lại với token cũ.
3. `baseUrl` có đúng môi trường của token không?

### 2.2. `onExpireToken()` bắn liên tục

SDK chỉ thử lại **một lần** cho mỗi request. Bắn nhiều lần nghĩa là nhiều request cùng hỏng, hoặc
`refreshToken` luôn trả `false`. Kiểm luồng refresh của host.

### 2.3. `onExpireToken()` bắn khi user không đủ quyền

Không đúng — chỉ **401** map sang `TOKEN_EXPIRED`; **403** rơi về nhánh lỗi thường. Nếu vẫn gặp, kiểm
xem backend có trả 401 cho lỗi phân quyền không.

### 2.4. Mọi API của SDK treo sau khi token hết hạn

`refreshToken` không gọi `onResult`. SDK có chốt **15 giây** rồi coi như thất bại, nhưng trong 15
giây đó mọi lời gọi xếp hàng. Đảm bảo mọi nhánh của `refreshToken` (kể cả nhánh lỗi) đều gọi
`onResult` đúng một lần.

### 2.5. iOS: app treo/đơ khi gọi API của SDK

`currentToken()` bị đánh dấu `@MainActor` hoặc chạm main thread. SDK gọi nó từ **thread nền** —
không được `@MainActor`, phải thread-safe.

### 2.6. Đổi user mà vẫn thấy dữ liệu của user cũ

Chưa gọi `PromotionSDK.release()` khi đăng xuất, hoặc chưa `initialize` lại với phiên mới.

---

## 3. Hiển thị & nghiệp vụ

### 3.1. Widget/màn "Chọn ưu đãi" rỗng dù tài khoản có voucher

- Chưa gọi `updateOrderInfo(orderId, productId, ...)` **trước khi** vào màn.
- `productId` không khớp `applicableProducts.productId` của voucher (**không** phải `sku`).
- Đơn không đạt điều kiện tối thiểu của campaign — kiểm bằng cách gọi thẳng
  `api.findEligible(...)` và đọc `unmatchedRules`.

### 3.2. Dịch vụ không hiện ở bottom sheet "Chọn dịch vụ"

`PromotionAvailableService.productId` phải khớp `applicableProducts.productId`. Danh sách bị **lọc
trùng theo `productId`** — mỗi `productId` chỉ khai **một** dòng, kể cả khi nó gắn nhiều SKU.

### 3.3. Bấm vào ưu đãi/màn nào đó thì hiện popup "tính năng tạm ngưng"

Feature flag đang tắt (`PRM_MOB_021`). SDK tự hiện popup; host nhận `FeatureDisabled` để **dừng luồng
thanh toán**, không phải để hiện chữ.

Muốn ẩn entry point của host thay vì để user bấm rồi ăn popup: hỏi trước bằng
`PromotionSDK.isFeatureEnabled(...)` / `isSdkEnabled()` / `refreshFeatureFlags { }`.

### 3.4. Tính năng bị tắt dù server bật hết

Cờ đọc từ **cache đồng bộ**. Gọi `refreshFeatureFlags { }` để nạp lại; cache chỉ cập nhật khi API cờ
trả về thành công.

### 3.5. Có lỗi nhưng không thấy thông báo nào

Đúng thiết kế ở một số màn. SDK **không còn toast**; lỗi hoặc là **popup**, hoặc **im lặng** khi màn
đã có empty-view/shimmer/list cũ nói thay. Xem
[ErrorHandling §4](./common/ErrorHandling.md).

### 3.6. Ảnh GIF không chạy

Mọi ô ảnh phải chạy được ảnh động ở cả hai nền tảng. Nếu GIF đứng hình, thường do một bước snapshot
view thành drawable ở giữa — báo đội SDK kèm màn hình và URL ảnh.

### 3.7. Màu sắc/giao diện không đúng brand

Truyền `PromotionSDKTheme` lúc `initialize`, hoặc gọi `PromotionSDK.configure(theme)` sau đó. Xem
[Theming](./common/Theming.md).

### 3.8. Màn SDK hiển thị tối trong khi SDK "ép light"

SDK ép light ở cả hai nền tảng. Nếu vẫn tối trên Android, kiểm theme của **host** — thường do
`Theme.Material3.DayNight` ở app host, không phải force-dark của SDK.

---

## 4. Headless API

### 4.1. Gọi `PromotionSDK.api` ném `IllegalStateException`

Chưa `initialize`. `api` chỉ dùng được sau khi khởi tạo thành công.

### 4.2. Không nhận được `onExpireToken()` khi dùng headless

Đã biết: `onExpireToken()` hiện **chưa áp dụng** cho `PromotionSDKApi`. Host headless phải tự bắt
`PromotionSDKError.SessionExpired` từ `PromotionApiResult.Failure`.

### 4.3. `getVouchers` trả rỗng nhưng `findEligible` có dữ liệu (hoặc ngược lại)

Hai hàm khác nhau về bản chất:

- `getVouchers` — voucher khách **đã sở hữu**, không xét đơn hàng.
- `findEligible` — ưu đãi **đủ điều kiện cho đơn hiện tại**, trả hai nhóm `myOffers` + `otherOffers`.

### 4.4. Ngày tháng trả về khó parse

Cố ý: DTO public trả **chuỗi thô của server**. Parse ở tầng SDK thì định dạng lạ sẽ thành `null` và
host không phân biệt được "vô thời hạn" với "server trả sai định dạng". Định dạng ngày là việc của UI.

---

## 5. Câu hỏi thường gặp

**SDK có gửi dữ liệu đi đâu ngoài `baseUrl` không?** Không. Không analytics, không crash reporting,
không dịch vụ bên thứ ba.

**SDK lưu gì xuống máy?** Chỉ cờ tính năng và theme. Không token, không PII, không có database.

**SDK có xin quyền gì không?** Android chỉ `INTERNET`; iOS không quyền nào.

**Có certificate pinning không?** Không có trong SDK — nếu cần, phải bàn trước vì đó là thay đổi hợp
đồng cấu hình.

**Có bắt buộc gọi `release()` không?** Có, khi user đăng xuất.

**Dùng được với Compose / SwiftUI không?** SDK trả `Fragment`/`View` (Android) và
`UIViewController`/`UIView` (iOS) — nhúng được vào Compose/SwiftUI qua interop tương ứng, nhưng SDK
không cung cấp API Compose/SwiftUI riêng.

**Có bản obfuscate không?** AAR phát hành **không** obfuscate — xem lý do và rủi ro ở
[Security §6.1](./common/Security.md).

**Nâng cấp version thì phải làm gì?** Đọc [ReleaseNotes](./release/ReleaseNotes.md) và
[MigrationGuide](./release/MigrationGuide.md); MAJOR đổi = chắc chắn phải sửa code.

---

## 6. Cần thêm dữ liệu để chẩn đoán

| Cần | Lấy ở đâu |
|---|---|
| Version SDK | Android: toạ độ Maven trong `build.gradle.kts`. iOS: `CFBundleShortVersionString` của framework |
| `X-Request-ID` | Header của request lỗi — mỗi request một UUID, dùng để đối soát log backend |
| Log chi tiết | **Chỉ bản debug**: Android build DEBUG; iOS chạy với `PROMOTION_SDK_DEBUG=1`. ⚠️ Log này in cả `Authorization` — không bật ở bản phát hành, không đính kèm nguyên văn khi gửi báo cáo |
| Stacktrace Android | Đọc thẳng được (AAR không obfuscate), không cần `mapping.txt` |
| Crash iOS | Cần symbolicate bằng dSYM của **đúng** version — đội SDK lưu theo version |
