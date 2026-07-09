//
//  BaseLogger.swift
//  Utility
//
//  Created by Nguyen Ngoc Minh on 9/25/20.
//  Copyright © 2020 ViettelPay App Team. All rights reserved.
//

import Foundation

public enum Level: String, Codable, CaseIterable {
    /// Appropriate for messages that contain information normally of use only when
    /// debugging a program.
    case debug

    /// Appropriate for informational messages.
    case info

    /// Appropriate for messages that are not error conditions, but more severe than
    /// `.notice`.
    case warning

    /// Appropriate for error conditions.
    case error
}

extension Level {

    var timeStamp: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd HH:mm a"
        formatter.locale = Locale.current
        formatter.calendar = Calendar(identifier: Calendar.Identifier.iso8601)
        formatter.locale = Locale(identifier: "en_US_POSIX")

        return formatter.string(from: Date())
    }

    var prefix: String {
        var prefixStr = ""
        switch self {
            case .debug:
                prefixStr =  "\u{0001F539} "
            case .info:
                prefixStr =  "\u{0001F538} "
            case .error:
                prefixStr =  "\u{0001F6AB} "
            case .warning:
                prefixStr =  "\u{26A0}\u{FE0F} "
        }

        return prefixStr
    }
}

extension Level {
    internal var naturalIntegralValue: Int {
        switch self {
            case .debug:
                return 0
            case .info:
                return 1
            case .warning:
                return 2
            case .error:
                return 3
        }
    }
}

extension Level: Comparable {
    public static func < (lhs: Level, rhs: Level) -> Bool {
        return lhs.naturalIntegralValue < rhs.naturalIntegralValue
    }
}

public protocol LogHandler {
    /// This method is called when a `LogHandler` must emit a log message. There is no need for the `LogHandler` to
    /// check if the `level` is above or below the configured `logLevel` as `Logger` already performed this check and
    /// determined that a message should be logged.
    ///
    /// - parameters:
    ///     - level: The log level the message was logged at.
    ///     - message: The message to log. To obtain a `String` representation call `message.description`.
    ///     - metadata: The metadata associated to this log message.
    ///     - file: The file the log message was emitted from.
    ///     - function: The function the log line was emitted from.
    ///     - line: The line the log message was emitted from.
    ///     - fileName: The name of files saved on disk.
    func log(_ level: Level,
             message: String,
             file: String,
             function: String,
             line: Int,
             fileName: String)

    /// Get or set the configured log level.
    ///
    /// - note: `LogHandler`s must treat the log level as a value type. This means that the change in metadata must
    ///         only affect this very `LogHandler`. It is acceptable to provide some form of global log level override
    ///         that means a change in log level on a particular `LogHandler` might not be reflected in any
    ///        `LogHandler`.
    var logLevel: Level { get set }
}
