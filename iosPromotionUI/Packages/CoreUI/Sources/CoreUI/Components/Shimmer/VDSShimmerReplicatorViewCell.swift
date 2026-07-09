//
//  VDSShimmerReplicatorViewCell.swift
//  CoreUIKit
//

import UIKit

/// VDSShimmerReplicatorView's each cell comforms to this protocol. The replicator view will replicate the cell as needed by the cell provider specified in its initializer.
/// Also, `VDSShimmerReplicatorView` starts all the cells' animation when its `startAnimating` is called.
public protocol VDSShimmerReplicatorViewCell: UIView {
    func startAnimating()
    func stopAnimating()
}
