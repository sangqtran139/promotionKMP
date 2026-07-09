//
//  VDSButtonShadowView.swift
//  CoreUI
//
//  Created by thachlh on 20/4/26.
//

import UIKit

@IBDesignable
public class VDSLargeButtonShadowView: VDSShadowView {
    override func shadowToken() -> Shadow {
        return Shadows.tokenShadowButtonLarge
    }
}

@IBDesignable
public class VDSMediumButtonShadowView: VDSShadowView {
    override func shadowToken() -> Shadow {
        return Shadows.tokenShadowButtonMedium
    }
}

@IBDesignable
public class VDSSmallButtonShadowView: VDSShadowView {
    override func shadowToken() -> Shadow {
        return Shadows.tokenShadowButtonSmall
    }
}
