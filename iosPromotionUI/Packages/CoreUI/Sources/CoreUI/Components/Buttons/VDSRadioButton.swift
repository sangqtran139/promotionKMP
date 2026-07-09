//
//  VDSRadioButton.swift
//  CoreUI
//
//  Created by thachlh on 21/4/26.
//

import Utility
import UIKit

@objc
public protocol RadioButtonStateDelegate: NSObjectProtocol {
    func onRadioButtonStateChange(_ sender: UIView)
}

@IBDesignable
public class VDSRadioButton: UIView {
    
    private let radioSize = Sizing.tokenSizing24
    private let bundle = Bundle(for: VDSRadioButton.self)

    public var checkedImageName: String = "ic_radio_check" {
        didSet { changeRadioState() }
    }
    
    public var uncheckedImageName: String = "ic_radio_uncheck" {
        didSet { changeRadioState() }
    }
    
    public var checkedImage: UIImage? {
        didSet { changeRadioState() }
    }
    
    public var uncheckedImage: UIImage? {
        didSet { changeRadioState() }
    }
    
    private lazy var radioImageView: UIImageView = {
        let imgView = UIImageView()
        imgView.translatesAutoresizingMaskIntoConstraints = false
        return imgView
    }()

    private var radioLabel: UILabel = {
        let label = UILabel()
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()

    private lazy var button: UIButton = {
        let btn = UIButton()
        btn.translatesAutoresizingMaskIntoConstraints = false
        btn.titleLabel?.text = ""
        btn.addTarget(self, action: #selector(onRadioButtonStateChange(_:)), for: .touchUpInside)
        return btn
    }()
    
    public weak var delegate: RadioButtonStateDelegate?
    
    public var isAnimationSelect = true
    
    public var isSelected: Bool = false {
        didSet {
            changeRadioState()
        }
    }
    
    public var isLeft: Bool = true {
        didSet {
            setupLayout()
        }
    }
    
    public var textFont: UIFont? {
        didSet {
            radioLabel.font = textFont
        }
    }
    
    public var textAlpha: CGFloat? {
        didSet {
            radioLabel.alpha = textAlpha ?? 1.0
        }
    }
    
    @IBInspectable public var radioTitle: String = "Title" {
        didSet {
            updateTitle()
        }
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        commonInit()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        commonInit()
    }
}

extension VDSRadioButton {
    private func commonInit() {
        translatesAutoresizingMaskIntoConstraints = false
        setup()
        setupLayout()
    }
    
    private func setup() {
        addSubview(radioImageView)
        addSubview(radioLabel)
        addSubview(button)
        button.makeAnchor { make in
            make.edges(to: self)
        }
    }
    
    private func setupLayout() {
        radioImageView.removeAllConstraints()
        radioLabel.removeAllConstraints()
        
        // Radio Image
        radioImageView.makeAnchor { make in
            make.centerY(equalTo: centerYAnchor)
                .size(CGSize(width: radioSize, height: radioSize))
        }
        if let customImage = uncheckedImage {
            radioImageView.image = customImage
        } else {
            radioImageView.image = UIImage.sdk(uncheckedImageName, in: bundle)
        }

        // Radio Title
        radioLabel.makeAnchor { make in
            make.top(equalTo: topAnchor, constant: Spacing.tokenSpacing08)
                .bottom(equalTo: bottomAnchor, constant: -Spacing.tokenSpacing08)
                .centerY(equalTo: centerYAnchor)
        }
        radioLabel.font = Typography.fontRegular14
        radioLabel.numberOfLines = 0
        
        configLayouts()
    }
    
    private func configLayouts() {
        if isLeft {
            radioImageView.makeAnchor { make in make.leading(equalTo: leadingAnchor) }
            radioLabel.makeAnchor { make in
                make.leading(equalTo: radioImageView.trailingAnchor, constant: Spacing.tokenSpacing08)
                    .trailing(equalTo: trailingAnchor, constant: -Spacing.tokenSpacing08)
            }
        } else {
            radioImageView.makeAnchor { make in make.trailing(equalTo: trailingAnchor) }
            radioLabel.makeAnchor { make in
                make.leading(equalTo: leadingAnchor, constant: Spacing.tokenSpacing08)
                    .trailing(equalTo: radioImageView.leadingAnchor, constant: -Spacing.tokenSpacing08)
            }
        }
    }
    
    private func changeRadioState() {
        if let customImage = self.isSelected ? checkedImage : uncheckedImage {
            radioImageView.image = customImage
        } else {
            radioImageView.image = UIImage.sdk(self.isSelected ? checkedImageName : uncheckedImageName, in: bundle)
        }
    }
    
    private func updateTitle() {
        let paragraphStyle = NSMutableParagraphStyle()
        paragraphStyle.lineSpacing = Spacing.tokenSpacing08
        let attrString = NSMutableAttributedString(string: radioTitle)
        attrString.addAttribute(.paragraphStyle, value:paragraphStyle, range: NSRange(location: 0, length: attrString.length))
        radioLabel.attributedText = attrString
    }
}

extension VDSRadioButton: RadioButtonStateDelegate {
    @objc
    public func onRadioButtonStateChange(_ sender: UIView) {
        if isSelected {
            return
        }
        delegate?.onRadioButtonStateChange(self)
    }
}
