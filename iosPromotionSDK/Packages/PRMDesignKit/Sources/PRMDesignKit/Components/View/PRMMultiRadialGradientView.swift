//
//  PRMMultiRadialGradientView.swift
//  PRMDesignKit
//
//  Created by thachlh on 8/5/26.
//

import UIKit

public class PRMMultiRadialGradientView: UIView {
    
    public struct RadialBlob {
        public var center: CGPoint
        public var radius: CGFloat
        public var color: UIColor
        public var alpha: CGFloat
        
        public init(center: CGPoint, radius: CGFloat, color: UIColor, alpha: CGFloat) {
            self.center = center
            self.radius = radius
            self.color  = color
            self.alpha  = alpha
        }
    }
    
    // MARK: - Config
    
    public var backgroundColor_: UIColor = UIColor(red: 0.98, green: 0.94, blue: 0.93, alpha: 1) {
        didSet { setNeedsDisplay() }
    }
    
    public var blobs: [RadialBlob] = [] {
        didSet { setNeedsDisplay() }
    }
    
    // MARK: - Init
    public override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = .clear
    }
    
    required public init?(coder: NSCoder) {
        super.init(coder: coder)
        backgroundColor = .clear
    }
    
    // MARK: - Draw
    public override func draw(_ rect: CGRect) {
        guard let context = UIGraphicsGetCurrentContext() else { return }

        context.setFillColor(backgroundColor_.cgColor)
        context.fill(rect)
        
        let w = rect.width
        let h = rect.height
        let maxDim = max(w, h)
        
        // Vẽ từng blob radial gradient
        for blob in blobs {
            let cx = blob.center.x * w
            let cy = blob.center.y * h
            let r  = blob.radius * maxDim
            
            guard let gradient = CGGradient(
                colorsSpace: CGColorSpaceCreateDeviceRGB(),
                colors: [
                    blob.color.withAlphaComponent(blob.alpha).cgColor,
                    blob.color.withAlphaComponent(0).cgColor
                ] as CFArray,
                locations: [0, 1]
            ) else { continue }
            
            context.saveGState()
            context.addRect(rect)
            context.clip()
            
            context.drawRadialGradient(
                gradient,
                startCenter: CGPoint(x: cx, y: cy), startRadius: 0,
                endCenter:   CGPoint(x: cx, y: cy), endRadius: r,
                options: [.drawsBeforeStartLocation, .drawsAfterEndLocation]
            )
            
            context.restoreGState()
        }
    }
}
