//
//  PRMRadioButtonGroup.swift
//  CoreUIKit
//
//  Created by Le Tuan Hung on 9/28/20.
//  Copyright © 2020 ViettelPay App Team. All rights reserved.
//

import UIKit
import PRMFoundation

@objc
public protocol RadioGroupDelegate: NSObjectProtocol {
    @objc
    optional func radioButtonGroup(_ radioButtonGroup: PRMRadioButtonGroup, didSelectRadioButtonAt index: Int)
}

@IBDesignable
public class PRMRadioButtonGroup: UIStackView {
    
    private var radioButtons: [PRMRadioButton] = []
    
    private let radioSpacing = Sizing.tokenSizing16
    
    public weak var delegate: RadioGroupDelegate?
    
    public var titles: [String] = [] {
        didSet {
            updateTitle()
        }
    }
    
    public var isLeft: Bool = true {
        didSet {
            updateViews()
        }
    }
    
    public var currentSelect: Int? {
        didSet {
            if let index = currentSelect, index < radioButtons.count {
                radioButtons[index].isSelected = true
            }
        }
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        commonInit()
        spacing = radioSpacing
    }
    
    required init(coder: NSCoder) {
        super.init(coder: coder)
        commonInit()
        spacing = radioSpacing
    }

}

extension PRMRadioButtonGroup {
    private func commonInit() {
        translatesAutoresizingMaskIntoConstraints = false
        setupGroup()
        axis = .vertical
    }
    
    private func setupGroup() {
        for (index, title) in titles.enumerated() {
            let radioButton = PRMRadioButton()
            radioButton.radioTitle = title
            radioButton.delegate = self
            radioButton.tag = index
            radioButtons.append(radioButton)
            addArrangedSubview(radioButton)
        }
    }
    
    private func updateTitle() {
        if radioButtons.count == titles.count {
            for (index, title) in titles.enumerated() {
                radioButtons[index].radioTitle = title
                radioButtons[index].tag = index
            }
        } else {
            radioButtons.forEach {
                $0.removeFromSuperview()
            }
            radioButtons.removeAll()
            setupGroup()
        }
    }
    
    private func updateViews() {
        for radioButon in radioButtons {
            radioButon.isLeft = isLeft
        }
    }
}

extension PRMRadioButtonGroup: RadioButtonStateDelegate {
    public func onRadioButtonStateChange(_ sender: UIView) {
        guard let currentRadioButton = sender as? PRMRadioButton else {
            return
        }
        radioButtons.forEach {
            $0.isSelected = false
        }
        currentRadioButton.isSelected = !currentRadioButton.isSelected
        guard let delegate = delegate else {
            return
        }
        delegate.radioButtonGroup?(self, didSelectRadioButtonAt: sender.tag)
    }
}
