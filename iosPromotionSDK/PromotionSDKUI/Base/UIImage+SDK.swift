//
//  UIImage+SDK.swift
//  PRMSDK
//

import UIKit
@_implementationOnly import PRMFoundation

/// Private class used to locate the SDK bundle.
private final class _PRMBundleToken {}

enum PRMBundleSetup {
    static let once: Void = {
        SDKBundle.register(Bundle(for: _PRMBundleToken.self))
    }()
}
