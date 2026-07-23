//
//  UnderlinedSegmentControlView.swift
//  PRMPromotionUI
//
//  Created by thachlh on 13/5/26.
//

import PRMDesignKit
import UIKit

public protocol UnderlinedSegmentControlViewDelegate: AnyObject {
    func segmentControl(_ segmentControl: UnderlinedSegmentControlView, didSelectItemAt index: Int)
}

public final class UnderlinedSegmentControlView: PRMBaseView {
    //MARK: - Properties
    public weak var delegate: UnderlinedSegmentControlViewDelegate?
    
    private var stackView: UIStackView!
    private var items: [UnderlinedSegmentControlItem] = []
    private(set) var selectedIndex: Int = 0
    
    //MARK: - Init
    public override init(frame: CGRect) {
        super.init(frame: frame)
        self.configStackView()
    }
    
    public required init?(coder: NSCoder) {
        super.init(coder: coder)
        self.configStackView()
    }
    
    //MARK: - Config
    private func configStackView() {
        self.stackView = UIStackView()
        self.stackView.axis = .horizontal
        self.stackView.alignment = .fill
        self.stackView.distribution = .fillEqually
        self.stackView.spacing = 0
        self.addSubview(self.stackView)
        self.stackView.makeAnchor { maker in
            maker.top(equalTo: self.topAnchor)
                .bottom(equalTo: self.bottomAnchor)
                .leading(equalTo: self.leadingAnchor)
                .trailing(equalTo: self.trailingAnchor)
        }
        // Theme: override màu nền thanh tab nếu host cấu hình.
        if let bg = PRMThemeRegistry.shared.tabUnderline()?.backgroundColor {
            self.backgroundColor = bg
        }
    }
    
    //MARK: - Public
    public func setItems(titles: [String]) {
        self.items.forEach { $0.removeFromSuperview() }
        self.items.removeAll()
        
        for (index, title) in titles.enumerated() {
            let item = UnderlinedSegmentControlItem()
            item.title = title
            item.isFocus = (index == 0)
            item.tag = index
            item.addTarget(self, action: #selector(itemTapped(_:)), for: .touchUpInside)
            self.stackView.addArrangedSubview(item)
            self.items.append(item)
        }
        
        self.selectedIndex = 0
    }
    
    public func selectItem(at index: Int) {
        guard index >= 0, index < self.items.count else { return }
        self.updateFocus(to: index)
    }
    
    //MARK: - Actions
    @objc private func itemTapped(_ sender: UnderlinedSegmentControlItem) {
        let index = sender.tag
        guard index != self.selectedIndex else { return }
        self.updateFocus(to: index)
        self.delegate?.segmentControl(self, didSelectItemAt: index)
    }
    
    //MARK: - Helper
    private func updateFocus(to index: Int) {
        self.items[self.selectedIndex].isFocus = false
        self.items[index].isFocus = true
        self.selectedIndex = index
    }
}
