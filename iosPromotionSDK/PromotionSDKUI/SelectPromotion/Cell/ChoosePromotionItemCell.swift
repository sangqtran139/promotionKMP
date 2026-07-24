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
        var dateString: String?
        // "Sắp hết hạn" do store (promotionLogic) quyết định theo `expireWarningDate` của server —
        // dùng chung Android (`ChooseOffer.expiringInDays`). Cell chỉ format; KHÔNG tự suy ngưỡng
        // (trước đây hardcode 3 ngày nên lệch Android). Màu giữ mặc định, khớp Android.
        if let days = viewModel.expiringInDays {
            dateString = PromotionUIStrings.remainingDays(days)
        } else if let date = viewModel.date {
            dateString = PromotionUIStrings.expiryDate(PRMPromotionDate.display(date))
        }

        let cardModel = PromotionCardModel(
            logoURLString: viewModel.imageURL,
            dateString: dateString,
            dateColor: nil,
            title: viewModel.title,
            highlightKeyword: viewModel.highlightKeyword,
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
        // Ở màn này tap card = CHỌN ưu đãi (không phải xem chi tiết) → ưu đãi không đủ điều kiện thì
        // bỏ qua, y như checkbox. Đối ứng Android `ChoosePromotionMainAdapter` (`if (!canUse) return`).
        guard let viewModel = self.viewModel, !viewModel.isDisabled else { return }
        self.delegate?.selectPromotionItemCellDidTap(self, id: viewModel.id)
    }
    
    func promotionCardViewDidTapButton(_ view: PRMPromotionUI.PromotionCardView) {
        self.delegate?.selectPromotionItemCellDidTapButton(self, id: self.viewModel?.id ?? "")
    }
    
    func promotionCardView(_ view: PRMPromotionUI.PromotionCardView, didToggleCheckbox isChecked: Bool) {
        // Handle checkbox toggle if needed
    }
}
