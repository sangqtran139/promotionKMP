//
//  ThemePlaygroundSupport.swift
//  VDSPromotionDemoApp
//
//  Toàn bộ UI nặng của Theme Playground. File này CỐ TÌNH KHÔNG import PromotionSDKUI
//  để swift-frontend không phải deserialize class graph của framework khi type-check
//  khối lượng lớn (tránh crash đệ quy deserializeClass). Mọi điểm chạm SDK nằm ở
//  ThemePreviewViewController.swift.
//

import UIKit

// MARK: - Closure-based controls

final class ClosureButton: UIButton {
    var onTap: (() -> Void)?
    override init(frame: CGRect) {
        super.init(frame: frame)
        addTarget(self, action: #selector(tapped), for: .touchUpInside)
    }
    required init?(coder: NSCoder) { fatalError() }
    @objc private func tapped() { onTap?() }
}

final class ClosureSwitch: UISwitch {
    var onChange: ((Bool) -> Void)?
    override init(frame: CGRect) {
        super.init(frame: frame)
        addTarget(self, action: #selector(changed), for: .valueChanged)
    }
    required init?(coder: NSCoder) { fatalError() }
    @objc private func changed() { onChange?(isOn) }
}

final class ClosureSlider: UISlider {
    var onChange: ((Float) -> Void)?
    override init(frame: CGRect) {
        super.init(frame: frame)
        addTarget(self, action: #selector(changed), for: .valueChanged)
    }
    required init?(coder: NSCoder) { fatalError() }
    @objc private func changed() { onChange?(value) }
}

// MARK: - Draft state (plain — không phụ thuộc SDK)

struct DraftTheme {
    var buttonBackground: UIColor?
    var buttonText: UIColor?
    var buttonShadow: UIColor?
    var buttonCorner: CGFloat?
    var searchBorder: UIColor?
    var searchHint: UIColor?
    var searchText: UIColor?
    var searchIcon: UIColor?
    var searchCorner: CGFloat?
    var listLink: UIColor?
    var listUsedText: UIColor?
    var listUsedBg: UIColor?
    var listRadioSelected: UIColor?
    var listRadioUnselected: UIColor?
    var tabChipActiveBg: UIColor?
    var tabChipInactiveBg: UIColor?
    var tabChipActiveText: UIColor?
    var tabChipInactiveText: UIColor?
    var tabChipCorner: CGFloat?
    var tabIndicator: UIColor?
    var tabActiveText: UIColor?
    var tabInactiveText: UIColor?
    var tabBg: UIColor?
    var dscAvailText: UIColor?
    var dscUnavailText: UIColor?
    var dscAvailBg: UIColor?
    var dscUnavailBg: UIColor?
    var dscAction: UIColor?
}

// MARK: - Row spec

enum ThemeRowSpec {
    case color(title: String, get: () -> UIColor?, set: (UIColor?) -> Void)
    case slider(title: String, get: () -> CGFloat?, set: (CGFloat?) -> Void)
}

// MARK: - Color utils

enum ThemeColorUtil {
    static func hexString(_ color: UIColor) -> String {
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        color.getRed(&r, green: &g, blue: &b, alpha: &a)
        return String(format: "#%02X%02X%02X", Int(r * 255), Int(g * 255), Int(b * 255))
    }
    static func color(fromHex hex: String) -> UIColor? {
        var s = hex.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        if s.hasPrefix("#") { s.removeFirst() }
        guard s.count == 6, let v = UInt32(s, radix: 16) else { return nil }
        return UIColor(
            red: CGFloat((v & 0xFF0000) >> 16) / 255,
            green: CGFloat((v & 0x00FF00) >> 8) / 255,
            blue: CGFloat(v & 0x0000FF) / 255,
            alpha: 1
        )
    }
}

// MARK: - UI building (extension — KHÔNG chạm SDK)

extension ThemePreviewViewController {

    func buildSections() {
        contentStack.addArrangedSubview(infoLabel(
            "Chỉnh màu/bo góc từng token — preview mô phỏng ngay dưới mỗi nhóm đổi LIVE. Bấm \"Áp dụng\" để "
            + "áp thật vào SDK; widget cuối trang + \"Mở màn Ưu đãi (UI)\" là render thật của SDK."
        ))

        let buttonRows: [ThemeRowSpec] = [
            .color(title: "backgroundColor", get: { [weak self] in self?.draft.buttonBackground }, set: { [weak self] in self?.draft.buttonBackground = $0 }),
            .color(title: "textColor", get: { [weak self] in self?.draft.buttonText }, set: { [weak self] in self?.draft.buttonText = $0 }),
            .color(title: "shadowColor", get: { [weak self] in self?.draft.buttonShadow }, set: { [weak self] in self?.draft.buttonShadow = $0 }),
            .slider(title: "cornerRadius", get: { [weak self] in self?.draft.buttonCorner }, set: { [weak self] in self?.draft.buttonCorner = $0 })
        ]
        addSection("BUTTON", rows: buttonRows) { [weak self] in self?.buttonPreview() }

        let searchRows: [ThemeRowSpec] = [
            .color(title: "borderColor", get: { [weak self] in self?.draft.searchBorder }, set: { [weak self] in self?.draft.searchBorder = $0 }),
            .color(title: "hintTextColor", get: { [weak self] in self?.draft.searchHint }, set: { [weak self] in self?.draft.searchHint = $0 }),
            .color(title: "textColor", get: { [weak self] in self?.draft.searchText }, set: { [weak self] in self?.draft.searchText = $0 }),
            .color(title: "iconColor", get: { [weak self] in self?.draft.searchIcon }, set: { [weak self] in self?.draft.searchIcon = $0 }),
            .slider(title: "cornerRadius", get: { [weak self] in self?.draft.searchCorner }, set: { [weak self] in self?.draft.searchCorner = $0 })
        ]
        addSection("SEARCH BAR", rows: searchRows) { [weak self] in self?.searchPreview() }

        let listRows: [ThemeRowSpec] = [
            .color(title: "linkTextColor", get: { [weak self] in self?.draft.listLink }, set: { [weak self] in self?.draft.listLink = $0 }),
            .color(title: "usedBadgeTextColor", get: { [weak self] in self?.draft.listUsedText }, set: { [weak self] in self?.draft.listUsedText = $0 }),
            .color(title: "usedBadgeBackgroundColor", get: { [weak self] in self?.draft.listUsedBg }, set: { [weak self] in self?.draft.listUsedBg = $0 }),
            .color(title: "radioSelectedColor", get: { [weak self] in self?.draft.listRadioSelected }, set: { [weak self] in self?.draft.listRadioSelected = $0 }),
            .color(title: "radioUnselectedColor", get: { [weak self] in self?.draft.listRadioUnselected }, set: { [weak self] in self?.draft.listRadioUnselected = $0 })
        ]
        addSection("LIST ITEM", rows: listRows) { [weak self] in self?.listItemPreview() }

        let tabChipRows: [ThemeRowSpec] = [
            .color(title: "activeBackgroundColor", get: { [weak self] in self?.draft.tabChipActiveBg }, set: { [weak self] in self?.draft.tabChipActiveBg = $0 }),
            .color(title: "inactiveBackgroundColor", get: { [weak self] in self?.draft.tabChipInactiveBg }, set: { [weak self] in self?.draft.tabChipInactiveBg = $0 }),
            .color(title: "activeTextColor", get: { [weak self] in self?.draft.tabChipActiveText }, set: { [weak self] in self?.draft.tabChipActiveText = $0 }),
            .color(title: "inactiveTextColor", get: { [weak self] in self?.draft.tabChipInactiveText }, set: { [weak self] in self?.draft.tabChipInactiveText = $0 }),
            .slider(title: "cornerRadius", get: { [weak self] in self?.draft.tabChipCorner }, set: { [weak self] in self?.draft.tabChipCorner = $0 })
        ]
        addSection("TAB CHIP (Ưu đãi của tôi)", rows: tabChipRows) { [weak self] in self?.tabChipPreview() }

        let tabRows: [ThemeRowSpec] = [
            .color(title: "indicatorColor", get: { [weak self] in self?.draft.tabIndicator }, set: { [weak self] in self?.draft.tabIndicator = $0 }),
            .color(title: "activeTextColor", get: { [weak self] in self?.draft.tabActiveText }, set: { [weak self] in self?.draft.tabActiveText = $0 }),
            .color(title: "inactiveTextColor", get: { [weak self] in self?.draft.tabInactiveText }, set: { [weak self] in self?.draft.tabInactiveText = $0 }),
            .color(title: "backgroundColor", get: { [weak self] in self?.draft.tabBg }, set: { [weak self] in self?.draft.tabBg = $0 })
        ]
        addSection("TAB UNDERLINE", rows: tabRows) { [weak self] in self?.tabUnderlinePreview() }

        let discountRows: [ThemeRowSpec] = [
            .color(title: "availableTextColor", get: { [weak self] in self?.draft.dscAvailText }, set: { [weak self] in self?.draft.dscAvailText = $0 }),
            .color(title: "unavailableTextColor", get: { [weak self] in self?.draft.dscUnavailText }, set: { [weak self] in self?.draft.dscUnavailText = $0 }),
            .color(title: "availableBackgroundColor", get: { [weak self] in self?.draft.dscAvailBg }, set: { [weak self] in self?.draft.dscAvailBg = $0 }),
            .color(title: "unavailableBackgroundColor", get: { [weak self] in self?.draft.dscUnavailBg }, set: { [weak self] in self?.draft.dscUnavailBg = $0 }),
            .color(title: "actionTextColor", get: { [weak self] in self?.draft.dscAction }, set: { [weak self] in self?.draft.dscAction = $0 })
        ]
        addSection("DISCOUNT BADGE", rows: discountRows) { [weak self] in self?.discountBadgePreview() }

        contentStack.addArrangedSubview(actionButton("✓  Áp dụng", color: .systemGreen) { [weak self] in self?.applyTheme() })
        contentStack.addArrangedSubview(actionButton("↺  Reset về mặc định", color: .systemGray) { [weak self] in self?.resetTheme() })
        contentStack.addArrangedSubview(actionButton("→  Mở màn Ưu đãi (UI)", color: .systemBlue) { [weak self] in self?.openMyPromotions() })

        contentStack.addArrangedSubview(sectionTitle("PREVIEW THẬT (widget SDK)"))
        widgetContainer.heightAnchor.constraint(greaterThanOrEqualToConstant: 80).isActive = true
        contentStack.addArrangedSubview(widgetContainer)
    }

    /// `preview`: builder trả (view mô phỏng, closure refresh theo draft). Row đổi giá trị → refresh preview LIVE.
    private func addSection(_ title: String, rows: [ThemeRowSpec], preview: (() -> (UIView, () -> Void)?)? = nil) {
        contentStack.addArrangedSubview(sectionTitle(title))
        var previewRefresh: (() -> Void)?
        let card = UIStackView()
        card.axis = .vertical
        card.spacing = 0
        card.backgroundColor = .secondarySystemGroupedBackground
        card.layer.cornerRadius = 12
        card.isLayoutMarginsRelativeArrangement = true
        card.layoutMargins = UIEdgeInsets(top: 4, left: 12, bottom: 4, right: 12)
        for spec in rows {
            card.addArrangedSubview(makeRow(spec, onChanged: { previewRefresh?() }))
        }
        contentStack.addArrangedSubview(card)

        if let built = preview?() {
            let (previewView, refresh) = built
            previewRefresh = refresh
            previewRefreshers.append(refresh)
            contentStack.addArrangedSubview(previewWrap(previewView))
            refresh()
        }
    }

    private func makeRow(_ spec: ThemeRowSpec, onChanged: @escaping () -> Void) -> UIView {
        switch spec {
        case let .color(title, get, set):
            return colorRow(title: title, get: get, set: set, onChanged: onChanged)
        case let .slider(title, get, set):
            return sliderRow(title: title, get: get, set: set, onChanged: onChanged)
        }
    }

    private func colorRow(title: String, get: @escaping () -> UIColor?, set: @escaping (UIColor?) -> Void, onChanged: @escaping () -> Void) -> UIView {
        let label = UILabel()
        label.text = title
        label.font = .systemFont(ofSize: 14)
        label.adjustsFontSizeToFitWidth = true
        label.minimumScaleFactor = 0.7

        let hexLabel = UILabel()
        hexLabel.font = .monospacedSystemFont(ofSize: 11, weight: .regular)
        hexLabel.textColor = .secondaryLabel

        let swatch = ClosureButton()
        swatch.layer.cornerRadius = 14
        swatch.layer.borderWidth = 1
        swatch.layer.borderColor = UIColor.separator.cgColor
        swatch.clipsToBounds = true
        swatch.widthAnchor.constraint(equalToConstant: 28).isActive = true
        swatch.heightAnchor.constraint(equalToConstant: 28).isActive = true

        let clear = ClosureButton()
        clear.setTitle("⌫", for: .normal)
        clear.setTitleColor(.systemRed, for: .normal)

        let refresh: () -> Void = { [weak swatch, weak hexLabel] in
            let c: UIColor? = get()
            swatch?.backgroundColor = c ?? .clear
            hexLabel?.text = c.map { ThemeColorUtil.hexString($0) } ?? "mặc định"
        }
        swatch.onTap = { [weak self] in
            self?.openColorPicker(initial: get()) { color in
                set(color)
                refresh()
                onChanged()
            }
        }
        clear.onTap = {
            set(nil)
            refresh()
            onChanged()
        }
        rowRefreshers.append(refresh)
        refresh()

        let row = UIStackView(arrangedSubviews: [label, hexLabel, swatch, clear])
        row.axis = .horizontal
        row.spacing = 8
        row.alignment = .center
        label.setContentHuggingPriority(.defaultLow, for: .horizontal)
        return wrapRow(row)
    }

    private func sliderRow(title: String, get: @escaping () -> CGFloat?, set: @escaping (CGFloat?) -> Void, onChanged: @escaping () -> Void) -> UIView {
        let label = UILabel()
        label.text = title
        label.font = .systemFont(ofSize: 14)

        let valueLabel = UILabel()
        valueLabel.font = .monospacedSystemFont(ofSize: 11, weight: .regular)
        valueLabel.textColor = .secondaryLabel

        let toggle = ClosureSwitch()
        let slider = ClosureSlider()
        slider.minimumValue = 0
        slider.maximumValue = 30

        let refresh: () -> Void = { [weak slider, weak toggle, weak valueLabel] in
            let v: CGFloat? = get()
            if let v = v {
                toggle?.isOn = true
                slider?.isEnabled = true
                slider?.value = Float(v)
                valueLabel?.text = "\(Int(v)) pt"
            } else {
                toggle?.isOn = false
                slider?.isEnabled = false
                valueLabel?.text = "mặc định"
            }
        }
        toggle.onChange = { [weak slider] isOn in
            let current: Float = slider?.value ?? 0
            set(isOn ? CGFloat(current.rounded()) : nil)
            refresh()
            onChanged()
        }
        slider.onChange = { [weak valueLabel] value in
            guard get() != nil else { return }
            let rounded: Int = Int(value.rounded())
            set(CGFloat(rounded))
            valueLabel?.text = "\(rounded) pt"
            onChanged()
        }
        rowRefreshers.append(refresh)
        refresh()

        let top = UIStackView(arrangedSubviews: [label, valueLabel, toggle])
        top.axis = .horizontal
        top.spacing = 8
        top.alignment = .center
        label.setContentHuggingPriority(.defaultLow, for: .horizontal)

        let col = UIStackView(arrangedSubviews: [top, slider])
        col.axis = .vertical
        col.spacing = 4
        return wrapRow(col)
    }

    private func wrapRow(_ content: UIView) -> UIView {
        let container = UIView()
        content.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(content)
        NSLayoutConstraint.activate([
            content.topAnchor.constraint(equalTo: container.topAnchor, constant: 10),
            content.bottomAnchor.constraint(equalTo: container.bottomAnchor, constant: -10),
            content.leadingAnchor.constraint(equalTo: container.leadingAnchor),
            content.trailingAnchor.constraint(equalTo: container.trailingAnchor)
        ])
        let sep = UIView()
        sep.backgroundColor = .separator
        sep.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(sep)
        NSLayoutConstraint.activate([
            sep.heightAnchor.constraint(equalToConstant: 0.5),
            sep.leadingAnchor.constraint(equalTo: container.leadingAnchor),
            sep.trailingAnchor.constraint(equalTo: container.trailingAnchor),
            sep.bottomAnchor.constraint(equalTo: container.bottomAnchor)
        ])
        return container
    }

    private func sectionTitle(_ text: String) -> UILabel {
        let l = UILabel()
        l.text = text
        l.font = .systemFont(ofSize: 13, weight: .semibold)
        l.textColor = .secondaryLabel
        return l
    }

    private func infoLabel(_ text: String) -> UILabel {
        let l = UILabel()
        l.text = text
        l.numberOfLines = 0
        l.font = .systemFont(ofSize: 12)
        l.textColor = .secondaryLabel
        return l
    }

    private func actionButton(_ title: String, color: UIColor, _ onTap: @escaping () -> Void) -> UIButton {
        let btn = ClosureButton()
        btn.setTitle(title, for: .normal)
        btn.titleLabel?.font = .systemFont(ofSize: 16, weight: .semibold)
        btn.backgroundColor = color
        btn.setTitleColor(.white, for: .normal)
        btn.layer.cornerRadius = 10
        btn.heightAnchor.constraint(equalToConstant: 46).isActive = true
        btn.onTap = onTap
        return btn
    }

    func openColorPicker(initial: UIColor?, setter: @escaping (UIColor?) -> Void) {
        if #available(iOS 14.0, *) {
            activeColorSetter = setter
            let picker = UIColorPickerViewController()
            picker.selectedColor = initial ?? .white
            picker.delegate = self
            present(picker, animated: true)
        } else {
            promptHexColor(initial: initial, setter: setter)
        }
    }

    private func promptHexColor(initial: UIColor?, setter: @escaping (UIColor?) -> Void) {
        let alert = UIAlertController(title: "Nhập màu HEX", message: "VD: #EE0033", preferredStyle: .alert)
        alert.addTextField { tf in tf.text = initial.map { ThemeColorUtil.hexString($0) } ?? "#" }
        alert.addAction(UIAlertAction(title: "OK", style: .default) { _ in
            let text: String = alert.textFields?.first?.text ?? ""
            setter(ThemeColorUtil.color(fromHex: text))
        })
        alert.addAction(UIAlertAction(title: "Huỷ", style: .cancel))
        present(alert, animated: true)
    }

    func toast(_ message: String) {
        let alert = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        present(alert, animated: true)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) { alert.dismiss(animated: true) }
    }
}

// MARK: - Mock previews (mô phỏng component SDK bằng UIKit thường — KHÔNG chạm SDK)

extension ThemePreviewViewController {

    // Màu default xấp xỉ SDK (khi token nil).
    private var pvRed: UIColor { ThemeColorUtil.color(fromHex: "#EE0033") ?? .systemRed }
    private var pvDark: UIColor { ThemeColorUtil.color(fromHex: "#1F1F1F") ?? .darkText }
    private var pvGray: UIColor { ThemeColorUtil.color(fromHex: "#7A7A7A") ?? .gray }
    private var pvLight: UIColor { ThemeColorUtil.color(fromHex: "#F2F2F2") ?? .systemGray6 }

    func previewWrap(_ view: UIView) -> UIView {
        let caption = UILabel()
        caption.text = "Preview (mô phỏng)"
        caption.font = .systemFont(ofSize: 11)
        caption.textColor = .tertiaryLabel
        let stack = UIStackView(arrangedSubviews: [caption, view])
        stack.axis = .vertical
        stack.spacing = 8
        stack.translatesAutoresizingMaskIntoConstraints = false
        let container = UIView()
        container.backgroundColor = .secondarySystemGroupedBackground
        container.layer.cornerRadius = 12
        container.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.topAnchor.constraint(equalTo: container.topAnchor, constant: 12),
            stack.bottomAnchor.constraint(equalTo: container.bottomAnchor, constant: -12),
            stack.leadingAnchor.constraint(equalTo: container.leadingAnchor, constant: 12),
            stack.trailingAnchor.constraint(equalTo: container.trailingAnchor, constant: -12)
        ])
        return container
    }

    private func chip(_ text: String) -> UIButton {
        let b = UIButton(type: .custom)
        b.setTitle(text, for: .normal)
        b.titleLabel?.font = .systemFont(ofSize: 13, weight: .medium)
        b.contentEdgeInsets = UIEdgeInsets(top: 6, left: 12, bottom: 6, right: 12)
        b.isUserInteractionEnabled = false
        b.layer.cornerRadius = 8
        b.clipsToBounds = true
        return b
    }

    // BUTTON
    func buttonPreview() -> (UIView, () -> Void) {
        let btn = UILabel()
        btn.text = "Áp dụng"
        btn.textAlignment = .center
        btn.font = .systemFont(ofSize: 15, weight: .semibold)
        btn.heightAnchor.constraint(equalToConstant: 46).isActive = true
        let refresh: () -> Void = { [weak self, weak btn] in
            guard let self = self, let btn = btn else { return }
            let d = self.draft
            btn.backgroundColor = d.buttonBackground ?? self.pvRed
            btn.textColor = d.buttonText ?? .white
            btn.layer.cornerRadius = d.buttonCorner ?? 8
            if let shadow = d.buttonShadow {
                btn.layer.shadowColor = shadow.cgColor
                btn.layer.shadowOpacity = 0.55
                btn.layer.shadowRadius = 6
                btn.layer.shadowOffset = CGSize(width: 0, height: 3)
                btn.layer.masksToBounds = false
            } else {
                btn.layer.shadowOpacity = 0
            }
        }
        return (btn, refresh)
    }

    // SEARCH BAR
    func searchPreview() -> (UIView, () -> Void) {
        let container = UIView()
        container.backgroundColor = .white
        container.layer.borderWidth = 1
        container.heightAnchor.constraint(equalToConstant: 44).isActive = true

        let icon = UIImageView(image: UIImage(systemName: "magnifyingglass"))
        icon.contentMode = .scaleAspectFit
        let hint = UILabel()
        hint.text = "Tìm ưu đãi"
        hint.font = .systemFont(ofSize: 14)
        let spacer = UIView()
        spacer.setContentHuggingPriority(.defaultLow, for: .horizontal)
        // Icon clear (X) — phải tint CÙNG iconColor với icon search.
        let clearIcon = UIImageView(image: UIImage(systemName: "xmark.circle.fill"))
        clearIcon.contentMode = .scaleAspectFit

        let row = UIStackView(arrangedSubviews: [icon, hint, spacer, clearIcon])
        row.axis = .horizontal
        row.spacing = 8
        row.alignment = .center
        row.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(row)
        NSLayoutConstraint.activate([
            icon.widthAnchor.constraint(equalToConstant: 18),
            icon.heightAnchor.constraint(equalToConstant: 18),
            clearIcon.widthAnchor.constraint(equalToConstant: 18),
            clearIcon.heightAnchor.constraint(equalToConstant: 18),
            row.leadingAnchor.constraint(equalTo: container.leadingAnchor, constant: 12),
            row.trailingAnchor.constraint(equalTo: container.trailingAnchor, constant: -12),
            row.centerYAnchor.constraint(equalTo: container.centerYAnchor)
        ])
        let refresh: () -> Void = { [weak self, weak container, weak icon, weak hint, weak clearIcon] in
            guard let self = self else { return }
            let d = self.draft
            container?.layer.cornerRadius = d.searchCorner ?? 8
            container?.layer.borderColor = (d.searchBorder ?? self.pvLight).cgColor
            let iconTint = d.searchIcon ?? self.pvGray
            icon?.tintColor = iconTint
            clearIcon?.tintColor = iconTint
            hint?.textColor = d.searchHint ?? self.pvGray
        }
        return (container, refresh)
    }

    // LIST ITEM
    func listItemPreview() -> (UIView, () -> Void) {
        let title = UILabel()
        title.text = "Voucher giảm 50.000đ"
        title.font = .systemFont(ofSize: 15, weight: .medium)
        title.textColor = pvDark

        let link = UILabel()
        link.text = "Dùng ngay ›"
        link.font = .systemFont(ofSize: 13, weight: .medium)

        let badge = chip("Đã dùng")
        badge.titleLabel?.font = .systemFont(ofSize: 11, weight: .bold)

        let spacer = UIView()
        spacer.setContentHuggingPriority(.defaultLow, for: .horizontal)
        let row = UIStackView(arrangedSubviews: [link, spacer, badge])
        row.axis = .horizontal
        row.spacing = 8
        row.alignment = .center

        // Radio preview: 1 đã chọn + 1 chưa chọn.
        let radioOn = UIImageView(image: UIImage(systemName: "checkmark.circle.fill"))
        let radioOff = UIImageView(image: UIImage(systemName: "circle"))
        [radioOn, radioOff].forEach {
            $0.contentMode = .scaleAspectFit
            $0.widthAnchor.constraint(equalToConstant: 22).isActive = true
            $0.heightAnchor.constraint(equalToConstant: 22).isActive = true
        }
        let radioLabel = UILabel()
        radioLabel.text = "Radio:"
        radioLabel.font = .systemFont(ofSize: 13)
        radioLabel.textColor = .secondaryLabel
        let radioSpacer = UIView()
        radioSpacer.setContentHuggingPriority(.defaultLow, for: .horizontal)
        let radioRow = UIStackView(arrangedSubviews: [radioLabel, radioSpacer, radioOn, radioOff])
        radioRow.axis = .horizontal
        radioRow.spacing = 10
        radioRow.alignment = .center

        let col = UIStackView(arrangedSubviews: [title, row, radioRow])
        col.axis = .vertical
        col.spacing = 8
        let refresh: () -> Void = { [weak self, weak link, weak badge, weak radioOn, weak radioOff] in
            guard let self = self else { return }
            let d = self.draft
            link?.textColor = d.listLink ?? self.pvRed
            badge?.setTitleColor(d.listUsedText ?? self.pvGray, for: .normal)
            badge?.backgroundColor = d.listUsedBg ?? self.pvLight
            radioOn?.tintColor = d.listRadioSelected ?? self.pvRed
            radioOff?.tintColor = d.listRadioUnselected ?? self.pvGray
        }
        return (col, refresh)
    }

    // TAB CHIP
    func tabChipPreview() -> (UIView, () -> Void) {
        let active = chip("Tất cả")
        let inactive = chip("Sắp hết hạn")
        let spacer = UIView()
        spacer.setContentHuggingPriority(.defaultLow, for: .horizontal)
        let row = UIStackView(arrangedSubviews: [active, inactive, spacer])
        row.axis = .horizontal
        row.spacing = 8
        let refresh: () -> Void = { [weak self, weak active, weak inactive] in
            guard let self = self else { return }
            let d = self.draft
            let corner = d.tabChipCorner ?? 8
            active?.backgroundColor = d.tabChipActiveBg ?? self.pvRed
            active?.setTitleColor(d.tabChipActiveText ?? .white, for: .normal)
            active?.layer.cornerRadius = corner
            inactive?.backgroundColor = d.tabChipInactiveBg ?? self.pvLight
            inactive?.setTitleColor(d.tabChipInactiveText ?? self.pvDark, for: .normal)
            inactive?.layer.cornerRadius = corner
        }
        return (row, refresh)
    }

    // TAB UNDERLINE
    func tabUnderlinePreview() -> (UIView, () -> Void) {
        func tab(_ text: String) -> (UIStackView, UILabel, UIView) {
            let l = UILabel()
            l.text = text
            l.font = .systemFont(ofSize: 14, weight: .medium)
            l.textAlignment = .center
            let line = UIView()
            line.heightAnchor.constraint(equalToConstant: 2).isActive = true
            let s = UIStackView(arrangedSubviews: [l, line])
            s.axis = .vertical
            s.spacing = 6
            return (s, l, line)
        }
        let (s1, activeLabel, activeLine) = tab("Của tôi")
        let (s2, inactiveLabel, inactiveLine) = tab("Khác")
        let container = UIView()
        let row = UIStackView(arrangedSubviews: [s1, s2])
        row.axis = .horizontal
        row.distribution = .fillEqually
        row.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(row)
        NSLayoutConstraint.activate([
            row.topAnchor.constraint(equalTo: container.topAnchor, constant: 8),
            row.bottomAnchor.constraint(equalTo: container.bottomAnchor, constant: -8),
            row.leadingAnchor.constraint(equalTo: container.leadingAnchor, constant: 8),
            row.trailingAnchor.constraint(equalTo: container.trailingAnchor, constant: -8)
        ])
        let refresh: () -> Void = { [weak self, weak container, weak activeLabel, weak activeLine, weak inactiveLabel, weak inactiveLine] in
            guard let self = self else { return }
            let d = self.draft
            container?.backgroundColor = d.tabBg ?? .white
            activeLabel?.textColor = d.tabActiveText ?? self.pvDark
            inactiveLabel?.textColor = d.tabInactiveText ?? self.pvGray
            activeLine?.backgroundColor = d.tabIndicator ?? self.pvRed
            inactiveLine?.backgroundColor = .clear
        }
        return (container, refresh)
    }

    // DISCOUNT BADGE
    func discountBadgePreview() -> (UIView, () -> Void) {
        let available = chip("Giảm 50K")
        let unavailable = chip("Chưa đủ ĐK")
        let action = UILabel()
        action.text = "Sử dụng"
        action.font = .systemFont(ofSize: 13, weight: .semibold)
        let spacer = UIView()
        spacer.setContentHuggingPriority(.defaultLow, for: .horizontal)
        let row = UIStackView(arrangedSubviews: [available, unavailable, spacer, action])
        row.axis = .horizontal
        row.spacing = 8
        row.alignment = .center
        let refresh: () -> Void = { [weak self, weak available, weak unavailable, weak action] in
            guard let self = self else { return }
            let d = self.draft
            available?.setTitleColor(d.dscAvailText ?? self.pvRed, for: .normal)
            available?.backgroundColor = d.dscAvailBg ?? self.pvRed.withAlphaComponent(0.08)
            unavailable?.setTitleColor(d.dscUnavailText ?? self.pvGray, for: .normal)
            unavailable?.backgroundColor = d.dscUnavailBg ?? self.pvLight
            action?.textColor = d.dscAction ?? self.pvRed
        }
        return (row, refresh)
    }
}

// MARK: - UIColorPickerViewControllerDelegate

@available(iOS 14.0, *)
extension ThemePreviewViewController: UIColorPickerViewControllerDelegate {
    func colorPickerViewControllerDidSelectColor(_ viewController: UIColorPickerViewController) {
        activeColorSetter?(viewController.selectedColor)
    }
    func colorPickerViewControllerDidFinish(_ viewController: UIColorPickerViewController) {
        activeColorSetter = nil
    }
}
