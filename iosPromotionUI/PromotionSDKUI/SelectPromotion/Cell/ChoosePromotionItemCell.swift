//
//  ChoosePromotionItemCell.swift
//  PromotionSDK
//

import UIKit
@_implementationOnly import PRMPromotionUI
@_implementationOnly import PRMDesignKit
@_implementationOnly import PRMFoundation
@_implementationOnly import PRMKotlinBridge

protocol SelectPromotionItemCellDelegate: AnyObject {
    func selectPromotionItemCellDidTap(_ cell: ChoosePromotionItemCell, id: String)
    func selectPromotionItemCellDidTapButton(_ cell: ChoosePromotionItemCell, id: String)
}

final class ChoosePromotionItemCell: UITableViewCell {
    @IBOutlet private weak var promotionCardView: PromotionCardView!
    @IBOutlet private weak var warningView: UIView!
    
    weak var delegate: SelectPromotionItemCellDelegate?
    private var viewModel: MyPromotionCellViewModel?
    
    //MARK: - Init
    override func awakeFromNib() {
        super.awakeFromNib()
        selectionStyle = .none
        
        promotionCardView.delegate = self
        
        warningView.layer.cornerRadius = 8
        warningView.layer.maskedCorners = [.layerMinXMaxYCorner, .layerMaxXMaxYCorner]
    }
    
    override func prepareForReuse() {
        super.prepareForReuse()
        self.viewModel = nil
    }
    
    //MARK: - BindData
    func bindData(_ viewModel: MyPromotionCellViewModel) {
        self.viewModel = viewModel
        var dateString: String? = nil
        var dateColor: UIColor? = nil
        
        if let date = viewModel.date {
            if PromotionDate.isExpiringSoon(date, thresholdDays: 3) {
                let interval = date.timeIntervalSince(Date())
                let days = max(1, Int(ceil(interval / 86400)))
                dateString = "HSD: Còn \(days) ngày"
                dateColor = Colors.warningOrangeColor
            } else {
                dateString = "HSD: \(PromotionDate.display(date))"
            }
        }
        
        let cardModel = PromotionCardModel(
            icon: UIImage.sdk("prm_ic_vtm"),
            dateString: dateString,
            dateColor: dateColor,
            title: viewModel.title,
            descriptionText: viewModel.description,
            buttonTitle: viewModel.buttonTitle,
            stateText: viewModel.stateText,
            showsCheckbox: viewModel.showsCheckbox,
            isChecked: viewModel.isChecked,
            isDisabled: viewModel.isDisabled,
            checkedImage: viewModel.checkedImage,
            uncheckedImage: viewModel.uncheckedImage
        )
        promotionCardView.backgroundColor = .clear
        promotionCardView.configure(with: cardModel)
        
        if viewModel.isEligible {
            warningView.isHidden = true
            // Cập nhật lại bo góc dưới cho card nếu cần (VD: tuỳ code PromotionCardView, tạm thời để mặc định)
        } else {
            warningView.isHidden = false
        }
    }
}

//MARK: - Extension
extension ChoosePromotionItemCell: PromotionCardViewDelegate {
    func promotionCardViewDidTap(_ view: PRMPromotionUI.PromotionCardView) {
        self.delegate?.selectPromotionItemCellDidTap(self, id: self.viewModel?.id ?? "")
    }
    
    func promotionCardViewDidTapButton(_ view: PRMPromotionUI.PromotionCardView) {
        self.delegate?.selectPromotionItemCellDidTapButton(self, id: self.viewModel?.id ?? "")
    }
    
    func promotionCardView(_ view: PRMPromotionUI.PromotionCardView, didToggleCheckbox isChecked: Bool) {
        // Handle checkbox toggle if needed
    }
}
