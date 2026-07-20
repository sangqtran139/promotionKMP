//
//  PRMEndowView.swift
//  PRMPromotionUI
//
//  Created by thachlh on 14/5/26.
//

import UIKit
import PRMDesignKit
import PRMFoundation

// MARK: - State (tương tự Android EndowViewState)
public enum SelectPromtionViewState {
    case loading
    case notApplied(count: Int)            // Có ưu đãi, chưa chọn → "Sử dụng"
    case applied(voucherTitle: String)     // Đã chọn ưu đãi → "Hủy"
    case unavailable(voucherTitle: String) // Đã áp nhưng không còn hợp lệ → "Chọn lại"
    case empty                             // Không có ưu đãi nào → ẩn nút
}

// MARK: - DataSource Protocol (tương tự Android ViewModel.create())
public protocol PRMEndowViewDataSource: AnyObject {
    /// Gọi khi view được gắn vào window lần đầu — host load dữ liệu rồi gọi setState
    func selectPromtionViewDidAttachToWindow(_ view: PRMEndowView)
}

// MARK: - Delegate Protocol
public protocol PRMEndowViewDelegate: AnyObject {
    func selectPromtionViewDidTapSelect(_ view: PRMEndowView)
}

// MARK: - PRMEndowView
final public class PRMEndowView: UIView {

    // MARK: - Public Properties
    public weak var delegate: PRMEndowViewDelegate?
    public weak var dataSource: PRMEndowViewDataSource?
    public private(set) var currentState: SelectPromtionViewState = .loading

    private var hasLoadedInitial = false

    // MARK: - Private UI
    private var titleLabel: UILabel!
    private var couponsStackView: UIStackView!
    private var actionTapableView: PRMTapableView!
    private var actionLabel: UILabel!
    /// Shimmer phủ vùng nút "Sử dụng" trong lúc loading (nil khi không loading).
    private var loadingActionShimmer: PRMShimmerView?

    private let maxVisibleCoupons = 2

    /// Theme token host cấu hình (nil = giữ default SDK).
    private var themeToken: PRMDiscountBadgeThemeToken? { PRMThemeRegistry.shared.discountBadge() }

    // MARK: - Init
    public override init(frame: CGRect) {
        super.init(frame: frame)
        self.config()
    }

    public required init?(coder: NSCoder) {
        super.init(coder: coder)
        self.config()
    }

    // MARK: - Lifecycle — tương tự Android onAttachedToWindow
    public override func didMoveToWindow() {
        super.didMoveToWindow()
        guard window != nil, !hasLoadedInitial else { return }
        hasLoadedInitial = true
        setState(.loading)
        dataSource?.selectPromtionViewDidAttachToWindow(self)
    }

    // MARK: - Config
    private func config() {
        self.backgroundColor = .clear
        self.configTitleLabel()
        self.configCouponsStackView()
        self.configActionView()
        self.setupConstraints()
    }

    private func configTitleLabel() {
        titleLabel = UILabel()
        titleLabel.text = "Ưu đãi"
        titleLabel.font = Typography.fontBold18
        titleLabel.textColor = Colors.tokenDark100
        addSubview(titleLabel)
    }

    private func configCouponsStackView() {
        couponsStackView = UIStackView()
        couponsStackView.axis = .horizontal
        couponsStackView.spacing = 6
        couponsStackView.alignment = .center
    }

    private func configActionView() {
        actionTapableView = PRMTapableView()

        actionLabel = UILabel()
        // Theme: màu chữ nút action ("Sử dụng"/"Hủy").
        actionLabel.textColor = themeToken?.actionTextColor ?? Colors.tokenRed100
        actionLabel.font = Typography.fontMedium16
        actionLabel.translatesAutoresizingMaskIntoConstraints = false
        actionTapableView.addSubview(actionLabel)
        actionLabel.fitSuperview()

        actionTapableView.addTarget(self, action: #selector(didTapAction), for: .touchUpInside)
        actionTapableView.setContentHuggingPriority(.required, for: .horizontal)
        actionTapableView.setContentCompressionResistancePriority(.required, for: .horizontal)
    }

    private func setupConstraints() {
        addSubview(couponsStackView)
        addSubview(actionTapableView)

        titleLabel.makeAnchor { maker in
            maker.top(equalTo: topAnchor)
                .leading(equalTo: leadingAnchor)
                .trailing(equalTo: trailingAnchor)
        }

        couponsStackView.makeAnchor { maker in
            maker.top(equalTo: titleLabel.bottomAnchor, constant: 8)
                .leading(equalTo: leadingAnchor)
                .bottom(equalTo: bottomAnchor)
        }

        actionTapableView.makeAnchor { maker in
            maker.centerY(equalTo: couponsStackView.centerYAnchor)
                .trailing(equalTo: trailingAnchor)
        }

        couponsStackView.trailingAnchor.constraint(
            lessThanOrEqualTo: actionTapableView.leadingAnchor,
            constant: -8
        ).isActive = true

        let huggingWidth = couponsStackView.widthAnchor.constraint(equalToConstant: 0)
        huggingWidth.priority = .fittingSizeLevel
        huggingWidth.isActive = true
    }

    // MARK: - Public API

    /// Cập nhật trạng thái hiển thị — tương tự Android renderState()
    public func setState(_ state: SelectPromtionViewState) {
        currentState = state
        clearCoupons()
        clearActionShimmer()

        switch state {
        case .loading:
            showLoadingState()
        case .notApplied(let count):
            showNotAppliedState(count: count)
        case .applied(let voucherTitle):
            showAppliedState(voucherTitle: voucherTitle)
        case .unavailable(let voucherTitle):
            showUnavailableState(voucherTitle: voucherTitle)
        case .empty:
            showEmptyState()
        }
    }

    // MARK: - Private: render states (tương tự Android show*State())

    private func showLoadingState() {
        // Shimmer cho chip "số lượng ưu đãi".
        let countShimmer = makeShimmerPiece(corner: 4)
        couponsStackView.addArrangedSubview(countShimmer)
        NSLayoutConstraint.activate([
            countShimmer.widthAnchor.constraint(equalToConstant: 120),
            countShimmer.heightAnchor.constraint(equalToConstant: 20)
        ])

        // Shimmer phủ vùng nút "Sử dụng": giữ label để định cỡ (ẩn màu), phủ shimmer lên trên.
        actionLabel.text = "Sử dụng"
        actionLabel.alpha = 0
        let actionShimmer = makeShimmerPiece(corner: 4)
        actionTapableView.addSubview(actionShimmer)
        actionShimmer.fitSuperview()
        actionTapableView.isHidden = false
        actionTapableView.isUserInteractionEnabled = false
        loadingActionShimmer = actionShimmer

        countShimmer.startAnimating()
        actionShimmer.startAnimating()
    }

    private func showNotAppliedState(count: Int) {
        let text = "Bạn có \(count) ưu đãi"
        let chip = createTextChip(text)
        couponsStackView.addArrangedSubview(chip)
        actionLabel.text = "Sử dụng"
        actionTapableView.isHidden = false
    }

    private func showAppliedState(voucherTitle: String) {
        let couponView = createCouponView(text: voucherTitle)
        couponsStackView.addArrangedSubview(couponView)
        actionLabel.text = "Hủy"
        actionTapableView.isHidden = false
    }

    private func showUnavailableState(voucherTitle: String) {
        let couponView = createCouponView(text: voucherTitle, unavailable: true)
        couponsStackView.addArrangedSubview(couponView)
        actionLabel.text = "Chọn lại"
        actionTapableView.isHidden = false
    }

    private func showEmptyState() {
        let chip = createTextChip("Bạn không có ưu đãi nào")
        couponsStackView.addArrangedSubview(chip)
        actionTapableView.isHidden = true
    }

    private func clearCoupons() {
        couponsStackView.arrangedSubviews.forEach {
            couponsStackView.removeArrangedSubview($0)
            $0.removeFromSuperview()
        }
    }

    /// Gỡ shimmer nút + khôi phục nút về trạng thái tương tác bình thường.
    private func clearActionShimmer() {
        loadingActionShimmer?.stopAnimating()
        loadingActionShimmer?.removeFromSuperview()
        loadingActionShimmer = nil
        actionLabel.alpha = 1
        actionTapableView.isUserInteractionEnabled = true
    }

    /// Tạo 1 mảnh shimmer (PRMShimmerView + nền xám tokenDark05) — pattern chung của SDK.
    private func makeShimmerPiece(corner: CGFloat) -> PRMShimmerView {
        let piece = PRMShimmerView()
        piece.backgroundColor = Colors.tokenDark05
        piece.layer.cornerRadius = corner
        piece.clipsToBounds = true
        piece.translatesAutoresizingMaskIntoConstraints = false
        return piece
    }

    // MARK: - Private Helpers

    private func createCouponView(text: String, unavailable: Bool = false) -> CouponValueView {
        let couponView = CouponValueView()
        couponView.title = text
        couponView.titleFont = Typography.fontMedium12
        // Theme: badge ưu đãi — "available" khi đã áp hợp lệ, "unavailable" khi không còn hợp lệ.
        if unavailable {
            couponView.titleColor = themeToken?.unavailableTextColor ?? Colors.tokenDark60
            couponView.couponFillColor = themeToken?.unavailableBackgroundColor ?? Colors.tokenDark05
        } else {
            couponView.titleColor = themeToken?.availableTextColor ?? Colors.tokenRed100
            couponView.couponFillColor = themeToken?.availableBackgroundColor ?? Colors.tokenRed100.withAlphaComponent(0.08)
        }
        couponView.couponCornerRadius = 4
        couponView.cutoutRadius = 4
        couponView.setContentHuggingPriority(.required, for: .horizontal)
        couponView.setContentCompressionResistancePriority(.required, for: .horizontal)
        couponView.makeAnchor { maker in maker.height(equalTo: 28) }
        return couponView
    }

    private func createTextChip(_ text: String) -> UIView {
        let label = UILabel()
        label.text = text
        label.font = Typography.fontMedium14
        // Theme: chip text trạng thái chưa áp/không khả dụng = "unavailable".
        label.textColor = themeToken?.unavailableTextColor ?? Colors.tokenDark60

        // Nếu host cấu hình nền "không khả dụng" → bọc thành pill có padding (null-safe).
        guard let bg = themeToken?.unavailableBackgroundColor else { return label }
        let container = UIView()
        container.backgroundColor = bg
        container.layer.cornerRadius = 4
        container.clipsToBounds = true
        container.addSubview(label)
        label.makeAnchor { maker in
            maker.top(equalTo: container.topAnchor, constant: 4)
                .bottom(equalTo: container.bottomAnchor, constant: -4)
                .leading(equalTo: container.leadingAnchor, constant: 8)
                .trailing(equalTo: container.trailingAnchor, constant: -8)
        }
        return container
    }

    // MARK: - Action
    @objc private func didTapAction() {
        print("[PRMEndowView] didTapAction fired. delegate=\(String(describing: delegate)) state=\(currentState)")
        delegate?.selectPromtionViewDidTapSelect(self)
    }
}
