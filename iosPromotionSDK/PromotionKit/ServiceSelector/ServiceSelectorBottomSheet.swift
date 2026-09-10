//
//  ServiceSelectorBottomSheet.swift
//  PromotionSDK
//
//  Bottom sheet "Chọn dịch vụ" — hiện khi user bấm "Áp dụng" trên voucher.
//  Liệt kê các dịch vụ khả dụng (lưới 3 cột, cuộn dọc, card cao tối đa 60% màn hình)
//  được lọc theo `applicableProducts` của voucher giao với `availableServices` host
//  cung cấp. Parity Android `ServiceSelectorBottomSheet` + `ServiceSelectorAdapter`.
//

import UIKit
@_implementationOnly import PRMDesignKit
@_implementationOnly import PRMFoundation

/// Item hiển thị 1 dịch vụ trong bottom sheet (parity Android `ServiceSelectorUiItem`).
struct ServiceSelectorItem: Equatable {
    let productId: String
    let productName: String
    let skuSourceId: String
    let iconUrl: String
}

final class ServiceSelectorBottomSheet: UIViewController {

    // MARK: - Config
    /// Số cột của lưới; dịch vụ dư ra thì xuống hàng. Khớp `SPAN_COUNT` của Android.
    private static let spanCount = 3
    /// Icon 48 + spacing 4 + tên 2 dòng (~32). Mọi ô cao bằng nhau nên nhân số hàng ra chiều cao lưới.
    private static let itemHeight: CGFloat = 48 + 4 + 32
    /// Trần chiều cao card theo màn hình — khớp `MAX_HEIGHT_RATIO` của Android.
    private static let maxHeightRatio: CGFloat = 0.6

    private let services: [ServiceSelectorItem]
    private let onServiceSelected: (ServiceSelectorItem) -> Void

    // MARK: - UI
    private let dimView = UIView()
    private let cardView = UIView()
    /// **Lưới `spanCount` cột, cuộn dọc.** Nhiều dịch vụ thì xuống hàng; cao quá trần
    /// `maxHeightRatio` thì cuộn trong lưới — xem `computedCardHeight()`.
    private lazy var collectionView: UICollectionView = {
        let layout = UICollectionViewFlowLayout()
        layout.scrollDirection = .vertical
        layout.minimumLineSpacing = 0
        layout.minimumInteritemSpacing = 0
        return UICollectionView(frame: .zero, collectionViewLayout: layout)
    }()
    private let emptyLabel = UILabel()
    private var cardBottomConstraint: NSLayoutConstraint?
    /// Bề rộng đã dùng để tính size item lần gần nhất — xem `viewDidLayoutSubviews`.
    private var lastLaidOutWidth: CGFloat = 0

    // MARK: - Init
    init(services: [ServiceSelectorItem], onServiceSelected: @escaping (ServiceSelectorItem) -> Void) {
        self.services = services
        self.onServiceSelected = onServiceSelected
        super.init(nibName: nil, bundle: nil)
        modalPresentationStyle = .overFullScreen
        modalTransitionStyle = .crossDissolve
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    /// Lối vào DUY NHẤT của bottom sheet — mọi màn (Ưu đãi của tôi, Tìm kiếm, Chi tiết) gọi qua đây
    /// để luật sau không phải lặp lại ở từng màn:
    ///
    /// **Đúng 1 dịch vụ → chọn thẳng, không mở sheet** (TLNV MOB_002 control #5). 0 dịch vụ vẫn
    /// mở (sheet hiện "Không có dịch vụ thoả mãn") để user biết vì sao không đi tiếp được.
    ///
    /// Đối ứng `ServiceSelectorBottomSheet.present(host:services:onServiceSelected:)` bên Android.
    static func present(from presenter: UIViewController,
                        services: [ServiceSelectorItem],
                        onServiceSelected: @escaping (ServiceSelectorItem) -> Void) {
        if services.count == 1, let only = services.first {
            onServiceSelected(only)
            return
        }
        let sheet = ServiceSelectorBottomSheet(services: services, onServiceSelected: onServiceSelected)
        presenter.present(sheet, animated: false)
    }

    // MARK: - Lifecycle
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        animateIn()
    }

    /// `sizeForItemAt` chia theo `collectionView.bounds.width`, mà lần hỏi đầu tiên bounds có thể
    /// còn 0 → item ra bề rộng rác và list "không cuộn được". Bounds đổi thì tính lại đúng một lần.
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        guard lastLaidOutWidth != collectionView.bounds.width else { return }
        lastLaidOutWidth = collectionView.bounds.width
        collectionView.collectionViewLayout.invalidateLayout()
    }

    // MARK: - Setup
    private func setupUI() {
        view.backgroundColor = .clear

        dimView.backgroundColor = UIColor.black.withAlphaComponent(0.4)
        dimView.alpha = 0
        dimView.translatesAutoresizingMaskIntoConstraints = false
        dimView.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(didTapDim)))
        view.addSubview(dimView)

        cardView.backgroundColor = .white
        cardView.layer.cornerRadius = 16
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        cardView.clipsToBounds = true
        cardView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(cardView)

        // Drag handle
        let handle = UIView()
        handle.backgroundColor = Colors.tokenDark10
        handle.layer.cornerRadius = 2
        handle.translatesAutoresizingMaskIntoConstraints = false
        cardView.addSubview(handle)

        // Title
        let titleLabel = UILabel()
        titleLabel.text = "Chọn dịch vụ"
        titleLabel.font = Typography.fontBold18
        titleLabel.textColor = Colors.tokenDark100
        titleLabel.textAlignment = .center
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        cardView.addSubview(titleLabel)

        // Divider
        let divider = UIView()
        divider.backgroundColor = Colors.tokenDark10
        divider.translatesAutoresizingMaskIntoConstraints = false
        cardView.addSubview(divider)

        // Collection
        collectionView.backgroundColor = .clear
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        collectionView.dataSource = self
        collectionView.delegate = self
        collectionView.isScrollEnabled = true
        collectionView.alwaysBounceVertical = true
        collectionView.contentInset = UIEdgeInsets(top: 16, left: 8, bottom: 24, right: 8)
        collectionView.register(ServiceSelectorCell.self, forCellWithReuseIdentifier: ServiceSelectorCell.reuseId)
        cardView.addSubview(collectionView)

        // Empty
        emptyLabel.text = "Không có dịch vụ thoả mãn"
        emptyLabel.font = Typography.fontRegular16
        emptyLabel.textColor = Colors.tokenDark60
        emptyLabel.textAlignment = .center
        emptyLabel.numberOfLines = 0
        emptyLabel.isHidden = !services.isEmpty
        emptyLabel.translatesAutoresizingMaskIntoConstraints = false
        cardView.addSubview(emptyLabel)
        collectionView.isHidden = services.isEmpty

        // Chiều cao card: nội dung thật, cắt trần theo tỉ lệ màn hình.
        let cardHeight = computedCardHeight()
        let bottom = cardView.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: cardHeight)
        cardBottomConstraint = bottom

        NSLayoutConstraint.activate([
            dimView.topAnchor.constraint(equalTo: view.topAnchor),
            dimView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            dimView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            dimView.bottomAnchor.constraint(equalTo: view.bottomAnchor),

            cardView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            cardView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            cardView.heightAnchor.constraint(equalToConstant: cardHeight),
            bottom,

            handle.topAnchor.constraint(equalTo: cardView.topAnchor, constant: 8),
            handle.centerXAnchor.constraint(equalTo: cardView.centerXAnchor),
            handle.widthAnchor.constraint(equalToConstant: 32),
            handle.heightAnchor.constraint(equalToConstant: 4),

            titleLabel.topAnchor.constraint(equalTo: handle.bottomAnchor, constant: 16),
            titleLabel.leadingAnchor.constraint(equalTo: cardView.leadingAnchor, constant: 16),
            titleLabel.trailingAnchor.constraint(equalTo: cardView.trailingAnchor, constant: -16),

            divider.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 16),
            divider.leadingAnchor.constraint(equalTo: cardView.leadingAnchor),
            divider.trailingAnchor.constraint(equalTo: cardView.trailingAnchor),
            divider.heightAnchor.constraint(equalToConstant: 1),

            collectionView.topAnchor.constraint(equalTo: divider.bottomAnchor),
            collectionView.leadingAnchor.constraint(equalTo: cardView.leadingAnchor),
            collectionView.trailingAnchor.constraint(equalTo: cardView.trailingAnchor),
            collectionView.bottomAnchor.constraint(equalTo: cardView.bottomAnchor),

            emptyLabel.topAnchor.constraint(equalTo: divider.bottomAnchor, constant: 24),
            emptyLabel.leadingAnchor.constraint(equalTo: cardView.leadingAnchor, constant: 16),
            emptyLabel.trailingAnchor.constraint(equalTo: cardView.trailingAnchor, constant: -16)
        ])
    }

    /// Ước lượng chiều cao card: header (handle+title+divider) + lưới services, cắt trần 60% màn hình.
    private func computedCardHeight() -> CGFloat {
        let screenHeight = UIScreen.main.bounds.height
        let headerHeight: CGFloat = 8 + 4 + 16 + 22 + 16 + 1   // handle + title + paddings + divider
        let safeBottom = UIApplication.shared.keyWindow?.safeAreaInsets.bottom ?? 0

        if services.isEmpty {
            return headerHeight + 24 + 24 + 40 + safeBottom
        }

        // Lưới `spanCount` cột → số hàng làm tròn LÊN (7 dịch vụ / 3 cột = 3 hàng, hàng cuối 1 ô).
        // Card cao vừa đủ nội dung, và bị cắt trần `maxHeightRatio`; phần dư thì cuộn trong lưới.
        let rowCount = (services.count + Self.spanCount - 1) / Self.spanCount
        let listHeight = CGFloat(rowCount) * Self.itemHeight + 16 + 24   // + contentInset top/bottom
        let raw = headerHeight + listHeight + safeBottom
        return min(raw, screenHeight * Self.maxHeightRatio)
    }

    // MARK: - Animation
    private func animateIn() {
        view.layoutIfNeeded()
        cardBottomConstraint?.constant = 0
        UIView.animate(withDuration: 0.25) {
            self.dimView.alpha = 1
            self.view.layoutIfNeeded()
        }
    }

    private func animateOut(completion: @escaping () -> Void) {
        cardBottomConstraint?.constant = cardView.bounds.height
        UIView.animate(withDuration: 0.2, animations: {
            self.dimView.alpha = 0
            self.view.layoutIfNeeded()
        }, completion: { _ in
            self.dismiss(animated: false, completion: completion)
        })
    }

    @objc private func didTapDim() {
        animateOut {}
    }
}

// MARK: - UICollectionView
extension ServiceSelectorBottomSheet: UICollectionViewDataSource, UICollectionViewDelegateFlowLayout {

    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        services.count
    }

    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: ServiceSelectorCell.reuseId, for: indexPath)
        if let cell = cell as? ServiceSelectorCell {
            cell.configure(with: services[indexPath.item])
        }
        return cell
    }

    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout,
                        sizeForItemAt indexPath: IndexPath) -> CGSize {
        // Chia đúng `spanCount` phần bề ngang → ô thứ 4 trở đi rơi xuống hàng dưới.
        // `floor` để tổng bề rộng một hàng không vượt quá chỗ trống vì số lẻ — dư 1pt là
        // FlowLayout đẩy ô cuối xuống hàng riêng.
        let insets = collectionView.contentInset.left + collectionView.contentInset.right
        let width = ((collectionView.bounds.width - insets) / CGFloat(Self.spanCount)).rounded(.down)
        return CGSize(width: max(width, 1), height: Self.itemHeight)
    }

    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        let item = services[indexPath.item]
        animateOut { [weak self] in
            self?.onServiceSelected(item)
        }
    }
}

// MARK: - Cell
private final class ServiceSelectorCell: UICollectionViewCell {
    static let reuseId = "ServiceSelectorCell"

    private let iconView = UIImageView()
    private let nameLabel = UILabel()

    override init(frame: CGRect) {
        super.init(frame: frame)
        iconView.contentMode = .scaleAspectFill
        iconView.clipsToBounds = true
        // Bo TRÒN: icon dịch vụ bên Android đi qua `loadPromotionVoucherLogo` → Glide `.circleCrop()`,
        // nên thiếu dòng này là Android tròn mà iOS vuông. 24 = nửa của 48 (kích thước ghim dưới).
        iconView.layer.cornerRadius = 24
        iconView.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(iconView)

        nameLabel.font = Typography.fontRegular12
        nameLabel.textColor = Colors.tokenDark100
        nameLabel.textAlignment = .center
        nameLabel.numberOfLines = 2
        nameLabel.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(nameLabel)

        NSLayoutConstraint.activate([
            iconView.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 8),
            iconView.centerXAnchor.constraint(equalTo: contentView.centerXAnchor),
            iconView.widthAnchor.constraint(equalToConstant: 48),
            iconView.heightAnchor.constraint(equalToConstant: 48),

            nameLabel.topAnchor.constraint(equalTo: iconView.bottomAnchor, constant: 4),
            nameLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 4),
            nameLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -4)
        ])
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    func configure(with item: ServiceSelectorItem) {
        let title = item.productName.isEmpty ? item.productId : item.productName
        nameLabel.text = title
        iconView.setImage(urlString: item.iconUrl)
    }
}
