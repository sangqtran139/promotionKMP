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
            guard let value = value else { return }

            // Cùng khoảng hợp lệ với getter ở trên. Bản cũ chỉ chặn `index >= count` nên index âm
            // vẫn rơi xuống dòng gán và crash — đúng thứ mà `subscript(safe:)` sinh ra để tránh.
            guard index >= 0, index < count else {
                // `ConsoleLogger` chứ không phải `NSLog`: log của SDK phải đi qua `os_log` có
                // subsystem để dev host lọc được, và không ghi thẳng vào syslog của app host.
                ConsoleLogger.shared.log(
                    .warning,
                    message: "index:\(index) is out of range, so ignored. (count:\(count))",
                    file: #file, function: #function, line: #line,
                    fileName: FileLogger.defaultFileName
                )
                return
            }

            self[index] = value
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
