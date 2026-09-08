# Third-Party Notices — TTCN Promotion SDK

TTCN Promotion SDK sử dụng các thành phần mã nguồn mở dưới đây. Bản quyền và giấy phép thuộc về tác
giả tương ứng. Tài liệu này đi kèm mọi bản phát hành.

| Mục | Giá trị |
|---|---|
| Sản phẩm | TTCN Promotion SDK |
| Phiên bản | `1.0.0` |
| Lập ngày | 2026-09-08 |
| Nguồn | `gradle/libs.versions.toml`, `*/build.gradle.kts`, `iosPromotionSDK/Packages/*/Package.swift` |

> ⚠️ **Trước khi bàn giao chính thức**, đối chiếu lại cột "Giấy phép" với file `LICENSE` thật trong
> từng artifact (xem §5). Bảng này liệt kê phụ thuộc trực tiếp; phụ thuộc bắc cầu (transitive) đi
> theo chúng.

## Mục lục

<!-- toc -->
- [1. Lõi Kotlin Multiplatform — `:promotionLogic`](#1-lõi-kotlin-multiplatform--promotionlogic)
- [2. SDK Android — `:AndroidPromotionSDK`](#2-sdk-android--androidpromotionsdk)
- [3. SDK iOS — `iosPromotionSDK`](#3-sdk-ios--iospromotionsdk)
- [4. Công cụ chỉ dùng lúc build (không phát hành kèm sản phẩm)](#4-công-cụ-chỉ-dùng-lúc-build-không-phát-hành-kèm-sản-phẩm)
- [5. Cách kiểm chứng lại danh sách](#5-cách-kiểm-chứng-lại-danh-sách)
- [6. Toàn văn giấy phép](#6-toàn-văn-giấy-phép)
<!-- /toc -->

---

## 1. Lõi Kotlin Multiplatform — `:promotionLogic`

| Thành phần | Version | Bản quyền | Giấy phép |
|---|---|---|---|
| Kotlin Standard Library | 2.2.0 | JetBrains s.r.o. và cộng tác viên | Apache-2.0 |
| kotlinx.coroutines | 1.10.2 | JetBrains s.r.o. | Apache-2.0 |
| kotlinx.serialization | 1.8.1 | JetBrains s.r.o. | Apache-2.0 |
| Ktor Client (core, content-negotiation, logging, okhttp, darwin, serialization-kotlinx-json) | 3.3.0 | JetBrains s.r.o. | Apache-2.0 |
| OkHttp (qua `ktor-client-okhttp`, chỉ Android) | theo Ktor | Square, Inc. | Apache-2.0 |
| multiplatform-settings | 1.3.0 | Russell Wolf | Apache-2.0 |

## 2. SDK Android — `:AndroidPromotionSDK`

| Thành phần | Version | Bản quyền | Giấy phép |
|---|---|---|---|
| AndroidX AppCompat | 1.7.1 | The Android Open Source Project | Apache-2.0 |
| AndroidX Fragment KTX | 1.6.2 | The Android Open Source Project | Apache-2.0 |
| AndroidX Lifecycle (ViewModel/Runtime KTX) | 2.7.0 | The Android Open Source Project | Apache-2.0 |
| AndroidX RecyclerView | 1.3.2 | The Android Open Source Project | Apache-2.0 |
| AndroidX ConstraintLayout | 2.2.0-alpha10 | The Android Open Source Project | Apache-2.0 |
| AndroidX SwipeRefreshLayout | 1.1.0-alpha02 | The Android Open Source Project | Apache-2.0 |
| Material Components for Android | 1.13.0 | Google LLC | Apache-2.0 |
| Glide | 5.0.5 | Google, Inc. / Bump Technologies | Apache-2.0 (một số phần theo BSD 2-clause / MIT — xem `LICENSE` của Glide) |
| Shimmer for Android | 0.5.0 | Meta Platforms, Inc. (Facebook) | BSD — **kiểm lại `LICENSE` trong artifact** |
| Intuit `sdp-android` | 1.0.6 | Intuit Inc. | Apache-2.0 |
| Timber | 4.7.1 | Jake Wharton | Apache-2.0 |
| Gson | 2.11.0 | Google Inc. | Apache-2.0 |

> Gson chỉ dùng ở tầng UI Android để parse theme JSON của host; lõi dùng kotlinx.serialization.

## 3. SDK iOS — `iosPromotionSDK`

Không có phụ thuộc mã nguồn mở bên ngoài.

Bốn package Swift là **module nội bộ của chính SDK**, không phải thư viện bên thứ ba:
`PRMFoundation`, `PRMDesignKit`, `PRMPromotionUI`, `PRMKotlinBridge`.

Framework iOS có nhúng runtime Kotlin/Native cùng các thư viện ở §1 (link tĩnh bên trong
`Promotion.xcframework`) — giấy phép của chúng áp dụng như liệt kê ở §1.

> **RxSwift đã được gỡ bỏ hoàn toàn.** Nếu còn thấy `iosPromotionSDK/.spm/checkouts/RxSwift` trên máy,
> đó là cache SwiftPM cũ; nó **không** nằm trong gói phát hành và không link vào framework.

## 4. Công cụ chỉ dùng lúc build (không phát hành kèm sản phẩm)

| Thành phần | Version | Vai trò | Giấy phép |
|---|---|---|---|
| Android Gradle Plugin | 8.13.2 | Build | Apache-2.0 |
| Kover | 0.9.9 | Đo coverage | Apache-2.0 |
| SKIE | 0.10.13 | Cầu Kotlin → Swift | Apache-2.0 — **kiểm lại điều khoản của Touchlab** |
| kotlin-test / JUnit / MockEngine / multiplatform-settings-test | — | Kiểm thử | Apache-2.0 / EPL-1.0 (JUnit) |
| Retrofit, OkHttp logging-interceptor | 2.11.0 / 4.12.0 | **Chỉ app demo** (`androidApp`), không thuộc SDK | Apache-2.0 |

---

## 5. Cách kiểm chứng lại danh sách

```bash
# Cây phụ thuộc thực tế của artifact phát hành
./gradlew :AndroidPromotionSDK:dependencies --configuration releaseRuntimeClasspath
./gradlew :promotionLogic:dependencies

# Đối chiếu POM đã publish
cat ~/.m2/repository/vn/viettelpay/library/promotion/<version>/promotion-<version>.pom
```

Giấy phép chuẩn xác lấy từ file `LICENSE` trong chính artifact (`.jar`/`.aar`) hoặc từ repository gốc
của thư viện — **không** lấy từ tài liệu tổng hợp.

---

## 6. Toàn văn giấy phép

Phần lớn thành phần dùng **Apache License 2.0**; toàn văn tại
<https://www.apache.org/licenses/LICENSE-2.0>.

Với thành phần dùng giấy phép khác (Glide, Shimmer, JUnit), toàn văn nằm trong chính artifact và phải
được đính kèm khi phân phối lại theo yêu cầu của giấy phép đó.

Nếu đối tác yêu cầu bộ toàn văn dạng file, đóng gói vào `legal/licenses/` của gói bàn giao —
xem [PackagingGuide §3](./docs/release/PackagingGuide.md).
