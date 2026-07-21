//
//  PromotionSDKImpl.swift
//  PromotionSDK
//
//  Internal implementation box — keeps all PromotionLogic/RxSwift/PRMPromotionUI types out of
//  PromotionSDK's public class layout and module interface so the consuming app's compiler
//  never needs to load those modules for class metadata generation.
//

import UIKit
@_implementationOnly import RxSwift
@_implementationOnly import PRMPromotionUI
@_implementationOnly import PRMKotlinBridge
@_implementationOnly import PRMDesignKit

// Internal callbacks used by PromotionSDK to communicate back.
typealias OnApplyVoucher = (String) -> Void  // voucherId
typealias OnClearVoucher = () -> Void

final class PromotionSDKImpl: NSObject {

    var onApplyVoucher: OnApplyVoucher?
    var onClearVoucher: OnClearVoucher?
    var onUpdateWidgetCount: ((Int) -> Void)?
    var onClose: (() -> Void)?
    /// Báo host trạng thái bật/tắt SDK (feature flag Unleash) khi đã biết chắc.
    var onAvailabilityUpdate: ((Bool) -> Void)?

    /// Nguồn context duy nhất: session tĩnh + order/dịch vụ động. `updateContext` ghi vào đây,
    /// lõi Kotlin đọc lại ở **mỗi** request. Thay cho `HostRequestContextProvider` + các field rời cũ.
    let context: PromotionMutableContext

    var customerId: String { context.session.customerId }
    var token: String? { context.session.accessToken }

    // Định tuyến qua context để `updateContext` (host cập nhật khi mở widget thanh toán) và luồng
    // build request dùng chung một nguồn — không phải re-init SDK, giữ 1 phiên từ lúc login.
    var orderId: String? {
        get { context.orderId }
        set { context.orderId = newValue }
    }
    var orderValue: String? {
        get { context.orderValue }
        set { context.orderValue = newValue }
    }
    /// Dòng đơn hàng host truyền vào — cần cho Find Eligible Campaigns (orderInfo.items[]).
    var orderItems: [PromotionOrderItem]
    // Chỉ hai use case này còn dùng trực tiếp — cho widget checkout. Luồng headless đi qua
    // `PromotionSDKApi`, và lớp đó gọi `PromotionUseCases` của lõi.
    private let findEligibleUseCase: FindEligibleCampaignsUseCase
    private let validateDiscountsUseCase: ValidateStackableDiscountsUseCase
    let disposeBag = DisposeBag()

    var cachedListModel: EligibleOffersResult?
    var appliedPromotion: EligibleOffer?
    weak var activeWidget: PRMEndowView?

    init(options: PromotionSDKOptions) {
        self.context = PromotionMutableContext(session: options.session)
        self.orderItems = []
        // Khởi tạo lõi Kotlin qua map public→core (đối ứng `options.toCoreConfig` bên Android).
        // `isDebug`: bản DEBUG in toàn bộ request/response của Ktor ra console để đối chiếu schema thật
        // của server với DTO; bản Release tắt hẳn (không log token) — xem `isDebugBuild`.
        // `availableServices` đã nằm trong config (toCoreConfig) → `ServiceSelectorBuilder` đọc lại
        // từ `PromotionContainer.requireConfig()`, không cần holder Swift riêng (parity Android).
        PromotionContainer.shared.initialize(
            config: options.toCoreConfig(context: context, isDebug: PromotionSDKImpl.isDebugBuild)
        )
        self.findEligibleUseCase = FindEligibleCampaignsUseCase()
        self.validateDiscountsUseCase = ValidateStackableDiscountsUseCase()
        super.init()
        // Nạp cờ tính năng từ server. `refresh()` không ném lỗi: hỏng thì giữ cache (fail-open).
        Task { try? await PromotionFeatureGate.shared.refresh() }
    }

    // MARK: - Theming

    /// Lúc khởi tạo: host truyền theme → áp + lưu; không truyền → khôi phục theme đã lưu.
    /// Phải gọi **sau** `PromotionContainer.initialize` (đã chạy trong init) để `preferences` sẵn sàng.
    func restoreOrApplyTheme(_ theme: PromotionSDKTheme?) {
        if let theme {
            applyTheme(theme)
            PromotionThemeStore.save(theme)
        } else if let saved = PromotionThemeStore.load() {
            applyTheme(saved)
        }
    }

    /// `configure(theme:)`: áp + persist. `nil` = reset và xoá theme đã lưu.
    func applyAndPersistTheme(_ theme: PromotionSDKTheme?) {
        applyTheme(theme)
        if let theme {
            PromotionThemeStore.save(theme)
        } else {
            PromotionThemeStore.clear()
        }
    }

    /// Map public theme (UIColor/CGFloat) → PRMDesignKit registry config. Truyền nil để reset.
    func applyTheme(_ theme: PromotionSDKTheme?) {
        guard let theme else {
            PRMThemeRegistry.shared.clear()
            return
        }
        let config = PRMThemeConfig(
            button: theme.buttonToken.map {
                PRMButtonThemeToken(
                    backgroundColor: $0.backgroundColor,
                    textColor: $0.textColor,
                    shadowColor: $0.shadowColor,
                    cornerRadius: $0.cornerRadius
                )
            },
            searchBar: theme.searchBarToken.map {
                PRMSearchBarThemeToken(
                    borderColor: $0.borderColor,
                    hintTextColor: $0.hintTextColor,
                    textColor: $0.textColor,
                    iconColor: $0.iconColor,
                    cornerRadius: $0.cornerRadius
                )
            },
            listItem: theme.listItemToken.map {
                PRMListItemThemeToken(
                    linkTextColor: $0.linkTextColor,
                    usedBadgeTextColor: $0.usedBadgeTextColor,
                    usedBadgeBackgroundColor: $0.usedBadgeBackgroundColor,
                    radioSelectedColor: $0.radioButtonSelectedStrokeColor,
                    radioUnselectedColor: $0.radioButtonStrokeColor
                )
            },
            tabChip: theme.tabChipToken.map {
                PRMTabChipThemeToken(
                    activeBackgroundColor: $0.activeBackgroundColor,
                    inactiveBackgroundColor: $0.inactiveBackgroundColor,
                    activeTextColor: $0.activeTextColor,
                    inactiveTextColor: $0.inactiveTextColor,
                    cornerRadius: $0.cornerRadius
                )
            },
            tabUnderline: theme.tabUnderlineToken.map {
                PRMTabUnderlineThemeToken(
                    indicatorColor: $0.indicatorColor,
                    activeTextColor: $0.activeTextColor,
                    inactiveTextColor: $0.inactiveTextColor,
                    backgroundColor: $0.backgroundColor
                )
            },
            discountBadge: theme.discountBadgeToken.map {
                PRMDiscountBadgeThemeToken(
                    availableTextColor: $0.availableTextColor,
                    unavailableTextColor: $0.unavailableTextColor,
                    availableBackgroundColor: $0.availableBackgroundColor,
                    unavailableBackgroundColor: $0.unavailableBackgroundColor,
                    actionTextColor: $0.actionTextColor
                )
            }
        )
        PRMThemeRegistry.shared.configure(config)
    }

    /// Map config nội bộ trong registry → public theme (đảo ngược applyTheme).
    func currentTheme() -> PromotionSDKTheme? {
        guard let config = PRMThemeRegistry.shared.current else { return nil }
        return PromotionSDKTheme(
            buttonToken: config.button.map {
                ButtonToken(
                    backgroundColor: $0.backgroundColor, textColor: $0.textColor,
                    shadowColor: $0.shadowColor, cornerRadius: $0.cornerRadius
                )
            },
            searchBarToken: config.searchBar.map {
                SearchBarToken(
                    borderColor: $0.borderColor, hintTextColor: $0.hintTextColor,
                    textColor: $0.textColor, iconColor: $0.iconColor, cornerRadius: $0.cornerRadius
                )
            },
            listItemToken: config.listItem.map {
                ListItemToken(
                    linkTextColor: $0.linkTextColor, usedBadgeTextColor: $0.usedBadgeTextColor,
                    usedBadgeBackgroundColor: $0.usedBadgeBackgroundColor,
                    radioButtonStrokeColor: $0.radioUnselectedColor,
                    radioButtonSelectedStrokeColor: $0.radioSelectedColor
                )
            },
            tabChipToken: config.tabChip.map {
                TabChipToken(
                    activeBackgroundColor: $0.activeBackgroundColor,
                    inactiveBackgroundColor: $0.inactiveBackgroundColor,
                    activeTextColor: $0.activeTextColor, inactiveTextColor: $0.inactiveTextColor,
                    cornerRadius: $0.cornerRadius
                )
            },
            tabUnderlineToken: config.tabUnderline.map {
                TabUnderlineToken(
                    indicatorColor: $0.indicatorColor, activeTextColor: $0.activeTextColor,
                    inactiveTextColor: $0.inactiveTextColor, backgroundColor: $0.backgroundColor
                )
            },
            discountBadgeToken: config.discountBadge.map {
                DiscountBadgeToken(
                    availableTextColor: $0.availableTextColor, unavailableTextColor: $0.unavailableTextColor,
                    availableBackgroundColor: $0.availableBackgroundColor,
                    unavailableBackgroundColor: $0.unavailableBackgroundColor,
                    actionTextColor: $0.actionTextColor
                )
            }
        )
    }

    /// Facade headless chỉ cần `customerId`/`token`: nó gọi thẳng `PromotionUseCases` của lõi Kotlin,
    /// không nhận use case tiêm từ ngoài nữa.
    func makeApi() -> PromotionSDKApi {
        PromotionSDKApi(customerId: customerId, token: token)
    }

    /// Cập nhật order cho luồng widget thanh toán (eligible + validate dùng các giá trị này).
    func updateOrder(orderId: String?, orderValue: String?, orderItems: [PromotionOrderItem]? = nil) {
        if let orderId { self.orderId = orderId }
        if let orderValue { self.orderValue = orderValue }
        if let orderItems { self.orderItems = orderItems }
    }

    /// Ghi context động — gọi từ `PromotionSDK.updateContext`. Overwrite cả 4 trường (nil = xoá),
    /// đối ứng `PromotionSDK.updateContext` bên Android (ghi thẳng vào `PromotionMutableContext`).
    func updateContext(orderId: String?, orderValue: String?, serviceCode: String?, metaData: String?) {
        context.orderId = orderId
        context.orderValue = orderValue
        context.serviceCode = serviceCode
        context.metaData = metaData
    }

    /// Giải phóng đồ thị DI + reset theme trong bộ nhớ. Đối ứng `PromotionSDK.release()` bên Android:
    /// **không** xoá theme đã lưu (nó sống qua release/init), chỉ reset registry đang chạy.
    func teardown() {
        PromotionContainer.shared.clear()
        applyTheme(nil)
    }

    /// Map order items (public) → model của lõi Kotlin cho Find Eligible Campaigns.
    private func eligibleOrderItems() -> [EligibleOrderItem] {
        orderItems.map {
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
    }

    /// Hai điểm gác của tầng UI, uỷ quyền cho `PromotionFeatureGate` của lõi Kotlin (dùng chung với
    /// Android). Cờ đọc từ cache đồng bộ, không gọi mạng; `completion` về main thread để chỗ gọi
    /// push/present được ngay.
    ///
    /// Tên tính năng chỉ tồn tại ở `PromotionFeatureFlag` bên Kotlin — không có enum nào bên Swift.
    func canOpenVoucherList(_ completion: @escaping (Bool) -> Void) {
        let enabled = PromotionFeatureGate.shared.canOpenVoucherList()
        DispatchQueue.main.async { completion(enabled) }
    }

    func canOpenVoucherDetail(_ completion: @escaping (Bool) -> Void) {
        let enabled = PromotionFeatureGate.shared.canOpenVoucherDetail()
        DispatchQueue.main.async { completion(enabled) }
    }

    /// Popup lỗi nghiệp vụ khi tính năng đang TẮT (PRM_MOB_021) — dùng cho các thao tác UI (bấm mở màn).
    func showFeatureDisabledDialog(on viewController: UIViewController) {
        let message = PromotionSDKError.featureDisabled.errorDescription
            ?? "Tính năng hiện đang tạm thời không khả dụng. Vui lòng thử lại sau."
        PRMConfirmationDialog.showError(message, in: viewController.view)
    }

    /// Mở màn chi tiết ưu đãi theo `voucherId`. Màn tự fetch chi tiết đầy đủ; trong lúc chờ hiện shimmer.
    /// Gate bởi cờ `VOUCHER_DETAIL` (đã gate ngầm bởi master): TẮT → popup lỗi PRM_MOB_021 + báo host.
    /// Có navigationController → push, ngược lại → present modal.
    func openPromotionDetail(voucherId: String, on viewController: UIViewController, navigator: UINavigationController?) {
        canOpenVoucherDetail { [weak self] enabled in
            guard let self else { return }
            guard enabled else {
                self.showFeatureDisabledDialog(on: viewController)
                self.onAvailabilityUpdate?(false)
                return
            }
            let nav = navigator ?? viewController.navigationController ?? (viewController as? UINavigationController)
            // Seed tối thiểu từ voucherId — card trống + shimmer cho tới khi fetch detail xong.
            let seed = PRMPromotionCardSeed(voucherId: voucherId)
            let vc = PromotionDetailBuilder.build(
                with: .init(promotion: seed, customerId: self.customerId, token: self.token),
                navigator: nav
            )
            if let nav {
                nav.pushViewController(vc, animated: true)
            } else {
                let wrapper = UINavigationController(rootViewController: vc)
                wrapper.modalPresentationStyle = .fullScreen
                viewController.present(wrapper, animated: true)
            }
        }
    }

    func makeEndowView(presentFrom host: UIViewController, navigator: UINavigationController?) -> UIView {
        self._host = host
        self._navigator = navigator

        // Container gate theo cờ VOUCHER_SELECTION (đã gate ngầm bởi master ENABLE_ALL).
        // FAIL-OPEN: `observe` gọi NGAY (lúc chưa có config → BẬT lạc quan) rồi gọi LẠI khi
        // config thật về. BẬT → dựng widget (idempotent, chỉ 1 lần). TẮT/server báo tắt sau
        // → rút widget + thu height 0. Báo host mỗi lần đổi.
        let container = UIView()
        let collapse = container.heightAnchor.constraint(equalToConstant: 0)
        // Lõi Kotlin không có `observe`. Giữ nguyên ngữ nghĩa: đọc cache NGAY (fail-open → hiện lạc
        // quan), rồi `refresh()` xong gọi LẠI với giá trị thật từ server.
        let applyFlag: (Bool) -> Void = { [weak self, weak container] enabled in
            guard let self, let container else { return }
            self.onAvailabilityUpdate?(enabled)
            guard enabled else {
                // Server báo tắt (kể cả sau khi đã hiện lạc quan) → rút widget lại.
                self.activeWidget?.removeFromSuperview()
                self.activeWidget = nil
                container.isHidden = true
                collapse.isActive = true
                return
            }
            collapse.isActive = false
            container.isHidden = false
            // Đã dựng rồi thì bỏ qua (tránh dựng lại khi `.update` bắn lặp).
            guard self.activeWidget == nil else { return }
            let widget = PRMEndowView()
            widget.delegate = self
            widget.dataSource = self
            self.activeWidget = widget
            widget.translatesAutoresizingMaskIntoConstraints = false
            container.addSubview(widget)
            NSLayoutConstraint.activate([
                widget.topAnchor.constraint(equalTo: container.topAnchor),
                widget.leadingAnchor.constraint(equalTo: container.leadingAnchor),
                widget.trailingAnchor.constraint(equalTo: container.trailingAnchor),
                widget.bottomAnchor.constraint(equalTo: container.bottomAnchor)
            ])
        }

        let gate = PromotionFeatureGate.shared
        // Gọi ngay với cache hiện có (chưa có cache → bật lạc quan).
        applyFlag(gate.canShowVoucherSelection())
        // Rồi làm mới từ server và gọi lại nếu giá trị đổi.
        Task { @MainActor in
            try? await gate.refresh()
            applyFlag(gate.canShowVoucherSelection())
        }
        return container
    }

    /// Bật log body HTTP của lõi Kotlin.
    ///
    /// **Không** dùng `#if DEBUG`: xcframework luôn được archive ở cấu hình Release, nên cờ đó
    /// vĩnh viễn là `false` bên trong SDK dù app host build Debug. Đọc biến môi trường của tiến
    /// trình để bật được cả trên bản phát hành khi cần chẩn đoán:
    ///
    ///     xcrun simctl launch --console booted <bundle-id> PROMOTION_SDK_DEBUG=1
    ///
    /// Mặc định tắt — log body sẽ in cả `Authorization`.
    private static var isDebugBuild: Bool {
        ProcessInfo.processInfo.environment["PROMOTION_SDK_DEBUG"] == "1"
    }

    // Stored weakly to avoid retain cycles — these are UIKit types (fine in module interface context)
    weak var _host: UIViewController?
    weak var _navigator: UINavigationController?

    func loadVouchers(completion: @escaping (EligibleOffersResult?) -> Void) {
        // Luồng "Chọn ưu đãi" dùng Find Eligible Campaigns (my + other), khớp màn chọn (openChoosePromotion).
        // Token do host cấp qua `PromotionRequestContextProvider` của lõi.
        let request = FindEligibleCampaignsRequest(
            customerId: customerId,
            orderId: orderId ?? "",
            orderValue: orderValue ?? "0",
            items: eligibleOrderItems(),
            currency: "VND",
            channel: "MOBILE",
            customerType: nil, segment: nil, tier: nil,
            tabCode: nil,
            section: nil,
            myPage: 0, mySize: 10,
            otherPage: 0, otherSize: 10,
            filterOptions: EligibleFilterOptions(includeExpired: false, checkBudgetAvailability: true, includePreview: true)
        )
        let useCase = findEligibleUseCase
        singleFromKotlin { try await useCase.invoke(request: request) }
            .observeOn(MainScheduler.instance)
            .subscribe(
                onSuccess: { completion($0) },
                onError: { _ in completion(nil) }
            )
            .disposed(by: disposeBag)
    }

    /// Format số tiền giảm (chuỗi số thô) -> "Giảm x.xxxđ".
    private static func formatDiscount(_ raw: String) -> String {
        let digits = raw.filter { $0.isNumber }
        guard let value = Int(digits) else { return raw }
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.groupingSeparator = "."
        let formatted = formatter.string(from: NSNumber(value: value)) ?? "\(value)"
        return "Giảm \(formatted)đ"
    }

    /// Auto-apply voucher `isAutoApplied`: validate với order data → áp + báo host. Lỗi → giữ "chưa áp".
    private func autoApply(_ promotion: EligibleOffer, on view: PRMEndowView) {
        let request = ValidateDiscountsRequest(
            customerId: customerId,
            orderId: orderId ?? "",
            orderValue: orderValue ?? "0",
            items: [DiscountItemRequest(objectId: promotion.id, objectType: promotion.objectType)]
        )
        let useCase = validateDiscountsUseCase
        singleFromKotlin { try await useCase.invoke(request: request) }
            .observeOn(MainScheduler.instance)
            .subscribe(
                onSuccess: { [weak self, weak view] result in
                    guard let self, let view, let result else { return }
                    let item = result.items.first(where: { $0.objectId == promotion.id })
                    if item?.valid == false {
                        // Voucher auto-apply không còn hợp lệ → UNAVAILABLE (không báo host).
                        view.setState(.unavailable(voucherTitle: promotion.campaignName ?? ""))
                        return
                    }
                    let discount = item?.calculatedDiscount ?? result.totalDiscountAmount
                    self.appliedPromotion = promotion
                    view.setState(.applied(voucherTitle: Self.formatDiscount(discount)))
                    self.onApplyVoucher?(promotion.id)
                },
                onError: { _ in
                    // Validate lỗi → không auto-apply (giữ trạng thái "chưa áp").
                }
            )
            .disposed(by: disposeBag)
    }

    func openChoosePromotion() {
        guard let host = _host else { return }
        let nav = _navigator ?? host.navigationController

        // Push NGAY. Truyền data widget đã load (cachedListModel) để tránh double call — giống Android.
        // Nếu chưa có cache → màn chọn tự fetch trang 0 (hiện shimmer).
        let cached = cachedListModel
        let vc = ChoosePromotionBuilder.build(
            with: .init(
                customerId: customerId,
                token: token,
                orderId: orderId,
                orderValue: orderValue,
                orderItems: eligibleOrderItems(),
                preloadedMy: cached?.myOffers ?? [],
                preloadedOther: cached?.otherOffers ?? [],
                myIsLastPage: cached?.myIsLastPage ?? true,
                otherIsLastPage: cached?.otherIsLastPage ?? true,
                preSelectedVoucherId: appliedPromotion?.id
            ),
            navigator: nav
        )
        vc.onApplyVoucher = { [weak self, weak host, weak vc] (promotion: EligibleOffer) in
            guard let self else { return }

            let pop: () -> Void = {
                if let navCtrl = host?.navigationController {
                    navCtrl.popViewController(animated: true)
                } else {
                    host?.dismiss(animated: true)
                }
            }
            let finish: (String) -> Void = { [weak self] displayText in
                guard let self else { return }
                self.appliedPromotion = promotion
                self.activeWidget?.setState(.applied(voucherTitle: displayText))
                self.onApplyVoucher?(promotion.id)
                pop()
            }

            // Bấm "Áp dụng" -> validate voucher với order data, hiển thị calculatedDiscount.
            let request = ValidateDiscountsRequest(
                customerId: self.customerId,
                orderId: self.orderId ?? "",
                orderValue: self.orderValue ?? "0",
                items: [DiscountItemRequest(objectId: promotion.id, objectType: promotion.objectType)]
            )
            let useCase = self.validateDiscountsUseCase
            singleFromKotlin { try await useCase.invoke(request: request) }
                .observeOn(MainScheduler.instance)
                .subscribe(
                    onSuccess: { [weak self] result in
                        guard let result else { pop(); return }
                        let item = result.items.first(where: { $0.objectId == promotion.id })
                        if item?.valid == false {
                            // Voucher không còn hợp lệ → UNAVAILABLE (không báo host).
                            self?.activeWidget?.setState(.unavailable(voucherTitle: promotion.campaignName ?? ""))
                            pop()
                            return
                        }
                        let discount = item?.calculatedDiscount ?? result.totalDiscountAmount
                        finish(Self.formatDiscount(discount))
                    },
                    onError: { _ in
                        // Validate lỗi -> KHÔNG áp dụng; ở lại màn chọn + báo lỗi (khớp Android, khớp autoApply).
                        if let vc = vc {
                            PRMConfirmationDialog.showError("Không thể áp dụng ưu đãi lúc này. Vui lòng thử lại.", in: vc.view)
                        }
                    }
                )
                .disposed(by: self.disposeBag)
        }

        if let navCtrl = nav {
            navCtrl.pushViewController(vc, animated: true)
        } else {
            let wrapper = UINavigationController(rootViewController: vc)
            wrapper.modalPresentationStyle = .fullScreen
            host.present(wrapper, animated: true)
        }
    }
}

// MARK: - PRMEndowViewDataSource (internal conformance)

extension PromotionSDKImpl: PRMEndowViewDataSource {
    func selectPromtionViewDidAttachToWindow(_ view: PRMEndowView) {
        if let applied = appliedPromotion {
            view.setState(.applied(voucherTitle: applied.campaignName ?? ""))
            return
        }
        loadVouchers { [weak self, weak view] listModel in
            guard let self, let view else { return }
            self.cachedListModel = listModel
            if let model = listModel {
                // Số voucher = totalElements của myVouchers + otherVouchers (không đếm length mảng đã phân trang).
                let total = Int(model.myTotalElements + model.otherTotalElements)
                view.setState(total == 0 ? .empty : .notApplied(count: total))
                self.onUpdateWidgetCount?(total)
                // TODO(auto-apply): `EligibleOffer` không có `isAutoApplied` — API Find Eligible
                // Campaigns không trả trường này. Bản iOS cũ cũng luôn gán `false` ở
                // `FindEligibleCampaignsUseCase`, nên nhánh auto-apply chưa từng chạy.
                // Android auto-apply từ Search API (`VoucherItem.isAutoApplied`) — hai widget đang
                // dùng hai API khác nhau. Cần backend xác nhận trước khi hợp nhất.
            } else {
                view.setState(.empty)
            }
        }
    }
}

// MARK: - PRMEndowViewDelegate (internal conformance)

extension PromotionSDKImpl: PRMEndowViewDelegate {
    func selectPromtionViewDidTapSelect(_ view: PRMEndowView) {
        switch view.currentState {
        case .applied:
            appliedPromotion = nil
            onClearVoucher?()
            if let cached = cachedListModel {
                // Đếm theo totalElements (tổng thật từ server), nhất quán với lúc load — không dùng length mảng đã phân trang.
                let total = Int(cached.myTotalElements + cached.otherTotalElements)
                view.setState(total == 0 ? .empty : .notApplied(count: total))
            } else {
                selectPromtionViewDidAttachToWindow(view)
            }
        default:
            openChoosePromotion()
        }
    }
}
