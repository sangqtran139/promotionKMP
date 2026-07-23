//
//  PRMButtonShadowView.swift
//  PRMDesignKit
//
//  Created by thachlh on 20/4/26.
//

import UIKit

@IBDesignable
public class PRMLargeButtonShadowView: PRMShadowView {
    override func shadowToken() -> Shadow {
        return Shadows.tokenShadowButtonLarge
    }
}

@IBDesignable
public class PRMMediumButtonShadowView: PRMShadowView {
    override func shadowToken() -> Shadow {
        return Shadows.tokenShadowButtonMedium
    }
}

@IBDesignable
public class PRMSmallButtonShadowView: PRMShadowView {
    override func shadowToken() -> Shadow {
        return Shadows.tokenShadowButtonSmall
    }
}
