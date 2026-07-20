import UIKit

internal extension UIResponder {
    var nearestShimmerSyncTarget: (PRMShimmerSyncTarget & UIResponder)? {
        var current: UIResponder? = self.next
        while current != nil {
            if let syncTarget = current as? (PRMShimmerSyncTarget & UIResponder) {
                return syncTarget
            } else {
                current = current?.next
            }
        }
        return nil
    }
}
