//
//  PRMTapableView.swift
//  PRMDesignKit
//
//  Created by thachlh on 21/4/26.
//

import UIKit

open class PRMTapableView: UIControl {
    @IBInspectable public var scaleOnHighlight: CGFloat = 0.8
    @IBInspectable public var alphaTouchBegan: CGFloat = 0.6
    @IBInspectable public var delayEventDuration: Double = 0.5
    @IBInspectable public var interactiveInset: UIEdgeInsets = .zero
    private var isPreventTouch = false

    open override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        if self.isPreventTouch {
            return
        }

        super.touchesBegan(touches, with: event)
        self.animate(alpha: alphaTouchBegan, scale: self.scaleOnHighlight)
    }

    open override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        if self.isPreventTouch {
            return
        }

        self.animate(alpha: 1, scale: 1)

        // `guard` chứ không phải `touches.first!`: `Set` rỗng là hợp lệ về kiểu, và crash ở đây
        // là **SDK làm crash app host** ngay trong lúc user chạm màn hình.
        guard let location = touches.first?.location(in: self) else { return }
        if self.bounds.inset(by: self.interactiveInset).contains(location) {
            self.sendActions(for: .touchUpInside)
            self.disableOverrideInteractiveFor(seconds: delayEventDuration)
        }
    }

    open override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        if self.isPreventTouch {
            return
        }

        super.touchesCancelled(touches, with: event)
        self.animate(alpha: 1, scale: 1)
    }

    private func animate(alpha: CGFloat, scale: CGFloat) {
        UIView.animate(withDuration: 0.3) {
            self.alpha = alpha
            if scale == 1 {
                self.transform = .identity
            } else {
                self.transform = CGAffineTransform.init(scaleX: scale, y: scale)
            }
        }
    }

    private func disableOverrideInteractiveFor(seconds: Double) {
        if seconds == 0 {
            return
        }

        self.isPreventTouch = true
        DispatchQueue.main.asyncAfter(deadline: .now() + seconds) {
            self.isPreventTouch = false
        }
    }

    public override func point(inside point: CGPoint, with event: UIEvent?) -> Bool {
        return self.bounds.inset(by: self.interactiveInset).contains(point)
    }
}
