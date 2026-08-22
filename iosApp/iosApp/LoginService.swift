//
//  LoginService.swift
//  PromotionSDKDemo
//
//  CHỈ dùng cho app demo: gọi API đăng nhập ViettelMoney để lấy `accessToken` thật rồi truyền
//  vào SDK (`PromotionSDK.initialize(tokenSource:...)`). Ngoài đời việc này do app HOST làm;
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
import PRM

enum LoginError: Error {
    case invalidResponse
    case server(code: String, message: String)   // status.code != "00"/AUT0014
    case missingRequestId                          // bước 1 không trả requestId
    case missingToken                              // bước 2 không trả accessToken
}

/// Kho token của app demo — đứng cho thứ mà host thật đã có sẵn (session manager / keychain wrapper).
/// SDK đọc lại nó ở **mỗi** request qua `PromotionTokenSource`, nên đổi lúc nào cũng được, không phải
/// báo gì cho SDK. Có khoá vì SDK đọc từ **thread nền** còn luồng đăng nhập ghi từ thread khác.
/// Đối ứng `demoAccessToken` bên Android.
enum DemoTokenStore {
    private static let lock = NSLock()
    private static var _token = ""

    static var token: String {
        get {
            lock.lock()
            defer { lock.unlock() }
            return _token
        }
        set {
            lock.lock()
            defer { lock.unlock() }
            _token = newValue
        }
    }
}

/// Nguồn token mà app demo đưa cho SDK — cài đặt `PromotionTokenSource`.
/// Soi gương `DemoTokenSource` bên Android (cùng hai hàm). Sửa một bên thì sửa cả hai.
///
/// Singleton (sống bằng tuổi process) chứ không phải property của view controller: SDK giữ object
/// này tới tận `PromotionSDK.release()`. Trỏ vào màn hình là giữ màn hình đó vĩnh viễn.
///
/// - `currentToken()` — SDK gọi ở **mỗi** request. Chỉ đọc `DemoTokenStore`, không I/O, không chặn.
/// - `refreshToken(_:)` — SDK gọi khi ăn 401. Ở app thật đây là API refresh token của bạn; demo không
///   có nên login lại từ đầu. Điều **quan trọng** là ghi vào `DemoTokenStore` **trước** khi báo
///   `true`: lượt thử lại của SDK đọc token qua `currentToken()` chứ không dùng giá trị nào ta trả về.
final class DemoTokenSource: PromotionTokenSource {

    static let shared = DemoTokenSource()
    private init() {}

    /// SDK đã gộp các request 401 cùng lúc thành một lần gọi; hàng đợi này chặn nốt hai màn mở cách
    /// nhau vài giây, và bảo đảm mọi lượt chờ đều nhận đúng một callback.
    private let lock = NSLock()
    private var isRefreshing = false
    private var waiters: [(Bool) -> Void] = []

    func currentToken() -> String? { DemoTokenStore.token }

    func refreshToken(_ onResult: @escaping (Bool) -> Void) {
        print("[Demo] SDK ăn 401 → app đang lấy token mới…")

        lock.lock()
        waiters.append(onResult)
        if isRefreshing {
            lock.unlock()
            return
        }
        isRefreshing = true
        lock.unlock()

        LoginService.shared.login { [weak self] result in
            guard let self else { return }
            var ok = false
            switch result {
            case .success(let login):
                DemoTokenStore.token = login.accessToken   // ghi vào kho TRƯỚC khi báo
                ok = true
                print("[Demo] Đã có token mới → SDK sẽ thử lại request hỏng")
            case .failure(let error):
                print("[Demo] Lấy token mới thất bại: \(error) → SDK bắn onExpireToken()")
            }

            self.lock.lock()
            let pending = self.waiters
            self.waiters = []
            self.isRefreshing = false
            self.lock.unlock()

            pending.forEach { $0(ok) }
        }
    }
}

/// **Chỉ để demo.** Ghi một chuỗi rác vào kho token để lượt gọi API kế tiếp của SDK ăn 401 — nhìn
/// thấy cơ chế refresh chạy mà không phải ngồi chờ token thật hết hạn (~15 phút).
/// Đối ứng `expireTokenForDemo()` bên Android.
func expireTokenForDemo() {
    DemoTokenStore.token = "expired-token-for-demo"
    print("[Demo] Đã làm hỏng token trong kho app (SDK KHÔNG được báo gì)")
}

/// Kết quả đăng nhập cần cho SDK.
struct LoginResult {
    let accessToken: String
    let username: String   // msisdn — chỉ để hiển thị ở demo, SDK không cần
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
