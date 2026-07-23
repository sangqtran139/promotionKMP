//
//  PromotionDetailShimmerView.swift
//  PromotionSDK
//
//  Skeleton (shimmer) cho màn Chi tiết ưu đãi lúc đang gọi API chi tiết.
//  Mô phỏng bố cục thật: banner → voucher card → hàng tab → card nội dung → nút áp dụng.
//  Theo pattern PRMPromotionCardShimmerCell (PRMShimmerView + Colors.tokenDark05).
//

import UIKit
@_implementationOnly import PRMDesignKit

final class PromotionDetailShimmerView: UIView {

    private var pieces: [PRMShimmerView] = []

    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    private func setup() {
        backgroundColor = Colors.tokenDark02

        let banner = makePiece(corner: 0)                 // ảnh banner
        let card = makeCard()                             // voucher card
        let logo = makePiece(corner: 22)                  // logo trong card
        let brandBar = makePiece(corner: 4)               // tên merchant
        let titleBar = makePiece(corner: 4)               // tên ưu đãi
        let dateBar = makePiece(corner: 4)                // hạn sử dụng
        let tab1 = makePiece(corner: 6)                   // tab 1
        let tab2 = makePiece(corner: 6)                   // tab 2
        let contentCard = makeCard()                      // card nội dung
        let line1 = makePiece(corner: 4)
        let line2 = makePiece(corner: 4)
        let line3 = makePiece(corner: 4)
        let button = makePiece(corner: 16)                // nút áp dụng

        [logo, brandBar, titleBar, dateBar].forEach { card.addSubview($0) }
        [line1, line2, line3].forEach { contentCard.addSubview($0) }
        [banner, card, tab1, tab2, contentCard, button].forEach { addSubview($0) }

        NSLayoutConstraint.activate([
            banner.topAnchor.constraint(equalTo: topAnchor),
            banner.leadingAnchor.constraint(equalTo: leadingAnchor),
            banner.trailingAnchor.constraint(equalTo: trailingAnchor),
            banner.heightAnchor.constraint(equalToConstant: 173),

            // Voucher card đè lên đáy banner 20pt.
            card.topAnchor.constraint(equalTo: banner.bottomAnchor, constant: -20),
            card.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 20),
            card.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -20),
            card.heightAnchor.constraint(equalToConstant: 128),

            logo.widthAnchor.constraint(equalToConstant: 44),
            logo.heightAnchor.constraint(equalToConstant: 44),
            logo.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            logo.topAnchor.constraint(equalTo: card.topAnchor, constant: 20),

            brandBar.leadingAnchor.constraint(equalTo: logo.trailingAnchor, constant: 12),
            brandBar.centerYAnchor.constraint(equalTo: logo.centerYAnchor),
            brandBar.widthAnchor.constraint(equalToConstant: 120),
            brandBar.heightAnchor.constraint(equalToConstant: 12),

            titleBar.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            titleBar.topAnchor.constraint(equalTo: logo.bottomAnchor, constant: 12),
            titleBar.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            titleBar.heightAnchor.constraint(equalToConstant: 14),

            dateBar.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            dateBar.topAnchor.constraint(equalTo: titleBar.bottomAnchor, constant: 10),
            dateBar.widthAnchor.constraint(equalToConstant: 160),
            dateBar.heightAnchor.constraint(equalToConstant: 12),

            tab1.topAnchor.constraint(equalTo: card.bottomAnchor, constant: 20),
            tab1.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 20),
            tab1.widthAnchor.constraint(equalToConstant: 120),
            tab1.heightAnchor.constraint(equalToConstant: 16),

            tab2.topAnchor.constraint(equalTo: tab1.topAnchor),
            tab2.leadingAnchor.constraint(equalTo: tab1.trailingAnchor, constant: 24),
            tab2.widthAnchor.constraint(equalToConstant: 120),
            tab2.heightAnchor.constraint(equalToConstant: 16),

            contentCard.topAnchor.constraint(equalTo: tab1.bottomAnchor, constant: 16),
            contentCard.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 20),
            contentCard.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -20),
            contentCard.bottomAnchor.constraint(equalTo: button.topAnchor, constant: -16),

            line1.topAnchor.constraint(equalTo: contentCard.topAnchor, constant: 20),
            line1.leadingAnchor.constraint(equalTo: contentCard.leadingAnchor, constant: 16),
            line1.trailingAnchor.constraint(equalTo: contentCard.trailingAnchor, constant: -16),
            line1.heightAnchor.constraint(equalToConstant: 12),

            line2.topAnchor.constraint(equalTo: line1.bottomAnchor, constant: 12),
            line2.leadingAnchor.constraint(equalTo: line1.leadingAnchor),
            line2.trailingAnchor.constraint(equalTo: line1.trailingAnchor),
            line2.heightAnchor.constraint(equalToConstant: 12),

            line3.topAnchor.constraint(equalTo: line2.bottomAnchor, constant: 12),
            line3.leadingAnchor.constraint(equalTo: line1.leadingAnchor),
            line3.widthAnchor.constraint(equalToConstant: 200),
            line3.heightAnchor.constraint(equalToConstant: 12),

            button.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 16),
            button.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -16),
            button.heightAnchor.constraint(equalToConstant: 48),
            button.bottomAnchor.constraint(equalTo: safeAreaLayoutGuide.bottomAnchor, constant: -16)
        ])
    }

    private func makeCard() -> UIView {
        let card = UIView()
        card.backgroundColor = Colors.tokenWhite
        card.layer.cornerRadius = 16
        card.clipsToBounds = true
        card.translatesAutoresizingMaskIntoConstraints = false
        return card
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

    func startAnimating() { pieces.forEach { $0.startAnimating() } }
    func stopAnimating() { pieces.forEach { $0.stopAnimating() } }
}
