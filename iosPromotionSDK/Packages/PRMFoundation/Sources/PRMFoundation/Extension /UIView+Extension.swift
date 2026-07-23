//
//  UIView+Extension.swift
//  Utility
//
//  Created by thachlh on 20/4/26.
//

import UIKit

public extension UIView {
    func anchor(
        top: NSLayoutYAxisAnchor? = nil,
        bottom: NSLayoutYAxisAnchor? = nil,
        leading: NSLayoutXAxisAnchor? = nil,
        trailing: NSLayoutXAxisAnchor? = nil,
        padding: UIEdgeInsets = .zero
    ) {
        translatesAutoresizingMaskIntoConstraints = false
        var constraints: [NSLayoutConstraint] = []
        
        if let top = top {
            constraints.append(topAnchor.constraint(equalTo: top, constant: padding.top))
        }
        if let bottom = bottom {
            constraints.append(bottomAnchor.constraint(equalTo: bottom, constant: -padding.bottom))
        }
        if let leading = leading {
            constraints.append(leadingAnchor.constraint(equalTo: leading, constant: padding.left))
        }
        if let trailing = trailing {
            constraints.append(trailingAnchor.constraint(equalTo: trailing, constant: -padding.right))
        }
        
        NSLayoutConstraint.activate(constraints)
    }
    
    func fitSuperview(padding: UIEdgeInsets = .zero) {
        guard let superview = superview else { return }
        anchor(
            top: superview.topAnchor,
            bottom: superview.bottomAnchor,
            leading: superview.leadingAnchor,
            trailing: superview.trailingAnchor,
            padding: padding
        )
    }
    
    func pinEdges(to other: UIView, insets: UIEdgeInsets = UIEdgeInsets(top: 0, left: 0, bottom: 0, right: 0)) {
        translatesAutoresizingMaskIntoConstraints = false
        leadingAnchor.constraint(equalTo: other.leadingAnchor, constant: insets.left).isActive = true
        trailingAnchor.constraint(equalTo: other.trailingAnchor, constant: insets.right).isActive = true
        topAnchor.constraint(equalTo: other.topAnchor, constant: insets.top).isActive = true
        bottomAnchor.constraint(equalTo: other.bottomAnchor, constant: insets.bottom).isActive = true
    }
}
