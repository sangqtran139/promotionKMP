//
//  SearchMyPromotionViewController.swift
//  PromotionSDK
//
//  Created by thachlh on 11/5/26.
//

import UIKit
@_implementationOnly import PRMDesignKit
@_implementationOnly import PRMPromotionUI
@_implementationOnly import PRMFoundation

final class SearchMyPromotionViewController: PRMBaseViewController<SearchMyPromotionViewModel> {

    // MARK: - UI Components
    @IBOutlet private weak var searchTextField: PRMSearchTextField!
    @IBOutlet private weak var tableView: UITableView!
    @IBOutlet private weak var searchHeaderView: UIView!
    @IBOutlet private weak var resultSearchLabel: UILabel!

    /// Nguồn dữ liệu list.
    private var promotionItems: [MyPromotionCellViewModel] = []

    /// Spinner "đang tải thêm" ở đáy list khi phân trang — đối ứng `MyPromotionListItem.Loading`
    /// mà Android nối vào cuối danh sách (`buildPromotionListItems(isLoadingMore:)`).
    private let loadMoreSpinner: UIActivityIndicatorView = {
        let v = UIActivityIndicatorView(style: .medium)
        v.hidesWhenStopped = true
        return v
    }()

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
        // Gõ mỗi ký tự → `queryChanged` (store lo debounce). Target-action thay cho `textPublisher`
        // của Combine — và KHÔNG phát giá trị đầu lúc bind như publisher cũ (`.prepend`), nên không
        // còn cú `QueryChanged("")` thừa ngay khi mở màn.
        searchTextField.addTarget(self, action: #selector(searchTextChanged), for: .editingChanged)
    }

    @objc private func searchTextChanged(_ sender: UITextField) {
        viewModel.handleAction(.queryChanged(sender.text ?? ""))
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
        // Bind list bằng dataSource cổ điển + reloadData.
        self.tableView.dataSource = self
    }

    // MARK: - Bind ViewModel
    //
    // Đối ứng `SearchMyPromotionFragment.observeData` bên Android: một `render(state)` cho toàn bộ
    // bề mặt + một nhánh xử lý effect. Gán `onState` là nhận ngay state hiện tại (VM replay).
    override func bindViewModel() {
        super.bindViewModel()

        viewModel.onState = { [weak self] state in self?.render(state) }
        viewModel.onEffect = { [weak self] effect in self?.handle(effect) }
    }

    private func render(_ state: SearchMyPromotionViewModel.UiState) {
        promotionItems = state.promotions
        tableView.reloadData()
        // Text "Kết quả tìm kiếm" chỉ hiện khi có kết quả (chưa search / không có KQ thì ẩn).
        resultSearchLabel.isHidden = state.promotions.isEmpty

        shimmerOverlay.isHidden = !state.isLoading
        if state.isLoading {
            tableView.isHidden = true
            searchNoResultView.isHidden = true
            shimmerView.startAnimating()
        } else {
            shimmerView.stopAnimating()
            searchNoResultView.isHidden = !state.isEmpty
            tableView.isHidden = state.isEmpty
        }

        renderLoadMore(state.isLoadingMore)
    }

    /// Gắn/gỡ spinner ở đáy list. Chỉ đụng `tableFooterView` khi trạng thái thật sự đổi để không
    /// bắt table layout lại mỗi lần render.
    private func renderLoadMore(_ isLoadingMore: Bool) {
        let isShowing = tableView.tableFooterView === loadMoreSpinner
        guard isShowing != isLoadingMore else { return }

        if isLoadingMore {
            loadMoreSpinner.frame = CGRect(x: 0, y: 0, width: tableView.bounds.width, height: 44)
            tableView.tableFooterView = loadMoreSpinner
            loadMoreSpinner.startAnimating()
        } else {
            loadMoreSpinner.stopAnimating()
            tableView.tableFooterView = nil
        }
    }

    private func handle(_ effect: SearchMyPromotionViewModel.Effect) {
        switch effect {
        // Lỗi nghiệp vụ → toast (đồng nhất Android: Fragment map code → chuỗi rồi showToast).
        case .showError(let code):
            PromotionToast.show(PromotionUIStrings.errorMessage(code), in: view)
        case .showServiceSelector(let voucherId, let services):
            showServiceSelector(voucherId: voucherId, services: services)
        }
    }

    /// Giống `SearchMyPromotionFragment.showServiceSelector` bên Android — cùng bottom sheet, cùng sự kiện host.
    private func showServiceSelector(voucherId: String, services: [ServiceSelectorItem]) {
        ServiceSelectorBottomSheet.present(from: self, services: services) { [weak self] service in
            PromotionSDK.getCallback()?.onServiceSelected(selection: PromotionServiceSelection(
                voucherId: voucherId,
                serviceCode: service.serviceCode,
                serviceName: service.serviceName,
                serviceType: service.serviceType,
                iconUrl: service.iconUrl
            ))
            self?.viewModel.handleAction(.serviceSelected(service))
        }
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
            viewModel.handleAction(.loadMore)
        }
    }
}

// MARK: - UITableViewDataSource
extension SearchMyPromotionViewController: UITableViewDataSource {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        promotionItems.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueCell(MyPromotionCell.self, for: indexPath)
        cell.delegate = self
        cell.bindData(promotionItems[indexPath.row])
        return cell
    }
}

// MARK: - UITextFieldDelegate (search action on return key)
extension SearchMyPromotionViewController: UITextFieldDelegate {
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        viewModel.handleAction(.search)
        textField.resignFirstResponder()
        return true
    }

    func textField(_ textField: UITextField,
                   shouldChangeCharactersIn range: NSRange,
                   replacementString string: String) -> Bool {
        PromotionSearchLimit.shouldChange(textField, range: range, replacement: string)
    }
}

// MARK: - MyPromotionCellDelegate
extension SearchMyPromotionViewController: MyPromotionCellDelegate {
    func myPromotionCellDidTap(_ cell: MyPromotionCell, id: String) {
        viewModel.handleAction(.selectPromotion(id))
    }

    func myPromotionCellDidTapUse(_ cell: MyPromotionCell, voucherId: String) {
        viewModel.handleAction(.openServiceSelector(voucherId))
    }
}
