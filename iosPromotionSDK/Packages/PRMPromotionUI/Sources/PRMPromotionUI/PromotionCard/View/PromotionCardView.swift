//
//  PromotionCardView.swift
//  PRMDesignKit
//
//  Created by thachlh on 20/4/26.
//


import UIKit
import PRMFoundation
import PRMDesignKit

public protocol PromotionCardViewDelegate: AnyObject {
    func promotionCardViewDidTap(_ view: PromotionCardView)
    func promotionCardViewDidTapButton(_ view: PromotionCardView)
    func promotionCardView(_ view: PromotionCardView, didToggleCheckbox isChecked: Bool)
}

public class PromotionCardView: PRMTapableView {
    
    // MARK: - Delegate
    public weak var delegate: PromotionCardViewDelegate?
    
    // MARK: - UI Components
    
    private let backgroundView: CouponBackgroundView = {
        let view = CouponBackgroundView()
        view.absoluteDashPosition = 108
        return view
    }()
    
    private let iconContainer: UIView = {
        let view = UIView()
        view.backgroundColor = Colors.tokenSpaceBlue05
        view.layer.cornerRadius = 24
        view.clipsToBounds = true
        return view
    }()
    
    private let iconImageView: UIImageView = {
        let imageView = UIImageView()
        imageView.contentMode = .scaleAspectFit
        imageView.clipsToBounds = true
        return imageView
    }()
    
    /// HSD: 1 dòng, dài quá thì TỰ CHẠY CHỮ (marquee) chứ không cắt ba chấm.
    /// - hugging cao hơn `spacerView` → phần dư dồn cho spacer, nút "Sử dụng" luôn sát mép phải.
    /// - compression resistance THẤP → máy nhỏ thì HSD bị bóp trước, khối "Sử dụng + icon"
    ///   và badge trạng thái không bao giờ co (trước đây cả hai cùng 750 → hoà, Auto Layout
    ///   tự chọn bên nào bóp nên hay mất chữ "Sử dụng"/mũi tên).
    private let dateLabel: PRMMarqueeLabel = {
        let label = PRMMarqueeLabel()
        label.font = Typography.fontRegular12
        label.textColor = Colors.tokenDark60
        label.setContentHuggingPriority(.defaultHigh, for: .horizontal)
        label.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        return label
    }()
    
    private let titleLabel: UILabel = {
        let label = UILabel()
        label.font = Typography.fontRegular12
        label.textColor = Colors.tokenDark60
        label.numberOfLines = 1               // merchant: 1 dòng
        label.lineBreakMode = .byTruncatingTail // dài quá → ba chấm
        label.setContentHuggingPriority(.defaultHigh + 1, for: .vertical)
        label.setContentCompressionResistancePriority(.defaultHigh + 1, for: .vertical)
        return label
    }()
    
    private let descriptionLabel: UILabel = {
        let label = UILabel()
        label.font = Typography.fontMedium16
        label.textColor = Colors.tokenDark100
        label.numberOfLines = 2                // title: 2 dòng
        label.lineBreakMode = .byTruncatingTail // dài quá → ba chấm
        return label
    }()
    
    /// Nút "Sử dụng + icon": **luôn hiện đủ chữ**. Hugging + resistance `.required` → stack không
    /// nở cũng không bóp, bề rộng bám đúng nội dung; phần rộng còn lại mới nhường cho HSD.
    private let actionButton: UIButton = {
        let button = UIButton(type: .custom)
        button.titleLabel?.font = Typography.fontMedium14
        button.setTitleColor(Colors.tokenViettelPayRed100, for: .normal)
        button.contentHorizontalAlignment = .right
        button.titleLabel?.lineBreakMode = .byClipping
        button.setContentHuggingPriority(.required, for: .horizontal)
        button.setContentCompressionResistancePriority(.required, for: .horizontal)
        return button
    }()

    /// Badge trạng thái ("Đã dùng"/"Hết hạn"…) — cùng ràng buộc như [actionButton].
    private let stateContainerView: UIView = {
        let view = UIView()
        view.backgroundColor = Colors.tokenDark05
        view.layer.cornerRadius = 16
        view.clipsToBounds = true
        view.setContentHuggingPriority(.required, for: .horizontal)
        view.setContentCompressionResistancePriority(.required, for: .horizontal)
        return view
    }()
    
    private let stateLabel: UILabel = {
        let label = UILabel()
        label.font = Typography.fontBold9
        label.textColor = Colors.tokenDark60
        label.textAlignment = .center
        return label
    }()
    
    /// Khe giữa HSD và khối "Sử dụng"/badge: TỐI THIỂU 5px (xem `setupConstraints`), dư thì nở ra.
    private let spacerView: UIView = {
        let view = UIView()
        view.setContentHuggingPriority(.defaultLow, for: .horizontal)
        return view
    }()

    /// `spacing = 0` — khe duy nhất là `spacerView` (min 5px), tránh cộng dồn 8 + 8 = 16px như trước.
    private let footerStackView: UIStackView = {
        let stack = UIStackView()
        stack.axis = .horizontal
        stack.spacing = 0
        stack.alignment = .center
        return stack
    }()
    
    private let checkboxButton: PRMRadioButton = {
        let radio = PRMRadioButton()
        radio.radioTitle = ""
        radio.isAnimationSelect = true
        radio.isLeft = false   // ảnh radio nằm PHẢI → khớp mép phải, không méo do label rỗng
        return radio
    }()
    
    private let blurOverlayView: UIView = {
        let view = UIView()
        view.backgroundColor = UIColor.white.withAlphaComponent(0.6)
        view.layer.cornerRadius = 16
        view.clipsToBounds = true
        view.isHidden = true
        view.isUserInteractionEnabled = false
        return view
    }()
    
    private var model: PromotionCardModel?

    /// Theme token host cấu hình (nil = giữ default SDK).
    private var themeToken: PRMListItemThemeToken? { PRMThemeRegistry.shared.listItem() }

    // MARK: - Init
    
    public override init(frame: CGRect) {
        super.init(frame: frame)
        setupView()
        setupConstraints()
        setupActions()
    }
    
    public required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupView()
        setupConstraints()
        setupActions()
    }
    
    private func setupView() {
        self.backgroundColor = .clear
        
        self.backgroundView.fillColor = Colors.tokenWhite
        addSubview(backgroundView)
        
        addSubview(iconContainer)
        iconContainer.addSubview(iconImageView)
        
        addSubview(titleLabel)       // merchant name
        addSubview(descriptionLabel) // title (tên ưu đãi)
        
        addSubview(footerStackView)
        // Nút và badge là hai arranged subview RIÊNG, không bọc chung container nữa: chúng loại trừ
        // nhau, mà stack tự thu hồi chỗ của arranged subview bị `isHidden` — còn view thường thì
        // constraint vẫn sống. Bọc chung khiến `button.width == stateLabel.width + 16` (required, qua
        // container) luôn đúng kể cả khi badge đang ẩn + rỗng → nút bị ép còn 16px, chữ "Sử dụng" ra "…".
        footerStackView.addArrangedSubview(dateLabel)
        footerStackView.addArrangedSubview(spacerView)
        footerStackView.addArrangedSubview(actionButton)
        footerStackView.addArrangedSubview(stateContainerView)

        stateContainerView.addSubview(stateLabel)
        addSubview(checkboxButton)
        addSubview(blurOverlayView)
    }
    
    private func setupConstraints() {
        backgroundView.makeAnchor { make in
            make.edges(to: self)
        }
        
        iconContainer.makeAnchor { make in
            make.centerY(equalTo: self.centerYAnchor)
                .centerX(equalTo: self.leadingAnchor, constant: 54)
                .size(CGSize(width: 48, height: 48))
        }
        
        iconImageView.makeAnchor { make in
            make.edges(to: iconContainer)
        }
        
        // Checkbox: ảnh radio 24×24 (nội tại), cách trên 9px, mép phải 15px.
        // KHÔNG ép width cả PRMRadioButton (label rỗng bên trong sẽ âm width → méo); chỉ ghim height.
        checkboxButton.makeAnchor { make in
            make.top(equalTo: self.topAnchor, constant: 9)
                .trailing(equalTo: self.trailingAnchor, constant: -15)
                .height(equalTo: 24)
        }

        // Merchant: 1 dòng, neo GIỮA theo checkbox (radio), kết thúc trước checkbox 8px.
        titleLabel.makeAnchor { make in
            make.leading(equalTo: self.leadingAnchor, constant: 124)
                .trailing(equalTo: checkboxButton.leadingAnchor, constant: -8)
                .centerY(equalTo: checkboxButton.centerYAnchor)
        }

        // Title (tên ưu đãi): 2 dòng, dưới merchant 4px.
        descriptionLabel.makeAnchor { make in
            make.top(equalTo: titleLabel.bottomAnchor, constant: 4)
                .leading(equalTo: self.leadingAnchor, constant: 124)
                .trailing(equalTo: self.trailingAnchor, constant: -16)
        }

        // HSD + Chi tiết: CỐ ĐỊNH cách title 5px và chừa đủ 2 dòng title (pin theo title.top + 2 dòng),
        // nên title 1 hay 2 dòng thì hàng này không đổi vị trí. Đáy cách bottom 9px, phải 15px (đồng bộ checkbox).
        let twoLineTitleHeight = (descriptionLabel.font.lineHeight * 2).rounded(.up)
        footerStackView.makeAnchor { make in
            make.top(equalTo: descriptionLabel.topAnchor, constant: twoLineTitleHeight + 5)
                .leading(equalTo: self.leadingAnchor, constant: 124)
                .trailing(equalTo: self.trailingAnchor, constant: -15)
                .bottom(equalTo: self.bottomAnchor, constant: -9)
                .height(greaterThanOrEqualTo: 24)
        }
        
        // Khe HSD ↔ "Sử dụng"/badge: tối thiểu 5px; hugging thấp nên khi HSD ngắn thì spacer nở ra.
        spacerView.makeAnchor { make in
            make.width(greaterThanOrEqualTo: 5)
        }

        // Nút và badge tự co theo nội dung (không còn constraint ép bằng bề rộng container).
        stateLabel.makeAnchor { make in
            make.top(equalTo: stateContainerView.topAnchor, constant: 4)
            make.bottom(equalTo: stateContainerView.bottomAnchor, constant: -4)
            make.leading(equalTo: stateContainerView.leadingAnchor, constant: 8)
            make.trailing(equalTo: stateContainerView.trailingAnchor, constant: -8)
        }
        
        blurOverlayView.makeAnchor { make in
            make.edges(to: self)
        }
    }
    
    private func setupActions() {
        self.addTarget(self, action: #selector(handleCardTap), for: .touchUpInside)
        actionButton.addTarget(self, action: #selector(handleButtonTap), for: .touchUpInside)
        checkboxButton.delegate = self
    }
    
    // MARK: - Actions
    
    @objc private func handleCardTap() {
        // Card tap = xem chi tiết → luôn cho bấm, kể cả voucher đã dùng/hết hạn/không đủ điều kiện.
        guard model != nil else { return }
        delegate?.promotionCardViewDidTap(self)
    }
    
    @objc private func handleButtonTap() {
        guard let model = model else { return }
        delegate?.promotionCardViewDidTapButton(self)
    }
    
    private func toggleCheckbox() {
        guard let model = model, !model.isDisabled else { return }
        delegate?.promotionCardViewDidTap(self)
    }
    
    // MARK: - Configuration
    
    public func configure(with model: PromotionCardModel) {
        self.model = model
        
        // Logo từ API: có URL → load; rỗng/lỗi → phủ nền xám (không icon brand mặc định).
        // `placeholder` = model.icon (thường nil) → nil thì hiện nền xám.
        iconImageView.setImage(urlString: model.logoURLString, placeholder: model.icon)

        dateLabel.text = model.dateString
        dateLabel.textColor = model.dateColor ?? Colors.tokenDark60
        dateLabel.isHidden = (model.dateString?.isEmpty ?? true)
        
        // Highlight keyword ở CẢ merchant (titleLabel) và tên ưu đãi (descriptionLabel) — khớp
        // Android MyPromotionAdapter (toHighlightedSpannable trên txtVoucherName + tvContent), phần khớp đỏ + đậm.
        applyHighlight(to: titleLabel, text: model.title, highlightKeyword: model.highlightKeyword,
                       baseColor: Colors.tokenDark60, baseFont: Typography.fontRegular12, matchFont: Typography.fontBold12)

        applyHighlight(to: descriptionLabel, text: model.descriptionText ?? "", highlightKeyword: model.highlightKeyword,
                       baseColor: Colors.tokenDark100, baseFont: Typography.fontMedium16, matchFont: Typography.fontBold16)
        descriptionLabel.isHidden = (model.descriptionText?.isEmpty ?? true)
        
        actionButton.setTitle(model.buttonTitle, for: .normal)
        actionButton.isHidden = (model.buttonTitle?.isEmpty ?? true)
        
        if let title = model.buttonTitle, !title.isEmpty {
            actionButton.setImage(UIImage.sdk("prm_ic_right_arrow_16", in: .module), for: .normal)
            actionButton.semanticContentAttribute = .forceRightToLeft
            actionButton.imageEdgeInsets = UIEdgeInsets(top: 0, left: 2, bottom: 0, right: -2)
            actionButton.contentEdgeInsets = UIEdgeInsets(top: 0, left: 0, bottom: 0, right: 2)
        } else {
            actionButton.setImage(nil, for: .normal)
            actionButton.semanticContentAttribute = .unspecified
            actionButton.imageEdgeInsets = .zero
            actionButton.contentEdgeInsets = .zero
        }
        
        stateLabel.text = model.stateText
        stateContainerView.isHidden = (model.stateText?.isEmpty ?? true)

        // Theme: link (nút action) + badge trạng thái "đã dùng" (null-safe).
        if let link = themeToken?.linkTextColor {
            actionButton.setTitleColor(link, for: .normal)
        }
        stateLabel.textColor = themeToken?.usedBadgeTextColor ?? Colors.tokenDark60
        stateContainerView.backgroundColor = themeToken?.usedBadgeBackgroundColor ?? Colors.tokenDark05
        
        footerStackView.isHidden = dateLabel.isHidden && actionButton.isHidden && stateContainerView.isHidden
        
        checkboxButton.isHidden = !model.showsCheckbox
        
        blurOverlayView.isHidden = !model.isDisabled
        
        if model.isDisabled {
            self.scaleOnHighlight = 1.0
        } else {
            self.scaleOnHighlight = 0.98
        }
        
        updateCheckboxState()
    }
    
    /// Gán text cho label, tô đỏ (#EE0033) + in đậm các đoạn khớp keyword.
    /// Port trực tiếp Android `CharSequence.toHighlightedSpannable` + `findHighlightRanges`:
    /// bỏ dấu, tách keyword theo khoảng trắng (mọi cụm token liên tiếp), match không đè, ưu tiên cụm dài.
    private func applyHighlight(to label: UILabel, text: String, highlightKeyword: String?,
                                baseColor: UIColor, baseFont: UIFont, matchFont: UIFont) {
        // Reset màu/font tường minh — KHÔNG đọc label.textColor/font (khi label đang có attributedText,
        // chúng trả về thuộc tính ký tự ĐẦU, có thể là ĐỎ/đậm từ lần trước → dính cả chuỗi, cell reuse không reset).
        let keyword = highlightKeyword ?? ""
        let ranges = text.isEmpty ? [] : findHighlightRanges(text: text, keyword: keyword)
        guard !ranges.isEmpty else {
            label.attributedText = nil
            label.textColor = baseColor
            label.font = baseFont
            label.text = text
            return
        }

        let attributed = NSMutableAttributedString(
            string: text,
            attributes: [.foregroundColor: baseColor, .font: baseFont]
        )
        let chars = Array(text)
        for range in ranges {
            let lower = text.index(text.startIndex, offsetBy: range.lowerBound)
            let upper = text.index(text.startIndex, offsetBy: min(range.upperBound, chars.count))
            attributed.addAttributes([.foregroundColor: Colors.tokenViettelPayRed100, .font: matchFont],
                                     range: NSRange(lower..<upper, in: text))
        }
        label.attributedText = attributed
    }

    /// Bỏ dấu + lowercase (mirror Android `normalizeForSearch` = lowercase + unAccent). "đ"/"Đ" → "d"
    /// (Foundation `.diacriticInsensitive` không tự fold đ).
    private func normalizeForSearch(_ text: String) -> String {
        text.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: .current)
            .replacingOccurrences(of: "đ", with: "d")
            .replacingOccurrences(of: "Đ", with: "d")
    }

    /// Sinh mọi cụm token liên tiếp từ keyword, dedupe theo dạng chuẩn hoá, sort độ dài giảm dần (mirror Android).
    private func buildSearchPatterns(_ keyword: String) -> [String] {
        let trimmed = keyword.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return [] }
        let tokens = trimmed.split(whereSeparator: { $0.isWhitespace }).map(String.init)
        guard !tokens.isEmpty else { return [] }

        var ordered: [String] = []
        var seenPhrase = Set<String>()
        for start in tokens.indices {
            for end in start..<tokens.count {
                let phrase = tokens[start...end].joined(separator: " ")
                if seenPhrase.insert(phrase).inserted { ordered.append(phrase) }
            }
        }
        var seenNorm = Set<String>()
        var distinct: [String] = []
        for phrase in ordered where seenNorm.insert(normalizeForSearch(phrase)).inserted {
            distinct.append(phrase)
        }
        return distinct.enumerated().sorted { lhs, rhs in
            let lenL = normalizeForSearch(lhs.element).count
            let lenR = normalizeForSearch(rhs.element).count
            return lenL != lenR ? lenL > lenR : lhs.offset < rhs.offset
        }.map { $0.element }
    }

    /// Tìm các range (offset ký tự gốc) cần highlight — mirror Android `findHighlightRanges`.
    /// Chuẩn hoá text char-by-char kèm indexMap (chịu được ký tự chuẩn hoá đổi độ dài); dùng mảng `claimed`
    /// để không highlight đè khi 1 cụm dài đã chiếm chỗ.
    private func findHighlightRanges(text: String, keyword: String) -> [Range<Int>] {
        let trimmed = keyword.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty, !trimmed.isEmpty else { return [] }

        var normalized: [Character] = []
        var indexMap: [Int] = []
        for (originalIndex, char) in text.enumerated() {
            for normChar in normalizeForSearch(String(char)) {
                normalized.append(normChar)
                indexMap.append(originalIndex)
            }
        }
        guard !normalized.isEmpty else { return [] }

        let patterns = buildSearchPatterns(keyword)
        var claimed = [Bool](repeating: false, count: normalized.count)
        var ranges: [Range<Int>] = []

        for pattern in patterns {
            let normPattern = Array(normalizeForSearch(pattern))
            if normPattern.isEmpty { continue }
            var startIndex = 0
            while startIndex <= normalized.count - normPattern.count {
                guard let matchIndex = firstIndex(of: normPattern, in: normalized, from: startIndex) else { break }
                let matchEnd = matchIndex + normPattern.count
                if !(matchIndex..<matchEnd).contains(where: { claimed[$0] }) {
                    for index in matchIndex..<matchEnd { claimed[index] = true }
                    ranges.append(indexMap[matchIndex]..<(indexMap[matchEnd - 1] + 1))
                }
                startIndex = matchIndex + 1
            }
        }
        return ranges.sorted { $0.lowerBound < $1.lowerBound }
    }

    private func firstIndex(of needle: [Character], in haystack: [Character], from: Int) -> Int? {
        guard !needle.isEmpty, haystack.count - from >= needle.count else { return nil }
        var index = from
        while index <= haystack.count - needle.count {
            var offset = 0
            while offset < needle.count && haystack[index + offset] == needle[offset] { offset += 1 }
            if offset == needle.count { return index }
            index += 1
        }
        return nil
    }

    private func updateCheckboxState() {
        guard let model = model else { return }
        // Theme: đổi màu radio theo listItem token (selected/unselected). nil = giữ ảnh gốc SDK.
        let selectedColor = themeToken?.radioSelectedColor
        let unselectedColor = themeToken?.radioUnselectedColor

        // Đã chọn: KHÔNG template ảnh phẳng (đĩa + check trắng) vì tint sẽ nuốt mất dấu check.
        // Thay vào đó VẼ đĩa màu selectedColor + check trắng → luôn còn dấu check.
        if let selected = selectedColor {
            checkboxButton.checkedImage = Self.makeCheckedRadioImage(circleColor: selected)
        } else {
            checkboxButton.checkedImage = model.checkedImage
        }

        // Chưa chọn: chỉ là vòng tròn 1 màu → template tint an toàn.
        if let unselected = unselectedColor {
            checkboxButton.uncheckedImage = model.uncheckedImage?.withRenderingMode(.alwaysTemplate)
            checkboxButton.tintColor = unselected   // áp cho ảnh template (unchecked)
        } else {
            checkboxButton.uncheckedImage = model.uncheckedImage
        }
        checkboxButton.isSelected = model.isChecked
    }

    /// Vẽ radio "đã chọn": đĩa tròn màu `circleColor` + dấu check `checkColor` (mặc định trắng).
    /// Dùng thay ảnh phẳng để tint không làm mất dấu check.
    private static func makeCheckedRadioImage(circleColor: UIColor,
                                              checkColor: UIColor = .white,
                                              size: CGFloat = 24) -> UIImage {
        let rect = CGRect(x: 0, y: 0, width: size, height: size)
        let renderer = UIGraphicsImageRenderer(size: rect.size)
        return renderer.image { _ in
            circleColor.setFill()
            UIBezierPath(ovalIn: rect).fill()

            let check = UIBezierPath()
            check.move(to: CGPoint(x: size * 0.28, y: size * 0.52))
            check.addLine(to: CGPoint(x: size * 0.43, y: size * 0.67))
            check.addLine(to: CGPoint(x: size * 0.72, y: size * 0.34))
            check.lineWidth = size * 0.09
            check.lineCapStyle = .round
            check.lineJoinStyle = .round
            checkColor.setStroke()
            check.stroke()
        }.withRenderingMode(.alwaysOriginal)
    }

    public override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
        let view = super.hitTest(point, with: event)
        if view == actionButton || view?.isDescendant(of: checkboxButton) == true {
            return view
        }
        
        return view
    }
}

// MARK: - RadioButtonStateDelegate

extension PromotionCardView: RadioButtonStateDelegate {
    public func onRadioButtonStateChange(_ sender: UIView) {
        toggleCheckbox()
    }
}
