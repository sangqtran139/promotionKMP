//
//  SDKBundle.swift
//  Utility
//
//  Created by thachlh on 20/4/26.
//



import UIKit

/// Registry for the default SDK bundle.
/// Call `SDKBundle.register(_:)` once at SDK startup.
public final class SDKBundle {
    private init() {}
    
    static var current: Bundle = .main
    
    public static func register(_ bundle: Bundle) {
        current = bundle
    }
}

public extension UIImage {
    /// Load image from the registered SDK bundle.
    /// Call `SDKBundle.register(_:)` once before using this method.
    /// Pass a custom bundle to load from a specific package instead.
    static func sdk(_ named: String, in bundle: Bundle? = nil) -> UIImage? {
        let resolvedBundle = bundle ?? SDKBundle.current
        return UIImage(named: named, in: resolvedBundle, compatibleWith: nil)
    }
}
