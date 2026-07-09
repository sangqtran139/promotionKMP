//
//  PromotionSDKApi.swift
//  PromotionSDK
//
//  Public headless API for partners — closure-based only.
//  No Kotlin, RxSwift or internal SPM types in the public interface.
//

import Foundation
@_implementationOnly import PromotionKit

/// Bề mặt API headless cho đối tác. **Không phải use case** — nó là **ranh giới chuyển đổi** giữa
/// lõi Kotlin và app host. Toàn bộ nghiệp vụ (gác feature flag, bắt lỗi, chuẩn hoá `errorCode`)
/// nằm trong `PromotionUseCases` của lõi, dùng chung với Android; lớp này chỉ uỷ quyền rồi map.
///
/// (Tên cũ `PromotionSDKUseCases` gây hiểu lầm đúng ở điểm đó.)
///
/// Việc nó làm, và chỉ một việc: đổi model Kotlin sang DTO Swift (`PromotionVoucher`,
/// `PromotionEligibleOffer`, …), và đổi `PromotionResult` sang `Result` cho quen tay Swift.
///
/// **Vì sao bắt buộc phải map, không trả thẳng model Kotlin?**
/// iOS ship **một** xcframework: Kotlin được link tĩnh và giấu sau `@_implementationOnly import`.
/// Type nào xuất hiện trong API public sẽ bị ghi vào `.swiftinterface` của framework, kéo theo
/// `import PromotionKit` — module mà app host không có. Host sẽ **không build được**:
///
///     error: Unable to find module dependency: 'PromotionKit'
///
/// Android không gặp chuyện này vì nó ship **hai** AAR và khai `api(projects.promotionLogic)`,
/// nên host thấy thẳng type lõi và không cần mapper. Muốn iOS bỏ mapper thì phải ship kèm
/// `PromotionLogic.xcframework` — tức đổi mô hình phân phối.
public final class PromotionSDKApi {

    // MARK: - Private

    private let customerId: String
    private let token: String?
    private let useCases: PromotionUseCases

    init(
        customerId: String,
        token: String?,
        useCases: PromotionUseCases = PromotionUseCases()
    ) {
        self.customerId = customerId
        self.token = token
        self.useCases = useCases
    }

    // MARK: - PromotionResult → Result

    /// `PromotionUseCases` không ném lỗi nghiệp vụ: nó trả `PromotionResult.Failure` kèm `errorCode`.
    /// Chỉ `CancellationException` mới thoát ra thành `Error` của Swift.
    ///
    /// - Parameter onEmpty: xử lý `NO_RESULT` (server trả `data: null`). Danh sách coi là rỗng và
    ///   vẫn thành công; còn chi tiết / validate / redemption thì đó là `.parseFailed`.
    ///   Giữ đúng hành vi trước khi lớp này chuyển sang gọi lõi.
    private func handle<Model, Out>(
        _ completion: @escaping (Result<Out, PromotionSDKError>) -> Void,
        onEmpty: @escaping () -> Result<Out, PromotionSDKError> = { .failure(.parseFailed) },
        map: @escaping (Model) -> Out,
        call: @escaping () async throws -> any PromotionResult
    ) {
        Task { @MainActor in
            do {
                let result = try await call()
                if let failure = result as? PromotionResultFailure {
                    if failure.errorCode == PromotionErrorCodes.shared.NO_RESULT {
                        completion(onEmpty())
                    } else {
                        completion(.failure(Self.mapFailure(failure)))
                    }
                } else if let success = result as? PromotionResultSuccess<AnyObject> {
                    guard let model = success.data as? Model else {
                        completion(.failure(.parseFailed))
                        return
                    }
                    completion(.success(map(model)))
                } else {
                    // Không phải Success cũng không phải Failure: giả định về cầu nối Kotlin↔Swift
                    // đã sai (vd `PromotionResult` thêm biến thể mới). Đây **không** phải lỗi dữ
                    // liệu — đừng để nó giả dạng `.parseFailed`, sẽ đi tìm bug ở nhầm chỗ.
                    completion(.failure(.unknown(Self.bridgeMismatch(result))))
                }
            } catch {
                completion(.failure(.unknown(error)))
            }
        }
    }

    private static func bridgeMismatch(_ result: any PromotionResult) -> NSError {
        NSError(
            domain: "PromotionSDK",
            code: -1,
            userInfo: [NSLocalizedDescriptionKey:
                "PromotionResult không rõ biến thể: \(type(of: result)). Kiểm tra PromotionSDKApi.handle."]
        )
    }

    private static func mapFailure(_ failure: PromotionResultFailure) -> PromotionSDKError {
        let codes = PromotionErrorCodes.shared
        switch failure.errorCode {
        case codes.FEATURE_DISABLED:
            return .featureDisabled
        case codes.TIMEOUT:
            return .timeout
        case codes.NETWORK_ERROR:
            return .networkFailure(code: nil, message: failure.message ?? "")
        default:
            // Mã nghiệp vụ của server (vd VOUCHER_EXPIRED) đi kèm httpStatus nếu có.
            return .networkFailure(code: failure.httpStatus?.intValue, message: failure.message ?? "")
        }
    }

    // MARK: - Mapping sang DTO public

    private static func toVoucher(_ p: VoucherItem) -> PromotionVoucher {
        PromotionVoucher(
            id: p.voucherId,
            merchantName: p.merchantName ?? "",
            title: p.title ?? "",           // tên ưu đãi
            imageURL: p.logo,
            expireDate: PromotionDate.parse(p.expirationDate),
            isUsed: p.displayState() == .used,
            status: p.status,
            displayStatusLabel: p.displayStatusLabel
        )
    }

    private static func toOffer(_ p: EligibleOffer) -> PromotionEligibleOffer {
        PromotionEligibleOffer(
            id: p.id,
            name: p.campaignName ?? "",
            objectType: p.objectType,
            usable: p.usable,
            estimatedDiscount: p.estimatedDiscount,
            expireDate: PromotionDate.parse(p.expireDate),
            // Lõi không dựng sẵn câu tiếng Việt — trả rule thô cho host tự hiển thị.
            ineligibleReason: p.usable ? nil : p.unmatchedRules.first
        )
    }

    // MARK: - Public API

    /// Lấy voucher **của khách** (Search Customer Vouchers) — chỉ voucher đã sở hữu.
    /// Để lấy "Ưu đãi khác" (campaign chưa sở hữu, đủ điều kiện cho đơn) dùng `findEligible(...)`.
    /// - Tăng `myPage` để load thêm. Lọc tuỳ chọn: `keyword`, `serviceCode`, `tab` ("all"/"expiring_soon").
    /// - Cờ `VOUCHER_LIST` TẮT → trả `.featureDisabled`.
    public func getVouchers(
        keyword: String? = nil,
        serviceCode: String? = nil,
        tab: String? = nil,
        myPage: Int = 0,
        mySize: Int = 10,
        completion: @escaping (Result<PromotionVoucherPage, PromotionSDKError>) -> Void
    ) {
        // Token do host cấp qua `PromotionRequestContextProvider` của lõi.
        let request = SearchCustomerVouchersRequest(
            customerId: customerId,
            keyword: keyword,
            serviceCode: serviceCode,
            tab: tab ?? "all",
            page: boxed(myPage),
            size: boxed(mySize)
        )
        let useCases = self.useCases
        handle(
            completion,
            onEmpty: { .success(PromotionVoucherPage(myVouchers: [], myIsLastPage: true)) },
            map: { (model: SearchCustomerVouchersResult) in
                PromotionVoucherPage(
                    myVouchers: model.content.map(Self.toVoucher),
                    myIsLastPage: model.last?.boolValue ?? true
                )
            },
            call: { try await useCases.searchVouchers(request: request) }
        )
    }

    /// Tìm ưu đãi đủ điều kiện cho đơn (Find Eligible Campaigns) — luồng "Chọn ưu đãi" khi checkout.
    /// Trả 2 nhóm "của tôi" (voucher đã sở hữu) + "khác" (campaign công khai chưa sở hữu), phân trang ĐỘC LẬP.
    /// - `items`: dòng đơn hàng — bắt buộc để lấy campaign theo SKU (rỗng → chỉ campaign cấp đơn).
    /// - Tăng `myPage`/`otherPage` để load thêm từng nhóm.
    /// - Cờ `VOUCHER_SELECTION` TẮT → trả `.featureDisabled`.
    public func findEligible(
        orderId: String,
        orderValue: String,
        items: [PromotionOrderItem] = [],
        tabCode: String? = nil,
        myPage: Int = 0,
        mySize: Int = 10,
        otherPage: Int = 0,
        otherSize: Int = 10,
        completion: @escaping (Result<PromotionEligibleResult, PromotionSDKError>) -> Void
    ) {
        let orderItems = items.map {
            EligibleOrderItem(
                skuId: $0.skuId,
                quantity: Int32($0.quantity),
                unitPrice: $0.unitPrice,
                orderItemId: nil,
                productId: $0.productId,
                productName: $0.productName,
                productCategory: $0.productCategory
            )
        }
        let request = FindEligibleCampaignsRequest(
            customerId: customerId,
            orderId: orderId,
            orderValue: orderValue,
            items: orderItems,
            currency: "VND",
            channel: "MOBILE",
            customerType: nil, segment: nil, tier: nil,
            tabCode: tabCode,
            section: nil,
            myPage: Int32(myPage), mySize: Int32(mySize),
            otherPage: Int32(otherPage), otherSize: Int32(otherSize),
            filterOptions: EligibleFilterOptions(includeExpired: false, checkBudgetAvailability: true, includePreview: true)
        )
        let useCases = self.useCases
        handle(
            completion,
            onEmpty: {
                .success(PromotionEligibleResult(myOffers: [], otherOffers: [], myIsLastPage: true, otherIsLastPage: true))
            },
            map: { (model: EligibleOffersResult) in
                PromotionEligibleResult(
                    myOffers: model.myOffers.map(Self.toOffer),
                    otherOffers: model.otherOffers.map(Self.toOffer),
                    myIsLastPage: model.myIsLastPage,
                    otherIsLastPage: model.otherIsLastPage
                )
            },
            call: { try await useCases.findEligible(request: request) }
        )
    }

    /// Lấy chi tiết 1 voucher của khách (Get Customer Voucher Detail).
    /// - `serviceCode`: tuỳ chọn — lọc thông tin theo dịch vụ đang thanh toán.
    /// - Cờ `VOUCHER_DETAIL` TẮT → trả `.featureDisabled`.
    public func getVoucherDetail(
        voucherId: String,
        serviceCode: String? = nil,
        completion: @escaping (Result<PromotionVoucherDetail, PromotionSDKError>) -> Void
    ) {
        let useCases = self.useCases
        let customerId = self.customerId
        handle(
            completion,
            map: { (model: VoucherDetail) in
                PromotionVoucherDetail(
                    id: model.voucherId,
                    merchantName: model.merchantName ?? "",
                    title: model.title ?? "",
                    descriptionText: model.description_ ?? "",
                    guideline: model.guideline ?? "",
                    startDate: PromotionDate.parse(model.startDate),
                    expireDate: PromotionDate.parse(model.expirationDate),
                    bannerURL: model.banner,
                    logoURL: model.logo,
                    status: model.status ?? "",
                    displayStatusLabel: model.displayStatusLabel
                )
            },
            call: {
                try await useCases.getVoucherDetail(voucherId: voucherId, customerId: customerId, service: serviceCode)
            }
        )
    }

    /// Validate a set of voucher IDs against an order before applying.
    /// - Cờ `VOUCHER_APPLY` TẮT → trả `.featureDisabled`.
    public func validateDiscounts(
        orderId: String,
        orderValue: String,
        voucherIds: [String],
        objectType: String = "CAMPAIGN",
        completion: @escaping (Result<PromotionValidationResult, PromotionSDKError>) -> Void
    ) {
        let items = voucherIds.map { DiscountItemRequest(objectId: $0, objectType: objectType) }
        let request = ValidateDiscountsRequest(
            customerId: customerId,
            orderId: orderId,
            orderValue: orderValue,
            items: items
        )
        let useCases = self.useCases
        handle(
            completion,
            map: { (result: ValidateDiscountsResult) in
                PromotionValidationResult(
                    overallValid: result.overallValid,
                    totalDiscountAmount: result.totalDiscountAmount,
                    finalAmount: result.finalAmount,
                    items: result.items.map {
                        PromotionDiscountItem(
                            objectId: $0.objectId,
                            discountAmount: $0.calculatedDiscount,
                            isValid: $0.valid,
                            eligibilityStatus: $0.eligibilityStatus
                        )
                    }
                )
            },
            call: { try await useCases.validateDiscounts(request: request) }
        )
    }

    /// Create a redemption session to confirm payment with selected vouchers.
    /// - Cờ `VOUCHER_REDEEM` TẮT → trả `.featureDisabled`.
    public func createRedemption(
        orderId: String,
        orderValue: String,
        voucherIds: [String],
        objectType: String = "CAMPAIGN",
        completion: @escaping (Result<PromotionRedemptionResult, PromotionSDKError>) -> Void
    ) {
        let items = voucherIds.map {
            RedemptionItemRequest(objectId: $0, objectType: objectType, expectedDiscount: nil)
        }
        let request = CreateRedemptionRequest(
            customerId: customerId,
            orderId: orderId,
            orderValue: orderValue,
            items: items
        )
        let useCases = self.useCases
        handle(
            completion,
            map: { (result: CreateRedemptionResult) in
                PromotionRedemptionResult(
                    sessionId: result.sessionId,
                    totalDiscount: result.totalDiscount,
                    finalAmount: result.finalAmount,
                    validationErrors: result.validationErrors.map {
                        PromotionRedemptionError(code: $0.code, message: $0.message)
                    }
                )
            },
            call: { try await useCases.createRedemption(request: request) }
        )
    }
}
