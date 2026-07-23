import UIKit
import PRMDesignKit
import PRMFoundation

public protocol PromotionHeaderViewDelegate: AnyObject {
    func didTapBackButton()
    func didTapSearchButton()
}

public class PromotionHeaderView: UIView {
    
    // MARK: - Properties
    
    public weak var delegate: PromotionHeaderViewDelegate?
    
    // MARK: - Configurable Properties
    
    public var titleText: String? {
        didSet {
            titleLabel.text = titleText
        }
    }
    
    public var titleFont: UIFont? {
        didSet {
            if let font = titleFont {
                titleLabel.font = font
            }
        }
    }
    
    public var promotionIcon: UIImage? = UIImage.sdk("prm_ic_default_coupone", in: .module) {
        didSet {
            iconImageView.image = promotionIcon
            iconImageView.isHidden = (promotionIcon == nil)
        }
    }
    
    public var backIcon: UIImage? = UIImage.sdk("prm_ic_arrow_left", in: .module) {
        didSet {
            if let icon = backIcon {
                backImageView.image = icon
            }
        }
    }
    
    public var searchIcon: UIImage? = UIImage.sdk("prm_ic_search", in: .module) {
        didSet {
            if let icon = searchIcon {
                searchImageView.image = icon
            }
        }
    }
    
    public let backButton: PRMTapableView = {
        let view = PRMTapableView()
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    
    public let backImageView: UIImageView = {
        let imageView = UIImageView()
        imageView.translatesAutoresizingMaskIntoConstraints = false
        imageView.contentMode = .scaleAspectFit
        return imageView
    }()
    
    public let searchButton: PRMTapableView = {
        let view = PRMTapableView()
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    
    public let searchImageView: UIImageView = {
        let imageView = UIImageView()
        imageView.translatesAutoresizingMaskIntoConstraints = false
        imageView.contentMode = .scaleAspectFit
        return imageView
    }()
    
    public let titleLabel: UILabel = {
        let label = UILabel()
        label.translatesAutoresizingMaskIntoConstraints = false
        label.font = .boldSystemFont(ofSize: 20)
        label.textColor = Colors.tokenDark100
        return label
    }()
    
    public let iconImageView: UIImageView = {
        let imageView = UIImageView()
        imageView.translatesAutoresizingMaskIntoConstraints = false
        imageView.contentMode = .scaleAspectFit
        NSLayoutConstraint.activate([
            imageView.widthAnchor.constraint(equalToConstant: 28),
            imageView.heightAnchor.constraint(equalToConstant: 28)
        ])
        return imageView
    }()
    
    public let titleStackView: UIStackView = {
        let stackView = UIStackView()
        stackView.translatesAutoresizingMaskIntoConstraints = false
        stackView.axis = .horizontal
        stackView.spacing = 8
        stackView.alignment = .center
        return stackView
    }()
    
    private let backgroundGradientView: PRMMultiRadialGradientView = {
        let view = PRMMultiRadialGradientView()
        view.backgroundColor_ = UIColor(red:0.98,green:0.94,blue:0.93,alpha:1)
        view.blobs = [
          .init(center:CGPoint(x:0.92,y:0.0), radius:1.00, color: UIColor(red:0.86,green:0.43,blue:0.35,alpha:1), alpha:0.65),
          .init(center:CGPoint(x:1.0,y:1.0), radius:0.80, color: UIColor(red:0.91,green:0.59,blue:0.42,alpha:1), alpha:0.55),
          .init(center:CGPoint(x:0.05,y:1.0), radius:0.65, color: UIColor(red:0.83,green:0.63,blue:0.78,alpha:1), alpha:0.30),
          .init(center:CGPoint(x:0.0,y:0.05), radius:0.60, color: UIColor(red:0.94,green:0.78,blue:0.75,alpha:1), alpha:0.25),
        ]
        
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    
    // MARK: - Init
    
    override public init(frame: CGRect) {
        super.init(frame: frame)
        setupViews()
        setupAllConstraints()
        setupActions()
    }
    
    required public init?(coder: NSCoder) {
        super.init(coder: coder)
        setupViews()
        setupAllConstraints()
        setupActions()
    }
    
    // MARK: - Setup Views
    
    private func setupViews() {
        addSubview(backgroundGradientView)
        addSubview(backButton)
        backButton.addSubview(backImageView)
        addSubview(searchButton)
        searchButton.addSubview(searchImageView)
        
        titleStackView.addArrangedSubview(titleLabel)
        
        self.iconImageView.image = self.promotionIcon
        self.iconImageView.isHidden = (self.promotionIcon == nil)
        titleStackView.addArrangedSubview(iconImageView)
        addSubview(titleStackView)
    }
    
    // MARK: - Constraints
    
    private func setupAllConstraints() {
        setupBackgroundConstraints()
        setupBackButtonConstraints()
        setupSearchButtonConstraints()
        setupTitleStackConstraints()
    }
    
    private func setupBackgroundConstraints() {
        NSLayoutConstraint.activate([
            backgroundGradientView.topAnchor.constraint(equalTo: self.topAnchor),
            backgroundGradientView.leadingAnchor.constraint(equalTo: self.leadingAnchor),
            backgroundGradientView.trailingAnchor.constraint(equalTo: self.trailingAnchor),
            backgroundGradientView.bottomAnchor.constraint(equalTo: self.bottomAnchor),
        ])
    }
    
    private func setupBackButtonConstraints() {
        self.backImageView.image = self.backIcon
        
        NSLayoutConstraint.activate([
            backButton.topAnchor.constraint(equalTo: self.topAnchor, constant: 45),
            backButton.leadingAnchor.constraint(equalTo: self.leadingAnchor, constant: 16),
            backButton.widthAnchor.constraint(equalToConstant: 32),
            backButton.heightAnchor.constraint(equalToConstant: 32),
            
            backImageView.topAnchor.constraint(equalTo: backButton.topAnchor),
            backImageView.bottomAnchor.constraint(equalTo: backButton.bottomAnchor),
            backImageView.leadingAnchor.constraint(equalTo: backButton.leadingAnchor),
            backImageView.trailingAnchor.constraint(equalTo: backButton.trailingAnchor)
        ])
    }
    
    private func setupSearchButtonConstraints() {
        self.searchImageView.image = self.searchIcon
        
        NSLayoutConstraint.activate([
            searchButton.trailingAnchor.constraint(equalTo: self.trailingAnchor, constant: -20),
            searchButton.bottomAnchor.constraint(equalTo: self.bottomAnchor, constant: -40),
            searchButton.widthAnchor.constraint(equalToConstant: 32),
            searchButton.heightAnchor.constraint(equalToConstant: 32),
            
            searchImageView.topAnchor.constraint(equalTo: searchButton.topAnchor),
            searchImageView.bottomAnchor.constraint(equalTo: searchButton.bottomAnchor),
            searchImageView.leadingAnchor.constraint(equalTo: searchButton.leadingAnchor),
            searchImageView.trailingAnchor.constraint(equalTo: searchButton.trailingAnchor)
        ])
    }
    
    private func setupTitleStackConstraints() {
        NSLayoutConstraint.activate([
            titleStackView.topAnchor.constraint(equalTo: backButton.bottomAnchor, constant: 12),
            titleStackView.leadingAnchor.constraint(equalTo: self.leadingAnchor, constant: 20),
            titleStackView.bottomAnchor.constraint(equalTo: self.bottomAnchor, constant: -40),
            
            titleStackView.trailingAnchor.constraint(lessThanOrEqualTo: searchButton.leadingAnchor, constant: -16)
        ])
    }
    
    // MARK: - Actions
    
    private func setupActions() {
        backButton.addTarget(self, action: #selector(backButtonTapped), for: .touchUpInside)
        searchButton.addTarget(self, action: #selector(searchButtonTapped), for: .touchUpInside)
    }
    
    @objc private func backButtonTapped() {
        delegate?.didTapBackButton()
    }
    
    @objc private func searchButtonTapped() {
        delegate?.didTapSearchButton()
    }
    
    // MARK: - Configurations
    
    public func updateBackground(backgroundColor: UIColor? = nil, blobs: [PRMMultiRadialGradientView.RadialBlob]? = nil) {
        if let backgroundColor = backgroundColor {
            backgroundGradientView.backgroundColor_ = backgroundColor
        }
        
        if let blobs = blobs {
            backgroundGradientView.blobs = blobs
        }
    }
    
    public func updateViewStyle(cornerRadius: CGFloat? = nil, borderWidth: CGFloat? = nil, borderColor: UIColor? = nil) {
        if let cornerRadius = cornerRadius {
            self.layer.cornerRadius = cornerRadius
            self.clipsToBounds = true
        }
        
        if let borderWidth = borderWidth {
            self.layer.borderWidth = borderWidth
        }
        
        if let borderColor = borderColor {
            self.layer.borderColor = borderColor.cgColor
        }
    }
}
