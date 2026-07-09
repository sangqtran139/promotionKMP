//
//  CommonUtil.swift
//  CoreUtilsKit
//
//  Created by Chi Nguyen on 23/12/2022.
//  Copyright © 2022 ViettelPay App Team. All rights reserved.
//

import Foundation
import UIKit

public class CommonUtil {
    public static let shared = CommonUtil()
    public var shouldTrackingNetwork = false
    public let baseDeeplinkUrl = ["vtpay.page.link",
                                  "viettelpay.onelink.me",
                                  "viettelpay.vn",
                                  "vt.viettelpay.vn",
                                  "vtmoney.onelink.me",
                                  "km.vtmoney.vn",
                                  "viettelmoney.onelink.me",
                                  "km.viettelmoney.vn",
                                  "vtmoney.go.link",
                                  "qhlp.adj.st",
                                  "viettelmoney.go.link",
                                  "vtmoneyuat.go.link",
                                  "vtmoneysb.go.link",
                                  "vtmstaging.go.link"]
    
    public func adjustDomain(withoutSlash: Bool = false) -> String {
        let bundleId = Bundle.main.bundleIdentifier ?? ""
        let suffix = withoutSlash ? "" : "/"

        if bundleId.contains("staging-3") {
            return "vtmoneysb.go.link" + suffix
        }
        if bundleId.contains("staging-2") {
            return "vtmstaging.go.link" + suffix
        }
        if bundleId.contains("uat") {
            return "vtmoneyuat.go.link" + suffix
        }
        return "viettelmoney.go.link" + suffix
    }
    
    public let defaultErrorMessage = "Bạn vui lòng đợi vài phút trước khi thử lại nhé."
    
    public let blServiceApple = [
        "VTCBAV","TRUONGSA","THGAMOTA","MOBILOTT","BORROW","PHONGTHUY","VAY_TIEN","NTTOPI",
        "TSONLINEVT","EASYREG","LUCKYBEST","MUA_VE_VIETLOTT","ETC","NAP_TIEN_DICH_VU","YMM",
        "VLTSMS","TET2022","THIEN_NGUYEN","BAOHIEMVT",
        "PHUHUNGLIFE","GENERALI","TT_TIENBAOHIEM","DIGITALKINGDOM",
        "TRAITIM", "UNGHO", "BHTD","GAMEMEO2023","DAOVANG","EVENT_HUB_2024","GAME",
        "VINESEDU","MISAEDU", "PAYSSC", "DTSOFT", "INTERLAND", "HELISOFT", "ELSA2", "BSCEDU",// học phí
        "TOANCAUEDU", "JETPAYEDU", "PMSEDU", "DTSOFTEDU", "UNISOFTEDU", "UVEDU", "VTECH", "HOCMAI2", "TVUEDU", "VICTORYTVHEDU",// học phí
        "TH_DOANH_NGHIEP", "DAUKHIMN1", "TNFRANCHI", "GHN1", "MRSPEEDY3", "AHAMOVE", // thu hộ doanh nghiệp
        "UNICA1", "WAKA", // học phí
        "BAOHIEM123", "PRUDENTIAL", "THBVAG", "FWDINS", "MBAL", "HANWHALIFE", // Thanh toán phí bảo hiểm
        "SAVENOW", // đầu tư tài chính
        "NAPNGAY", // nạp thẻ game
        "AUTO_RECHARGE", // Nạp tự động
        "GAME_PORTAL",
        "TRUYEN_HINH", "000003", "KPLUS", "KPLUS2", "THFPTPLAY", "AVG", "HDVEGACLIP1", "THHTVC", "VTVCAB", "VTVCABON",
        "DVCTTHC", "DVCVPHC", "AMIGO", "PVOIL", "DVC", "UNIVERSAL_SIM", "MONEYMARKET",
        "VFCAKE", "PLBCAKE", "CLBCAKE", "CCBCAKE", "SABCAKE", "PRBCAKE", "PAYDAY", "CLBMCD", "HM_MCREDIT", "EASYCRD", "FSTMNY", "TIN_VAY", // vay
        "TT_KHOANVAY", "SHBFINANCE", "TOYOTA", "HCDEBT", "MIRAEASSET", "FEVPBC", "FECARD", "FECRDT", "FEVP", "FEGALAXY", "ACSCRDT",
        "CFCBILLING", "OCBCRDT", "MCREDIT", "SHINHAN", "PTFINANCE", "TPFICO", "EASYCRDT", "LOTTE", "FCCOM" // thanh toan khoan vay
    ]

    public let appUrl = "https://apps.apple.com/vn/app/viettelpay/id1344204781?l=vi"
    
    public let blGroupCode = ["TT_KHOANVAY", "TRUYEN_HINH"]
    
    public let blGroupNameApple = ["Học phí", "Truyền hình"]

    public func isEnterpriseBundle() -> Bool {
        return Bundle.main.bundleIdentifier?.contains("enterprise") == true
    }
    
    public func isStoreBundle() -> Bool {
        return Bundle.main.bundleIdentifier == "com.viettel.viettelpay"
    }
    
    public func safeAreaInsetTop() -> CGFloat {
        if #available(iOS 11.0, *) {
            let topHeight = UIApplication.shared.keyWindow?.safeAreaInsets.top ?? 20.0
            return topHeight < 20.0 ? 20.0 : topHeight
        } else {
            let topHeight = UIApplication.shared.statusBarFrame.size.height
            return topHeight < 20.0 ? 20.0 : topHeight
        }
    }
    
    public func goToStoreLink() {
        guard let url = URL(string: appUrl),
            UIApplication.shared.canOpenURL(url) else { return }
        if #available(iOS 10, *) {
            UIApplication.shared.open(url)
        } else {
            UIApplication.shared.openURL(url)
        }
    }
    
    @objc public static func shouldShowForceUpdate(minVersionSupport: String) -> Bool {
        let appVersion = VDSAppHelper.appVersion()
        if !minVersionSupport.isEmpty && VDSAppHelper.compareVersions(appVersion, minVersionSupport) == .lessThan {
            return true
        } else {
            return false
        }
    }
}
