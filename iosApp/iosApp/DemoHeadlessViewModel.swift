//
//  DemoHeadlessViewModel.swift
//  PromotionSDKDemo
//
//  Chế độ headless: đối tác tự dựng UI, chỉ gọi `PromotionSDK.api`.
//
//  Soi gương `DemoHeadlessViewModel.kt` bên Android — cùng tên hàm (`searchVouchers` / `findEligible`
//  / `getVoucherDetail` / `validateDiscounts` / `createRedemption`), cùng 5 bước, cùng thứ tự, cùng
//  chuỗi log. VM chỉ giữ logic gọi SDK + format log; VC chỉ dựng UI và forward tap. Sửa một bên thì
//  sửa cả hai.
//
//  KHÔNG dùng Combine: phơi callback `onLog` / `onLoading` — đối ứng `log: SharedFlow<String>` /
//  `isLoading: StateFlow<Bool>` bên Android.
//

import Foundation
import PromotionKit

final class DemoHeadlessViewModel {

    private let api = PromotionSDK.api

    // ─── Đọc lại giá trị đã set qua PromotionSDK.updateOrderInfo() ─────────────
    private var orderId: String { PromotionSDK.currentOrderId ?? "" }
    private var orderValue: String { PromotionSDK.currentOrderValue ?? "" }

    // ─── State → callback (đối ứng `isLoading` / `log` bên Android) ──────────────
    var onLoading: ((Bool) -> Void)?
    var onLog: ((String) -> Void)?

    // Giữ voucherId lấy từ search để các bước sau dùng
    private var firstVoucherId: String?
    private var validatedVoucherIds: [String] = []

    // ─── SDK context (đối ứng `init {}` bên Android) ────────────────────────────
    /// Gọi sau khi VC đã gán `onLog` để không rớt dòng log đầu.
    func logSdkContext() {
        let session = PromotionSDK.session
        emit("── SDK context ──────────────────────")
        emit("   language    : \(session?.language ?? "(null)")")
        emit("   orderId     : \(PromotionSDK.currentOrderId ?? "(null)")")
        emit("   orderValue  : \(PromotionSDK.currentOrderValue ?? "(null)")")
        emit("   serviceCode : \(PromotionSDK.currentServiceCode ?? "(null)")")
        emit("─────────────────────────────────────")
    }

    // ─── Step 1: Search vouchers ──────────────────────────────────────────────
    func searchVouchers() {
        setLoading(true)
        api.getVouchers(page: 0, size: Self.pageSize) { [weak self] result in
            guard let self else { return }
            switch result {
            case .success(let page):
                let vouchers = page.vouchers
                self.firstVoucherId = vouchers.first?.id
                self.emit("✅ getVouchers")
                self.emit("   vouchers: \(vouchers.count) items (lastPage=\(page.isLastPage))")
                vouchers.prefix(Self.maxLogItems).forEach { self.emit("   - [\($0.id)] \($0.title)") }
                if vouchers.count > Self.maxLogItems {
                    self.emit("   ... +\(vouchers.count - Self.maxLogItems) more")
                }
            case .failure(let error):
                self.emit(self.formatError("getVouchers", error))
            }
            self.setLoading(false)
        }
    }

    // ─── Step 2: Find eligible ────────────────────────────────────────────────
    func findEligible() {
        setLoading(true)
        api.findEligible(
            orderId: orderId,
            orderValue: orderValue,
            items: demoOrderItems(),
            myPage: 0,
            mySize: Self.pageSize,
            otherPage: 0,
            otherSize: Self.pageSize
        ) { [weak self] result in
            guard let self else { return }
            switch result {
            case .success(let data):
                self.emit("✅ findEligible")
                self.emit("   myOffers   : \(data.myOffers.count) items (lastPage=\(data.myIsLastPage))")
                data.myOffers.prefix(Self.maxLogItems).forEach {
                    self.emit("   - [\($0.id)] \($0.name) usable=\($0.usable)")
                }
                self.emit("   otherOffers: \(data.otherOffers.count) items (lastPage=\(data.otherIsLastPage))")
                data.otherOffers.prefix(Self.maxLogItems).forEach {
                    self.emit("   - [\($0.id)] \($0.name) usable=\($0.usable)")
                }
            case .failure(let error):
                self.emit(self.formatError("findEligible", error))
            }
            self.setLoading(false)
        }
    }

    // ─── Step 3: Get voucher detail ───────────────────────────────────────────
    func getVoucherDetail() {
        guard let voucherId = requireVoucherId() else { return }
        setLoading(true)
        api.getVoucherDetail(voucherId: voucherId) { [weak self] result in
            guard let self else { return }
            switch result {
            case .success(let detail):
                self.emit("✅ getVoucherDetail [\(detail.id)]")
                self.emit("   title      : \(detail.title)")
                self.emit("   merchant   : \(detail.merchantName)")
                self.emit("   expires    : \(detail.expireDate ?? "(null)")")
                self.emit("   status     : \(detail.status)")
            case .failure(let error):
                self.emit(self.formatError("getVoucherDetail", error))
            }
            self.setLoading(false)
        }
    }

    // ─── Step 4: Validate discounts ───────────────────────────────────────────
    func validateDiscounts() {
        guard let voucherId = requireVoucherId() else { return }
        setLoading(true)
        api.validateDiscounts(
            orderId: orderId,
            orderValue: orderValue,
            voucherIds: [voucherId]
        ) { [weak self] result in
            guard let self else { return }
            switch result {
            case .success(let data):
                self.validatedVoucherIds = data.items.filter { $0.isValid }.map { $0.objectId }
                self.emit("✅ validateDiscounts")
                self.emit("   overallValid       : \(data.overallValid)")
                self.emit("   totalDiscountAmount: \(data.totalDiscountAmount)")
                self.emit("   finalAmount        : \(data.finalAmount)")
                data.items.forEach {
                    self.emit("   [\($0.objectId)] valid=\($0.isValid) discount=\($0.discountAmount)")
                }
            case .failure(let error):
                self.emit(self.formatError("validateDiscounts", error))
            }
            self.setLoading(false)
        }
    }

    // ─── Step 5: Create redemption ────────────────────────────────────────────
    func createRedemption() {
        guard !validatedVoucherIds.isEmpty else {
            emit("⚠️ Chưa validate, hãy Validate trước")
            return
        }
        setLoading(true)
        api.createRedemption(
            orderId: orderId,
            orderValue: orderValue,
            voucherIds: validatedVoucherIds
        ) { [weak self] result in
            guard let self else { return }
            switch result {
            case .success(let data):
                self.emit("✅ createRedemption")
                self.emit("   sessionId    : \(data.sessionId)")
                self.emit("   totalDiscount: \(data.totalDiscount)")
                self.emit("   finalAmount  : \(data.finalAmount)")
                self.emit("   hasErrors    : \(!data.validationErrors.isEmpty)")
                data.validationErrors.forEach { self.emit("   ⚠️ \($0.code): \($0.message)") }
            case .failure(let error):
                self.emit(self.formatError("createRedemption", error))
            }
            self.setLoading(false)
        }
    }

    private func requireVoucherId() -> String? {
        guard let firstVoucherId else {
            emit("⚠️ Chưa có voucherId, hãy Search trước")
            return nil
        }
        return firstVoucherId
    }

    /// Dòng đơn hàng giả lập — `findEligible` cần items để lấy campaign theo SKU (rỗng thì chỉ nhận
    /// campaign cấp đơn). Context của SDK không đọc ngược ra `orderItems` được, nên demo tự dựng.
    private func demoOrderItems() -> [PromotionOrderItem] {
        [PromotionOrderItem(
            skuSourceId: Self.demoSkuId,
            productId: Self.demoProductId,
            quantity: 1,
            unitPrice: orderValue
        )]
    }

    private func setLoading(_ loading: Bool) { onLoading?(loading) }

    private func emit(_ msg: String) { onLog?(msg) }

    /// `❌ tên: [type] message (serverCode=…)` — cùng định dạng với bên Android.
    private func formatError(_ name: String, _ error: PromotionSDKError) -> String {
        let serverCode = error.serverCode.map { " (serverCode=\($0))" } ?? ""
        let message = error.errorDescription ?? error.localizedDescription
        return "❌ \(name): [\(errorType(error))] \(message)\(serverCode)"
    }

    private func errorType(_ error: PromotionSDKError) -> String {
        switch error {
        case .networkFailure:  return "networkFailure"
        case .sessionExpired:  return "sessionExpired"
        case .timeout:         return "timeout"
        case .parseFailed:     return "parseFailed"
        case .featureDisabled: return "featureDisabled"
        // Hai case mới: lỗi NGHIỆP VỤ của server (trước đây bị nhét vào `.networkFailure` với
        // `message = errorCode`), và ca gọi `api` trước `initialize` (trước đây crash app).
        case .businessRule(let code, _): return "businessRule(\(code))"
        case .notInitialized:  return "notInitialized"
        case .unknown:         return "unknown"
        }
    }

    private static let pageSize = 10
    private static let maxLogItems = 3
    private static let demoSkuId = "SKU-01"
    private static let demoProductId = "P-01"
}
