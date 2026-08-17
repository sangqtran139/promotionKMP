//
//  PromotionSDKApi.swift
//  PromotionSDK
//
//  Public headless API for partners — closure-based only.
//  No Kotlin or internal SPM types in the public interface.
//

import Foundation
@_implementationOnly import PRMKotlinBridge

/// Bề mặt API headless cho đối tác. **Không phải use case** — nó là **ranh giới chuyển đổi** giữa
/// lõi Kotlin và app host. Toàn bộ nghiệp vụ (gác feature flag, bắt lỗi, chuẩn hoá `errorCode`)
/// nằm trong `PromotionUseCases` của lõi, dùng chung với Android; lớp này chỉ uỷ quyền rồi map.
///
/// (Tên cũ `PromotionSDKUseCases` gây hiểu lầm đúng ở điểm đó.)
///
/// Việc nó làm, và chỉ một việc: đổi model Kotlin sang DTO Swift (`PromotionVoucher`,
/// `PromotionEligibleOffer`, …), và đổi `PromotionResult` sang `PromotionApiResult`.
///
/// **Song ánh với `PromotionSDKApi.kt` bên Android**: cùng 5 hàm, cùng tên tham số, cùng DTO, cùng
/// thứ tự khai báo, cùng cách xử lý `NO_RESULT`. Khác duy nhất ở kiểu bất đồng bộ — Swift dùng
/// closure, Kotlin dùng `suspend`. Sửa một bên thì sửa cả hai.
///
/// Ràng buộc: Kotlin được link tĩnh và giấu sau `@_implementationOnly import`, nên type nào lọt vào
/// API public sẽ bị ghi vào `.swiftinterface` và kéo theo `import PRMKotlinBridge` — module app host
/// không có (`error: Unable to find module dependency: 'PRMKotlinBridge'`).
///
/// Android chịu ràng buộc tương đương nhưng nhẹ hơn: nó khai `implementation(projects.promotionLogic)`
/// nên `com.ttcn.promotionsdk.*` nằm ngoài compile classpath của host, và type lõi lọt vào chữ
/// ký public sẽ khiến host không resolve được.
public final class PromotionSDKApi {

    // MARK: - Private

    /// Định danh khách không truyền từng request — BFF lấy từ JWT `sub`. Token host cấp qua
    /// `PromotionRequestContextProvider` của lõi ở tầng network.
    private let useCases: PromotionUseCases

    init(useCases: PromotionUseCases = PromotionUseCases()) {
        self.useCases = useCases
    }

    // MARK: - Public API

    /// Lấy voucher **của khách** (Search Customer Vouchers) — chỉ voucher đã sở hữu.
    /// Để lấy "Ưu đãi khác" (campaign chưa sở hữu, đủ điều kiện cho đơn) dùng `findEligible(...)`.
    ///
    /// Tăng `page` để load thêm. Cờ `VOUCHER_LIST` TẮT → `.featureDisabled`.
    ///
    /// - Parameter tab: "all" hoặc "expiring_soon".
    public func getVouchers(
        keyword: String? = nil,
        serviceCode: String? = nil,
        tab: String? = nil,
        page: Int = 0,
        size: Int = 10,
        completion: @escaping (PromotionApiResult<PromotionVoucherPage>) -> Void
    ) {
        // Token do host cấp qua `PromotionRequestContextProvider` của lõi.
        let request = SearchCustomerVouchersRequest(
            keyword: keyword,
            serviceCode: serviceCode,
            tab: tab ?? "all",
            page: boxed(page),
            size: boxed(size)
        )
        let useCases = self.useCases
        handle(
            completion,
            onEmpty: { .success(PromotionVoucherPage(vouchers: [], isLastPage: true)) },
            map: { (model: SearchCustomerVouchersResult) in
                PromotionVoucherPage(
                    vouchers: model.content.map(Self.toVoucher),
                    isLastPage: model.last?.boolValue ?? true,
                    expireWarningDate: model.expireWarningDate?.intValue
                )
            },
            call: { try await useCases.searchVouchers(request: request) }
        )
    }

    /// Tìm ưu đãi đủ điều kiện cho đơn (Find Eligible Campaigns) — luồng "Chọn ưu đãi" khi checkout.
    /// Trả 2 nhóm: "của tôi" (voucher đã sở hữu) và "khác" (campaign công khai chưa sở hữu), phân
    /// trang ĐỘC LẬP — tăng `myPage` / `otherPage` để load thêm từng nhóm.
    ///
    /// Cờ `VOUCHER_SELECTION` TẮT → `.featureDisabled`.
    ///
    /// - Parameter items: dòng đơn hàng — bắt buộc để lấy campaign theo SKU (rỗng → chỉ campaign cấp đơn).
    public func findEligible(
        orderId: String,
        orderValue: String,
        items: [PromotionOrderItem] = [],
        tabCode: String? = nil,
        myPage: Int = 0,
        mySize: Int = 10,
        otherPage: Int = 0,
        otherSize: Int = 10,
        completion: @escaping (PromotionApiResult<PromotionEligibleResult>) -> Void
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
            orderId: orderId,
            orderValue: orderValue,
            items: orderItems,
            currency: "VND",
            channel: "MOBILE",
            customerType: nil, segment: nil, tier: nil,
            // Ba tham số này Kotlin có default nhưng Obj-C bridging KHÔNG mang default sang, nên
            // Swift buộc phải truyền đủ (xem commit "update serviceCode -> productId…").
            orderDate: nil, orderMetadata: nil, scenario: nil,
            tabCode: tabCode,
            section: nil,
            keyword: nil,
            myPage: Int32(myPage), mySize: Int32(mySize),
            otherPage: Int32(otherPage), otherSize: Int32(otherSize),
            filterOptions: EligibleFilterOptions(campaignTypes: nil, discountTypes: nil, includeExpired: false, checkBudgetAvailability: true, includePreview: true)
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
                    otherIsLastPage: model.otherIsLastPage,
                    expireWarningDate: model.expireWarningDate?.intValue
                )
            },
            call: { try await useCases.findEligible(request: request) }
        )
    }

    /// Lấy chi tiết 1 voucher của khách (Get Customer Voucher Detail).
    /// Cờ `VOUCHER_DETAIL` TẮT → `.featureDisabled`.
    ///
    /// - Parameter serviceCode: lọc thông tin theo dịch vụ đang thanh toán.
    public func getVoucherDetail(
        voucherId: String,
        serviceCode: String? = nil,
        completion: @escaping (PromotionApiResult<PromotionVoucherDetail>) -> Void
    ) {
        let useCases = self.useCases
        handle(
            completion,
            map: Self.toVoucherDetail,
            call: {
                try await useCases.getVoucherDetail(voucherId: voucherId, service: serviceCode)
            }
        )
    }

    /// Validate một tập voucher với đơn hàng trước khi áp.
    /// Cờ `VOUCHER_APPLY` TẮT → `.featureDisabled`.
    public func validateDiscounts(
        orderId: String,
        orderValue: String,
        voucherIds: [String],
        objectType: String = "CAMPAIGN",
        completion: @escaping (PromotionApiResult<PromotionValidationResult>) -> Void
    ) {
        let items = voucherIds.map { DiscountItemRequest(objectId: $0, objectType: objectType) }
        let request = ValidateDiscountsRequest(
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

    /// Tạo redemption session để xác nhận thanh toán với voucher đã chọn.
    /// Cờ `VOUCHER_REDEEM` TẮT → `.featureDisabled`.
    public func createRedemption(
        orderId: String,
        orderValue: String,
        voucherIds: [String],
        objectType: String = "CAMPAIGN",
        completion: @escaping (PromotionApiResult<PromotionRedemptionResult>) -> Void
    ) {
        let items = voucherIds.map {
            RedemptionItemRequest(objectId: $0, objectType: objectType, expectedDiscount: nil)
        }
        let request = CreateRedemptionRequest(
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

    // MARK: - PromotionResult → PromotionApiResult

    /// `PromotionUseCases` không ném lỗi nghiệp vụ: nó trả `PromotionResult.Failure` kèm `errorCode`.
    /// Chỉ `CancellationException` mới thoát ra thành `Error` của Swift.
    ///
    /// - Parameter onEmpty: xử lý `NO_RESULT` (server trả `data: null`). Danh sách coi là rỗng và
    ///   vẫn thành công; còn chi tiết / validate / redemption thì đó là `.parseFailed`.
    ///   Giữ đúng hành vi của `PromotionSDKApi.kt`.
    private func handle<Model, Out>(
        _ completion: @escaping (PromotionApiResult<Out>) -> Void,
        onEmpty: @escaping () -> PromotionApiResult<Out> = { .failure(.parseFailed) },
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
                        completion(.failure(Self.toSdkError(failure)))
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

    private static func toSdkError(_ failure: PromotionResultFailure) -> PromotionSDKError {
        let codes = PromotionErrorCodes.shared
        switch failure.errorCode {
        case codes.FEATURE_DISABLED:
            return .featureDisabled
        case codes.TIMEOUT:
            return .timeout
        case codes.NO_RESULT:
            return .parseFailed
        case codes.NETWORK_ERROR:
            return .networkFailure(code: nil, message: failure.message ?? "")
        default:
            // Mã nghiệp vụ của server (vd VOUCHER_EXPIRED) đi kèm httpStatus nếu có.
            return .networkFailure(code: failure.httpStatus?.intValue, message: failure.message ?? "")
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

    // MARK: - Lõi → DTO public

    private static func toVoucher(_ model: VoucherItem) -> PromotionVoucher {
        PromotionVoucher(
            id: model.voucherId,
            merchantName: model.merchantName ?? "",
            title: model.title ?? "",           // tên ưu đãi
            imageURL: model.logo,
            expireDate: model.expirationDate,
            isUsed: model.displayState() == .used,
            status: model.status,
            displayStatusLabel: model.displayStatusLabel
        )
    }

    /// Domain `VoucherDetail` → DTO public `PromotionVoucherDetail`.
    ///
    /// `internal` chứ không `private`: hai nơi cần đúng phép map này — `getVoucherDetail` (headless)
    /// và `PromotionSDKImpl.openPromotionDetail` (trả object về host). Đối ứng
    /// `VoucherDetail.toPublicDetail()` bên Android.
    static func toVoucherDetail(_ model: VoucherDetail) -> PromotionVoucherDetail {
        PromotionVoucherDetail(
            id: model.voucherId,
            merchantName: model.merchantName ?? "",
            title: model.title ?? "",
            description: model.description_ ?? "",
            guideline: model.guideline ?? "",
            startDate: model.startDate,
            expireDate: model.expirationDate,
            bannerURL: model.banner,
            logoURL: model.logo,
            status: model.status ?? "",
            displayStatusLabel: model.displayStatusLabel,
            codes: model.codes,
            usageGuideUrl: model.usageGuideUrl
        )
    }

    private static func toOffer(_ model: EligibleOffer) -> PromotionEligibleOffer {
        PromotionEligibleOffer(
            id: model.id,
            name: model.displayName ?? "",
            objectType: model.objectType,
            usable: model.usable,
            estimatedDiscount: model.estimatedDiscount,
            expireDate: model.expireDate,
            // Lõi không dựng sẵn câu tiếng Việt — trả rule thô cho host tự hiển thị.
            ineligibleReason: model.usable ? nil : model.unmatchedRules.first,
            logoUrl: model.logoUrl,
            partnerName: model.partnerName,
            voucherCode: model.voucherCode
        )
    }
}
