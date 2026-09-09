//
//  NSObject+Extension.swift
//  Utility
//
//  Created by thachlh on 24/4/26.
//

import Foundation

public extension NSObject {
    func toJsonString() -> String {
        do {
            let jsonData = try JSONSerialization.data(withJSONObject: self,
                                                      options: .prettyPrinted)
            return String(data: jsonData, encoding: .utf8) ?? ""
        } catch {
            return ""
        }
    }
}

public extension Encodable {
    /// Converting object to postable JSON
    func toJsonString(_ encoder: JSONEncoder = JSONEncoder()) -> String {
        do {
            let data = try encoder.encode(self)
            let result = String(decoding: data, as: UTF8.self)
            return result
        } catch {
            return ""
        }
    }
}

public extension Encodable {
    func toDictionary(_ encoder: JSONEncoder = JSONEncoder()) -> [String: Any] {
        do {
            encoder.keyEncodingStrategy = .useDefaultKeys
            let data = try encoder.encode(self)
            let object = try JSONSerialization.jsonObject(with: data)
            guard let json = object as? [String: Any] else {
                let context = DecodingError.Context(codingPath: [], debugDescription: "Deserialized object is not a dictionary")
                throw DecodingError.typeMismatch(type(of: object), context)
            }
            return json
        } catch {
            return [:]
        }
    }
}
