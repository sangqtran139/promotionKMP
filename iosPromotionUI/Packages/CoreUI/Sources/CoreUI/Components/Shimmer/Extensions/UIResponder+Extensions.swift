import UIKit

internal extension UIResponder {
    var nearestShimmerSyncTarget: (VDSShimmerSyncTarget & UIResponder)? {
        var current: UIResponder? = self.next
        while current != nil {
            if let syncTarget = current as? (VDSShimmerSyncTarget & UIResponder) {
                return syncTarget
            } else {
                current = current?.next
            }
        }
        return nil
    }
}
