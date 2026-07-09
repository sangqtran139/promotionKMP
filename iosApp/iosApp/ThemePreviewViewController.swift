//
//  ThemePreviewViewController.swift
//  VDSPromotionDemoApp
//
//  Theme Playground — chỉnh màu/bo góc từng token và xem SDK áp dụng trực tiếp.
//
//  LƯU Ý: file này (import PromotionSDKUI) cố tình giữ NHỎ. Toàn bộ UI nặng nằm ở
//  ThemePlaygroundUI.swift (KHÔNG import SDK) để tránh swift-frontend đệ quy quá sâu
//  (deserializeClass) khi 1 file vừa nạp module SDK vừa type-check khối lượng lớn.
//

import UIKit
import PromotionSDKUI

final class ThemePreviewViewController: UIViewController {

    let sdk: PromotionSDK
    var draft = DraftTheme()
    var rowRefreshers: [() -> Void] = []
    /// Refresh các preview mock (mô phỏng) dưới mỗi nhóm token — gọi khi reset để vẽ lại theo draft mặc định.
    var previewRefreshers: [() -> Void] = []
    var activeColorSetter: ((UIColor?) -> Void)?

    let contentStack: UIStackView = {
        let sv = UIStackView()
        sv.axis = .vertical
        sv.spacing = 8
        sv.translatesAutoresizingMaskIntoConstraints = false
        return sv
    }()

    let widgetContainer: UIView = {
        let v = UIView()
        v.backgroundColor = .secondarySystemBackground
        v.layer.cornerRadius = 12
        return v
    }()
    private var widget: UIView?

    init(sdk: PromotionSDK) {
        self.sdk = sdk
        super.init(nibName: nil, bundle: nil)
    }
    required init?(coder: NSCoder) { fatalError() }

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "Theme Playground"
        view.backgroundColor = .systemGroupedBackground
        // Khôi phục theme đã lưu (mirror Android: load → configure).
        if let saved = ThemePreferenceManager.shared.load() {
            loadDraft(from: saved)
            sdk.configure(theme: saved)
        }
        setupLayout()
        buildSections()
        reloadWidget()
    }

    /// Map theme đã lưu → draft (để các hàng hiển thị đúng giá trị đã lưu).
    private func loadDraft(from t: PromotionSDKTheme) {
        draft.buttonBackground = t.button?.backgroundColor
        draft.buttonText = t.button?.textColor
        draft.buttonShadow = t.button?.shadowColor
        draft.buttonCorner = t.button?.cornerRadius
        draft.searchBorder = t.searchBar?.borderColor
        draft.searchHint = t.searchBar?.hintTextColor
        draft.searchText = t.searchBar?.textColor
        draft.searchIcon = t.searchBar?.iconColor
        draft.searchCorner = t.searchBar?.cornerRadius
        draft.listLink = t.listItem?.linkTextColor
        draft.listUsedText = t.listItem?.usedBadgeTextColor
        draft.listUsedBg = t.listItem?.usedBadgeBackgroundColor
        draft.listRadioSelected = t.listItem?.radioSelectedColor
        draft.listRadioUnselected = t.listItem?.radioUnselectedColor
        draft.tabChipActiveBg = t.tabChip?.activeBackgroundColor
        draft.tabChipInactiveBg = t.tabChip?.inactiveBackgroundColor
        draft.tabChipActiveText = t.tabChip?.activeTextColor
        draft.tabChipInactiveText = t.tabChip?.inactiveTextColor
        draft.tabChipCorner = t.tabChip?.cornerRadius
        draft.tabIndicator = t.tabUnderline?.indicatorColor
        draft.tabActiveText = t.tabUnderline?.activeTextColor
        draft.tabInactiveText = t.tabUnderline?.inactiveTextColor
        draft.tabBg = t.tabUnderline?.backgroundColor
        draft.dscAvailText = t.discountBadge?.availableTextColor
        draft.dscUnavailText = t.discountBadge?.unavailableTextColor
        draft.dscAvailBg = t.discountBadge?.availableBackgroundColor
        draft.dscUnavailBg = t.discountBadge?.unavailableBackgroundColor
        draft.dscAction = t.discountBadge?.actionTextColor
    }

    private func setupLayout() {
        let scroll = UIScrollView()
        scroll.translatesAutoresizingMaskIntoConstraints = false
        // Widget SDK dùng TapableView (UIControl xử lý touchesBegan/Ended). UIScrollView mặc định
        // delaysContentTouches = true sẽ nuốt/trễ touch → nút "Sử dụng" không bấm được. Tắt đi.
        // scroll.delaysContentTouches = false
        view.addSubview(scroll)
        scroll.addSubview(contentStack)
        NSLayoutConstraint.activate([
            scroll.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            scroll.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scroll.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scroll.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            contentStack.topAnchor.constraint(equalTo: scroll.contentLayoutGuide.topAnchor, constant: 12),
            contentStack.leadingAnchor.constraint(equalTo: scroll.frameLayoutGuide.leadingAnchor, constant: 16),
            contentStack.trailingAnchor.constraint(equalTo: scroll.frameLayoutGuide.trailingAnchor, constant: -16),
            contentStack.bottomAnchor.constraint(equalTo: scroll.contentLayoutGuide.bottomAnchor, constant: -24)
        ])
    }

    // MARK: - SDK touch points (chỉ những hàm này chạm PromotionSDKUI)

    /// Map DraftTheme (UIColor/CGFloat) → PromotionSDKTheme.
    private func makeTheme() -> PromotionSDKTheme {
        let button = VDSPromotionButtonToken(
            backgroundColor: draft.buttonBackground, textColor: draft.buttonText,
            shadowColor: draft.buttonShadow, cornerRadius: draft.buttonCorner
        )
        let search = VDSPromotionSearchBarToken(
            borderColor: draft.searchBorder, hintTextColor: draft.searchHint,
            textColor: draft.searchText, iconColor: draft.searchIcon, cornerRadius: draft.searchCorner
        )
        let list = VDSPromotionListItemToken(
            linkTextColor: draft.listLink, usedBadgeTextColor: draft.listUsedText,
            usedBadgeBackgroundColor: draft.listUsedBg,
            radioSelectedColor: draft.listRadioSelected,
            radioUnselectedColor: draft.listRadioUnselected
        )
        let tabChip = VDSPromotionTabChipToken(
            activeBackgroundColor: draft.tabChipActiveBg, inactiveBackgroundColor: draft.tabChipInactiveBg,
            activeTextColor: draft.tabChipActiveText, inactiveTextColor: draft.tabChipInactiveText,
            cornerRadius: draft.tabChipCorner
        )
        let tab = VDSPromotionTabUnderlineToken(
            indicatorColor: draft.tabIndicator, activeTextColor: draft.tabActiveText,
            inactiveTextColor: draft.tabInactiveText, backgroundColor: draft.tabBg
        )
        let discount = VDSPromotionDiscountBadgeToken(
            availableTextColor: draft.dscAvailText, unavailableTextColor: draft.dscUnavailText,
            availableBackgroundColor: draft.dscAvailBg, unavailableBackgroundColor: draft.dscUnavailBg,
            actionTextColor: draft.dscAction
        )
        return PromotionSDKTheme(
            button: button, searchBar: search, listItem: list,
            tabChip: tabChip, tabUnderline: tab, discountBadge: discount
        )
    }

    func applyTheme() {
        let theme = makeTheme()
        sdk.configure(theme: theme)
        ThemePreferenceManager.shared.save(theme)   // persist (mirror Android)
        reloadWidget()
        toast("Đã áp dụng + lưu theme")
    }

    func resetTheme() {
        draft = DraftTheme()
        sdk.configure(theme: nil)
        ThemePreferenceManager.shared.clear()
        for refresh in rowRefreshers { refresh() }
        for refresh in previewRefreshers { refresh() }
        reloadWidget()
        toast("Đã reset về mặc định")
    }

    func openMyPromotions() {
        sdk.configure(theme: makeTheme())
        sdk.openMyPromotion(from: self)
    }

    func reloadWidget() {
        widget?.removeFromSuperview()
        let w: UIView = sdk.createEndowView(from: self)
        w.translatesAutoresizingMaskIntoConstraints = false
        widgetContainer.addSubview(w)
        NSLayoutConstraint.activate([
            w.topAnchor.constraint(equalTo: widgetContainer.topAnchor, constant: 12),
            w.leadingAnchor.constraint(equalTo: widgetContainer.leadingAnchor, constant: 12),
            w.trailingAnchor.constraint(equalTo: widgetContainer.trailingAnchor, constant: -12),
            w.bottomAnchor.constraint(equalTo: widgetContainer.bottomAnchor, constant: -12)
        ])
        widget = w
    }
}
