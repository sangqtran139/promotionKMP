//
//  VDSLogger.swift
//  Utility
//
//  Created by Nguyen Ngoc Minh on 10/5/20.
//  Copyright © 2020 ViettelPay App Team. All rights reserved.
//

import Foundation

@objc
public class VDSLogger: NSObject {

    /// Get or set the log level configured for this `Logger`.
    ///
    /// - note: `Logger`s treat `logLevel` as a value. This means that a change in `logLevel` will only affect this
    ///         very `Logger`. It it acceptable for logging backends to have some form of global log level override
    ///         that affects multiple or even all loggers. This means a change in `logLevel` to one `Logger` might in
    ///         certain cases have no effect.
    public var logLevel: Level  = .debug

    /// An identifier of the creator of this `VDSLogger`.
    let allowToPrint: Bool?
    let logToFile: Bool?
    let logToView: Bool?

    @objc
    public init(_ allowToPrint: Bool = true,
                logToFile: Bool = false,
                logToView: Bool = false) {
        self.allowToPrint = allowToPrint
        self.logToFile = logToFile
        self.logToView = logToView
    }

    func getLogHandler() -> [LogHandler] {
        var result = [LogHandler]()

        if let toPrint = allowToPrint, toPrint {
            result.append(ConsoleLogger.shared)
        }

        if let toFile = logToFile, toFile {
            result.append(FileLogger.shared)
        }

        if let toView = logToView, toView {
            result.append(ViewLogger.shared)
        }

        return result
    }
}

extension VDSLogger {
    /// Log a message passing the log level as a parameter.
    ///
    /// If the `logLevel` passed to this method is more severe than the `Logger`'s `logLevel`, it will be logged,
    /// otherwise nothing will happen.
    ///
    /// - parameters:
    ///    - level: The log level to log `message` at. For the available log levels, see `Logger.Level`.
    ///    - message: The message to be logged. `message` can be used with any string interpolation literal.
    ///    - metadata: One-off metadata to attach to this log message.
    ///    - source: The source this log messages originates to. Currently, it defaults to the folder containing the
    ///              file that is emitting the log message, which usually is the module.
    ///    - file: The file this log message originates from (there's usually no need to pass it explicitly as it
    ///            defaults to `#file`).
    ///    - function: The function this log message originates from (there's usually no need to pass it explicitly as
    ///                it defaults to `#function`).
    ///    - line: The line this log message originates from (there's usually no need to pass it explicitly as it
    ///            defaults to `#line`).
    ///    - fileName: The name of the file will be saved on disk, defaut by `VDSLogger.log`.
    public func log(level: Level,
                    _ message: String,
                    file: String = #file, function: String = #function, line: Int = #line, fileName: String = FileLogger.defaultFileName) {
        if logLevel <= level {
            let list = self.getLogHandler()
            if !list.isEmpty {
                list.forEach { (handler) in
                    handler.log(level, message: message,
                                file: file, function: function, line: line, fileName: fileName)
                }
            }
        }
    }
}

extension VDSLogger {

    /// Log a message passing with the `Level.debug` log level.
    ///
    /// If `.debug` is at least as severe as the `VDSLogger`'s `logLevel`, it will be logged,
    /// otherwise nothing will happen.
    ///
    /// - parameters:
    ///    - message: The message to be logged. `message` can be used with any string interpolation literal.
    ///    - metadata: One-off metadata to attach to this log message.
    ///    - source: The source this log messages originates to. Currently, it defaults to the folder containing the
    ///              file that is emitting the log message, which usually is the module.
    ///    - file: The file this log message originates from (there's usually no need to pass it explicitly as it
    ///            defaults to `#file`).
    ///    - function: The function this log message originates from (there's usually no need to pass it explicitly as
    ///                it defaults to `#function`).
    ///    - line: The line this log message originates from (there's usually no need to pass it explicitly as it
    ///            defaults to `#line`).
    @objc
    public func debug(_ message: String,
                      file: String = #file, function: String = #function, line: Int = #line) {
        log(level: .debug, message, file: file, function: function, line: line)
    }

    /// Log a message passing with the `Level.info` log level.
    ///
    /// If `.info` is at least as severe as the `VDSLogger`'s `logLevel`, it will be logged,
    /// otherwise nothing will happen.
    ///
    @objc
    public func info(_ message: String,
                     file: String = #file, function: String = #function, line: Int = #line, fileName: String = FileLogger.defaultFileName) {
        log(level: .info, message, file: file, function: function, line: line, fileName: fileName)
    }

    /// Log a message passing with the `Level.warning` log level.
    ///
    /// If `.warning` is at least as severe as the `VDSLogger`'s `logLevel`, it will be logged,
    /// otherwise nothing will happen.
    @objc
    public func warning(_ message: String,
                        file: String = #file, function: String = #function, line: Int = #line) {
        log(level: .warning, message, file: file, function: function, line: line)
    }

    /// Log a message passing with the `Level.error` log level.
    ///
    /// If `.error` is at least as severe as the `VDSLogger`'s `logLevel`, it will be logged,
    /// otherwise nothing will happen.
    ///
    @objc
    public func error(_ message: String,
                      file: String = #file, function: String = #function, line: Int = #line, fileName: String = FileLogger.defaultFileName) {
        log(level: .error, message, file: file, function: function, line: line, fileName: fileName)
    }
}
