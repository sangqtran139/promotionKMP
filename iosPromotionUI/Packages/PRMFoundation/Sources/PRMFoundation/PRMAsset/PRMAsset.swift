//
//  PRMAsset.swift
//  Utility
//
//  Created by thachlh on 20/4/26.
//

import UIKit

public enum PRMAsset {
    public static func image(_ named: String, from bundle: Bundle) -> UIImage? {
        UIImage(named: named, in: bundle, compatibleWith: nil)
    }
}
