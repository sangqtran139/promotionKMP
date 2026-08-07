
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
    /// Keyword highlight title (chỉ dùng ở màn Search). nil = không highlight.
    let highlightKeyword: String?
    /// Số ngày còn lại khi voucher sắp hết hạn — **do store (promotionLogic) tính** theo
    /// `expireWarningDate` của server; `nil` = không sắp hết hạn. Cell chỉ format "Còn X ngày",
    /// KHÔNG tự suy ngưỡng.
    let expiringInDays: Int?
    /// API **không trả** HSD (nil/rỗng) = voucher không có hạn dùng → hiện "HSD: Không hết hạn".
    /// Khác với `date == nil` do parse hỏng (chuỗi có giá trị nhưng sai format) — ca đó giấu dòng
    /// ngày như cũ. Đối ứng Android `expirationDate.isBlank()`.
    let neverExpires: Bool

    init(id: String,
         title: String,
         description: String,
         imageURL: String? = nil,
         date: Date? = nil,
         neverExpires: Bool = false,
         buttonTitle: String? = nil,
         showsCheckbox: Bool = false,
         isChecked: Bool = false,
         stateText: String? = nil,
         isDisabled: Bool = false,
         checkedImage: UIImage? = nil,
         uncheckedImage: UIImage? = nil,
         isEligible: Bool = true,
         highlightKeyword: String? = nil,
         expiringInDays: Int? = nil) {
        self.id = id
        self.title = title
        self.description = description
        self.imageURL = imageURL
        self.date = date
        self.neverExpires = neverExpires
        self.buttonTitle = buttonTitle
        self.showsCheckbox = showsCheckbox
        self.isChecked = isChecked
        self.stateText = stateText
        self.isDisabled = isDisabled
        self.checkedImage = checkedImage
        self.uncheckedImage = uncheckedImage
        self.isEligible = isEligible
        self.highlightKeyword = highlightKeyword
        self.expiringInDays = expiringInDays
    }

    /// Voucher khách đã sở hữu (Search API). Không có `estimatedDiscount` / `ineligibleReason` —
    /// hai trường đó chỉ tồn tại ở luồng checkout (`EligibleOffer`).
    ///
    /// - Parameter isEnabled: voucher còn dùng được — **quyết định lấy từ store**
    ///   (`MyPromotionVoucher.isEnabled` ở promotionLogic), cell KHÔNG tự suy lại (đối ứng Android
    ///   `MyVoucherListItem.isEnabled`). Nhãn trạng thái/nút vẫn format ở native.
    init(voucher: VoucherItem,
         isEnabled: Bool,
         buttonTitle: String? = nil,
         showsCheckbox: Bool = false,
         isChecked: Bool = false,
         stateText: String? = nil,
         checkedImage: UIImage? = nil,
         uncheckedImage: UIImage? = nil,
         highlightKeyword: String? = nil,
         expiringInDays: Int? = nil) {

        let state = voucher.displayState()
        var derivedStateText: String? = stateText
        var derivedButtonTitle: String? = buttonTitle
        let derivedIsDisabled = !isEnabled

        // Luôn suy trạng thái theo status: voucher không dùng được phải hiện nhãn trạng thái
        // + disable, kể cả khi caller truyền buttonTitle ("Chi tiết").
        switch state {
        // ĐÃ DÙNG / HẾT HẠN: LUÔN dùng chuỗi của SDK. `displayStatusLabel` là
        // `metadata.disabledReason` thô của server — mã enum ("REDEEMED", "EXPIRED"), không phải
        // chuỗi hiển thị; ưu tiên nó thì tag lòi chữ tiếng Anh ra UI. Khớp Android
        // (`MyPromotionAdapter`), chỉ nhánh không đủ điều kiện mới giữ nhãn server.
        case .used:
            derivedStateText = stateText ?? PromotionUIStrings.used
            derivedButtonTitle = nil
        case .expired:
            derivedStateText = stateText ?? PromotionUIStrings.expired
            derivedButtonTitle = nil
        case .ineligible:
            derivedStateText = stateText ?? voucher.displayStatusLabel ?? PromotionUIStrings.ineligible
            derivedButtonTitle = nil
        case .usable:
            // Nhãn nút lấy từ server — vd voucher AVAILABLE_TO_CLAIM hiện "Nhận" thay vì "Sử dụng".
            if derivedButtonTitle == nil && derivedStateText == nil {
                derivedButtonTitle = voucher.displayStatusLabel ?? PromotionUIStrings.use
            }
        default:
            derivedStateText = stateText ?? voucher.displayStatusLabel
            derivedButtonTitle = nil
        }

        self.init(
            id: voucher.voucherId,
            title: voucher.merchantName ?? "",
            // `title` rỗng → rơi về `description` (đối ứng Android `title.ifBlank { description }`).
            description: (voucher.title?.isEmpty == false ? voucher.title : voucher.description_) ?? "",
            imageURL: voucher.logo,
            date: PRMPromotionDate.parse(voucher.expirationDate),
            neverExpires: PRMPromotionDate.isMissing(voucher.expirationDate),
            buttonTitle: derivedButtonTitle,
            showsCheckbox: showsCheckbox,
            isChecked: isChecked,
            stateText: derivedStateText,
            isDisabled: derivedIsDisabled,
            checkedImage: checkedImage,
            uncheckedImage: uncheckedImage,
            isEligible: isEnabled,
            highlightKeyword: highlightKeyword,
            expiringInDays: expiringInDays
        )
    }

    /// Ưu đãi đủ/không đủ điều kiện cho đơn hàng (Find Eligible Campaigns).
    ///
    /// - Parameter isEnabled: quyết định từ store (`ChooseOffer.isUsable`) — xem init phía trên.
    init(offer: EligibleOffer,
         isEnabled: Bool,
         buttonTitle: String? = nil,
         showsCheckbox: Bool = false,
         isChecked: Bool = false,
         stateText: String? = nil,
         checkedImage: UIImage? = nil,
         uncheckedImage: UIImage? = nil,
         highlightKeyword: String? = nil,
         expiringInDays: Int? = nil) {

        var derivedStateText: String? = stateText
        var derivedButtonTitle: String? = buttonTitle

        if !isEnabled {
            // Ưu tiên gợi ý từ `unmatchedRules`; lõi không dựng sẵn câu tiếng Việt.
            derivedStateText = stateText ?? offer.unmatchedRules.first ?? PromotionUIStrings.ineligible
            derivedButtonTitle = nil
        } else if derivedButtonTitle == nil && derivedStateText == nil {
            derivedButtonTitle = PromotionUIStrings.use
        }

        // Hai dòng chữ, đối ứng `EligibleOffer.toMyVoucherListItem()` bên Android:
        // `title` = dòng nhỏ phía trên (merchant), `description` = dòng to phía dưới (số tiền giảm).
        var derivedDescription = ""
        if let discount = offer.estimatedDiscount, let formatted = Self.formatDiscount(discount) {
            derivedDescription = formatted
        }

        self.init(
            id: offer.id,
            // Ưu tiên tên đối tác/merchant (partnerName, v1.6), fallback tên ưu đãi.
            title: offer.partnerName ?? offer.campaignName ?? "",
            description: derivedDescription,
            imageURL: offer.logoUrl,
            date: PRMPromotionDate.parse(offer.expireDate),
            neverExpires: PRMPromotionDate.isMissing(offer.expireDate),
            buttonTitle: derivedButtonTitle,
            showsCheckbox: showsCheckbox,
            isChecked: isChecked,
            stateText: derivedStateText,
            isDisabled: !isEnabled,
            checkedImage: checkedImage,
            uncheckedImage: uncheckedImage,
            isEligible: isEnabled,
            highlightKeyword: highlightKeyword,
            expiringInDays: expiringInDays
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
        return PromotionUIStrings.discount(formatted)
    }
}

protocol MyPromotionCellDelegate: AnyObject {
    func myPromotionCellDidTap(_ cell: MyPromotionCell, id: String)
    /// User bấm nút "Dùng" (parity Android onUseClick) — VC forward sang ViewModel, VM lọc dịch vụ.
    func myPromotionCellDidTapUse(_ cell: MyPromotionCell, voucherId: String)
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
        // "Sắp hết hạn" do store (promotionLogic) quyết định theo `expireWarningDate` của server —
        // dùng chung Android. Cell chỉ format; KHÔNG tự suy ngưỡng.
        //
        // Màu: sắp hết hạn → cam `tokenCarrotOrange100`, khớp Android (`MyPromotionAdapter`);
        // các trường hợp còn lại để nil → PromotionCardView dùng màu mặc định `tokenDark60`.
        if let days = viewModel.expiringInDays {
            dateString = PromotionUIStrings.remainingDays(days)
            dateColor = Colors.tokenCarrotOrange100
        } else if let date = viewModel.date {
            dateString = PromotionUIStrings.expiryDate(PRMPromotionDate.display(date))
        } else if viewModel.neverExpires {
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
    }
}

extension MyPromotionCell: PromotionCardViewDelegate {
    func promotionCardViewDidTap(_ view: PRMPromotionUI.PromotionCardView) {
        self.delegate?.myPromotionCellDidTap(self, id: self.viewModel?.id ?? "")
    }

    func promotionCardViewDidTapButton(_ view: PRMPromotionUI.PromotionCardView) {
        // Chỉ báo "user bấm Dùng trên voucher nào". Việc lọc dịch vụ khả dụng là nghiệp vụ → do
        // ViewModel làm (đối ứng Android `OpenServiceSelector` → `servicesForApplicableProducts`),
        // cell không tự gọi `ServiceSelectorBuilder` nữa.
        self.delegate?.myPromotionCellDidTapUse(self, voucherId: self.viewModel?.id ?? "")
    }

    func promotionCardView(_ view: PRMPromotionUI.PromotionCardView, didToggleCheckbox isChecked: Bool) {}
}
