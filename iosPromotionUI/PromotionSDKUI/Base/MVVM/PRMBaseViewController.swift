//
//  PRMBaseViewController.swift
//  PromotionSDK
//

import UIKit
@_implementationOnly import RxSwift
@_implementationOnly import RxCocoa

class PRMBaseViewController<VM>: UIViewController {

    let viewModel: VM
    let disposeBag = DisposeBag()

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
        self.navigationController?.navigationBar.isHidden = true
        setupUI()
        bindViewModel()
    }

    func setupUI() {}
    func bindViewModel() {}
}
