//
//  Array+Extension.swift
//  Utility
//
//  Created by thachlh on 23/4/26.
//

import Foundation
extension Array {
    subscript (safe index: Int) -> Element? {
        get {
            return (0 <= index && index < count) ? self[index] : nil
        }
        set (value) {
            if value == nil {
                return
            }

            if !(count > index) {
                NSLog("WARN: index:\(index) is out of range, so ignored. (array:\(self))")
                return
            }

            self[index] = value!
        }
    }
}

extension Array where Element: Equatable {
    @discardableResult
    mutating func removeObject(_ object: Element) -> Bool {
        guard let index = self.firstIndex(where: { (element) -> Bool in
            return object == element
        }) else { return false }

        self.remove(at: index)
        return true
    }
    
    mutating func popFirst() -> Element? {
        if let first = self.first {
            self.removeObject(first)
            return first
        }
        
        return nil
    }
}
