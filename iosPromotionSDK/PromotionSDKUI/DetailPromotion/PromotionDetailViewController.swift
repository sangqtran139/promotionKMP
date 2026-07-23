//
//  PromotionDetailViewController.swift
//  PRMSDK
//
//  Created by thachlh on 13/5/26.
//

import UIKit
@_implementationOnly import PRMPromotionUI
@_implementationOnly import PRMFoundation
@_implementationOnly import PRMDesignKit

final class PromotionDetailViewController: PRMBaseViewController<PromotionDetailViewModel> {
    // MARK: - UI Components
    @IBOutlet private weak var bannerImageView: UIImageView!
    @IBOutlet private weak var voucherCardView: VoucherCardView!
    @IBOutlet private weak var underlinedSegmentControlView: UnderlinedSegmentControlView!
    @IBOutlet private weak var applyButton: PRMButton!

    /// Pager 2 trang vuốt được (Thông tin chi tiết / Hướng dẫn sử dụng) — dựng bằng code.
    private let contentScrollView: UIScrollView = {
        let scroll = UIScrollView()
        scroll.isPagingEnabled = true
        scroll.showsHorizontalScrollIndicator = false
        scroll.translatesAutoresizingMaskIntoConstraints = false
        return scroll
    }()
    private let detailTextView = UITextView()
    private let guideTextView = UITextView()

    /// Shimmer phủ toàn màn lúc gọi API chi tiết.
    private lazy var shimmerView: PromotionDetailShimmerView = {
        let view = PromotionDetailShimmerView()
        view.translatesAutoresizingMaskIntoConstraints = false
        view.isHidden = true
        return view
    }()

    // MARK: - Lifecycle
    override func viewDidLoad() {
        super.viewDidLoad()
    }

    // MARK: - Setup UI
    override func setupUI() {
        super.setupUI()

        self.configUnderlinedSegmentControlView()
        self.configContentPager()
        self.configShimmer()
        self.applyButton.addTarget(self, action: #selector(didTapApplyButton), for: .touchUpInside)
    }

    private func configVoucherCardView(voucherCardViewModel: VoucherCardViewModel) {
        self.voucherCardView.brandName = voucherCardViewModel.title
        self.voucherCardView.title = voucherCardViewModel.description
        self.voucherCardView.expiryText = voucherCardViewModel.date
        self.voucherCardView.setLogo(urlString: voucherCardViewModel.logoURL)
    }

    private func configUnderlinedSegmentControlView() {
        self.underlinedSegmentControlView.delegate = self
        self.underlinedSegmentControlView.setItems(titles: ["Thông tin chi tiết", "Hướng dẫn sử dụng"])
    }

    /// Dựng pager: 2 trang, mỗi trang là 1 card (giống VoucherCardView) bọc textview cuộn được.
    private func configContentPager() {
        self.view.addSubview(contentScrollView)
        contentScrollView.delegate = self

        let stack = UIStackView()
        stack.axis = .horizontal
        stack.distribution = .fillEqually
        stack.translatesAutoresizingMaskIntoConstraints = false
        contentScrollView.addSubview(stack)

        let detailPage = makeContentPage(textView: detailTextView)
        let guidePage = makeContentPage(textView: guideTextView)
        stack.addArrangedSubview(detailPage)
        stack.addArrangedSubview(guidePage)

        // Đáy pager LUÔN neo thẳng vào NÚT áp dụng, cách 32px — kể cả khi nút bị ẩn
        // (isHidden vẫn giữ layout/anchor của nút) → card luôn cách nút một khoảng như nhau.
        let bottomAnchorTarget = applyButton?.topAnchor ?? view.safeAreaLayoutGuide.bottomAnchor

        NSLayoutConstraint.activate([
            contentScrollView.topAnchor.constraint(equalTo: underlinedSegmentControlView.bottomAnchor, constant: 16),
            contentScrollView.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor),
            contentScrollView.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor),
            contentScrollView.bottomAnchor.constraint(equalTo: bottomAnchorTarget, constant: -32),

            stack.topAnchor.constraint(equalTo: contentScrollView.contentLayoutGuide.topAnchor),
            stack.bottomAnchor.constraint(equalTo: contentScrollView.contentLayoutGuide.bottomAnchor),
            stack.leadingAnchor.constraint(equalTo: contentScrollView.contentLayoutGuide.leadingAnchor),
            stack.trailingAnchor.constraint(equalTo: contentScrollView.contentLayoutGuide.trailingAnchor),
            stack.heightAnchor.constraint(equalTo: contentScrollView.frameLayoutGuide.heightAnchor),

            // Mỗi trang rộng bằng khung nhìn để paging snap chuẩn.
            detailPage.widthAnchor.constraint(equalTo: contentScrollView.frameLayoutGuide.widthAnchor),
            guidePage.widthAnchor.constraint(equalTo: contentScrollView.frameLayoutGuide.widthAnchor)
        ])
    }

    /// 1 trang nội dung: card trắng bo góc + viền + đổ bóng (đồng bộ VoucherCardView), bọc textview cuộn.
    private func makeContentPage(textView: UITextView) -> UIView {
        let page = UIView()
        page.translatesAutoresizingMaskIntoConstraints = false

        let card = UIView()
        card.backgroundColor = Colors.tokenWhite
        card.layer.cornerRadius = 16
        card.layer.borderWidth = 0.5
        card.layer.borderColor = Colors.tokenDark20.cgColor
        card.layer.shadowColor = Colors.tokenBlack.cgColor
        card.layer.shadowOpacity = 0.06
        card.layer.shadowOffset = CGSize(width: 0, height: 2)
        card.layer.shadowRadius = 10
        card.layer.masksToBounds = false
        card.translatesAutoresizingMaskIntoConstraints = false
        page.addSubview(card)

        configContentTextView(textView)
        card.addSubview(textView)

        NSLayoutConstraint.activate([
            // Card canh lề 20 trong mỗi trang → trùng lề VoucherCardView phía trên.
            card.topAnchor.constraint(equalTo: page.topAnchor),
            card.bottomAnchor.constraint(equalTo: page.bottomAnchor),
            card.leadingAnchor.constraint(equalTo: page.leadingAnchor, constant: 20),
            card.trailingAnchor.constraint(equalTo: page.trailingAnchor, constant: -20),

            textView.topAnchor.constraint(equalTo: card.topAnchor, constant: 16),
            textView.bottomAnchor.constraint(equalTo: card.bottomAnchor, constant: -16),
            textView.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            textView.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16)
        ])
        return page
    }

    private func configContentTextView(_ textView: UITextView) {
        textView.translatesAutoresizingMaskIntoConstraints = false
        textView.isEditable = false
        textView.isScrollEnabled = true
        textView.backgroundColor = .clear
        textView.textContainerInset = .zero
        textView.textContainer.lineFragmentPadding = 0
        textView.font = Typography.fontRegular14
        textView.textColor = Colors.tokenDark100
    }

    private func configShimmer() {
        self.view.addSubview(shimmerView)
        NSLayoutConstraint.activate([
            shimmerView.topAnchor.constraint(equalTo: view.topAnchor),
            shimmerView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            shimmerView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            shimmerView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
    }

    // MARK: - Bind ViewModel
    //
    // Đối ứng `PromotionDetailFragment.observeData` bên Android: một `render(state)` cho toàn bộ bề
    // mặt, một nhánh effect, rồi `start()` (kích fetch detail).
    override func bindViewModel() {
        super.bindViewModel()

        viewModel.onState = { [weak self] state in self?.render(state) }
        viewModel.onEffect = { [weak self] effect in self?.handle(effect) }

        viewModel.handleAction(.loadDetail)
    }

    private func render(_ state: PromotionDetailViewModel.UiState) {
        configVoucherCardView(voucherCardViewModel: state.card)
        bannerImageView.setImage(urlString: state.banner)

        applyContent(state.tabContents.detail, to: detailTextView)
        applyContent(state.tabContents.guide, to: guideTextView)

        applyButton.setTitle(state.applyTitle, for: .normal)
        applyButton.isEnabled = state.isApplyEnabled
        // Chỉ ẩn/hiện NÚT; luôn giữ chỗ thanh đáy để card cách một khoảng đồng nhất.
        // Không ACTIVE → nền thanh đáy trong suốt (hoà nền màn), có nút → nền trắng.
        applyButton.isHidden = !state.isApplyVisible
        applyButton.superview?.backgroundColor = state.isApplyVisible ? Colors.tokenWhite : .clear

        shimmerView.isHidden = !state.isLoading
        if state.isLoading {
            shimmerView.startAnimating()
        } else {
            shimmerView.stopAnimating()
        }
    }

    private func handle(_ effect: PromotionDetailViewModel.Effect) {
        switch effect {
        // Lỗi nghiệp vụ → Confirmation Dialog (đồng nhất Android/MyPromotion).
        case .showError(let code):
            PRMConfirmationDialog.showError(PromotionUIStrings.errorMessage(code), in: view)
        case .showServiceSelector(let items):
            showServiceSelector(items)
        }
    }

    /// Giống `PromotionDetailFragment.showServiceSelector` bên Android — cùng bottom sheet, cùng
    /// sự kiện host, rồi vẫn để VM xử lý điều hướng nội bộ.
    private func showServiceSelector(_ items: [ServiceSelectorItem]) {
        let voucherId = viewModel.voucherId
        ServiceSelectorBottomSheet.present(from: self, services: items) { [weak self] service in
            PRMSDK.getCallback()?.onServiceSelected(selection: PRMServiceSelection(
                voucherId: voucherId,
                serviceCode: service.serviceCode,
                serviceName: service.serviceName,
                iconUrl: service.iconUrl
            ))
            self?.viewModel.handleAction(.serviceSelected(service))
        }
    }

    /// Gán nội dung 1 tab: HTML → attributed; ngược lại → text thường (rỗng = để trống).
    private func applyContent(_ display: PromotionDetailViewModel.ContentDisplay, to textView: UITextView) {
        if display.isHTML,
           let attributed = display.text.htmlToAttributedString(
                font: Typography.fontRegular14,
                color: Colors.tokenDark100
           ) {
            textView.attributedText = attributed
        } else {
            textView.attributedText = nil
            textView.text = display.text
        }
    }

    //MARK: - Action
    @IBAction private func didTapBackButton(_ sender: Any) {
        self.viewModel.routeToParent()
    }

    @objc private func didTapApplyButton() {
        // Bấm "Áp dụng" → mở bottom sheet chọn dịch vụ (parity Android tvUse → OpenServiceSelector).
        viewModel.handleAction(.openServiceSelector)
    }
}

//MARK: - UnderlinedSegmentControlViewDelegate
extension PromotionDetailViewController: UnderlinedSegmentControlViewDelegate {
    func segmentControl(_ segmentControl: PRMPromotionUI.UnderlinedSegmentControlView, didSelectItemAt index: Int) {
        // Tap tab → cuộn pager tới trang tương ứng (setContentOffset animated không kích didEndDecelerating).
        guard contentScrollView.frame.width > 0 else { return }
        let offset = CGPoint(x: CGFloat(index) * contentScrollView.frame.width, y: 0)
        contentScrollView.setContentOffset(offset, animated: true)
    }
}

//MARK: - UIScrollViewDelegate (đồng bộ vuốt pager → focus tab)
extension PromotionDetailViewController: UIScrollViewDelegate {
    func scrollViewDidEndDecelerating(_ scrollView: UIScrollView) {
        guard scrollView == contentScrollView, scrollView.frame.width > 0 else { return }
        let page = Int(round(scrollView.contentOffset.x / scrollView.frame.width))
        underlinedSegmentControlView.selectItem(at: page)
    }
}
