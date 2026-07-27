//
//  PRMMarqueeLabel.swift
//  PRMDesignKit
//
//  Created by Claude on 27/7/26.
//

import UIKit

/// Label 1 dòng tự chạy chữ (marquee) khi text rộng hơn khung.
///
/// Chạy vòng LIÊN TỤC theo một chiều (kiểu bảng LED): render 2 bản text nối đuôi nhau rồi
/// dịch trái đúng 1 chu kỳ `textWidth + gap` — tới cuối chu kỳ bản thứ 2 nằm đúng vị trí bản
/// thứ nhất nên vòng lặp không có điểm nhảy.
///
/// Text ngắn hơn khung → đứng yên, không animation (không tốn CPU khi list dài).
/// Bật "Giảm chuyển động" (Reduce Motion) → không chạy, cắt ba chấm như UILabel thường.
public final class PRMMarqueeLabel: UIView {

    // MARK: - Config

    /// Tốc độ chạy chữ (pt/giây).
    public var scrollSpeed: CGFloat = 30 {
        didSet { rebuild(force: true) }
    }

    /// Khoảng hở giữa hai bản text trong một vòng lặp.
    public var loopGap: CGFloat = 24 {
        didSet { rebuild(force: true) }
    }

    // MARK: - UILabel-compatible API

    public var text: String? {
        get { primaryLabel.text }
        set {
            primaryLabel.text = newValue
            secondaryLabel.text = newValue
            invalidateIntrinsicContentSize()
            rebuild(force: true)
        }
    }

    public var textColor: UIColor? {
        get { primaryLabel.textColor }
        set {
            primaryLabel.textColor = newValue
            secondaryLabel.textColor = newValue
        }
    }

    public var font: UIFont? {
        get { primaryLabel.font }
        set {
            primaryLabel.font = newValue
            secondaryLabel.font = newValue
            invalidateIntrinsicContentSize()
            rebuild(force: true)
        }
    }

    // MARK: - UI

    private let contentView = UIView()

    private let primaryLabel: UILabel = {
        let label = UILabel()
        label.numberOfLines = 1
        return label
    }()

    /// Bản sao chạy nối sau `primaryLabel` để vòng lặp liền mạch. Ẩn khi text không tràn.
    private let secondaryLabel: UILabel = {
        let label = UILabel()
        label.numberOfLines = 1
        label.isHidden = true
        return label
    }()

    /// Khoá của lần dựng animation gần nhất — tránh restart mỗi vòng `layoutSubviews` (gây giật).
    private var lastLayoutKey: String?

    private static let animationKey = "prm.marquee.scroll"

    // MARK: - Init

    public override init(frame: CGRect) {
        super.init(frame: frame)
        setupView()
    }

    public required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupView()
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    private func setupView() {
        clipsToBounds = true
        backgroundColor = .clear
        isUserInteractionEnabled = false

        addSubview(contentView)
        contentView.addSubview(primaryLabel)
        contentView.addSubview(secondaryLabel)

        // App về foreground → CoreAnimation đã xoá animation, phải dựng lại.
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(handleAppDidBecomeActive),
                                               name: UIApplication.didBecomeActiveNotification,
                                               object: nil)
    }

    // MARK: - Layout

    public override var intrinsicContentSize: CGSize {
        primaryLabel.intrinsicContentSize
    }

    public override func layoutSubviews() {
        super.layoutSubviews()
        // Khung/text không đổi → GIỮ NGUYÊN animation đang chạy (dựng lại mỗi vòng layout sẽ giật hình).
        rebuild(force: false)

        // Tự chữa: đáng lẽ đang phải chạy mà animation không còn (CoreAnimation gỡ khi layer rời
        // render tree, cell bị reuse, app từng vào background…) → dựng lại. Rẻ, và tránh phải đoán
        // đúng mọi thời điểm UIKit gọi layout.
        if shouldAnimate && contentView.layer.animation(forKey: Self.animationKey) == nil {
            rebuild(force: true)
        }
    }

    /// Rời khỏi window (cell bị reuse / màn hình đóng) → dừng hẳn animation.
    public override func didMoveToWindow() {
        super.didMoveToWindow()
        if window == nil {
            stopAnimating()
            lastLayoutKey = nil
        } else {
            rebuild(force: true)
        }
    }

    private var textWidth: CGFloat {
        primaryLabel.intrinsicContentSize.width.rounded(.up)
    }

    /// Text tràn khung → mới chạy chữ.
    private var isOverflowing: Bool {
        bounds.width > 0 && textWidth > bounds.width + 0.5
    }

    private func layoutLabels() {
        let height = bounds.height
        let width = textWidth

        if isOverflowing {
            primaryLabel.lineBreakMode = .byClipping
            secondaryLabel.lineBreakMode = .byClipping
            secondaryLabel.isHidden = false
            primaryLabel.frame = CGRect(x: 0, y: 0, width: width, height: height)
            secondaryLabel.frame = CGRect(x: width + loopGap, y: 0, width: width, height: height)
            contentView.frame = CGRect(x: 0, y: 0, width: width * 2 + loopGap, height: height)
        } else {
            // Không tràn: đứng yên, canh trái như UILabel thường.
            primaryLabel.lineBreakMode = .byTruncatingTail
            secondaryLabel.isHidden = true
            primaryLabel.frame = bounds
            contentView.frame = bounds
        }
    }

    // MARK: - Animation

    @objc private func handleAppDidBecomeActive() {
        rebuild(force: true)
    }

    /// Dừng animation → dựng lại frame 2 bản text → chạy lại (nếu cần).
    /// `force = false`: bỏ qua khi khung + text y hệt lần trước.
    private func rebuild(force: Bool) {
        let key = "\(text ?? "")|\(bounds.width)|\(textWidth)|\(scrollSpeed)|\(loopGap)"
        guard force || key != lastLayoutKey else { return }
        lastLayoutKey = key

        stopAnimating()
        layoutLabels()

        guard shouldAnimate else { return }

        // Một chu kỳ = dịch trái hết bản text thứ nhất + khoảng hở → bản thứ 2 về đúng chỗ bản 1.
        let cycle = textWidth + loopGap

        // Dùng CABasicAnimation chứ KHÔNG dùng UIView.animate: table view layout cell trong ngữ cảnh
        // `UIView.areAnimationsEnabled == false` (reloadData, lúc scroll) — khi đó block animation chạy
        // ngay lập tức, gán thẳng giá trị cuối và KHÔNG có animation nào được tạo (chữ đứng im).
        // CABasicAnimation không phụ thuộc cờ đó, và không đụng `transform` của model layer nên
        // `layoutLabels()` set frame sau đó vẫn an toàn.
        let animation = CABasicAnimation(keyPath: "transform.translation.x")
        animation.fromValue = 0
        animation.toValue = -cycle
        animation.duration = TimeInterval(cycle / scrollSpeed)
        animation.repeatCount = .greatestFiniteMagnitude
        animation.timingFunction = CAMediaTimingFunction(name: .linear)
        animation.isRemovedOnCompletion = false
        contentView.layer.add(animation, forKey: Self.animationKey)
    }

    /// Có đủ điều kiện chạy chữ không (đang trên màn hình, text tràn khung, không bật Giảm chuyển động).
    private var shouldAnimate: Bool {
        window != nil && isOverflowing && scrollSpeed > 0 && !UIAccessibility.isReduceMotionEnabled
    }

    private func stopAnimating() {
        contentView.layer.removeAnimation(forKey: Self.animationKey)
    }
}
