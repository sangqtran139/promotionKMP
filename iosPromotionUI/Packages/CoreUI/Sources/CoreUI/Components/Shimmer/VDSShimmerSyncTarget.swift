//
//  VDSShimmerSyncTarget.swift
//  CoreUIKit
//

import UIKit

/// The shimmering effect would be effectively displayed when all the subviews’s effect in the screen is synced and animated together.
/// Shimmer view finds its nearest VDSShimmerSyncTarget in its responder chain and adjusts its shimmering effect according to the sync target's configuration.
public protocol VDSShimmerSyncTarget: UIResponder {
    
    /// The style of the shimmering effect.
    var style: VDSShimmerViewStyle { get }

    /// This effect begin time will be used in `ShimmerView`'s `CAAnimation` begin time and time offset.
    /// Please specify `CACurrentMediaTime()` when the effect should be started, for example, when the object is created.
    var effectBeginTime: CFTimeInterval { get }

    /// All the child `ShimmerView` under ShimmerSyncTarget will calculate its effect for this sync target view's frame.
    var syncTargetView: UIView { get }
}

public extension VDSShimmerSyncTarget where Self: UIView {
    var syncTargetView: UIView {
        return self
    }
}

public extension VDSShimmerSyncTarget where Self: UIViewController {
    var syncTargetView: UIView {
        return view
    }
}
