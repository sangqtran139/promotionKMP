//
//  PRMBaseViewController.swift
//  PromotionSDK
//

import UIKit

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
        bindViewModel()
    }

    func setupUI() {}
    func bindViewModel() {}
}
