//
//  PRMRefreshTableView.swift
//  CyHomeVersion2
//
//  Created by CyFeer Mobile on 10/27/20.
//  Copyright © 2020 An. All rights reserved.
//

import UIKit
import Combine

public class PRMRefreshTableView: UITableView {
    /// Kéo-để-làm-mới và cuộn-tới-đáy.
    public let refreshPublisher = PassthroughSubject<Void, Never>()
    public let loadMorePublisher = PassthroughSubject<Void, Never>()
    
    public var isHasMorePage: Bool = true
    
    private let vds_refreshControl = UIRefreshControl()
    
    public var hasInfinityScrolling: Bool = false {
        didSet {
            if hasInfinityScrolling {
                self.addInfiniteScrolling { [weak self] in
                    guard let self = self else { return }
                    if self.isHasMorePage {
                        self.loadMorePublisher.send(())
                    } else {
                        self.infiniteScrollingView?.stopAnimating()
                    }
                }
                self.showsInfiniteScrolling = true
            } else {
                self.showsInfiniteScrolling = false
            }
        }
    }
    
    public override init(frame: CGRect, style: UITableView.Style) {
        super.init(frame: frame, style: style)
        commonInit()
    }
    
    public required init?(coder: NSCoder) {
        super.init(coder: coder)
        commonInit()
    }
    
    private func commonInit() {
        if #available(iOS 10.0, *) {
            self.refreshControl = vds_refreshControl
        } else {
            self.addSubview(vds_refreshControl)
        }
        
        vds_refreshControl.addTarget(self, action: #selector(handleRefreshControl), for: .valueChanged)
    }
    
    @objc private func handleRefreshControl() {
        refreshPublisher.send(())
    }
    
    public func startRefreshing() {
        if self.window != nil {
            vds_refreshControl.beginRefreshing()
        } else {
            DispatchQueue.main.async { [weak self] in
                self?.vds_refreshControl.beginRefreshing()
            }
        }
    }
    
    public func stopRefreshing() {
        vds_refreshControl.endRefreshing()
    }
    
    public func startLoadingMore() {
        infiniteScrollingView?.startAnimating()
    }
    
    public func stopLoadingMore() {
        infiniteScrollingView?.stopAnimating()
    }
    
    public var isRefreshingView: Bool {
        return vds_refreshControl.isRefreshing
    }
}
