//
//  String+Extension.swift
//  Utility
//
//  Created by thachlh on 24/4/26.
//

import Foundation

public extension String {
    func phoneNotContent84() -> String {
        var phoneNumber = self.convertPhoneNumber()
        if phoneNumber.count > 3 {
            if phoneNumber.hasPrefix("84") {
                phoneNumber = "0" + phoneNumber.dropFirst(2)
            }
        }
        return phoneNumber.trimmingCharacters(in: .whitespacesAndNewlines)
    }
    
    func convertPhoneNumber() -> String {
        return self.components(separatedBy: CharacterSet.decimalDigits.inverted).joined()
    }
    
    func getPhoneNumber() -> String {
        var phone = self
        
        let range = (phone as NSString).range(of: "(")
        if range.length > 0 {
            phone = (phone as NSString).substring(to: range.location)
        }
        
        phone = phone.components(separatedBy: CharacterSet.decimalDigits.inverted).joined(separator: "")
        
        phone = phone.replacingOccurrences(of: " ", with: "")
        phone = phone.replacingOccurrences(of: "-", with: "")
        phone = phone.replacingOccurrences(of: "(", with: "")
        phone = phone.replacingOccurrences(of: ")", with: "")
        phone = phone.replacingOccurrences(of: "+", with: "")
        phone = phone.replacingOccurrences(of: ".", with: "")
        phone = phone.replacingOccurrences(of: "-", with: "")
        return phone
    }
    
    func isNumeric() -> Bool {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        let number = formatter.number(from: self)
        return number != nil
    }
    
    func phoneContent84() -> String {
        let applePhoneNumber = "1234567899"
        if self == applePhoneNumber {
            return applePhoneNumber
        }
        
        var phoneNumber = getPhoneNumber()
        if phoneNumber.count > 3 {
            if !(((phoneNumber as NSString).substring(to: 2)) == "84") {
                phoneNumber = (phoneNumber as NSString).substring(from: 1)
                phoneNumber = "84\(phoneNumber)"
            }
        }
        return phoneNumber
    }
}
