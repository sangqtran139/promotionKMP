//
//  FileLogger.swift
//  Utility
//
//  Created by Nguyen Ngoc Minh on 10/5/20.
//  Copyright © 2020 ViettelPay App Team. All rights reserved.
//

import Foundation

public class FileLogger: NSObject, LogHandler {
    
    public static let defaultFileName = "VDSLogger.log"

    class var shared: FileLogger {
        struct StaticView {
            static let instance: FileLogger = FileLogger()
        }
        return StaticView.instance
    }

    var logFileURL: [URL]? = []
    var syncAfterEachWrite: Bool = false
    let fileManager = FileManager.default

    func checkFileExisting(fileName: String, _ completion: @escaping (URL?) -> Void) {
        if let logFileURL = logFileURL, let linkFile = logFileURL.first(where: { savedURL in
            return savedURL.lastPathComponent.contains(fileName)
        }) {
            completion(linkFile)
        } else {
            /// platform-dependent logfile directory default
            var baseURL: URL?
            #if os(OSX)
            if let url = fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first {
                baseURL = url
                /// try to use ~/Library/Caches/APP NAME instead of ~/Library/Caches
                if let appName = Bundle.main.object(forInfoDictionaryKey: "CFBundleExecutable") as? String {
                    do {
                        if let appURL = baseURL?.appendingPathComponent(appName, isDirectory: true) {
                            try fileManager.createDirectory(at: appURL,
                                                            withIntermediateDirectories: true, attributes: nil)
                            baseURL = appURL
                        }
                    } catch {
                        print("Warning! Could not create folder /Library/Caches/\(appName)")
                    }
                }
            }
            #else
            #if os(Linux)
            baseURL = URL(fileURLWithPath: "/var/cache")
            #else
            /// iOS, watchOS, etc. are using the caches directory
            if let url = fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first {
                baseURL = url
            }
            #endif
            #endif
            let fileURL = baseURL?.appendingPathComponent(fileName, isDirectory: false)
            if let fileURL = fileURL {
                logFileURL?.append(fileURL)
            }
            completion(fileURL)
        }
    }

    // MARK: - LogHandler Protocol
    public var logLevel: Level  = .debug

    public func log(_ level: Level, message: String, file: String, function: String, line: Int, fileName: String = FileLogger.defaultFileName) {
        checkFileExisting(fileName: fileName) { [weak self] (fileURL) -> Void in
            guard let self = self else { return }
            if let url = fileURL {
                var text = ""
                text.append(contentsOf: "\(level.timeStamp) -> ")
                text.append(contentsOf: "\(message.map({"\($0)"}).joined(separator: "")) \n ")

//                if !function.isEmpty {
//                    text.append(contentsOf: "\(function) -> ")
//                }
//                if !file.isEmpty {
//                    text.append(contentsOf: "\(file) -> ")
//                }
//                text.append(contentsOf: "\(line)!\n")

                guard let data = text.data(using: String.Encoding.utf8) else {
                    return
                }

                // check file size and then save over data
                if self.checkFileSize(fileName: fileName) {
                    if self.deleteLogFile(fileURL: url) {
                        debugPrint("Success")
                    }
                }
                _ = self.write(data: data, to: url)
            }
        }
    }

    // MARK: - Private Func
    private func deleteLogFile(fileURL: URL) -> Bool {
        guard fileManager.fileExists(atPath: fileURL.path) == true else {
            return false
        }
        do {
            try fileManager.removeItem(at: fileURL)
            return true
        } catch {
            debugPrint("VDSLogger could not remove file \(fileURL).")
            return false
        }
    }

    private func checkFileSize(fileName: String) -> Bool {
        var text = ""
        var result = [String]()

        if let dir = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first {
            let fileURL = dir.appendingPathComponent(fileName)
            do {
                text = try String(contentsOf: fileURL, encoding: .utf8)
            }
            catch {/* error handling here */}
        }
        if !text.isEmpty {
            result = text.components(separatedBy: "\n")
        }
        if result.count > 5120 {
            /// delete file when it have more 5120 records
            return true
        }
        return false
    }

    private func write(data: Data, to url: URL) -> Bool {
        var success = false
        let coordinator = NSFileCoordinator(filePresenter: nil)
        var error: NSError?
        coordinator.coordinate(writingItemAt: url, error: &error) { url in
            do {
                if fileManager.fileExists(atPath: url.path) == false {

                    let directoryURL = url.deletingLastPathComponent()
                    if fileManager.fileExists(atPath: directoryURL.path) == false {
                        try fileManager.createDirectory(
                            at: directoryURL,
                            withIntermediateDirectories: true
                        )
                    }
                    fileManager.createFile(atPath: url.path, contents: nil)

                    #if os(iOS) || os(watchOS)
                    if #available(iOS 10.0, watchOS 3.0, *) {
                        var attributes = try fileManager.attributesOfItem(atPath: url.path)
                        attributes[FileAttributeKey.protectionKey] = FileProtectionType.none
                        try fileManager.setAttributes(attributes, ofItemAtPath: url.path)
                    }
                    #endif
                }

                let fileHandle = try FileHandle(forWritingTo: url)
                fileHandle.seekToEndOfFile()
                fileHandle.write(data)
                if syncAfterEachWrite {
                    fileHandle.synchronizeFile()
                }
                fileHandle.closeFile()
                success = true
            } catch {
                debugPrint("VDSLogger could not write to file \(url).")
            }
        }

        if let error = error {
            debugPrint("Failed writing file with error: \(String(describing: error))")
            return false
        }

        return success
    }
}
