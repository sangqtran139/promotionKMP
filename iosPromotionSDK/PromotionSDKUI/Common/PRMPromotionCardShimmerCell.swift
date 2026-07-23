//
//  PRMPromotionCardShimmerCell.swift
//  PromotionSDK
//
//  Cell skeleton (shimmer) mô phỏng PromotionCardView lúc đang load.
//  Dùng chung cho màn Chọn ưu đãi & Ưu đãi của tôi, qua PRMShimmerReplicatorView (PRMDesignKit).
//

import UIKit
@_implementationOnly import PRMDesignKit

final class PRMPromotionCardShimmerCell: UIView, PRMShimmerReplicatorViewCell {

    /// Chiều cao 1 dòng skeleton (card + khoảng cách dưới).
    static let itemHeight: CGFloat = 96

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

        NSLayoutConstraint.activate([
            card.topAnchor.constraint(equalTo: topAnchor),
            card.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 16),
            card.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -16),
            card.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -12),

            circle.widthAnchor.constraint(equalToConstant: 48),
            circle.heightAnchor.constraint(equalToConstant: 48),
            circle.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            circle.centerYAnchor.constraint(equalTo: card.centerYAnchor),

            bar1.leadingAnchor.constraint(equalTo: circle.trailingAnchor, constant: 16),
            bar1.topAnchor.constraint(equalTo: card.topAnchor, constant: 20),
            bar1.widthAnchor.constraint(equalToConstant: 90),
            bar1.heightAnchor.constraint(equalToConstant: 10),

            bar2.leadingAnchor.constraint(equalTo: bar1.leadingAnchor),
            bar2.topAnchor.constraint(equalTo: bar1.bottomAnchor, constant: 10),
            bar2.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            bar2.heightAnchor.constraint(equalToConstant: 14),

            bar3.leadingAnchor.constraint(equalTo: bar1.leadingAnchor),
            bar3.topAnchor.constraint(equalTo: bar2.bottomAnchor, constant: 10),
            bar3.widthAnchor.constraint(equalToConstant: 120),
            bar3.heightAnchor.constraint(equalToConstant: 10)
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
