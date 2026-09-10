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

    /// Dải "Chưa đủ điều kiện áp dụng" — port `ctlNotEnoughApplyVoucher` của Android
    /// (`prm_item_choose_promotion.xml`): dải cao 30, **luồn 8 điểm xuống DƯỚI card** nên chỉ lòi ra 22.
    ///
    /// Phần luồn vào là thứ làm nó khít: card là hình coupon có bo góc 12 và một khuyết tròn ở cạnh
    /// đáy, nếu dải chỉ nằm kề bên dưới thì mấy chỗ khuyết đó hở ra nền list. Cho dải nằm SAU card và
    /// ăn lên 8 điểm thì màu vàng lấp đúng những chỗ ấy — Android làm y hệt bằng `_minus22sdp` +
    /// `prm_bg_circle_gold` (ảnh tròn vàng đè lên khuyết đáy).
    private enum Metrics {
        /// Phần dải bị card che.
        static let warningOverlap: CGFloat = 8
        /// Bo góc dưới của dải = bo góc card (`CouponBackgroundView.cornerRadius`) để hai cạnh bên thẳng hàng.
        static let warningCornerRadius: CGFloat = 12
    }

    @IBOutlet private weak var contentStackView: UIStackView!
    @IBOutlet private weak var promotionCardView: PromotionCardView!
    @IBOutlet private weak var warningView: UIView!
    
    weak var delegate: SelectPromotionItemCellDelegate?
    private var viewModel: MyPromotionCellViewModel?
    
    //MARK: - Init
    override func awakeFromNib() {
        super.awakeFromNib()
        selectionStyle = .none
        
        promotionCardView.delegate = self
        
        warningView.backgroundColor = Colors.tokenGold20
        warningView.layer.cornerRadius = Metrics.warningCornerRadius
        warningView.layer.maskedCorners = [.layerMinXMaxYCorner, .layerMaxXMaxYCorner]

        // Khoảng cách ÂM = dải bị card đè lên 8 điểm. Stack ẩn arranged subview thì bỏ luôn khoảng
        // cách này, nên ca "đủ điều kiện" (warningView.isHidden) chiều cao cell vẫn đúng bằng card.
        contentStackView.spacing = -Metrics.warningOverlap
        // …và dải phải nằm SAU card. Đổi thứ tự `subviews` không đụng `arrangedSubviews` nên layout
        // giữ nguyên, chỉ đổi thứ tự vẽ.
        contentStackView.sendSubviewToBack(warningView)
    }
    
    override func prepareForReuse() {
        super.prepareForReuse()
        self.viewModel = nil
    }
    
    //MARK: - BindData
    func bindData(_ viewModel: MyPromotionCellViewModel) {
        self.viewModel = viewModel
        var dateString: String?
        var dateColor: UIColor?
        // "Sắp hết hạn" do store (promotionLogic) quyết định theo `expireWarningDate` của server —
        // dùng chung Android (`ChooseOffer.expiringInDays`). Cell chỉ format; KHÔNG tự suy ngưỡng
        // Sắp hết hạn → tô cam, khớp Android.
        if let days = viewModel.expiringInDays {
            dateString = PromotionUIStrings.remainingDays(days)
            dateColor = Colors.tokenCarrotOrange100
        } else if let date = viewModel.date {
            dateString = PromotionUIStrings.expiryDate(PRMPromotionDate.display(date))
        } else if viewModel.neverExpires {
            // API không trả HSD → "HSD: Không hết hạn" (đối ứng Android). Parse hỏng → vẫn giấu dòng.
            dateString = PromotionUIStrings.expiryNever
        }

        let cardModel = PromotionCardModel(
            logoURLString: viewModel.imageURL,
            dateString: dateString,
            dateColor: dateColor,
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
        
        // Chỉ ca "đơn hàng chưa thoả điều kiện" mới có dải. Voucher hết hạn cũng `isDisabled` nhưng
        // KHÔNG hiện dải — xem `ChooseOffer.showsIneligibleWarning()` bên promotionLogic. Ẩn dải thì
        // stack bỏ luôn khoảng cách âm, cell co đúng bằng card. Đối ứng Android
        // `ctlNotEnoughApplyVoucher.isVisible`.
        warningView.isHidden = !viewModel.showsIneligibleWarning
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
