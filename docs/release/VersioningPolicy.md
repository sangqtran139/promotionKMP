# VersioningPolicy — Chính sách phiên bản & hỗ trợ

Cam kết của đội SDK với đối tác tích hợp: **số version nói đúng mức độ rủi ro khi nâng cấp**.

## Mục lục

<!-- toc -->
- [1. SemVer](#1-semver)
- [2. Một số version cho cả gói](#2-một-số-version-cho-cả-gói)
- [3. Bản `-SNAPSHOT`](#3-bản--snapshot)
- [4. Không ghi đè bản đã phát hành](#4-không-ghi-đè-bản-đã-phát-hành)
- [5. Vòng đời hỗ trợ](#5-vòng-đời-hỗ-trợ)
- [6. Chính sách khai tử (deprecation)](#6-chính-sách-khai-tử-deprecation)
- [7. Thông báo cho đối tác](#7-thông-báo-cho-đối-tác)
- [8. Trạng thái hiện tại — cần chốt](#8-trạng-thái-hiện-tại--cần-chốt)
<!-- /toc -->

---

## 1. SemVer

`MAJOR.MINOR.PATCH` theo [SemVer](https://semver.org/). Bề mặt tính là "public API" chính là
[bề mặt công khai](../common/PublicApi.md) — và **chỉ** bề mặt đó.

| Tăng | Khi nào | Host phải làm gì |
|---|---|---|
| **MAJOR** | Xoá/đổi tên/đổi chữ ký thứ gì host đang gọi; đổi toạ độ Maven hoặc tên module iOS; đổi wire format khiến backend cũ không phục vụ được; nâng minSdk / iOS target | Sửa code, thử lại toàn bộ luồng |
| **MINOR** | Thêm hàm/DTO/tham số có giá trị mặc định; thêm tính năng; đổi hành vi theo hướng tương thích ngược | Đọc Release Notes, thử lại luồng liên quan |
| **PATCH** | Sửa lỗi, sửa hiệu năng, sửa UI — không đụng chữ ký, không đổi hành vi có chủ đích | Nâng thẳng |

**Đếm là breaking, dù nhìn có vẻ nhỏ:**

- Đổi giá trị mặc định của tham số (ví dụ `returnVoucherOnApply` từ `false` thành `true`).
- Bỏ một sự kiện khỏi `PromotionSDKCallback`.
- Đổi `SDK_GROUP` — mọi host đang khai toạ độ cũ sẽ không resolve được.
- Đổi tên class iOS đang public.
- Đổi từ `List<...>` sang field phẳng trong một hàm public.

**Không tính là breaking:**

- Đổi bất cứ thứ gì `internal` / không nằm trong `Entry`.
- Thêm cờ tính năng mới (SDK không biết cờ ⇒ mặc định bật).
- Đổi cấu trúc thư mục, tên file, tên biến nội bộ.

---

## 2. Một số version cho cả gói

| Nơi khai | Nền tảng |
|---|---|
| `gradle.properties` → `SDK_VERSION` | Android AAR **và** lõi KMP (hai artifact, **một** số) |
| `iosPromotionSDK/PRM.xcodeproj` → `MARKETING_VERSION` | iOS |

Hai chỗ này **phải trùng nhau** và hiện đồng bộ **thủ công**. Đó là điểm dễ sai nhất khi phát hành —
nó là dòng đầu tiên của [ReleaseChecklist](./ReleaseChecklist.md).

Vì sao không tách số riêng cho lõi: `:AndroidPromotionSDK` khai `implementation(projects.promotionLogic)`,
Gradle ghi version lõi vào POM/`module.json` của `promotion`, nên bump bên nào cũng **phải publish lại
cả hai**. Hai số chỉ tạo thêm một trạng thái sai để nhớ.

---

## 3. Bản `-SNAPSHOT`

Hậu tố `-SNAPSHOT` đẩy artifact sang repo snapshot thay vì release. Dùng cho bản thử giữa kỳ.

- **Không** bàn giao bản `-SNAPSHOT` cho đối tác.
- **Không** dùng `-SNAPSHOT` làm mốc nghiệm thu.

---

## 4. Không ghi đè bản đã phát hành

Bản đã publish là thứ người khác đang build theo. Cả hai script phát hành đều **chặn** ghi đè và bắt
phải `--force` tường minh; `--yes` không đồng nghĩa với đồng ý đè.

Nếu buộc phải đè (chỉ trong trường hợp bản vừa đẩy còn chưa ai lấy):

1. Phải có phê duyệt của người phụ trách phát hành.
2. `--force` sẽ dọn cache local của **đúng** version đó trên máy chạy lệnh.
3. **Máy đồng nghiệp và CI đã kéo bản cũ về thì vẫn giữ nó** — không có cách nào dọn từ xa. Vì vậy
   phương án đúng gần như luôn là **phát hành số mới**, không phải đè.

---

## 5. Vòng đời hỗ trợ

| Trạng thái | Nghĩa |
|---|---|
| **Current** | Bản MINOR/PATCH mới nhất của MAJOR hiện tại — nhận đủ tính năng mới và sửa lỗi |
| **Maintenance** | MAJOR liền trước — chỉ nhận sửa lỗi nghiêm trọng và lỗi bảo mật, trong **6 tháng** kể từ ngày MAJOR mới phát hành |
| **End of life** | Sau đó — không còn bản vá; yêu cầu hỗ trợ sẽ được trả lời bằng hướng dẫn nâng cấp |

Đề nghị với đối tác: nâng cấp trong vòng **một quý** kể từ khi có MAJOR mới.

---

## 6. Chính sách khai tử (deprecation)

Khi một API public sẽ bị bỏ:

1. Ở bản MINOR: đánh dấu `@Deprecated` (Kotlin) / `@available(*, deprecated:)` (Swift), **kèm chỉ dẫn
   thay thế trong chính thông báo**, và ghi vào Release Notes.
2. Giữ hoạt động bình thường suốt vòng đời MAJOR hiện tại.
3. Chỉ xoá ở MAJOR kế tiếp.

Ngoại lệ duy nhất: API rò rỉ thông tin nhạy cảm hoặc gây sai lệch dữ liệu — có thể xoá sớm, kèm thông
báo trực tiếp cho từng đối tác đang tích hợp.

---

## 7. Thông báo cho đối tác

| Loại thay đổi | Thông báo |
|---|---|
| MAJOR | Báo trước ≥ 2 tuần, kèm hướng dẫn nâng cấp trong [ReleaseNotes](./ReleaseNotes.md) và [MigrationGuide](./MigrationGuide.md) |
| MINOR | Gửi Release Notes khi phát hành |
| PATCH | Gửi Release Notes khi phát hành |
| Lỗi bảo mật | Thông báo trực tiếp, không chờ chu kỳ |

---

## 8. Trạng thái hiện tại — cần chốt

`1.0.0` đã phát hành ngày 2026-07-20 kèm cam kết SemVer. Từ đó tới nay `CHANGELOG.md` đã tích luỹ
**hơn 10 thay đổi breaking** (token source, `updateOrderInfo`, `skuSourceId`, chỉ `Entry` public,
bỏ `customerId`, đổi toạ độ Maven…), nhưng `SDK_VERSION` vẫn để `1.0.0`.

**Kết luận:** bản phát hành kế tiếp phải là **`2.0.0`**. Phát hành nó dưới số `1.0.x` hoặc `1.1.0` là
vi phạm chính cam kết ghi trong CHANGELOG và sẽ làm host nâng cấp mà không thử lại — trong khi code
của họ chắc chắn không còn biên dịch được.
