# Lệch giữa app và tài liệu nghiệp vụ (TLNV)

Đối chiếu code với bộ TLNV trong [`docs/tlnv/`](../tlnv/) — rà soát ngày **2026-07-28**:

| Tài liệu | Bản |
|---|---|
| `PRM_KBNV_MOB_000_Danh mục dùng chung (Mobile)` | v7 |
| `PRM_KBNV_MOB_001_Ưu đãi của tôi` | v54 |
| `PRM_KBNV_MOB_002_Ưu đãi của tôi_Xem chi tiết` | v42 |
| `PRM_KBNV_MOB_003_Đánh dấu đã sử dụng` | v4 (mới, **nội dung còn rỗng** — chỉ template) |
| `PRM_KBNV_MOB_004_Áp dụng ưu đãi` | v39 (trước là v29) |

> `PRM_KBNV_API001_ON/OFF tính năng` **không còn trong repo** dù MOB_001 và MOB_004 vẫn refer tới nó
> cho luồng kiểm tra feature flag. Cần khôi phục nếu xoá nhầm.

File này chỉ liệt kê **phần còn lệch**. Phần đã sửa xem CHANGELOG + doc tính năng tương ứng.

## Mục lục

<!-- toc -->
- [1. Lỗi nghiệp vụ: TLNV bắt Confirmation Dialog, app dùng toast — mà toast đang TẮT](#1-lỗi-nghiệp-vụ-tlnv-bắt-confirmation-dialog-app-dùng-toast--mà-toast-đang-tắt)
- [2. Màn "Chọn ưu đãi": TLNV v39 bỏ auto-search, thêm nút "Kiểm tra"](#2-màn-chọn-ưu-đãi-tlnv-v39-bỏ-auto-search-thêm-nút-kiểm-tra)
- [3. Chọn 1 ưu đãi phải disable các ưu đãi khác](#3-chọn-1-ưu-đãi-phải-disable-các-ưu-đãi-khác)
- [4. Chuỗi "không tìm thấy" thiếu vế sau](#4-chuỗi-không-tìm-thấy-thiếu-vế-sau)
- [5. "Không hết hạn": app thêm tiền tố "HSD:"](#5-không-hết-hạn-app-thêm-tiền-tố-hsd)
- [6. Thiếu banner "Săn thêm ưu đãi"](#6-thiếu-banner-săn-thêm-ưu-đãi)
- [7. Không tính lệch — phase sau, TLNV cũng chưa chốt](#7-không-tính-lệch--phase-sau-tlnv-cũng-chưa-chốt)
<!-- /toc -->

---

## 1. Lỗi nghiệp vụ: TLNV bắt Confirmation Dialog, app dùng toast — mà toast đang TẮT

**TLNV:** MOB_000 item #6 định nghĩa Confirmation Dialog (header "Thông báo", 1 nút "Đóng"). MOB_001
bước 4-5(2), MOB_002 bước 4(2), MOB_004 bước 5 & 7 đều yêu cầu popup này, kèm hành vi "Click Đóng:
giữ nguyên từ khoá / giữ nguyên màn hình".

**App:** đã bỏ `PRMConfirmationDialog`, chuyển toàn bộ sang toast (quyết định 2026-07-24, xem
[`ErrorHandling.md`](./ErrorHandling.md)), **và SDK đã bỏ hẳn toast** → phần lớn
lỗi hiện **không hiển thị gì**. Ngoại lệ duy nhất đang hiện là PRM_MOB_021 (cờ tính năng tắt).

**Mức độ:** cao — user không nhận được phản hồi khi API lỗi.

---

## 2. Màn "Chọn ưu đãi": TLNV v39 bỏ auto-search, thêm nút "Kiểm tra"

**TLNV:** MOB_004 control 2.2 — nút "Kiểm tra" (Enable khi có từ khoá, Disable khi rỗng);
*"Sau khi nhập từ khoá không autocomplete, chỉ search khi bấm Kiểm tra"*.

**App:** vẫn auto-search theo từng ký tự (`ChoosePromotionFragment.setupSearch` → `QueryChanged`,
store debounce 400ms). Chuỗi `prm_check` = "Kiểm tra" **đã có trong `strings.xml` nhưng không nơi nào
dùng**.

**Lưu ý:** màn "Ưu đãi của tôi" thì ngược lại — MOB_001 control 5.3 yêu cầu **tự động tìm kiếm**,
app đang đúng. Đừng sửa nhầm cả hai theo một kiểu.

---

## 3. Chọn 1 ưu đãi phải disable các ưu đãi khác

**TLNV:** MOB_004 control 2.4 (Phase 1) — "Cho phép chọn 1 ưu đãi trên màn hình. Khi checked 1 ưu
đãi: **disable không cho phép chọn các ưu đãi khác**".

**App:** `ChoosePromotionStore.onToggleSelection` — chọn đơn (`isMultiSelection = false`), nhưng bấm
voucher khác thì **thay thế** lựa chọn thay vì bị chặn. Kết quả cuối giống nhau (vẫn 1 voucher), khác
ở phản hồi UI.

---

## 4. Chuỗi "không tìm thấy" thiếu vế sau

**TLNV:** MOB_001 bước 4-5(1) và MOB_002 bước 4(1) — *"Không tìm thấy kết quả phù hợp. **Khám phá
thêm các đề xuất phù hợp với bạn nhé.**"*

**App:** `prm_no_result` / `prm_search_no_result` = "Không tìm thấy kết quả phù hợp" (thiếu câu 2).

Empty-view màn "Ưu đãi của tôi" đang dùng "Ngàn deal HOT chờ bạn" / "Lấp đầy kho quà…" — TLNV để
`<Q&A: Bổ sung UI/UX>` cho ca danh sách rỗng nên **chưa chốt**, không tính là lệch.

---

## 5. "Không hết hạn": app thêm tiền tố "HSD:"

**TLNV:** MOB_001 control 4.4 — "TH Ngày hết hạn được cấu hình 'Không hết hạn': hiển thị text
**'Không hết hạn'**".

**App:** hiện **"HSD: Không hết hạn"** (`prm_expiry_never` / `PromotionUIStrings.expiryNever`) — chốt
theo yêu cầu trực tiếp 2026-07-28. Giữ nguyên, ghi lại để không bị coi là bug.

---

## 6. Thiếu banner "Săn thêm ưu đãi"

**TLNV:** MOB_001 control 4.6 — banner (Image + Hyperlink) dưới danh sách, click → đi màn Ưu đãi.

**App:** không có trong `fragment_my_promotion.xml` và màn iOS tương ứng.

---

## 7. Không tính lệch — phase sau, TLNV cũng chưa chốt

- **MOB_003 "Đánh dấu đã sử dụng / tái sử dụng VTM"**: tài liệu **rỗng** (mọi mục còn placeholder,
  bảng luồng nghiệp vụ trống). App chưa có màn này là đúng.
- **Sorting Mức 1** theo sorting rule (MOB_001 control #4) — TLNV ghi `<to do: phase sau>`.
- **Stacking rules** — áp dụng đồng thời nhiều ưu đãi (MOB_004 bước 6.(5)) — `<phase sau>`.
- **Auto apply** (MOB_004 bước 1) — TLNV ghi "hiện tại chưa làm".
- **Đánh dấu đã sử dụng khi thanh toán xong** (MOB_004 bước 9) — chờ BE define redemption status.
- **`type = EXCLUDED` của `applicableProducts`**: `servicesForApplicableProducts` chỉ so `productId`,
  chưa đọc `type` → SKU bị loại trừ vẫn hiện trong sheet "Chọn dịch vụ". TLNV chưa mô tả rõ cách xử
  lý; xem [`PromotionDetail.md`](../features/PromotionDetail.md).
- **Nhiều SKU chung một `productId`**: sheet gộp thành 1 dòng (khoá khớp là `productId`, không phải
  `sku`) nên voucher 4 sản phẩm có thể chỉ ra 3 dịch vụ. Muốn tách phải đổi khoá khớp — quyết định
  chưa có.
