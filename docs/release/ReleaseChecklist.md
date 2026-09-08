# ReleaseChecklist — Danh mục kiểm trước khi phát hành

Dùng cho **mỗi** bản phát hành. In ra hoặc copy thành issue, tick từng dòng. Người phát hành ký tên
ở cuối.

> Quy ước: 🔴 = chặn phát hành nếu chưa xong. 🟡 = phải ghi lý do nếu bỏ qua.

## Mục lục

<!-- toc -->
- [1. Version & mã nguồn](#1-version--mã-nguồn)
- [2. Kiểm thử](#2-kiểm-thử)
- [3. Bề mặt public](#3-bề-mặt-public)
- [4. Bảo mật](#4-bảo-mật)
- [5. Đóng gói](#5-đóng-gói)
- [6. Nghiệm thu tích hợp (app demo đóng vai host)](#6-nghiệm-thu-tích-hợp-app-demo-đóng-vai-host)
- [7. Tài liệu](#7-tài-liệu)
- [8. Bàn giao](#8-bàn-giao)
<!-- /toc -->

---

## 1. Version & mã nguồn

- [ ] 🔴 `SDK_VERSION` trong `gradle.properties` = version sắp phát hành.
- [ ] 🔴 `MARKETING_VERSION` trong `iosPromotionSDK/PRM.xcodeproj` **trùng** `SDK_VERSION`.
- [ ] 🔴 Số version tuân thủ [VersioningPolicy](./VersioningPolicy.md) (breaking ⇒ bump major).
- [ ] 🔴 `git status` sạch; đang ở đúng branch phát hành.
- [ ] 🔴 `CHANGELOG.md`: mục `[Unreleased]` đã chuyển thành `[<version>] - <ngày>`.
- [ ] 🟡 Đã tạo tag `v<version>`.

## 2. Kiểm thử

- [ ] 🔴 `./gradlew :promotionLogic:testAndroidHostTest` — xanh.
- [ ] 🔴 `./gradlew :promotionLogic:iosSimulatorArm64Test` — xanh (cần macOS).
- [ ] 🔴 Đã **đọc số test thật**, không chỉ tin "BUILD SUCCESSFUL":
      `grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' promotionLogic/build/test-results/testAndroidHostTest/*.xml`
- [ ] 🔴 `./gradlew :AndroidPromotionSDK:testDebugUnitTest` — xanh.
- [ ] 🔴 `./gradlew :promotionLogic:koverVerify` — đạt ngưỡng (LINE ≥ 93%, INSTRUCTION ≥ 92%, BRANCH ≥ 90%).
- [ ] 🟡 `./scripts/test-report.sh --json` — lưu báo cáo vào hồ sơ nghiệm thu.

## 3. Bề mặt public

- [ ] 🔴 Grep public Android **không in ra gì ngoài `entry/`**:
      ```bash
      grep -rEn '^(public )?(open |abstract |sealed |data |enum |annotation |value |inline |suspend |const |fun )*(class|interface|object|fun|val|var|typealias) ' \
        --include='*.kt' AndroidPromotionSDK/src/main/java/com/ttcn/prm | grep -v '/entry/'
      ```
- [ ] 🔴 Grep public iOS không in ra gì:
      `grep -rn '^\s*\(public\|open\)\s' --include='*.swift' iosPromotionSDK/PromotionSDKUI`
- [ ] 🔴 Đổi bề mặt public ⇒ đã sửa **cả Kotlin lẫn Swift** trong cùng một thay đổi, giữ nguyên tên
      và thứ tự khai báo (parity).
- [ ] 🔴 `docs/common/PublicApi.md` khớp code sau thay đổi.

## 4. Bảo mật

- [ ] 🔴 Bản release **không** bật `LogLevel.BODY` và **không** bật `PromotionCurlLogging`.
- [ ] 🔴 `AndroidManifest.xml` của SDK chỉ khai `INTERNET`.
- [ ] 🔴 Không có endpoint/tài khoản/token nào hard-code trong source.
- [ ] 🟡 Đã chạy [danh mục kiểm của bên an ninh](../common/Security.md#8-danh-mục-kiểm-tra-cho-bên-an-ninh).

## 5. Đóng gói

- [ ] 🔴 Android: `./scripts/build-android.sh publish -v <version>` thành công, **cả hai** module
      (`promotion` + `promotionLogic`) lên cùng lượt.
- [ ] 🔴 iOS: `./scripts/build-ios.sh publish -v <version>` thành công.
- [ ] 🔴 **Không ghi đè** version đã tồn tại (nếu buộc phải đè: đã có phê duyệt và đã dọn cache).
- [ ] 🔴 iOS: **dSYM đã lưu** vào kho nội bộ theo version.
- [ ] 🟡 Đã sinh `MANIFEST.txt` + SHA-256 cho gói bàn giao.

## 6. Nghiệm thu tích hợp (app demo đóng vai host)

- [ ] 🔴 Android: `useMavenLocal=false` → app demo kéo **đúng artifact trên Artifactory** và chạy được.
- [ ] 🔴 iOS: app demo tích hợp **đúng zip vừa đẩy** và chạy được.
- [ ] 🔴 Mở đủ: Ưu đãi của tôi → Tìm kiếm → Chi tiết → Áp dụng → widget checkout → xác nhận sử dụng.
- [ ] 🔴 Bật R8/minify ở app demo Android: không `NoClassDefFoundError` / `AbstractMethodError`.
- [ ] 🟡 Thử kill-switch: tắt `PROMOTION.ENABLE_ALL` phía server → mọi điểm vào bị chặn đúng cách.
- [ ] 🟡 Thử token hết hạn giữa phiên → `onExpireToken()` bắn đúng một lần.

## 7. Tài liệu

- [ ] 🔴 `CHANGELOG.md` mô tả đủ thay đổi hành vi và breaking change kèm cách nâng cấp.
- [ ] 🔴 [ReleaseNotes](./ReleaseNotes.md) cho bản này đã viết.
- [ ] 🔴 [CompatibilityMatrix](./CompatibilityMatrix.md) cập nhật nếu đổi minSdk / iOS target / dependency.
- [ ] 🟡 [THIRD_PARTY_NOTICES](../../THIRD_PARTY_NOTICES.md) cập nhật nếu thêm/bỏ thư viện.
- [ ] 🟡 Số đầu mục + mục lục đã khớp — `python3 scripts/docs_toc.py --check` không báo lệch.
- [ ] 🟡 Link nội bộ còn nguyên — `python3 scripts/docs_links.py` báo 0 link hỏng.
- [ ] 🟡 Đã sinh lại bộ `.docx` (`scripts/docs_to_docx.py`).

## 8. Bàn giao

- [ ] 🔴 Hoàn tất [HandoverChecklist](./HandoverChecklist.md).
- [ ] 🔴 Đã thông báo đối tác: version mới, có breaking không, cần làm gì để nâng cấp.

---

**Người phát hành:** ______________  **Ngày:** ____________  **Version:** ____________

**Người rà soát:** ______________  **Ngày:** ____________
