//
//  PassThroughView.swift
//  CoreUIKit
//
//  Created by Natariannn on 8/7/20.
//  Copyright © 2020 ViettelPay App Team. All rights reserved.
//

import UIKit

public class PassThroughView: UIView {
    public override func point(inside point: CGPoint, with event: UIEvent?) -> Bool {
        for subview in subviews {
            if !subview.isHidden && subview.isUserInteractionEnabled && subview.point(inside: convert(point, to: subview), with: event) {
                return true
            }
        }
        return false
    }
}
