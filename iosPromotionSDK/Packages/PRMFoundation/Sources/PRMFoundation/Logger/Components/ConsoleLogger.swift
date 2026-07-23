//
//  ConsoleLogger.swift
//  Utility
//
//  Created by Nguyen Ngoc Minh on 10/9/20.
//  Copyright © 2020 ViettelPay App Team. All rights reserved.
//

import Foundation

public class ConsoleLogger: LogHandler {

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

        NSLog(text)
    }
}
