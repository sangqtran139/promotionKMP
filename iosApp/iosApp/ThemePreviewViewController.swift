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
    /// Default THẬT của SDK, dạng hex — nguồn để reset và để so "đã đổi hay chưa".
    private var sdkDisplayDefaults = PromotionThemeDisplay.Defaults()

    init(sdk: PromotionSDK) {
        self.sdk = sdk
        super.init(nibName: nil, bundle: nil)
    }
    required init?(coder: NSCoder) { fatalError() }

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "Theme Playground"
        view.backgroundColor = .systemGroupedBackground

        // Seed form bằng default THẬT của SDK, rồi phủ theme đang áp lên trên.
        // SDK đã tự khôi phục theme đã lưu lúc khởi tạo (PromotionSDK.init) → đọc qua `sdk.currentTheme`,
        // demo không tự persist nữa. Mirror ThemePreviewFragment bên Android (dùng PromotionTheme.currentTheme).
        sdkDisplayDefaults = PromotionThemeDisplay.load()
        loadDraft(from: PromotionThemeDisplay.mergeWithSaved(sdk: sdkDisplayDefaults, saved: sdk.currentTheme))
        setupLayout()
        buildSections()
        reloadWidget()
    }

    /// Map display values (default SDK + theme đã lưu) → draft cho các hàng hiển thị.
    private func loadDraft(from d: PromotionThemeDisplay.Defaults) {
        let button = d.button.toToken()
        let search = d.searchBar.toToken()
        let list = d.listItem.toToken()
        let chip = d.tabChip.toToken()
        let underline = d.tabUnderline.toToken()
        let badge = d.discountBadge.toToken()
        draft.buttonBackground = button.backgroundColor
        draft.buttonText = button.textColor
        draft.buttonShadow = button.shadowColor
        draft.buttonCorner = button.cornerRadius
        draft.searchBorder = search.borderColor
        draft.searchHint = search.hintTextColor
        draft.searchText = search.textColor
        draft.searchIcon = search.iconColor
        draft.searchCorner = search.cornerRadius
        draft.listLink = list.linkTextColor
        draft.listUsedText = list.usedBadgeTextColor
        draft.listUsedBg = list.usedBadgeBackgroundColor
        draft.listRadioSelected = list.radioButtonSelectedStrokeColor
        draft.listRadioUnselected = list.radioButtonStrokeColor
        draft.tabChipActiveBg = chip.activeBackgroundColor
        draft.tabChipInactiveBg = chip.inactiveBackgroundColor
        draft.tabChipActiveText = chip.activeTextColor
        draft.tabChipInactiveText = chip.inactiveTextColor
        draft.tabChipCorner = chip.cornerRadius
        draft.tabIndicator = underline.indicatorColor
        draft.tabActiveText = underline.activeTextColor
        draft.tabInactiveText = underline.inactiveTextColor
        draft.tabBg = underline.backgroundColor
        draft.dscAvailText = badge.availableTextColor
        draft.dscUnavailText = badge.unavailableTextColor
        draft.dscAvailBg = badge.availableBackgroundColor
        draft.dscUnavailBg = badge.unavailableBackgroundColor
        draft.dscAction = badge.actionTextColor
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
        let button = ButtonToken(
            backgroundColor: draft.buttonBackground, textColor: draft.buttonText,
            shadowColor: draft.buttonShadow, cornerRadius: draft.buttonCorner
        )
        let search = SearchBarToken(
            borderColor: draft.searchBorder, hintTextColor: draft.searchHint,
            textColor: draft.searchText, iconColor: draft.searchIcon, cornerRadius: draft.searchCorner
        )
        let list = ListItemToken(
            linkTextColor: draft.listLink, usedBadgeTextColor: draft.listUsedText,
            usedBadgeBackgroundColor: draft.listUsedBg,
            radioButtonStrokeColor: draft.listRadioUnselected,
            radioButtonSelectedStrokeColor: draft.listRadioSelected
        )
        let tabChip = TabChipToken(
            activeBackgroundColor: draft.tabChipActiveBg, inactiveBackgroundColor: draft.tabChipInactiveBg,
            activeTextColor: draft.tabChipActiveText, inactiveTextColor: draft.tabChipInactiveText,
            cornerRadius: draft.tabChipCorner
        )
        let tab = TabUnderlineToken(
            indicatorColor: draft.tabIndicator, activeTextColor: draft.tabActiveText,
            inactiveTextColor: draft.tabInactiveText, backgroundColor: draft.tabBg
        )
        let discount = DiscountBadgeToken(
            availableTextColor: draft.dscAvailText, unavailableTextColor: draft.dscUnavailText,
            availableBackgroundColor: draft.dscAvailBg, unavailableBackgroundColor: draft.dscUnavailBg,
            actionTextColor: draft.dscAction
        )
        return PromotionSDKTheme(
            buttonToken: button, searchBarToken: search, listItemToken: list,
            tabChipToken: tabChip, tabUnderlineToken: tab, discountBadgeToken: discount
        )
    }

    func applyTheme() {
        let theme = makeTheme()
        sdk.configure(theme: theme)   // SDK áp + lưu
        reloadWidget()
        toast("Đã áp dụng + lưu theme")
    }

    /// Demo: theme đến từ **file JSON** (`promotion_theme.json` trong bundle, dùng chung với Android).
    func applyThemeFromJsonFile() {
        guard let theme = DemoThemeSource.fromJsonFile() else {
            return toast("promotion_theme.json hỏng — giữ theme cũ")
        }
        apply(theme, message: "Đã áp theme từ promotion_theme.json")
    }

    /// Demo: cùng bộ màu nhưng dựng bằng token trong code, không qua JSON.
    func applyThemeFromObject() {
        apply(DemoThemeSource.fromObject(), message: "Đã áp theme dựng bằng PromotionSDKTheme object")
    }

    /// Áp + lưu vào SDK, rồi nạp lại form/preview để thấy đúng giá trị vừa áp.
    private func apply(_ theme: PromotionSDKTheme, message: String) {
        sdk.configure(theme: theme)
        loadDraft(from: PromotionThemeDisplay.mergeWithSaved(sdk: sdkDisplayDefaults, saved: sdk.currentTheme))
        for refresh in rowRefreshers { refresh() }
        for refresh in previewRefreshers { refresh() }
        reloadWidget()
        toast(message)
    }

    func resetTheme() {
        draft = DraftTheme()
        loadDraft(from: sdkDisplayDefaults)
        sdk.configure(theme: nil)     // SDK xoá theme đã lưu
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
