//
//  ViewLogger.swift
//  Utility
//
//  Created by Nguyen Ngoc Minh on 10/7/20.
//  Copyright © 2020 ViettelPay App Team. All rights reserved.
//

import UIKit

public class ViewLogger: LogHandler {

    var overlayView = UIView()
    var textView: UITextView?

    var addMore = false
    var strLogging: String?
    let sizeView = 255.0
    let sizeButton: CGFloat = 15.0

    var keyWindow: UIWindow? {
        if let app = UIApplication.shared.delegate, let window = app.window {
            return window
        }
        return UIWindow()
    }

    class var shared: ViewLogger {
        struct StaticView {
            static let instance: ViewLogger = ViewLogger()
        }
        return StaticView.instance
    }

    private func showOverlay(_ str: String) {
        if addMore, let currentTextView = textView {
            let text = currentTextView.text + "\(str)"
            currentTextView.text = text
        } else {
            addMore = true
            if let window = keyWindow {
                strLogging = str
                overlayView.frame = CGRect(x: 0.0, y: 0.0, width: sizeView, height: sizeView)
                overlayView.center = CGPoint(x: window.frame.width / 2.0, y: window.frame.height / 2.0)

                overlayView.layer.cornerRadius = 10.0
                overlayView.backgroundColor = UIColor.white
                overlayView.clipsToBounds = true

                overlayView.layer.borderColor = UIColor.gray.cgColor
                overlayView.layer.borderWidth = 1.0

                /// Content of file logger
                textView = UITextView(frame: CGRect(x: 0.0, y: 5.0,
                                                    width: sizeView - 5.0,
                                                    height: sizeView - 5.0))
                textView?.text = str
                textView?.font = UIFont.systemFont(ofSize: 12)
                textView?.textColor = UIColor.black
                textView?.isEditable = false

                /// Button to hidden overlayview
                let close = UIButton(frame: CGRect(x: overlayView.frame.width - 17.0, y: 3.0,
                                                   width: sizeButton, height: sizeButton))

                close.setImage(UIImage(named: "close",
                                       in: Bundle(for: type(of:self)),
                                       compatibleWith: nil),
                               for: UIControl.State.normal)
                close.addTarget(self, action: #selector(hideOverlayView(_:)), for: .touchDown)

                overlayView.addSubview(textView!)
                overlayView.addSubview(close)
                window.addSubview(overlayView)

                let pangesture = UIPanGestureRecognizer()
                pangesture.addTarget(self, action: #selector(handlePan(_:)))
                overlayView.addGestureRecognizer(pangesture)
            }
        }
    }

    @objc
    private func hideOverlayView(_ sendder: UIButton) {
        DispatchQueue.main.async {
            self.overlayView.removeFromSuperview()
        }
    }

    @objc
    private func handlePan(_ gestureRecognizer: UIPanGestureRecognizer) {
        if gestureRecognizer.state == .began || gestureRecognizer.state == .changed {
            if let gesView = gestureRecognizer.view {
                UIView.animate(withDuration: 0.1) {
                    let translation = gestureRecognizer.translation(in: self.overlayView)
                    gesView.center = CGPoint(x: gesView.center.x + translation.x,
                                             y: gesView.center.y + translation.y)
                    gestureRecognizer.setTranslation(CGPoint.zero, in: self.overlayView)
                }
            }
        }
    }

    // MARK: - LogHandler Protocol
    public var logLevel: Level = .debug

    public func log(_ level: Level, message: String, file: String, function: String, line: Int, fileName: String = FileLogger.defaultFileName) {
        var text = ""
        text.append(contentsOf: "\(level.timeStamp) -> ")
        text.append(contentsOf: "\(message.map({"\($0)"}).joined(separator: "")) -> ")

        if !function.isEmpty {
            text.append(contentsOf: "\(function) -> ")
        }
        if !file.isEmpty {
            text.append(contentsOf: "\(file) -> ")
        }
        text.append(contentsOf: "\(line)!\n")

        showOverlay(text)
    }
}
