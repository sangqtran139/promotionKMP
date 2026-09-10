//
//  ConsoleLogger.swift
//  Utility
//
//  Created by Nguyen Ngoc Minh on 10/9/20.
//  Copyright © 2020 ViettelPay App Team. All rights reserved.
//

import Foundation
import os

public class ConsoleLogger: LogHandler {

    /// Subsystem gắn bundle id của gói nên dev của host lọc được đúng log của SDK trong Console.app,
    /// thay vì phải bới toàn bộ syslog của app — thứ `NSLog` không cho làm.
    private static let osLog = OSLog(
        subsystem: Bundle(for: ConsoleLogger.self).bundleIdentifier ?? "com.vtm.PromotionKit",
        category: "PRMLogger"
    )

    class var shared: ConsoleLogger {
        struct StaticView {
            static let instance: ConsoleLogger = ConsoleLogger()
        }
        return StaticView.instance
    }

    // MARK: - LogHandler Protocol
    public var logLevel: Level = .debug

    public func log(_ level: Level, message: String, file: String, function: String, line: Int, fileName: String) {
        var text = ""
        text += level.prefix
        text.append(contentsOf: "\(message.map({"\($0)"}).joined(separator: "")) -> ")

        if !function.isEmpty {
            text.append(contentsOf: "\(function) -> ")
        }
        if !file.isEmpty {
            text.append(contentsOf: "\(file) -> ")
        }
        text.append(contentsOf: "\(line)!\n")

        // `os_log` chứ không phải `NSLog`: log của SDK không được ghi thẳng vào syslog của app host
        // mà không lọc được. `%{public}@` vì mặc định `os_log` che chuỗi thành `<private>`.
        // KHÔNG dùng `os.Logger` — nó chỉ có từ iOS 14, SDK còn hỗ trợ iOS 13.
        os_log("%{public}@", log: ConsoleLogger.osLog, type: level.osLogType, text)
    }
}

private extension Level {
    /// Mức của `Level` → mức của `os_log`, để Console.app lọc và tô màu đúng.
    /// `NSLog` không có khái niệm này: mọi dòng trông như nhau.
    var osLogType: OSLogType {
        switch self {
        case .debug:   return .debug
        case .info:    return .info
        case .warning: return .default   // os_log không có `.warning`; `.default` là mức trên `.info`
        case .error:   return .error
        }
    }
}
