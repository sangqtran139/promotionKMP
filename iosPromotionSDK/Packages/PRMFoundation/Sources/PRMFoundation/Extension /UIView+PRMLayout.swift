
//
//  UIView+Extension.swift
//  Utility
//
//  Created by thachlh on 20/4/26.
//


import UIKit

public class PRMAnchorMaker {
    public let view: UIView
    
    public init(view: UIView) {
        self.view = view
        view.translatesAutoresizingMaskIntoConstraints = false
    }
    
    @discardableResult public func top(equalTo anchor: NSLayoutYAxisAnchor, constant: CGFloat = 0) -> PRMAnchorMaker {
        view.topAnchor.constraint(equalTo: anchor, constant: constant).isActive = true
        return self
    }
    
    @discardableResult public func bottom(equalTo anchor: NSLayoutYAxisAnchor, constant: CGFloat = 0) -> PRMAnchorMaker {
        view.bottomAnchor.constraint(equalTo: anchor, constant: constant).isActive = true
        return self
    }
    
    @discardableResult public func leading(equalTo anchor: NSLayoutXAxisAnchor, constant: CGFloat = 0) -> PRMAnchorMaker {
        view.leadingAnchor.constraint(equalTo: anchor, constant: constant).isActive = true
        return self
    }
    
    @discardableResult public func trailing(equalTo anchor: NSLayoutXAxisAnchor, constant: CGFloat = 0) -> PRMAnchorMaker {
        view.trailingAnchor.constraint(equalTo: anchor, constant: constant).isActive = true
        return self
    }
    
    @discardableResult public func centerX(equalTo anchor: NSLayoutXAxisAnchor, constant: CGFloat = 0) -> PRMAnchorMaker {
        view.centerXAnchor.constraint(equalTo: anchor, constant: constant).isActive = true
        return self
    }
    
    @discardableResult public func centerY(equalTo anchor: NSLayoutYAxisAnchor, constant: CGFloat = 0) -> PRMAnchorMaker {
        view.centerYAnchor.constraint(equalTo: anchor, constant: constant).isActive = true
        return self
    }
    
    @discardableResult public func width(equalTo constant: CGFloat) -> PRMAnchorMaker {
        view.widthAnchor.constraint(equalToConstant: constant).isActive = true
        return self
    }
    
    @discardableResult public func height(equalTo constant: CGFloat) -> PRMAnchorMaker {
        view.heightAnchor.constraint(equalToConstant: constant).isActive = true
        return self
    }
    
    @discardableResult public func width(equalTo dimension: NSLayoutDimension, multiplier: CGFloat = 1.0, constant: CGFloat = 0) -> PRMAnchorMaker {
        view.widthAnchor.constraint(equalTo: dimension, multiplier: multiplier, constant: constant).isActive = true
        return self
    }
    
    @discardableResult public func height(equalTo dimension: NSLayoutDimension, multiplier: CGFloat = 1.0, constant: CGFloat = 0) -> PRMAnchorMaker {
        view.heightAnchor.constraint(equalTo: dimension, multiplier: multiplier, constant: constant).isActive = true
        return self
    }
    
    @discardableResult public func leading(greaterThanOrEqualTo anchor: NSLayoutXAxisAnchor, constant: CGFloat = 0) -> PRMAnchorMaker {
        view.leadingAnchor.constraint(greaterThanOrEqualTo: anchor, constant: constant).isActive = true
        return self
    }
    
    @discardableResult public func trailing(lessThanOrEqualTo anchor: NSLayoutXAxisAnchor, constant: CGFloat = 0) -> PRMAnchorMaker {
        view.trailingAnchor.constraint(lessThanOrEqualTo: anchor, constant: constant).isActive = true
        return self
    }
    
    @discardableResult public func top(greaterThanOrEqualTo anchor: NSLayoutYAxisAnchor, constant: CGFloat = 0) -> PRMAnchorMaker {
        view.topAnchor.constraint(greaterThanOrEqualTo: anchor, constant: constant).isActive = true
        return self
    }
    
    @discardableResult public func bottom(lessThanOrEqualTo anchor: NSLayoutYAxisAnchor, constant: CGFloat = 0) -> PRMAnchorMaker {
        view.bottomAnchor.constraint(lessThanOrEqualTo: anchor, constant: constant).isActive = true
        return self
    }
    
    @discardableResult public func height(greaterThanOrEqualTo constant: CGFloat) -> PRMAnchorMaker {
        view.heightAnchor.constraint(greaterThanOrEqualToConstant: constant).isActive = true
        return self
    }
    
    @discardableResult public func size(_ size: CGSize) -> PRMAnchorMaker {
        view.widthAnchor.constraint(equalToConstant: size.width).isActive = true
        view.heightAnchor.constraint(equalToConstant: size.height).isActive = true
        return self
    }
    
    @discardableResult public func edges(to superview: UIView, insets: UIEdgeInsets = .zero) -> PRMAnchorMaker {
        view.topAnchor.constraint(equalTo: superview.topAnchor, constant: insets.top).isActive = true
        view.leadingAnchor.constraint(equalTo: superview.leadingAnchor, constant: insets.left).isActive = true
        view.bottomAnchor.constraint(equalTo: superview.bottomAnchor, constant: -insets.bottom).isActive = true
        view.trailingAnchor.constraint(equalTo: superview.trailingAnchor, constant: -insets.right).isActive = true
        return self
    }
    
    @discardableResult public func center(in superview: UIView, offset: CGPoint = .zero) -> PRMAnchorMaker {
        view.centerXAnchor.constraint(equalTo: superview.centerXAnchor, constant: offset.x).isActive = true
        view.centerYAnchor.constraint(equalTo: superview.centerYAnchor, constant: offset.y).isActive = true
        return self
    }
}

public extension UIView {
    func makeAnchor(_ closure: (PRMAnchorMaker) -> Void) {
        let maker = PRMAnchorMaker(view: self)
        closure(maker)
    }
    
    func removeAllConstraints() {
        if let superview = self.superview {
            let superviewConstraints = superview.constraints.filter { 
                ($0.firstItem as? UIView) == self || ($0.secondItem as? UIView) == self
            }
            NSLayoutConstraint.deactivate(superviewConstraints)
        }
        let selfConstraints = self.constraints.filter {
            ($0.firstItem as? UIView) == self && $0.secondItem == nil
        }
        NSLayoutConstraint.deactivate(selfConstraints)
    }
    
    func updateSizeConstraints(_ size: CGSize) {
        let widthConstraints = constraints.filter { $0.firstAttribute == .width && $0.secondItem == nil }
        let heightConstraints = constraints.filter { $0.firstAttribute == .height && $0.secondItem == nil }
        
        if let wc = widthConstraints.first { wc.constant = size.width }
        else { widthAnchor.constraint(equalToConstant: size.width).isActive = true }
        
        if let hc = heightConstraints.first { hc.constant = size.height }
        else { heightAnchor.constraint(equalToConstant: size.height).isActive = true }
    }
    
    func updateHeightConstraint(_ height: CGFloat) {
        let heightConstraints = constraints.filter { $0.firstAttribute == .height && $0.secondItem == nil }
        if let hc = heightConstraints.first {
            hc.constant = height
        } else {
            heightAnchor.constraint(equalToConstant: height).isActive = true
        }
    }
}
