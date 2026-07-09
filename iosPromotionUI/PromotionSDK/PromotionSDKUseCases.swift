//
//  PromotionSDKUseCases.swift
//  PromotionSDK
//
//  Public headless API for partners — closure-based only.
//  No RxSwift or internal SPM types in the public interface.
//

import Foundation
@_implementationOnly import RxSwift
@_implementationOnly import PromotionKit

public final class PromotionSDKUseCases {

    // MARK: - Private

    private let customerId: String
    private let token: String?
    private let getMyPromotionUseCase: SearchCustomerVouchersUseCase
    private let findEligibleUseCase: FindEligibleCampaignsUseCase
    private let getDetailUseCase: GetCustomerVoucherDetailUseCase
    private let validateDiscountsUseCase: ValidateStackableDiscountsUseCase
    private let createRedemptionUseCase: CreateRedemptionSessionUseCase
    private let bag = DisposeBag()

    init(
        customerId: String,
        token: String?,
        getMyPromotionUseCase: SearchCustomerVouchersUseCase = SearchCustomerVouchersUseCase(),
        findEligibleUseCase: FindEligibleCampaignsUseCase = FindEligibleCampaignsUseCase(),
        getDetailUseCase: GetCustomerVoucherDetailUseCase = GetCustomerVoucherDetailUseCase(),
        validateDiscountsUseCase: ValidateStackableDiscountsUseCase = ValidateStackableDiscountsUseCase(),
        createRedemptionUseCase: CreateRedemptionSessionUseCase = CreateRedemptionSessionUseCase()
    ) {
        self.customerId = customerId
        self.token = token
        self.getMyPromotionUseCase = getMyPromotionUseCase
        self.findEligibleUseCase = findEligibleUseCase
        self.getDetailUseCase = getDetailUseCase
        self.validateDiscountsUseCase = validateDiscountsUseCase
        self.createRedemptionUseCase = createRedemptionUseCase
    }

    // MARK: - Feature flag gate

    /// Chạy `work` chỉ khi cờ `feature` đang BẬT; ngược lại trả `.featureDisabled` (PRM_MOB_021).
    ///
    /// `isEnabled` đọc cache đồng bộ của lõi Kotlin (không gọi mạng), nên khác bản cũ ở chỗ không
    /// còn `evaluate` bất đồng bộ. Callback lỗi vẫn về main thread để host bind UI an toàn.
    /// Fail-open giữ nguyên: chưa có cache → mọi cờ bật.
    private func whenEnabled<T>(
        _ feature: PromotionFeature,
        _ completion: @escaping (Result<T, PromotionSDKError>) -> Void,
        _ work: @escaping () -> Void
    ) {
        if PromotionFeatureFlagUseCases().isEnabled(featureName: feature.flagName) {
            work()
        } else {
            DispatchQueue.main.async { completion(.failure(.featureDisabled)) }
        }
    }

    // MARK: - Error Mapping

    /// `singleFromKotlin` đã chuẩn hoá mọi lỗi thành `PromotionError` (errorCode + httpStatus).
    private func mapError(_ error: Error) -> PromotionSDKError {
        guard let promotionError = error as? PromotionError else { return .unknown(error) }
        let codes = PromotionErrorCodes.shared
        switch promotionError.errorCode {
        case codes.TIMEOUT:
            return .timeout
        case codes.NETWORK_ERROR:
            return .networkFailure(code: nil, message: promotionError.message ?? "")
        default:
            // Mã nghiệp vụ của server (vd VOUCHER_EXPIRED) đi kèm httpStatus nếu có.
            return .networkFailure(code: promotionError.httpStatus, message: promotionError.message ?? "")
        }
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
        whenEnabled(.voucherList, completion) { [weak self] in
            guard let self = self else { return }
            // Token do host cấp qua `PromotionRequestContextProvider` của lõi.
            let request = SearchCustomerVouchersRequest(
                customerId: self.customerId,
                keyword: keyword,
                serviceCode: serviceCode,
                tab: tab ?? "all",
                page: boxed(myPage),
                size: boxed(mySize)
            )
            let useCase = self.getMyPromotionUseCase
            singleFromKotlin { try await useCase.invoke(request: request) }
                .observeOn(MainScheduler.instance)
                .subscribe(
                    onSuccess: { model in
                        guard let model else {
                            completion(.success(PromotionVoucherPage(myVouchers: [], myIsLastPage: true)))
                            return
                        }
                        let map: (VoucherItem) -> PromotionVoucher = { p in
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
                        completion(.success(PromotionVoucherPage(
                            myVouchers: model.content.map(map),
                            myIsLastPage: model.last?.boolValue ?? true
                        )))
                    },
                    onError: { [weak self] in completion(.failure(self?.mapError($0) ?? .unknown($0))) }
                )
                .disposed(by: self.bag)
        }
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
        whenEnabled(.voucherSelection, completion) { [weak self] in
            guard let self = self else { return }
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
                customerId: self.customerId,
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
            let useCase = self.findEligibleUseCase
            singleFromKotlin { try await useCase.invoke(request: request) }
                .observeOn(MainScheduler.instance)
                .subscribe(
                    onSuccess: { model in
                        guard let model else {
                            completion(.success(PromotionEligibleResult(myOffers: [], otherOffers: [], myIsLastPage: true, otherIsLastPage: true)))
                            return
                        }
                        let map: (EligibleOffer) -> PromotionEligibleOffer = { p in
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
                        completion(.success(PromotionEligibleResult(
                            myOffers: model.myOffers.map(map),
                            otherOffers: model.otherOffers.map(map),
                            myIsLastPage: model.myIsLastPage,
                            otherIsLastPage: model.otherIsLastPage
                        )))
                    },
                    onError: { [weak self] in completion(.failure(self?.mapError($0) ?? .unknown($0))) }
                )
                .disposed(by: self.bag)
        }
    }

    /// Lấy chi tiết 1 voucher của khách (Get Customer Voucher Detail).
    /// - `serviceCode`: tuỳ chọn — lọc thông tin theo dịch vụ đang thanh toán.
    /// - Cờ `VOUCHER_DETAIL` TẮT → trả `.featureDisabled`.
    public func getVoucherDetail(
        voucherId: String,
        serviceCode: String? = nil,
        completion: @escaping (Result<PromotionVoucherDetail, PromotionSDKError>) -> Void
    ) {
        whenEnabled(.voucherDetail, completion) { [weak self] in
            guard let self = self else { return }
            let useCase = self.getDetailUseCase
            let customerId = self.customerId
            singleFromKotlin { try await useCase.invoke(voucherId: voucherId, customerId: customerId, service: serviceCode) }
                .observeOn(MainScheduler.instance)
                .subscribe(
                    onSuccess: { model in
                        guard let model else { completion(.failure(.parseFailed)); return }
                        completion(.success(PromotionVoucherDetail(
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
                        )))
                    },
                    onError: { [weak self] in completion(.failure(self?.mapError($0) ?? .unknown($0))) }
                )
                .disposed(by: self.bag)
        }
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
        whenEnabled(.voucherApply, completion) { [weak self] in
            guard let self = self else { return }
            let items = voucherIds.map { DiscountItemRequest(objectId: $0, objectType: objectType) }
            let request = ValidateDiscountsRequest(
                customerId: self.customerId,
                orderId: orderId,
                orderValue: orderValue,
                items: items
            )
            let useCase = self.validateDiscountsUseCase
            singleFromKotlin { try await useCase.invoke(request: request) }
                .observeOn(MainScheduler.instance)
                .subscribe(
                    onSuccess: { result in
                        guard let result else { completion(.failure(.parseFailed)); return }
                        let discountItems = result.items.map {
                            PromotionDiscountItem(
                                objectId: $0.objectId,
                                discountAmount: $0.calculatedDiscount,
                                isValid: $0.valid,
                                eligibilityStatus: $0.eligibilityStatus
                            )
                        }
                        let wrapped = PromotionValidationResult(
                            overallValid: result.overallValid,
                            totalDiscountAmount: result.totalDiscountAmount,
                            finalAmount: result.finalAmount,
                            items: discountItems
                        )
                        completion(.success(wrapped))
                    },
                    onError: { [weak self] in completion(.failure(self?.mapError($0) ?? .unknown($0))) }
                )
                .disposed(by: self.bag)
        }
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
        whenEnabled(.voucherRedeem, completion) { [weak self] in
            guard let self = self else { return }
            let items = voucherIds.map {
                RedemptionItemRequest(objectId: $0, objectType: objectType, expectedDiscount: nil)
            }
            let request = CreateRedemptionRequest(
                customerId: self.customerId,
                orderId: orderId,
                orderValue: orderValue,
                items: items
            )
            let useCase = self.createRedemptionUseCase
            singleFromKotlin { try await useCase.invoke(request: request) }
                .observeOn(MainScheduler.instance)
                .subscribe(
                    onSuccess: { result in
                        guard let result else { completion(.failure(.parseFailed)); return }
                        let errors = result.validationErrors.map {
                            PromotionRedemptionError(code: $0.code, message: $0.message)
                        }
                        let wrapped = PromotionRedemptionResult(
                            sessionId: result.sessionId,
                            totalDiscount: result.totalDiscount,
                            finalAmount: result.finalAmount,
                            validationErrors: errors
                        )
                        completion(.success(wrapped))
                    },
                    onError: { [weak self] in completion(.failure(self?.mapError($0) ?? .unknown($0))) }
                )
                .disposed(by: self.bag)
        }
    }
}
