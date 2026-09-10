//
//  PRMAccessibility.swift
//  PromotionSDK
//
//  Định danh accessibility của toàn tầng UI, gom vào MỘT nơi.
//
//  Vì sao cần một bảng chứ không rải chuỗi tại chỗ: `accessibilityIdentifier` là thứ XCUITest bám
//  vào để tìm phần tử. Rải literal ở từng ViewController thì test và UI giữ hai bản chuỗi, và lần
//  đầu ai đó sửa một bên là test đỏ ở chỗ không liên quan gì tới thay đổi đó.
//
//  Định danh KHÁC nhãn:
//  - `accessibilityIdentifier` — cho máy (XCUITest). Không dịch, không đọc lên, không đổi theo
//    ngôn ngữ. Đó là lý do nó nằm ở đây chứ không nằm ở `PromotionUIStrings`.
//  - `accessibilityLabel` — cho người (VoiceOver). Là chuỗi hiển thị nên nằm ở
//    `PromotionUIStrings.A11y` cùng mọi chuỗi tiếng Việt khác.
//
//  ── Dynamic Type: đã bật, CÓ CHẶN TRẦN ───────────────────────────────────────────────────────
//  `Typography` (PRMDesignKit) nay là computed property đi qua `UIFontMetrics`, và
//  `PRMBaseViewController.setupAccessibility()` bật `adjustsFontForContentSizeCategory` cho cả cây
//  view — mở màn đúng cỡ, đổi cỡ chữ giữa chừng cũng đổi theo.
//
//  Trần đặt ở `Typography.dynamicTypeMaxScale = 1.3`, KHÔNG phải để cho có: 13 ràng buộc chiều cao
//  trong XIB đang cố định (13…173pt), nên chữ phóng tự do tới AX5 (~3,8×) sẽ bị CẮT — tệ hơn hiện
//  trạng. Nới trần là **hai việc, không phải một**: gỡ chiều cao cố định theo từng màn trước, rồi
//  mới nâng số, rồi duyệt lại bằng mắt ở cỡ chữ lớn nhất.
//
//  `DynamicTypeTests` khoá lại: font phải scale được, và không được vượt trần ở AX5.
//

// `UIKit` chứ không phải `Foundation`: `IndexPath.section`/`.row` là extension của UIKit.
import UIKit

/// Tiền tố `prm.` để phần tử của SDK phân biệt được với phần tử của app host trong cây
/// accessibility — cùng lý do với `prm_` của resource Android: SDK sống trong tiến trình của host.
enum PRMAccessibilityID {

    enum MyPromotion {
        static let table = "prm.myPromotion.table"
        static let tabExpiring = "prm.myPromotion.tab.expiring"
        static let tabAll = "prm.myPromotion.tab.all"
        static let header = "prm.myPromotion.header"
        static let backButton = "prm.myPromotion.button.back"
        static let searchButton = "prm.myPromotion.button.search"
    }

    enum ChoosePromotion {
        static let searchField = "prm.choosePromotion.searchField"
        static let table = "prm.choosePromotion.table"
        static let selectedSummary = "prm.choosePromotion.selectedSummary"
        static let selectedCount = "prm.choosePromotion.selectedCount"
    }

    enum PromotionDetail {
        static let banner = "prm.promotionDetail.banner"
        static let voucherCard = "prm.promotionDetail.voucherCard"
        static let segmentControl = "prm.promotionDetail.segmentControl"
        static let applyButton = "prm.promotionDetail.button.apply"
    }

    enum Search {
        static let searchField = "prm.search.searchField"
        static let table = "prm.search.table"
        static let resultLabel = "prm.search.resultLabel"
    }

    /// Cell dùng chung: định danh gốc + chỉ số hàng, vì XCUITest cần trỏ được vào **một** hàng cụ thể
    /// chứ không phải "một trong các cell".
    enum Cell {
        static let myPromotion = "prm.cell.myPromotion"
        static let choosePromotion = "prm.cell.choosePromotion"

        /// `prm.cell.myPromotion.3` — hàng thứ 3 (0-based) của section đầu.
        static func indexed(_ base: String, _ indexPath: IndexPath) -> String {
            "\(base).\(indexPath.section).\(indexPath.row)"
        }
    }
}
