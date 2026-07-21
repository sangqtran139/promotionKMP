
//
//  MyPromotionCell.swift
//  PromotionSDK
//
//  Created by thachlh on 6/5/26.
//

import UIKit
@_implementationOnly import PRMPromotionUI
@_implementationOnly import PRMDesignKit
@_implementationOnly import PRMFoundation
@_implementationOnly import PRMKotlinBridge

struct MyPromotionCellViewModel {
    let id: String
    let title: String
    let description: String
    let imageURL: String?
    let date: Date?
    let buttonTitle: String?
    let showsCheckbox: Bool
    let isChecked: Bool
    let stateText: String?
    let isDisabled: Bool
    let checkedImage: UIImage?
    let uncheckedImage: UIImage?
    let isEligible: Bool
    /// Dịch vụ/sản phẩm voucher áp dụng được — dùng mở bottom sheet "Chọn dịch vụ" khi bấm "Dùng".
    let applicableProducts: [ApplicableProduct]
    /// Keyword highlight title (chỉ dùng ở màn Search). nil = không highlight.
    let highlightKeyword: String?

    init(id: String,
         title: String,
         description: String,
         imageURL: String? = nil,
         date: Date? = nil,
         buttonTitle: String? = nil,
         showsCheckbox: Bool = false,
         isChecked: Bool = false,
         stateText: String? = nil,
         isDisabled: Bool = false,
         checkedImage: UIImage? = nil,
         uncheckedImage: UIImage? = nil,
         isEligible: Bool = true,
         applicableProducts: [ApplicableProduct] = [],
         highlightKeyword: String? = nil) {
        self.id = id
        self.title = title
        self.description = description
        self.imageURL = imageURL
        self.date = date
        self.buttonTitle = buttonTitle
        self.showsCheckbox = showsCheckbox
        self.isChecked = isChecked
        self.stateText = stateText
        self.isDisabled = isDisabled
        self.checkedImage = checkedImage
        self.uncheckedImage = uncheckedImage
        self.isEligible = isEligible
        self.applicableProducts = applicableProducts
        self.highlightKeyword = highlightKeyword
    }

    /// Voucher khách đã sở hữu (Search API). Không có `estimatedDiscount` / `ineligibleReason` —
    /// hai trường đó chỉ tồn tại ở luồng checkout (`EligibleOffer`).
    init(voucher: VoucherItem,
         buttonTitle: String? = nil,
         showsCheckbox: Bool = false,
         isChecked: Bool = false,
         stateText: String? = nil,
         checkedImage: UIImage? = nil,
         uncheckedImage: UIImage? = nil,
         highlightKeyword: String? = nil) {

        let state = voucher.displayState()
        var derivedStateText: String? = stateText
        var derivedButtonTitle: String? = buttonTitle
        var derivedIsDisabled = !state.isUsable

        // Luôn suy trạng thái theo status: voucher không dùng được phải hiện nhãn trạng thái
        // + disable, kể cả khi caller truyền buttonTitle ("Chi tiết").
        switch state {
        case .used:
            derivedStateText = stateText ?? voucher.displayStatusLabel ?? "Đã sử dụng"
            derivedButtonTitle = nil
        case .expired:
            derivedStateText = stateText ?? voucher.displayStatusLabel ?? "Hết hạn"
            derivedButtonTitle = nil
        case .ineligible:
            derivedStateText = stateText ?? voucher.displayStatusLabel ?? "Không đủ điều kiện"
            derivedButtonTitle = nil
        case .usable:
            // Nhãn nút lấy từ server — vd voucher AVAILABLE_TO_CLAIM hiện "Nhận" thay vì "Sử dụng".
            if derivedButtonTitle == nil && derivedStateText == nil {
                derivedButtonTitle = voucher.displayStatusLabel ?? "Sử dụng"
            }
        default:
            derivedStateText = stateText ?? voucher.displayStatusLabel
            derivedButtonTitle = nil
        }

        self.init(
            id: voucher.voucherId,
            title: voucher.merchantName ?? "",
            description: voucher.title ?? "",
            imageURL: voucher.logo,
            date: PRMPromotionDate.parse(voucher.expirationDate),
            buttonTitle: derivedButtonTitle,
            showsCheckbox: showsCheckbox,
            isChecked: isChecked,
            stateText: derivedStateText,
            isDisabled: derivedIsDisabled,
            checkedImage: checkedImage,
            uncheckedImage: uncheckedImage,
            isEligible: state.isUsable,
            applicableProducts: voucher.applicableProducts,
            highlightKeyword: highlightKeyword
        )
    }

    /// Ưu đãi đủ/không đủ điều kiện cho đơn hàng (Find Eligible Campaigns).
    init(offer: EligibleOffer,
         buttonTitle: String? = nil,
         showsCheckbox: Bool = false,
         isChecked: Bool = false,
         stateText: String? = nil,
         checkedImage: UIImage? = nil,
         uncheckedImage: UIImage? = nil,
         highlightKeyword: String? = nil) {

        var derivedStateText: String? = stateText
        var derivedButtonTitle: String? = buttonTitle

        if !offer.usable {
            // Ưu tiên gợi ý từ `unmatchedRules`; lõi không dựng sẵn câu tiếng Việt.
            derivedStateText = stateText ?? offer.unmatchedRules.first ?? "Không đủ điều kiện"
            derivedButtonTitle = nil
        } else if derivedButtonTitle == nil && derivedStateText == nil {
            derivedButtonTitle = "Sử dụng"
        }

        // Số tiền giảm dự kiến hiện ở label nhỏ phía trên tên ưu đãi.
        var derivedTitle = ""
        if let discount = offer.estimatedDiscount, let formatted = Self.formatDiscount(discount) {
            derivedTitle = formatted
        }

        self.init(
            id: offer.id,
            title: derivedTitle,
            // Ưu tiên tên đối tác/merchant (partnerName, v1.6), fallback tên ưu đãi.
            description: offer.partnerName ?? offer.campaignName ?? "",
            // logoUrl (v1.6) — trước đây findEligible không trả nên card offer để trống logo.
            imageURL: offer.logoUrl,
            date: PRMPromotionDate.parse(offer.expireDate),
            buttonTitle: derivedButtonTitle,
            showsCheckbox: showsCheckbox,
            isChecked: isChecked,
            stateText: derivedStateText,
            isDisabled: !offer.usable,
            checkedImage: checkedImage,
            uncheckedImage: uncheckedImage,
            isEligible: offer.usable,
            applicableProducts: [],
            highlightKeyword: highlightKeyword
        )
    }

    /// "100000" → "Giảm 100.000đ". nil nếu không parse được.
    private static func formatDiscount(_ raw: String) -> String? {
        let digits = raw.filter { $0.isNumber }
        guard let value = Int(digits), value > 0 else { return nil }
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.groupingSeparator = "."
        let formatted = formatter.string(from: NSNumber(value: value)) ?? "\(value)"
        return "Giảm \(formatted)đ"
    }
}

protocol MyPromotionCellDelegate: AnyObject {
    func myPromotionCellDidTap(_ cell: MyPromotionCell, id: String)
    /// User bấm nút "Dùng" → mở bottom sheet chọn dịch vụ (parity Android onUseClick).
    func myPromotionCellDidTapUse(_ cell: MyPromotionCell, voucherId: String, services: [ServiceSelectorItem])
}

final class MyPromotionCell: UITableViewCell {
    @IBOutlet private weak var promotionCardView: PromotionCardView!

    weak var delegate: MyPromotionCellDelegate?
    private var viewModel: MyPromotionCellViewModel?

    override func awakeFromNib() {
        super.awakeFromNib()
        selectionStyle = .none
        promotionCardView.delegate = self
    }

    override func prepareForReuse() {
        super.prepareForReuse()
        self.viewModel = nil
    }

    func bindData(_ viewModel: MyPromotionCellViewModel) {
        self.viewModel = viewModel
        var dateString: String?
        var dateColor: UIColor?
        if let date = viewModel.date {
            if PRMPromotionDate.isExpiringSoon(date, thresholdDays: 3) {
                let interval = date.timeIntervalSince(Date())
                let days = max(1, Int(ceil(interval / 86400)))
                dateString = "HSD: Còn \(days) ngày"
                dateColor = Colors.warningOrangeColor
            } else {
                dateString = "HSD: \(PRMPromotionDate.display(date))"
            }
        }

        let cardModel = PromotionCardModel(
            icon: UIImage.sdk("prm_ic_vtm"),
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
    }
}

extension MyPromotionCell: PromotionCardViewDelegate {
    func promotionCardViewDidTap(_ view: PRMPromotionUI.PromotionCardView) {
        self.delegate?.myPromotionCellDidTap(self, id: self.viewModel?.id ?? "")
    }

    func promotionCardViewDidTapButton(_ view: PRMPromotionUI.PromotionCardView) {
        let services = ServiceSelectorBuilder.items(forApplicableProducts: self.viewModel?.applicableProducts ?? [])
        self.delegate?.myPromotionCellDidTapUse(self, voucherId: self.viewModel?.id ?? "", services: services)
    }

    func promotionCardView(_ view: PRMPromotionUI.PromotionCardView, didToggleCheckbox isChecked: Bool) {}
}
