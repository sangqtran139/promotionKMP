//
//  SearchMyPromotionViewController.swift
//  PromotionSDK
//
//  Created by thachlh on 11/5/26.
//

import UIKit
@_implementationOnly import RxSwift
@_implementationOnly import RxCocoa
@_implementationOnly import PRMDesignKit
@_implementationOnly import PRMPromotionUI
@_implementationOnly import PRMFoundation

final class SearchMyPromotionViewController: PRMBaseViewController<SearchMyPromotionViewModel> {

    // MARK: - UI Components
    @IBOutlet private weak var searchTextField: PRMSearchTextField!
    @IBOutlet private weak var tableView: UITableView!
    @IBOutlet private weak var searchHeaderView: UIView!
    @IBOutlet private weak var resultSearchLabel: UILabel!

    // MARK: - Relays
    private let searchActionRelay = PublishRelay<Void>()
    private let loadMoreRelay = PublishRelay<Void>()
    private let selectPromotionRelay = PublishRelay<String>()

    // Shimmer skeleton khi đang tìm kiếm (khớp Android shimmerProvider).
    private let shimmerOverlay: UIView = {
        let view = UIView()
        view.backgroundColor = Colors.tokenDark02
        view.translatesAutoresizingMaskIntoConstraints = false
        view.isHidden = true
        return view
    }()
    private lazy var shimmerView: PRMShimmerReplicatorView = {
        let view = PRMShimmerReplicatorView(
            itemSize: .fixedHeight(PRMPromotionCardShimmerCell.itemHeight),
            cellProvider: { PRMPromotionCardShimmerCell() }
        )
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()

    // MARK: - Lazy
    private lazy var searchNoResultView: PromotionSearchNoResultView = {
        let view = PromotionSearchNoResultView()
        view.thumbnailImage = UIImage.sdk("prm_ic_search_no_result")
        self.view.addSubview(view)
        view.makeAnchor { make in
            make.centerY(equalTo: self.view.centerYAnchor)
                .centerX(equalTo: self.view.centerXAnchor)
                .leading(equalTo: self.view.leadingAnchor, constant: 16)
        }
        return view
    }()

    // MARK: - Lifecycle
    override func viewDidLoad() {
        super.viewDidLoad()
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardWillShow), name: UIResponder.keyboardWillShowNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardWillHide), name: UIResponder.keyboardWillHideNotification, object: nil)
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        searchTextField.becomeFirstResponder()
    }

    @objc private func keyboardWillShow(notification: NSNotification) {
        if let keyboardFrame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? NSValue {
            tableView.contentInset = UIEdgeInsets(top: 0, left: 0, bottom: keyboardFrame.cgRectValue.height, right: 0)
        }
    }

    @objc private func keyboardWillHide(notification: NSNotification) {
        tableView.contentInset = .zero
    }

    // MARK: - Setup UI
    override func setupUI() {
        super.setupUI()
        self.configTableView()
        self.configShimmer()
        searchTextField.returnKeyType = .search
        searchTextField.delegate = self
    }

    private func configShimmer() {
        self.view.addSubview(shimmerOverlay)
        shimmerOverlay.addSubview(shimmerView)
        NSLayoutConstraint.activate([
            // Bám sát ngay dưới thanh search (không ghim theo tableView vì table bị nhãn "Kết quả tìm kiếm"
            // + khoảng cách đẩy xuống dù nhãn đang ẩn → shimmer tụt thấp).
            shimmerOverlay.topAnchor.constraint(equalTo: searchHeaderView.bottomAnchor, constant: 16),
            shimmerOverlay.leadingAnchor.constraint(equalTo: tableView.leadingAnchor),
            shimmerOverlay.trailingAnchor.constraint(equalTo: tableView.trailingAnchor),
            shimmerOverlay.bottomAnchor.constraint(equalTo: tableView.bottomAnchor),

            shimmerView.topAnchor.constraint(equalTo: shimmerOverlay.topAnchor, constant: 8),
            shimmerView.leadingAnchor.constraint(equalTo: shimmerOverlay.leadingAnchor),
            shimmerView.trailingAnchor.constraint(equalTo: shimmerOverlay.trailingAnchor),
            shimmerView.bottomAnchor.constraint(equalTo: shimmerOverlay.bottomAnchor)
        ])
    }

    private func configTableView() {
        self.tableView.registerCell(MyPromotionCell.self)
        self.tableView.rowHeight = UITableView.automaticDimension
        self.tableView.separatorStyle = .none
        self.tableView.delegate = self
    }

    // MARK: - Bind ViewModel
    override func bindViewModel() {
        super.bindViewModel()

        let input = SearchMyPromotionViewModel.Input(
            searchText: searchTextField.rx.text.orEmpty.asObservable(),
            searchAction: searchActionRelay.asObservable(),
            loadMoreTrigger: loadMoreRelay.asObservable(),
            selectPromotionByIDRelay: selectPromotionRelay
        )
        let output = viewModel.transform(input: input)

        output.promotions
            .drive(tableView.rx.items(cellIdentifier: "MyPromotionCell", cellType: MyPromotionCell.self)) { [weak self] _, viewModel, cell in
                cell.delegate = self
                cell.bindData(viewModel)
            }
            .disposed(by: disposeBag)

        // Text "Kết quả tìm kiếm" chỉ hiện khi có kết quả (chưa search / không có KQ thì ẩn).
        output.promotions
            .map { $0.isEmpty }
            .drive(onNext: { [weak self] isEmpty in
                self?.resultSearchLabel.isHidden = isEmpty
            })
            .disposed(by: disposeBag)

        output.isEmpty
            .drive(onNext: { [weak self] isEmpty in
                self?.searchNoResultView.isHidden = !isEmpty
                self?.tableView.isHidden = isEmpty
            })
            .disposed(by: disposeBag)

        output.isLoading
            .drive(onNext: { [weak self] loading in
                guard let self = self else { return }
                self.shimmerOverlay.isHidden = !loading
                if loading {
                    self.tableView.isHidden = true
                    self.searchNoResultView.isHidden = true
                    self.shimmerView.startAnimating()
                } else {
                    self.shimmerView.stopAnimating()
                }
            })
            .disposed(by: disposeBag)
    }

    // MARK: - Action
    @IBAction private func didTapBackButton(_ sender: Any) {
        viewModel.routeToParent()
    }
}

// MARK: - UITableViewDelegate (load more)
extension SearchMyPromotionViewController: UITableViewDelegate {
    func scrollViewDidScroll(_ scrollView: UIScrollView) {
        let offsetY = scrollView.contentOffset.y
        let contentHeight = scrollView.contentSize.height
        let frameHeight = scrollView.frame.size.height
        if contentHeight > 0 && offsetY > contentHeight - frameHeight - 100 {
            loadMoreRelay.accept(())
        }
    }
}

// MARK: - UITextFieldDelegate (search action on return key)
extension SearchMyPromotionViewController: UITextFieldDelegate {
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        searchActionRelay.accept(())
        textField.resignFirstResponder()
        return true
    }
}

// MARK: - MyPromotionCellDelegate
extension SearchMyPromotionViewController: MyPromotionCellDelegate {
    func myPromotionCellDidTap(_ cell: MyPromotionCell, id: String) {
        selectPromotionRelay.accept(id)
    }

    func myPromotionCellDidTapUse(_ cell: MyPromotionCell, voucherId: String, services: [ServiceSelectorItem]) {
        ServiceSelectorBottomSheet.present(from: self, services: services) { service in
            PromotionSDK.getCallback()?.onServiceSelected(selection: PromotionServiceSelection(
                voucherId: voucherId,
                serviceCode: service.serviceCode,
                serviceName: service.serviceName,
                iconUrl: service.iconUrl
            ))
        }
    }
}
