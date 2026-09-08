# Giấy phép sử dụng — TTCN Promotion SDK

> ⚠️ **BẢN NHÁP — CẦN BỘ PHẬN PHÁP CHẾ PHÊ DUYỆT TRƯỚC KHI BÀN GIAO.**
> File này là khung điều khoản do đội kỹ thuật soạn để bộ hồ sơ bàn giao không thiếu mục giấy phép.
> Nó **không** phải văn bản pháp lý đã được rà soát. Thay `<CHỦ SỞ HỮU>` và đối chiếu với hợp đồng
> đã ký với đối tác trước khi phát hành.

Bản quyền © 2026 `<CHỦ SỞ HỮU>`. Bảo lưu mọi quyền.

## Mục lục

<!-- toc -->
- [1. Phạm vi](#1-phạm-vi)
- [2. Quyền được cấp](#2-quyền-được-cấp)
- [3. Hạn chế](#3-hạn-chế)
- [4. Sở hữu trí tuệ](#4-sở-hữu-trí-tuệ)
- [5. Thành phần mã nguồn mở](#5-thành-phần-mã-nguồn-mở)
- [6. Dữ liệu & bảo mật](#6-dữ-liệu--bảo-mật)
- [7. Hỗ trợ & vòng đời](#7-hỗ-trợ--vòng-đời)
- [8. Miễn trừ bảo đảm](#8-miễn-trừ-bảo-đảm)
- [9. Giới hạn trách nhiệm](#9-giới-hạn-trách-nhiệm)
- [10. Chấm dứt](#10-chấm-dứt)
- [11. Liên hệ](#11-liên-hệ)
<!-- /toc -->

---

## 1. Phạm vi

Giấy phép này áp dụng cho **TTCN Promotion SDK** — bao gồm thư viện Android
(`vn.viettelpay.library:promotion`, `vn.viettelpay.library:promotionLogic`), framework iOS
(`Promotion.xcframework`), tài liệu đi kèm và mọi bản cập nhật được cung cấp theo hợp đồng.

Đây là **phần mềm độc quyền**, không phải mã nguồn mở.

## 2. Quyền được cấp

Bên nhận được cấp quyền **không độc quyền, không chuyển nhượng, có thể thu hồi** để:

- Tích hợp SDK vào ứng dụng di động do bên nhận sở hữu và phát hành.
- Phân phối SDK **dưới dạng nhúng trong ứng dụng đã biên dịch** của bên nhận tới người dùng cuối.
- Sao chép SDK trong nội bộ phục vụ phát triển, kiểm thử và sao lưu.

## 3. Hạn chế

Bên nhận **không được**:

- Phân phối, bán, cho thuê, cấp phép lại SDK dưới dạng thư viện độc lập cho bên thứ ba.
- Dịch ngược, tháo rời, hoặc tìm cách khôi phục mã nguồn, trừ phạm vi luật pháp cho phép rõ ràng.
- Gỡ bỏ hoặc che giấu thông báo bản quyền, nhãn hiệu trong SDK và tài liệu.
- Sử dụng thành phần nội bộ (mọi thứ ngoài bề mặt công khai `com.ttcn.prm.entry.**` và module `PRM`)
  — bao gồm cả việc tiếp cận qua Java, reflection hay công cụ tương đương.
- Sửa đổi SDK rồi phát hành như sản phẩm của mình.

## 4. Sở hữu trí tuệ

SDK được **cấp phép, không phải bán**. Mọi quyền sở hữu trí tuệ thuộc về `<CHỦ SỞ HỮU>`.

## 5. Thành phần mã nguồn mở

SDK có sử dụng thư viện mã nguồn mở của bên thứ ba. Việc sử dụng các thư viện đó tuân theo giấy phép
riêng của chúng, được liệt kê tại [`THIRD_PARTY_NOTICES.md`](./THIRD_PARTY_NOTICES.md). Trong phạm vi
xung đột, giấy phép của thư viện bên thứ ba **được ưu tiên áp dụng cho chính thư viện đó**.

## 6. Dữ liệu & bảo mật

SDK không thu thập dữ liệu cho `<CHỦ SỞ HỮU>`; mọi yêu cầu mạng chỉ gửi tới máy chủ do bên nhận cấu
hình. Chi tiết tại [`docs/common/Security.md`](./docs/common/Security.md).

Bên nhận chịu trách nhiệm tuân thủ pháp luật về bảo vệ dữ liệu cá nhân đối với dữ liệu người dùng
cuối trong ứng dụng của mình.

## 7. Hỗ trợ & vòng đời

Phạm vi hỗ trợ, thời gian phản hồi và vòng đời phiên bản theo
[`docs/release/VersioningPolicy.md`](./docs/release/VersioningPolicy.md) và hợp đồng đã ký.

## 8. Miễn trừ bảo đảm

SDK được cung cấp **"NGUYÊN TRẠNG"**, không kèm bảo đảm dưới bất kỳ hình thức nào, dù rõ ràng hay
ngụ ý, bao gồm nhưng không giới hạn ở bảo đảm về khả năng thương mại, sự phù hợp cho một mục đích cụ
thể và không vi phạm quyền của bên thứ ba — trừ phần được quy định khác trong hợp đồng đã ký.

## 9. Giới hạn trách nhiệm

Trong phạm vi pháp luật cho phép, `<CHỦ SỞ HỮU>` không chịu trách nhiệm cho thiệt hại gián tiếp, ngẫu
nhiên, đặc biệt hoặc do hậu quả phát sinh từ việc sử dụng SDK — trừ phần được quy định khác trong hợp
đồng đã ký.

## 10. Chấm dứt

Giấy phép chấm dứt tự động khi bên nhận vi phạm điều khoản nêu trên. Khi chấm dứt, bên nhận phải ngừng
sử dụng và gỡ SDK khỏi các bản phát hành mới của ứng dụng.

## 11. Liên hệ

Mọi câu hỏi về giấy phép: qua đầu mối hợp đồng của `<CHỦ SỞ HỮU>`.
