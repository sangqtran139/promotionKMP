//
//  PRMPromotionCardShimmerCell.swift
//  PromotionSDK
//
//  Cell skeleton (shimmer) mô phỏng PromotionCardView lúc đang load.
//  Dùng chung cho màn Chọn ưu đãi & Ưu đãi của tôi, qua PRMShimmerReplicatorView (PRMDesignKit).
//

import UIKit
@_implementationOnly import PRMDesignKit
@_implementationOnly import PRMPromotionUI

final class PRMPromotionCardShimmerCell: UIView, PRMShimmerReplicatorViewCell {

    /// Khe dưới mỗi card — bằng đúng ràng buộc `bottom = 12` của `MyPromotionCell.xib`.
    private static let cardBottomGap: CGFloat = 12

    /// Chiều cao 1 dòng skeleton = **card thật + khe dưới**. Lấy thẳng
    /// `PromotionCardView.estimatedHeight` nên sửa card là shimmer tự theo.
    static var itemHeight: CGFloat { PromotionCardView.estimatedHeight + cardBottomGap }

    private var pieces: [PRMShimmerView] = []

    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }
    required init?(coder: NSCoder) { fatalError() }

    private func setup() {
        let card = UIView()
        card.backgroundColor = Colors.tokenWhite
        card.layer.cornerRadius = 16
        card.clipsToBounds = true
        card.translatesAutoresizingMaskIntoConstraints = false
        addSubview(card)

        let circle = makePiece(corner: 24)   // logo
        let bar1 = makePiece(corner: 4)       // tên merchant (ngắn)
        let bar2 = makePiece(corner: 4)       // tên ưu đãi (dài)
        let bar3 = makePiece(corner: 4)       // ngày
        [circle, bar1, bar2, bar3].forEach { card.addSubview($0) }

        // Mọi mốc lấy từ `PromotionCardView.Metrics` — skeleton phải nằm ĐÚNG chỗ khối thật sẽ hiện,
        // không thì lúc dữ liệu về chữ nhảy ngang một đoạn.
        typealias M = PromotionCardView.Metrics
        NSLayoutConstraint.activate([
            // Card thật full-bleed (backgroundView `edges(to: self)`), không thụt hai bên.
            card.topAnchor.constraint(equalTo: topAnchor),
            card.leadingAnchor.constraint(equalTo: leadingAnchor),
            card.trailingAnchor.constraint(equalTo: trailingAnchor),
            card.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -Self.cardBottomGap),

            circle.widthAnchor.constraint(equalToConstant: M.iconSize),
            circle.heightAnchor.constraint(equalToConstant: M.iconSize),
            circle.centerXAnchor.constraint(equalTo: card.leadingAnchor, constant: M.iconCenterX),
            circle.centerYAnchor.constraint(equalTo: card.centerYAnchor),

            // Merchant: 1 dòng, tâm trùng tâm checkbox của card thật.
            bar1.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: M.contentLeading),
            bar1.centerYAnchor.constraint(equalTo: card.topAnchor, constant: M.checkboxTop + M.checkboxSize / 2),
            bar1.widthAnchor.constraint(equalToConstant: 90),
            bar1.heightAnchor.constraint(equalToConstant: M.merchantLineHeight),

            // Tên ưu đãi: chừa đúng 2 dòng như card thật.
            bar2.leadingAnchor.constraint(equalTo: bar1.leadingAnchor),
            bar2.topAnchor.constraint(equalTo: bar1.bottomAnchor, constant: M.titleTopGap),
            bar2.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            bar2.heightAnchor.constraint(equalToConstant: (M.titleLineHeight * 2).rounded(.up)),

            // Hàng HSD.
            bar3.leadingAnchor.constraint(equalTo: bar1.leadingAnchor),
            bar3.topAnchor.constraint(equalTo: bar2.bottomAnchor, constant: M.footerTopGap),
            bar3.widthAnchor.constraint(equalToConstant: 120),
            bar3.heightAnchor.constraint(equalToConstant: M.merchantLineHeight)
        ])
    }

    private func makePiece(corner: CGFloat) -> PRMShimmerView {
        let v = PRMShimmerView()
        v.backgroundColor = Colors.tokenDark05
        v.layer.cornerRadius = corner
        v.clipsToBounds = true
        v.translatesAutoresizingMaskIntoConstraints = false
        pieces.append(v)
        return v
    }

    // MARK: - PRMShimmerReplicatorViewCell
    func startAnimating() { pieces.forEach { $0.startAnimating() } }
    func stopAnimating() { pieces.forEach { $0.stopAnimating() } }
}
