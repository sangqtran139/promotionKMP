//
//  CouponBackgroundView.swift
//  CoreUI
//
//  Created by thachlh on 20/4/26.
//

import UIKit

public class CouponBackgroundView: UIView {
    
    // MARK: - Cutout Orientation
    
    public enum CutoutOrientation {
        case topBottom
        case leftRight
    }
    
    // MARK: - Properties
    
    public var cutoutOrientation: CutoutOrientation = .topBottom {
        didSet { setNeedsPathUpdate() }
    }
    
    public var cornerRadius: CGFloat = 12.0 {
        didSet { setNeedsPathUpdate() }
    }
    
    public var cutoutRadius: CGFloat = 10.0 {
        didSet { setNeedsPathUpdate() }
    }
    
    public var dashPositionRatio: CGFloat = 0.28 {
        didSet { setNeedsPathUpdate() }
    }
    
    public var absoluteDashPosition: CGFloat? {
        didSet { setNeedsPathUpdate() }
    }
    
    public var dashColor: UIColor = UIColor.lightGray.withAlphaComponent(0.6) {
        didSet { dashedLineLayer.strokeColor = dashColor.cgColor }
    }
    
    public var fillColor: UIColor = .white {
        didSet { updateColors() }
    }
    
    public var showsDashedLine: Bool = true {
        didSet {
            dashedLineLayer.isHidden = !showsDashedLine
        }
    }
    
    public var showsShadow: Bool = true {
        didSet {
            layer.shadowOpacity = showsShadow ? 0.1 : 0
        }
    }
    
    private let cardLayer = CAShapeLayer()
    private let dashedLineLayer = CAShapeLayer()
    
    // MARK: - Cache
    
    private var lastBounds: CGRect = .zero
    private var needsPathUpdate = true
    
    // MARK: - Init
    
    public override init(frame: CGRect) {
        super.init(frame: frame)
        setupLayer()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupLayer()
    }
    
    private func setupLayer() {
        backgroundColor = .clear
        
        cardLayer.fillColor = fillColor.cgColor
        layer.addSublayer(cardLayer)
        
        dashedLineLayer.strokeColor = dashColor.cgColor
        dashedLineLayer.fillColor = UIColor.clear.cgColor
        dashedLineLayer.lineWidth = 1.5
        dashedLineLayer.lineDashPattern = [4, 4]
        layer.addSublayer(dashedLineLayer)
        
        layer.shadowColor = UIColor.black.cgColor
        layer.shadowOpacity = 0.08
        layer.shadowOffset = CGSize(width: 0, height: 4)
        layer.shadowRadius = 12
    }
    
    // MARK: - Layout
    
    public override func layoutSubviews() {
        super.layoutSubviews()
        
        guard bounds.width > 0, bounds.height > 0 else { return }
        
        if bounds != lastBounds || needsPathUpdate {
            rebuildPaths()
            lastBounds = bounds
            needsPathUpdate = false
        }
        
        updateColors()
    }
    
    private func setNeedsPathUpdate() {
        needsPathUpdate = true
        setNeedsLayout()
    }
    
    private func rebuildPaths() {
        switch cutoutOrientation {
        case .topBottom:
            buildTopBottomPath()
        case .leftRight:
            buildLeftRightPath()
        }
    }
    
    // MARK: - Top/Bottom Cutout Path
    
    private func buildTopBottomPath() {
        let width = bounds.width
        let height = bounds.height
        let dashX = absoluteDashPosition ?? (width * dashPositionRatio)
        

        let path = UIBezierPath()
        
        path.move(to: CGPoint(x: cornerRadius, y: 0))
        path.addLine(to: CGPoint(x: dashX - cutoutRadius, y: 0))
        
        path.addArc(withCenter: CGPoint(x: dashX, y: 0),
                    radius: cutoutRadius,
                    startAngle: .pi,
                    endAngle: 0,
                    clockwise: false)
        
        path.addLine(to: CGPoint(x: width - cornerRadius, y: 0))
        
        path.addArc(withCenter: CGPoint(x: width - cornerRadius, y: cornerRadius),
                    radius: cornerRadius,
                    startAngle: -(.pi / 2),
                    endAngle: 0,
                    clockwise: true)
        
        path.addLine(to: CGPoint(x: width, y: height - cornerRadius))
        
        path.addArc(withCenter: CGPoint(x: width - cornerRadius, y: height - cornerRadius),
                    radius: cornerRadius,
                    startAngle: 0,
                    endAngle: .pi / 2,
                    clockwise: true)
        
        path.addLine(to: CGPoint(x: dashX + cutoutRadius, y: height))
        
        path.addArc(withCenter: CGPoint(x: dashX, y: height),
                    radius: cutoutRadius,
                    startAngle: 0,
                    endAngle: .pi,
                    clockwise: false)
        
        path.addLine(to: CGPoint(x: cornerRadius, y: height))
        
        path.addArc(withCenter: CGPoint(x: cornerRadius, y: height - cornerRadius),
                    radius: cornerRadius,
                    startAngle: .pi / 2,
                    endAngle: .pi,
                    clockwise: true)
        
        path.addLine(to: CGPoint(x: 0, y: cornerRadius))
        
        path.addArc(withCenter: CGPoint(x: cornerRadius, y: cornerRadius),
                    radius: cornerRadius,
                    startAngle: .pi,
                    endAngle: 3 * .pi / 2,
                    clockwise: true)
        
        path.close()
        
        cardLayer.path = path.cgPath
        layer.shadowPath = path.cgPath
        

        let linePath = UIBezierPath()
        linePath.move(to: CGPoint(x: dashX, y: cutoutRadius))
        linePath.addLine(to: CGPoint(x: dashX, y: height - cutoutRadius))
        
        dashedLineLayer.path = linePath.cgPath
    }
    
    // MARK: - Left/Right Cutout Path
    
    private func buildLeftRightPath() {
        let width = bounds.width
        let height = bounds.height
        let dashY = absoluteDashPosition ?? (height * dashPositionRatio)
        
        let path = UIBezierPath()
        
        path.move(to: CGPoint(x: cornerRadius, y: 0))
        path.addLine(to: CGPoint(x: width - cornerRadius, y: 0))
        

        path.addArc(withCenter: CGPoint(x: width - cornerRadius, y: cornerRadius),
                    radius: cornerRadius,
                    startAngle: -(.pi / 2),
                    endAngle: 0,
                    clockwise: true)
        

        path.addLine(to: CGPoint(x: width, y: dashY - cutoutRadius))
        

        path.addArc(withCenter: CGPoint(x: width, y: dashY),
                    radius: cutoutRadius,
                    startAngle: -(.pi / 2),
                    endAngle: .pi / 2,
                    clockwise: false)
        

        path.addLine(to: CGPoint(x: width, y: height - cornerRadius))
        

        path.addArc(withCenter: CGPoint(x: width - cornerRadius, y: height - cornerRadius),
                    radius: cornerRadius,
                    startAngle: 0,
                    endAngle: .pi / 2,
                    clockwise: true)
        

        path.addLine(to: CGPoint(x: cornerRadius, y: height))
        

        path.addArc(withCenter: CGPoint(x: cornerRadius, y: height - cornerRadius),
                    radius: cornerRadius,
                    startAngle: .pi / 2,
                    endAngle: .pi,
                    clockwise: true)
        

        path.addLine(to: CGPoint(x: 0, y: dashY + cutoutRadius))
        

        path.addArc(withCenter: CGPoint(x: 0, y: dashY),
                    radius: cutoutRadius,
                    startAngle: .pi / 2,
                    endAngle: -(.pi / 2),
                    clockwise: false)
        

        path.addLine(to: CGPoint(x: 0, y: cornerRadius))
        

        path.addArc(withCenter: CGPoint(x: cornerRadius, y: cornerRadius),
                    radius: cornerRadius,
                    startAngle: .pi,
                    endAngle: 3 * .pi / 2,
                    clockwise: true)
        
        path.close()
        
        cardLayer.path = path.cgPath
        layer.shadowPath = path.cgPath
        

        let linePath = UIBezierPath()
        linePath.move(to: CGPoint(x: cutoutRadius, y: dashY))
        linePath.addLine(to: CGPoint(x: width - cutoutRadius, y: dashY))
        
        dashedLineLayer.path = linePath.cgPath
    }
    
    // MARK: - Color
    
    private func updateColors() {
        if #available(iOS 13.0, *) {
            traitCollection.performAsCurrent {
                cardLayer.fillColor = fillColor.cgColor
                dashedLineLayer.strokeColor = dashColor.cgColor
            }
        } else {
            cardLayer.fillColor = fillColor.cgColor
            dashedLineLayer.strokeColor = dashColor.cgColor
        }
    }
}
