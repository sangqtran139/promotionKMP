//
//  OptionalExt.swift
//  Utility
//
//  Created by thachlh on 23/4/26.
//

import Foundation

public extension Swift.Optional {
    func safeInt() -> Int {
        if self == nil || self is NSNull {
            return -1
        }
        if self is Int, let obj = self as? Int {
            return obj
        }
        if self is String, let obj = self as? String {
            if obj.isEmpty {
                return -1
            } else {
                if let number = Int(obj) {
                    return number
                }
            }
        }
        if self is NSDictionary {
            return -1
        }
        return -1
    }
    
    func safeNumber() -> NSNumber {
        if self == nil || self is NSNull {
            return NSNumber(value: -1)
        }
        if self is NSNumber, let obj = self as? NSNumber {
            return obj
        }
        return NSNumber(value: -1)
    }
    
    var safeString: String {
        if self == nil || self is NSNull {
            return ""
        }
        if self is String, let obj = self as? String {
            return obj
        }
        if self is Int, let obj = self as? Int {
            return String(obj)
        }
        return ""
    }
    
    var intValue: Int {
        if self == nil || self is NSNull {
            return -1
        }
        if self is Int, let obj = self as? Int {
            return obj
        }
        if self is String, let obj = self as? String {
            if obj.isEmpty {
                return -1
            } else {
                if let number = Int(obj) {
                    return number
                }
            }
        }
        if self is NSDictionary {
            return -1
        }
        return -1
    }
    
    var numberValue: NSNumber {
        if self == nil || self is NSNull {
            return NSNumber(value: -1)
        }
        if self is NSNumber, let obj = self as? NSNumber {
            return obj
        }
        return NSNumber(value: -1)
    }
    
}
