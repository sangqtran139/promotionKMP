//
//  PromotionCardModel.swift
//  CoreUI
//
//  Created by thachlh on 20/4/26.
//


import UIKit

public struct PromotionCardModel {
    public var icon: UIImage?
    public var dateString: String?
    public var dateColor: UIColor?
    public var title: String
    /// Keyword để highlight (tô đỏ) đoạn khớp trong title — dùng ở màn Search. nil/rỗng = không highlight.
    public var highlightKeyword: String?
    public var descriptionText: String?
    public var buttonTitle: String?
    public var stateText: String?
    public var showsCheckbox: Bool
    public var isChecked: Bool
    public var isDisabled: Bool
    public var checkedImage: UIImage?
    public var uncheckedImage: UIImage?
    
    public init(
        icon: UIImage? = nil,
        dateString: String? = nil,
        dateColor: UIColor? = nil,
        title: String,
        highlightKeyword: String? = nil,
        descriptionText: String? = nil,
        buttonTitle: String? = nil,
        stateText: String? = nil,
        showsCheckbox: Bool = false,
        isChecked: Bool = false,
        isDisabled: Bool = false,
        checkedImage: UIImage? = nil,
        uncheckedImage: UIImage? = nil
    ) {
        self.icon = icon
        self.dateString = dateString
        self.dateColor = dateColor
        self.title = title
        self.highlightKeyword = highlightKeyword
        self.descriptionText = descriptionText
        self.buttonTitle = buttonTitle
        self.stateText = stateText
        self.showsCheckbox = showsCheckbox
        self.isChecked = isChecked
        self.isDisabled = isDisabled
        self.checkedImage = checkedImage
        self.uncheckedImage = uncheckedImage
    }
}
