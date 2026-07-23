//
//  PromotionSearchNoResultView.swift
//  PRMPromotionUI
//
//  Created by thachlh on 13/5/26.
//


import PRMDesignKit
import UIKit
import PRMFoundation

final public class PromotionSearchNoResultView: UIView {
    public var thumbnailImage: UIImage? {
        didSet {
            self.thumbnailImageView.image = self.thumbnailImage
        }
    }
    
    public var title: String = "Không tìm thấy kết quả phù hợp" {
        didSet {
            self.titleLabel.text = self.title
        }
    }
    
    public var descriptionString = "Khám phá thêm các đề xuất phù hợp với bạn nhé." {
        didSet {
            self.descriptionLabel.text = self.descriptionString
        }
    }
    
    private var thumbnailImageView: UIImageView!
    private var titleLabel: UILabel!
    private var descriptionLabel: UILabel!
    
    //MARK: - Init
    override init(frame: CGRect) {
        super.init(frame: frame)
        self.config()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        self.config()
    }
    
    //MARK: - Config
    private func config() {
        self.configThumbnailImageView()
        self.configTitleLabel()
        self.configDescriptionLabel()
        self.setupLayouts()
    }
    
    private func configThumbnailImageView() {
        self.thumbnailImageView = UIImageView(image: self.thumbnailImage)
        self.thumbnailImageView.contentMode = .scaleAspectFit
        self.addSubview(self.thumbnailImageView)
    }
    
    private func configTitleLabel() {
        self.titleLabel = UILabel()
        self.titleLabel.text = self.title
        self.titleLabel.font = Typography.fontBold18
        self.titleLabel.textColor = Colors.tokenDark100
        self.titleLabel.numberOfLines = 0
        self.titleLabel.textAlignment = .center
        self.addSubview(self.titleLabel)
    }
    
    private func configDescriptionLabel() {
        self.descriptionLabel = UILabel()
        self.descriptionLabel.text = self.descriptionString
        self.descriptionLabel.font = Typography.fontRegular16
        self.descriptionLabel.textColor = Colors.tokenDark60
        self.descriptionLabel.numberOfLines = 0
        self.descriptionLabel.textAlignment = .center
        self.addSubview(self.descriptionLabel)
    }
    
    private func setupLayouts() {
        self.thumbnailImageView.makeAnchor { make in
            make.top(equalTo: self.topAnchor)
                .centerX(equalTo: self.centerXAnchor)
                .size(CGSize(width: 160, height: 160))
        }
        
        self.titleLabel.makeAnchor { make in
            make.top(equalTo: self.thumbnailImageView.bottomAnchor, constant: 12)
                .leading(equalTo: self.leadingAnchor, constant: 16)
                .trailing(equalTo: self.trailingAnchor, constant: -16)
        }
        
        self.descriptionLabel.makeAnchor { make in
            make.top(equalTo: self.titleLabel.bottomAnchor, constant: 12)
                .bottom(equalTo: self.bottomAnchor)
                .leading(equalTo: self.leadingAnchor, constant: 16)
                .trailing(equalTo: self.trailingAnchor, constant: -16)
        }
    }
}
