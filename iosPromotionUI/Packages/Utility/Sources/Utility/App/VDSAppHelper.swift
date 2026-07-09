//
//  VDSAppHelper.swift
//  Utility
//
//  Created by thachlh on 24/4/26.
//

import Foundation
import LocalAuthentication
import UIKit

public class VDSAppHelper {
    public static func appVersion(_ bundle: Bundle = .main) -> String {
        let appVersion = bundle.infoDictionary?["CFBundleShortVersionString"] as? String
        return appVersion ?? ""
    }
    
    public static func makeACall(to phoneNumber: String) {
        guard let url = URL(string: "tel://\(phoneNumber)"),
            UIApplication.shared.canOpenURL(url) else { return }
        if #available(iOS 10, *) {
            UIApplication.shared.open(url)
        } else {
            UIApplication.shared.openURL(url)
        }
    }
    
    public static func authenticationWithBiometric(_ completion: @escaping (Bool) -> Void) {
        let localAuthenticationContext = LAContext()
        localAuthenticationContext.localizedFallbackTitle = ""
        var authorizationError: NSError?
        var reason = CoreUtilsKitLocalization.required_touch_id.localized
        
        if LAContext().biometricType == .faceID {
            reason = CoreUtilsKitLocalization.required_face_id.localized
        }
        
        if localAuthenticationContext.canEvaluatePolicy(.deviceOwnerAuthentication, error: &authorizationError) {
            localAuthenticationContext.evaluatePolicy(.deviceOwnerAuthentication, localizedReason: reason) { success, _ in
                completion(success)
            }
        } else {
            completion(false)
        }
    }
}

public extension VDSAppHelper {
    enum VersionComparisonResult {
        case equal
        case greaterThan
        case lessThan
    }

    static func compareVersions(_ version1: String, _ version2: String) -> VersionComparisonResult {
        let version1Components = version1.split(separator: ".").compactMap { Int($0) }
        let version2Components = version2.split(separator: ".").compactMap { Int($0) }
        
        for index in 0..<min(version1Components.count, version2Components.count) {
            if version1Components[index] > version2Components[index] {
                return .greaterThan
            } else if version1Components[index] < version2Components[index] {
                return .lessThan
            }
        }
        
        if version1Components.count == version2Components.count {
            return .equal
        } else if version1Components.count > version2Components.count {
            return .greaterThan
        } else {
            return .lessThan
        }
    }
}
