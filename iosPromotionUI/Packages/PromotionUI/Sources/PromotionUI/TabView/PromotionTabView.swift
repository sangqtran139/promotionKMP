import UIKit
import CoreUI

public class PromotionTabView: TapableView {
    
    // MARK: - Properties
    
    public var onTapped: (() -> Void)?
    
    public var isFocusedState: Bool = false {
        didSet {
            updateState()
        }
    }
    
    // Configurable colors for states
    public var focusedBackgroundColor: UIColor = Colors.tokenDark80 {
        didSet { updateState() }
    }
    
    public var unfocusedBackgroundColor: UIColor = Colors.tokenDark05 {
        didSet { updateState() }
    }
    
    public var focusedTextColor: UIColor = Colors.tokenWhite {
        didSet { updateState() }
    }
    
    public var unfocusedTextColor: UIColor = Colors.tokenDark60 {
        didSet { updateState() }
    }
    
    public var titleText: String? {
        didSet {
            titleLabel.text = titleText
        }
    }
    
    // MARK: - UI Components
    
    public let titleLabel: UILabel = {
        let label = UILabel()
        label.translatesAutoresizingMaskIntoConstraints = false
        label.textAlignment = .center
        label.font = .systemFont(ofSize: 14, weight: .medium)
        return label
    }()
    
    // MARK: - Init
    
    override public init(frame: CGRect) {
        super.init(frame: frame)
        setupView()
    }
    
    required public init?(coder: NSCoder) {
        super.init(coder: coder)
        setupView()
    }
    
    // MARK: - Setup
    
    /// Theme token host cấu hình (nil = giữ default SDK).
    private var themeToken: VDSTabChipThemeToken? { VDSThemeRegistry.shared.tabChip() }

    private func setupView() {
        self.layer.cornerRadius = themeToken?.cornerRadius ?? 8
        self.clipsToBounds = true

        addSubview(titleLabel)
        
        NSLayoutConstraint.activate([
            titleLabel.centerXAnchor.constraint(equalTo: self.centerXAnchor),
            titleLabel.centerYAnchor.constraint(equalTo: self.centerYAnchor),
            titleLabel.topAnchor.constraint(greaterThanOrEqualTo: self.topAnchor, constant: 9),
            titleLabel.bottomAnchor.constraint(lessThanOrEqualTo: self.bottomAnchor, constant: -9),
            titleLabel.leadingAnchor.constraint(equalTo: self.leadingAnchor, constant: 8),
            titleLabel.trailingAnchor.constraint(equalTo: self.trailingAnchor, constant: -8)
        ])
        
        self.addTarget(self, action: #selector(handleTap), for: .touchUpInside)
        
        updateState()
    }
    
    private func updateState() {
        // Theme: token thắng default cho từng state (null-safe).
        let activeBg = themeToken?.activeBackgroundColor ?? focusedBackgroundColor
        let inactiveBg = themeToken?.inactiveBackgroundColor ?? unfocusedBackgroundColor
        let activeText = themeToken?.activeTextColor ?? focusedTextColor
        let inactiveText = themeToken?.inactiveTextColor ?? unfocusedTextColor
        self.backgroundColor = isFocusedState ? activeBg : inactiveBg
        self.titleLabel.textColor = isFocusedState ? activeText : inactiveText
    }
    
    // MARK: - Actions
    
    @objc private func handleTap() {
        onTapped?()
    }
    
    // MARK: - Public Configuration
    
    public func configure(title: String, isFocused: Bool = false) {
        self.titleText = title
        self.isFocusedState = isFocused
    }
}
