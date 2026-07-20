//
//  PromotionDetailViewModel.swift
//  PromotionSDK
//
//  Created by thachlh on 13/5/26.
//

import Foundation
@_implementationOnly import RxSwift
@_implementationOnly import RxCocoa
@_implementationOnly import PRMKotlinBridge

final class PromotionDetailViewModel: BaseViewModel<PromotionDetailRouter>, ViewModelType {

    struct Input {}

    /// Nội dung hiển thị theo tab cùng cờ cho biết text có phải HTML không.
    struct ContentDisplay {
        let text: String
        let isHTML: Bool
    }

    /// Nội dung cả 2 tab, render đồng thời vào 2 trang vuốt được.
    struct TabContents {
        let detail: ContentDisplay
        let guide: ContentDisplay
    }

    struct Output {
        let voucherCardViewModel: Driver<VoucherCardViewModel>
        let bannerImageName: Driver<String?>
        /// Nội dung cả 2 tab (Thông tin chi tiết / Hướng dẫn sử dụng).
        let tabContents: Driver<TabContents>
        let applyButtonTitle: Driver<String>
        let isApplyEnabled: Driver<Bool>
        /// Chỉ hiện nút áp dụng khi voucher ACTIVE; các status khác → ẩn hẳn.
        let isApplyVisible: Driver<Bool>
        /// Hiện shimmer trong lúc gọi API chi tiết; tắt khi có kết quả (thành công/lỗi).
        let isLoading: Driver<Bool>
    }

    /// Gói dữ liệu hiển thị — seed từ `promotion` (cơ bản), cập nhật khi fetch detail đầy đủ về.
    private struct Display {
        let card: VoucherCardViewModel
        let banner: String?
        let detailContent: String
        let guideContent: String
        let applyTitle: String
        let applyEnabled: Bool
        let applyVisible: Bool
    }

    /// Id voucher dùng cho callback "Áp dụng".
    let voucherId: String
    let data: PromotionDetailBuilder.DataModel

    private let getDetailUseCase: GetCustomerVoucherDetailUseCase
    private let displayRelay: BehaviorRelay<Display>
    private let isLoadingRelay = BehaviorRelay<Bool>(value: true)
    private var didFetch = false
    /// Dịch vụ/sản phẩm voucher áp dụng được — seed từ promotion, cập nhật khi fetch detail.
    private var applicableProducts: [ApplicableProduct]

    init(router: PromotionDetailRouter,
         data: PromotionDetailBuilder.DataModel,
         getDetailUseCase: GetCustomerVoucherDetailUseCase = GetCustomerVoucherDetailUseCase()) {
        self.data = data
        self.getDetailUseCase = getDetailUseCase
        self.voucherId = data.promotion.id
        self.applicableProducts = data.promotion.applicableProducts
        // Hiện ngay card từ promotion cơ bản; fetch detail đầy đủ sẽ cập nhật sau.
        self.displayRelay = BehaviorRelay(value: Self.display(from: data.promotion))
        super.init(router: router)
    }

    /// Danh sách dịch vụ cho bottom sheet "Chọn dịch vụ" (lọc theo applicableProducts của voucher).
    func serviceSelectorItems() -> [ServiceSelectorItem] {
        ServiceSelectorBuilder.items(forApplicableProducts: applicableProducts)
    }

    func transform(input: Input) -> Output {
        fetchDetail()

        // Cả 2 tab render đồng thời (2 trang vuốt được). API không trả nội dung → để TRỐNG,
        // KHÔNG dùng text mặc định (theo yêu cầu sản phẩm).
        let tabContents = displayRelay.asDriver().map { display -> TabContents in
            TabContents(
                detail: Self.contentDisplay(display.detailContent),
                guide: Self.contentDisplay(display.guideContent)
            )
        }

        return Output(
            voucherCardViewModel: displayRelay.asDriver().map { $0.card },
            bannerImageName: displayRelay.asDriver().map { $0.banner },
            tabContents: tabContents,
            applyButtonTitle: displayRelay.asDriver().map { $0.applyTitle },
            isApplyEnabled: displayRelay.asDriver().map { $0.applyEnabled },
            isApplyVisible: displayRelay.asDriver().map { $0.applyVisible },
            isLoading: isLoadingRelay.asDriver()
        )
    }

    /// Nội dung 1 tab: rỗng → để trống (không text mặc định); có → render HTML.
    private static func contentDisplay(_ raw: String) -> ContentDisplay {
        raw.isEmpty ? ContentDisplay(text: "", isHTML: false) : ContentDisplay(text: raw, isHTML: true)
    }

    // MARK: - Fetch detail (tự gọi API theo voucherId — giống Android)

    private func fetchDetail() {
        guard !didFetch else { return }
        didFetch = true
        // Token do host cấp qua `PromotionRequestContextProvider` của lõi, không truyền từng request.
        let voucherId = data.promotion.id
        let customerId = data.customerId
        let service = data.service
        singleFromKotlin { [getDetailUseCase] in
            try await getDetailUseCase.invoke(voucherId: voucherId, customerId: customerId, service: service)
        }
            .observeOn(MainScheduler.instance)
            .subscribe(
                onSuccess: { [weak self] detail in
                    guard let self, let detail else { return }
                    self.applicableProducts = detail.applicableProducts
                    self.displayRelay.accept(Self.display(from: detail))
                    self.isLoadingRelay.accept(false)
                },
                onError: { [weak self] _ in
                    // Lỗi → giữ nguyên card cơ bản từ promotion; nội dung tab để trống (không text mặc định).
                    self?.isLoadingRelay.accept(false)
                }
            )
            .disposed(by: disposeBag)
    }

    // MARK: - Display builders

    private static let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "dd/MM/yyyy"
        return f
    }()

    private static func display(from promotion: PromotionCardSeed) -> Display {
        let dateString = PromotionDate.parse(promotion.expirationDate)
            .map { "Hạn sử dụng \(PromotionDate.display($0))" } ?? ""
        let config = applyConfig(status: promotion.status, displayStatusLabel: promotion.displayStatusLabel)
        return Display(
            card: VoucherCardViewModel(
                // merchantName hiện ở label nhỏ, tên ưu đãi ở label lớn — giữ như bản cũ.
                title: promotion.merchantName,
                description: promotion.name,
                logoURL: nil,
                date: dateString
            ),
            banner: promotion.logo,
            // Seed nội dung tab để TRỐNG — chỉ điền khi API chi tiết trả về (không dùng data mặc định).
            detailContent: "",
            guideContent: "",
            applyTitle: config.title,
            applyEnabled: config.isEnabled,
            applyVisible: config.isVisible
        )
    }

    private static func display(from detail: VoucherDetail) -> Display {
        let dateString = PromotionDate.parse(detail.expirationDate)
            .map { "Hạn sử dụng \(PromotionDate.display($0))" } ?? ""
        let config = applyConfig(status: detail.status, displayStatusLabel: detail.displayStatusLabel)
        return Display(
            card: VoucherCardViewModel(
                title: detail.merchantName ?? "",
                description: detail.title ?? "",
                logoURL: detail.logo,
                date: dateString
            ),
            banner: detail.banner,
            detailContent: detail.description_ ?? "",
            guideContent: detail.guideline ?? "",
            applyTitle: config.title,
            applyEnabled: config.isEnabled,
            applyVisible: config.isVisible
        )
    }

    // MARK: - Helpers

    /// Cấu hình nút áp dụng. Quy tắc trạng thái nằm ở `promotionLogic`
    /// (`VoucherStatus.displayState()`), dùng chung với Android — trước đây hàm này chỉ chấp nhận
    /// `ACTIVE`, nên voucher `AVAILABLE_TO_CLAIM` bị ẩn nút dù danh sách vẫn cho chọn.
    /// Hoàn toàn dựa server status, KHÔNG check expiry phía client.
    private static func applyConfig(status: String?, displayStatusLabel: String?) -> (title: String, isEnabled: Bool, isVisible: Bool) {
        let state = VoucherStatus.companion.from(raw: status).displayState()
        return state.isUsable ? ("Sử dụng ngay", true, true) : ("", false, false)
    }
}
