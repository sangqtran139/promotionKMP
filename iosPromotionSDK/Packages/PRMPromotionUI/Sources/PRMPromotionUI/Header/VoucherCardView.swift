//
//  VoucherCardView.swift
//  PRMPromotionUI
//
//  Created by thachlh on 13/5/26.
//

import PRMDesignKit
import UIKit
import PRMFoundation

public final class VoucherCardView: PRMBaseView {
    // MARK: - Public Properties
    public var logoImage: UIImage? {
        didSet { self.logoImageView.image = logoImage }
    }

    /// Load logo từ URL (URLSession + cache). `placeholder` hiển thị khi URL rỗng/đang tải.
    public func setLogo(urlString: String?, placeholder: UIImage? = nil) {
        self.logoImageView.setImage(urlString: urlString, placeholder: placeholder)
    }
    
    public var brandName: String? {
        didSet { self.brandNameLabel.text = brandName }
    }
    
    public var title: String? {
        didSet { self.titleLabel.text = title }
    }
    
    /// API không trả HSD → **ẩn hẳn** dòng ngày thay vì để label rỗng chiếm chỗ.
    /// Đối ứng `binding.tvExpired.isVisible = displayDate.isNotBlank()` bên Android.
    public var expiryText: String? {
        didSet {
            self.expiryLabel.text = expiryText
            self.expiryLabel.isHidden = (expiryText ?? "").isEmpty
        }
    }

    /// Màu dòng HSD — cam khi voucher sắp hết hạn, nil = màu mặc định của label.
    /// Đối ứng `binding.tvExpired.setTextColor(...)` bên Android.
    public var expiryColor: UIColor? {
        didSet { self.expiryLabel.textColor = expiryColor ?? Colors.tokenDark60 }
    }

    // MARK: - Private Properties
    private var logoImageView: UIImageView!
    private var brandNameLabel: UILabel!
    private var titleLabel: UILabel!
    private var expiryLabel: UILabel!
    private var headerStack: UIStackView!

    // MARK: - Init
    public override init(frame: CGRect) {
        super.init(frame: frame)
        self.config()
    }

    public required init?(coder: NSCoder) {
        super.init(coder: coder)
        self.config()
    }

    // MARK: - Config
    private func config() {
        self.configCard()
        self.configLogo()
        self.configLabels()
        self.configStacks()
        self.setupConstraints()
    }

    private func configCard() {
        self.backgroundColor = Colors.tokenWhite
        self.cornerRadius = 16
        self.borderWidth = 0.5
        self.borderColor = Colors.tokenDark20
        
        self.layer.shadowColor = Colors.tokenBlack.cgColor
        self.layer.shadowOpacity = 0.06
        self.layer.shadowOffset = CGSize(width: 0, height: 2)
        self.layer.shadowRadius = 10
        self.layer.masksToBounds = false
    }

    private func configLogo() {
        self.logoImageView = UIImageView()
        self.logoImageView.contentMode = .scaleAspectFill
        self.logoImageView.clipsToBounds = true
        self.logoImageView.layer.cornerRadius = 22
        self.logoImageView.backgroundColor = Colors.tokenDark10
    }

    private func configLabels() {
        self.brandNameLabel = UILabel()
        self.brandNameLabel.font = Typography.fontRegular14
        self.brandNameLabel.textColor = Colors.tokenDark60
        // Tên brand (`VoucherCardViewModel.title`): tối đa 2 dòng, dài quá → ba chấm.
        // Trùng `txtVoucherName` bên Android (`maxLines=2` + `ellipsize=end`).
        self.brandNameLabel.numberOfLines = 2
        self.brandNameLabel.lineBreakMode = .byTruncatingTail

        self.titleLabel = UILabel()
        self.titleLabel.font = Typography.fontBold16
        self.titleLabel.textColor = Colors.tokenDark100
        // Tên ưu đãi (`VoucherCardViewModel.description`): tối đa 5 dòng, dài quá → ba chấm.
        // Trùng `tvContent` bên Android (`maxLines=5` + `ellipsize=end`).
        self.titleLabel.numberOfLines = 5
        self.titleLabel.lineBreakMode = .byTruncatingTail

        self.expiryLabel = UILabel()
        self.expiryLabel.font = Typography.fontRegular14
        self.expiryLabel.textColor = Colors.tokenDark60
        self.expiryLabel.numberOfLines = 1
        self.expiryLabel.lineBreakMode = .byTruncatingTail
    }

    private func configStacks() {
        self.headerStack = UIStackView(arrangedSubviews: [logoImageView, brandNameLabel])
        self.headerStack.axis = .horizontal
        self.headerStack.alignment = .center
        self.headerStack.spacing = 12
        
        self.addSubview(self.headerStack)
        self.addSubview(self.titleLabel)
        self.addSubview(self.expiryLabel)
    }

    private func setupConstraints() {
        self.logoImageView.makeAnchor { maker in
            maker.width(equalTo: 44)
                .height(equalTo: 44)
        }
        
        self.headerStack.makeAnchor { maker in
            maker.top(equalTo: self.topAnchor, constant: 20)
                .leading(equalTo: self.leadingAnchor, constant: 16)
                .trailing(equalTo: self.trailingAnchor, constant: -16)
        }
        
        // Title: cách header 8dp. Tối đa 5 dòng + ba chấm (cấu hình ở configLabels).
        self.titleLabel.makeAnchor { maker in
            maker.top(equalTo: self.headerStack.bottomAnchor, constant: 8)
                .leading(equalTo: self.leadingAnchor, constant: 16)
                .trailing(equalTo: self.trailingAnchor, constant: -16)
        }

        // Expiry LUÔN bám sát đáy title thật + 8dp (title 1 hay 2 dòng đều bám) → card co theo nội dung.
        self.expiryLabel.makeAnchor { maker in
            maker.top(equalTo: self.titleLabel.bottomAnchor, constant: 8)
                .leading(equalTo: self.leadingAnchor, constant: 16)
                .trailing(equalTo: self.trailingAnchor, constant: -16)
                .bottom(equalTo: self.bottomAnchor, constant: -20)
        }
    }
}
