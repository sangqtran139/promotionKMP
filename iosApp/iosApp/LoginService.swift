//
//  LoginService.swift
//  PromotionSDKDemo
//
//  CHỈ dùng cho app demo: gọi API đăng nhập ViettelMoney để lấy `accessToken` thật rồi truyền
//  vào SDK (`PromotionSDK.initialize(customerId:accessToken:...)`). Ngoài đời việc này do app HOST làm;
//  đây chỉ là giả lập để test SDK với token/BFF thật.
//
//  Luồng 2 bước (BE yêu cầu):
//    1) POST với `requestId = ""`  → server trả `status.code = AUT0014` (cần OTP) + `data.requestId`.
//    2) POST lại với `requestId = <data.requestId ở bước 1>` (giữ nguyên OTP) → `status.code = "00"`
//       + `data.accessToken` (Bearer token cho SDK) + `data.username` (msisdn).
//
//  ⚠️ API là HTTP + IP → cần ngoại lệ ATS trong Info.plist (đã thêm cho 125.235.38.229).
//

import Foundation

enum LoginError: Error {
    case invalidResponse
    case server(code: String, message: String)   // status.code != "00"/AUT0014
    case missingRequestId                          // bước 1 không trả requestId
    case missingToken                              // bước 2 không trả accessToken
}

/// Kết quả đăng nhập cần cho SDK.
struct LoginResult {
    let accessToken: String
    let username: String   // msisdn — dùng làm customerId truyền vào SDK
}

final class LoginService {

    static let shared = LoginService()
    private init() {}

    // MARK: - Cấu hình demo (sửa nhanh tại đây) — hardcode y theo curl BE.

    private let endpoint = URL(string: "http://125.235.38.229:8080/auth/v1/authn/login")!
    private let msisdn = "84983725525"
    private let pin = "123123"
    private let otp = "1111"
    private let imei = "VTP_5DC1C998734F3128B0024A36EDE94C6E"
    private let deviceId = "VTP_5DC1C998734F3128B0024A36EDE94C6E"
    private let publicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEtawmGR2BzRkEqfu/R7O23txmSssU0wzZwh0BZT/GkBOUvi7tjzKhyjUZdM5tJrjax8Gq+iSwlBn/sIdLKD6giw=="

    private let session = URLSession(configuration: .default)

    // MARK: - Public

    /// Đăng nhập 2 bước, trả `accessToken` + `username` (msisdn). `completion` chạy trên MAIN thread.
    func login(completion: @escaping (Result<LoginResult, Error>) -> Void) {
        // Bước 1: requestId rỗng → lấy requestId từ response.
        post(requestId: "") { [weak self] result in
            guard let self else { return }
            switch result {
            case .failure(let error):
                self.finish(completion, .failure(error))
            case .success(let response):
                // BE có thể hoàn tất ngay ở bước 1 (`code = "00"` + accessToken, KHÔNG trả requestId)
                // — luồng hiện tại của server. Khi đó bỏ qua bước 2.
                if response.status?.code == "00",
                   let token = response.data?.accessToken, !token.isEmpty {
                    let username = response.data?.username ?? self.msisdn
                    self.finish(completion, .success(LoginResult(accessToken: token, username: username)))
                    return
                }
                // Ngược lại: BE yêu cầu OTP (`AUT0014`) → phải có requestId để đi bước 2.
                guard let requestId = response.data?.requestId, !requestId.isEmpty else {
                    self.finish(completion, .failure(LoginError.missingRequestId))
                    return
                }
                // Bước 2: dùng lại requestId để hoàn tất đăng nhập.
                self.post(requestId: requestId) { result2 in
                    switch result2 {
                    case .failure(let error):
                        self.finish(completion, .failure(error))
                    case .success(let response2):
                        guard response2.status?.code == "00" else {
                            self.finish(completion, .failure(LoginError.server(
                                code: response2.status?.code ?? "?",
                                message: response2.status?.message ?? "Đăng nhập thất bại")))
                            return
                        }
                        guard let token = response2.data?.accessToken, !token.isEmpty else {
                            self.finish(completion, .failure(LoginError.missingToken))
                            return
                        }
                        let username = response2.data?.username ?? self.msisdn
                        self.finish(completion, .success(LoginResult(accessToken: token, username: username)))
                    }
                }
            }
        }
    }

    // MARK: - Private

    private func post(requestId: String, completion: @escaping (Result<LoginResponse, Error>) -> Void) {
        var request = URLRequest(url: endpoint)
        request.httpMethod = "POST"
        headers().forEach { request.setValue($0.value, forHTTPHeaderField: $0.key) }
        request.httpBody = try? JSONSerialization.data(withJSONObject: body(requestId: requestId))

        session.dataTask(with: request) { data, _, error in
            if let error { return completion(.failure(error)) }
            guard let data,
                  let decoded = try? JSONDecoder().decode(LoginResponse.self, from: data) else {
                return completion(.failure(LoginError.invalidResponse))
            }
            completion(.success(decoded))
        }.resume()
    }

    private func headers() -> [String: String] {
        [
            "accept-language": "vi",
            "app-version": "8.8.55-staging-DEBUG",
            "authority-party": "APP",
            "channel": "APP",
            "content-type": "application/json; charset=UTF-8",
            "device-id": deviceId,
            "device-model": "iPhone",
            "device-type": "Mobile",
            "imei": imei,
            "os-version": "16",
            "product": "VIETTELPAY",
            "trouble-shooting-id": "",
            "type-os": "ios",
            "x-request-id": Self.timestampId()
        ]
    }

    private func body(requestId: String) -> [String: Any] {
        [
            "checksumAccountInfo": "",
            "checksumSetting": "",
            "imei": imei,
            "loginType": "BASIC",
            "msisdn": msisdn,
            "otp": otp,
            "pin": pin,
            "publicKey": publicKey,
            "requestId": requestId,
            "typeOs": "ios",
            "userType": "msisdn",
            "username": msisdn
        ]
    }

    /// `x-request-id` dạng timestamp `yyyyMMddHHmmss` (mới mỗi lần gọi, tránh server dedup).
    private static func timestampId() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyyMMddHHmmss"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        return formatter.string(from: Date())
    }

    private func finish(_ completion: @escaping (Result<LoginResult, Error>) -> Void,
                        _ result: Result<LoginResult, Error>) {
        DispatchQueue.main.async { completion(result) }
    }
}

// MARK: - DTO response

private nonisolated struct LoginResponse: Decodable {
    let status: Status?
    let data: DataPayload?

    struct Status: Decodable {
        let code: String?
        let message: String?
        let displayMessage: String?
    }

    struct DataPayload: Decodable {
        let requestId: String?
        let accessToken: String?
        let refreshToken: String?
        let username: String?
    }
}
