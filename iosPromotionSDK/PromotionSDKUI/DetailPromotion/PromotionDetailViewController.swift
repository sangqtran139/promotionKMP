//
//  PromotionDetailViewController.swift
//  PromotionSDK
//
//  Created by thachlh on 13/5/26.
//

import UIKit
import WebKit
@_implementationOnly import PRMPromotionUI
@_implementationOnly import PRMFoundation
@_implementationOnly import PRMDesignKit
@_implementationOnly import PRMKotlinBridge

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
    /// Hai tab render bằng **WKWebView** (không phải UITextView) để khớp Android — cùng nạp chuỗi
    /// HTML do `wrapPromotionHtml` (promotionLogic) sinh ra. NSAttributedString cũ render bảng/list
    /// khác trình duyệt nên hai nền tảng lệch.
    private let detailWebView = PromotionDetailViewController.makeContentWebView()
    private let guideWebView = PromotionDetailViewController.makeContentWebView()

    /// `navigationDelegate` là `weak` — phải có ai đó giữ, nếu không nó rụng ngay và hết chặn.
    private let webViewNavigationBlocker = PromotionContentWebViewNavigationBlocker()

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
        // Chuỗi lấy từ PromotionUIStrings (nơi tập trung, parity strings.xml) — không hardcode tại chỗ.
        self.underlinedSegmentControlView.setItems(
            titles: [PromotionUIStrings.tabDetailInfo, PromotionUIStrings.tabUsageGuide]
        )
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

        detailWebView.navigationDelegate = webViewNavigationBlocker
        guideWebView.navigationDelegate = webViewNavigationBlocker

        let detailPage = makeContentPage(webView: detailWebView)
        let guidePage = makeContentPage(webView: guideWebView)
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

    /// 1 trang nội dung: card trắng bo góc + viền + đổ bóng (đồng bộ VoucherCardView), bọc webview cuộn.
    private func makeContentPage(webView: WKWebView) -> UIView {
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

        card.addSubview(webView)

        NSLayoutConstraint.activate([
            // Card canh lề 20 trong mỗi trang → trùng lề VoucherCardView phía trên.
            card.topAnchor.constraint(equalTo: page.topAnchor),
            card.bottomAnchor.constraint(equalTo: page.bottomAnchor),
            card.leadingAnchor.constraint(equalTo: page.leadingAnchor, constant: 20),
            card.trailingAnchor.constraint(equalTo: page.trailingAnchor, constant: -20),

            webView.topAnchor.constraint(equalTo: card.topAnchor, constant: 16),
            webView.bottomAnchor.constraint(equalTo: card.bottomAnchor, constant: -16),
            webView.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            webView.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16)
        ])
        return page
    }

    /// WebView 1 tab: nền trong suốt (card trắng phía sau lo nền), cuộn dọc trong card, **không**
    /// bounce/cuộn ngang để không tranh cử chỉ với pager vuốt ngang ở ngoài.
    ///
    /// **JavaScript tắt.** Nội dung là HTML do backend trả (mô tả / hướng dẫn campaign), chỉ cần
    /// render text + ảnh; bật JS là cho script của bên thứ ba chạy trong app host mà chẳng để làm gì.
    /// Đối ứng `javaScriptEnabled = false` ở `PrmContentDetailEndowFragment` bên Android — sửa một
    /// bên thì sửa cả hai.
    private static func makeContentWebView() -> WKWebView {
        let configuration = WKWebViewConfiguration()
        if #available(iOS 14.0, *) {
            configuration.defaultWebpagePreferences.allowsContentJavaScript = false
        } else {
            configuration.preferences.javaScriptEnabled = false
        }
        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.translatesAutoresizingMaskIntoConstraints = false
        webView.backgroundColor = .clear
        webView.isOpaque = false
        webView.scrollView.backgroundColor = .clear
        webView.scrollView.bounces = false
        webView.scrollView.showsHorizontalScrollIndicator = false
        return webView
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

        viewModel.onDisplay = { [weak self] display in self?.render(display) }
        viewModel.onEffect = { [weak self] effect in self?.handle(effect) }

        viewModel.loadDetailIfNeeded()
    }

    private func render(_ state: PromotionDetailViewModel.Display) {
        configVoucherCardView(voucherCardViewModel: state.card)
        bannerImageView.setImage(urlString: state.banner)

        applyContent(state.tabContents.detail, to: detailWebView)
        applyContent(state.tabContents.guide, to: guideWebView)

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

    /// Lỗi nghiệp vụ → toast (đồng nhất Android/MyPromotion).
    private func handle(_ effect: PRMEffect) {
        if let error = effect as? PRMEffectShowError {
            // KHÔNG hiện gì: SDK đã bỏ toast. Màn này còn empty-view/list cũ nên user
            // vẫn hiểu được. Đối ứng `is PRMEffect.ShowError -> Unit` bên Android.
            _ = error
        }
    }

    /// Giống `PromotionDetailFragment.showServiceSelector` bên Android — cùng bottom sheet, cùng
    /// sự kiện host, rồi vẫn để VM xử lý điều hướng nội bộ.
    private func showServiceSelector(_ items: [ServiceSelectorItem]) {
        ServiceSelectorBottomSheet.present(from: self, services: items) { [weak self] service in
            self?.onServiceSelected(service)
        }
    }

    /// Một dịch vụ đã được chọn — dù qua bottom sheet hay đi thẳng (chỉ có 1 dịch vụ khả dụng,
    /// effect `.serviceChosen`). Báo host rồi vẫn để VM xử lý điều hướng nội bộ.
    /// Đối ứng `PromotionDetailFragment.onServiceSelected` bên Android.
    private func onServiceSelected(_ service: ServiceSelectorItem) {
        PromotionSDK.getCallback()?.onServiceSelected(selection: PromotionServiceSelection(
            voucherId: viewModel.voucherId,
            productId: service.productId,
            productName: service.productName,
            skuSourceId: service.skuSourceId,
            iconUrl: service.iconUrl
        ))
    }

    /// Nạp trang HTML đã bọc sẵn (dùng chung với Android) vào WebView. Rỗng → trang trắng.
    /// `baseURL: nil` — khớp `loadDataWithBaseURL(null, …)` bên Android.
    private func applyContent(_ display: PromotionDetailViewModel.ContentDisplay, to webView: WKWebView) {
        webView.loadHTMLString(display.html, baseURL: nil)
    }

    //MARK: - Action
    @IBAction private func didTapBackButton(_ sender: Any) {
        self.viewModel.routeToParent()
    }

    @objc private func didTapApplyButton() {
        // Hai hành vi tuỳ nơi mở màn — parity Android `PromotionDetailFragment.onActionClick`
        // (TLNV MOB_002 control #5). Từ luồng thanh toán: KHÔNG chọn dịch vụ, chỉ trả voucherId về
        // màn "Chọn ưu đãi" (tick sẵn) rồi đóng màn này.
        if viewModel.returnVoucherOnApply {
            // Báo TRƯỚC khi pop — nhờ vậy `hostHandlesDismiss` chạy được: host nhận data lúc màn
            // vẫn còn sống rồi tự quyết khi nào đóng (đối ứng Android `onActionClick`).
            viewModel.notifyVoucherApplied()
            if !viewModel.hostHandlesDismiss { viewModel.routeToParent() }
            return
        }
        showServiceSelector(viewModel.serviceOptions())
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

//MARK: - Chặn điều hướng trong WebView nội dung
/// Cho **đúng** lần nạp HTML ban đầu (`loadHTMLString` → `about:blank`), chặn mọi thứ còn lại: link
/// trong nội dung ưu đãi không được điều hướng WebView đi đâu cả.
///
/// Nội dung là HTML do backend/merchant nhập, nên không chặn thì một thẻ `<a>` là đủ để render trang
/// bất kỳ **bên trong UI của SDK** — người dùng vẫn tưởng đang ở màn ưu đãi. Android chặn sẵn bằng
/// `shouldOverrideUrlLoading` trả `true`; đây là phần đối ứng, trước giờ iOS thiếu.
final class PromotionContentWebViewNavigationBlocker: NSObject, WKNavigationDelegate {
    func webView(
        _ webView: WKWebView,
        decidePolicyFor navigationAction: WKNavigationAction,
        decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
    ) {
        let url = navigationAction.request.url
        let isInitialHtmlLoad = url == nil || url?.absoluteString == "about:blank"
        decisionHandler(isInitialHtmlLoad ? .allow : .cancel)
    }
}
