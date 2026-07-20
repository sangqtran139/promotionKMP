//  Array+Extension.swift
//  Utility
//
//  Created by thachlh on 23/4/26.
//

import UIKit
import RxSwift
import RxCocoa

public extension Reactive where Base: UIControl {
    var tap: ControlEvent<Void> {
        controlEvent(.touchUpInside)
    }
}
