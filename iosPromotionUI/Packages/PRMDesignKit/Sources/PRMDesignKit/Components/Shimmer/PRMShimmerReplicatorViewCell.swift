//
//  PRMShimmerReplicatorViewCell.swift
//  CoreUIKit
//

import UIKit

/// PRMShimmerReplicatorView's each cell comforms to this protocol. The replicator view will replicate the cell as needed by the cell provider specified in its initializer.
/// Also, `PRMShimmerReplicatorView` starts all the cells' animation when its `startAnimating` is called.
public protocol PRMShimmerReplicatorViewCell: UIView {
    func startAnimating()
    func stopAnimating()
}
