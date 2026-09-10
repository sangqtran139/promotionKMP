//
//  PRMBaseViewController.swift
//  PromotionSDK
//

import UIKit
@_implementationOnly import PRMDesignKit

class PRMBaseViewController<VM>: UIViewController {

    let viewModel: VM

    init(viewModel: VM, nibName: String? = nil, bundle: Bundle? = nil) {
        self.viewModel = viewModel
        let resolvedNibName = nibName ?? String(describing: type(of: self))
            .components(separatedBy: "<").first ?? String(describing: type(of: self))
        let resolvedBundle = bundle ?? Bundle(for: type(of: self))
        super.init(nibName: resolvedNibName, bundle: resolvedBundle)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("Use init(viewModel:) instead")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        _ = PRMBundleSetup.once
        // SDK CHỈ hỗ trợ light mode: ép màn SDK về light bất kể hệ thống đang dark — màu SDK không có
        // biến thể dark nên dark mode làm lệch UI. Ép trên CHÍNH VC (cascade xuống toàn subtree + VC
        // con + toast/dialog gắn vào `view`); KHÔNG đụng `navigationController` vì đó có thể là nav
        // của host (SDK push lên nav host) → sẽ rò light sang màn host.
        overrideUserInterfaceStyle = .light
        self.navigationController?.navigationBar.isHidden = true
        setupUI()
        setupAccessibility()
        bindViewModel()
    }

    func setupUI() {}
    func bindViewModel() {}

    /// Gán `accessibilityIdentifier`/`accessibilityLabel` cho các control của màn.
    ///
    /// Tách khỏi `setupUI()` có chủ đích: đây là **hợp đồng với XCUITest**, không phải trình bày.
    /// Để lẫn vào `setupUI()` thì lần refactor giao diện nào cũng có nguy cơ mất định danh mà không
    /// ai nhận ra — cho tới khi test đỏ hàng loạt và trông như test hỏng.
    ///
    /// Gọi **sau** `setupUI()` vì view dựng bằng code chỉ tồn tại từ sau đó. Danh sách định danh ở
    /// `PRMAccessibilityID`.
    func setupAccessibility() {
        enableDynamicType(in: view)
    }

    /// Bật `adjustsFontForContentSizeCategory` cho mọi label/nút/ô nhập trong cây view.
    ///
    /// Cỡ chữ đã scale sẵn ở `Typography` (computed + `UIFontMetrics`), nên mở màn là đúng cỡ rồi.
    /// Cờ này lo nốt vế còn lại: user đổi cỡ chữ **trong lúc màn đang mở** thì chữ đổi theo, không
    /// phải thoát ra vào lại.
    ///
    /// Duyệt cây một lần ở đây thay vì đặt ở từng call-site: 14 file đang gán font, và một chỗ quên
    /// thì lỗi hiện ra dưới dạng "một label không chịu đổi cỡ" — thứ gần như không ai truy ra.
    private func enableDynamicType(in root: UIView) {
        for subview in root.subviews {
            switch subview {
            case let label as UILabel:      label.adjustsFontForContentSizeCategory = true
            case let field as UITextField:  field.adjustsFontForContentSizeCategory = true
            case let textView as UITextView: textView.adjustsFontForContentSizeCategory = true
            case let button as UIButton:    button.titleLabel?.adjustsFontForContentSizeCategory = true
            default:                        break
            }
            enableDynamicType(in: subview)
        }
    }

    /// Báo lỗi cho user bằng **popup** (`PRMConfirmationDialog`, 1 nút "Đóng").
    ///
    /// Thay cho `PromotionToast` đã bỏ: SDK không dùng toast nữa (toast dễ bị hệ/OEM chặn và trôi
    /// qua quá nhanh). Chỗ nào đã có empty-view/list cũ nói thay thì **không hiện gì**; chỉ dùng hàm
    /// này khi im lặng sẽ khiến user không hiểu chuyện gì — ví dụ vừa chủ động bấm và đang chờ.
    ///
    /// Đối ứng `PRMBaseFragment.showErrorDialog` bên Android.
    func showErrorDialog(_ message: String) {
        PRMConfirmationDialog.showError(message, in: view)
    }
}
