//
//  UIImage+SDK.swift
//  PromotionSDK
//

import UIKit
@_implementationOnly import Utility

/// Private class used to locate the SDK bundle.
private final class _VDSBundleToken {}

enum VDSBundleSetup {
    static let once: Void = {
        SDKBundle.register(Bundle(for: _VDSBundleToken.self))
    }()
}
