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
//    2) POST lại với `requestId = <data.requestId ở bước 1>` + OTP **người dùng nhập** →
//       `status.code = "00"` + `data.accessToken` (Bearer cho SDK) + `data.username` (msisdn).
//
//  Bước 1 gửi `otp` RỖNG. Trước đây gắn cứng "1111"; mã đó không còn đúng và mỗi lượt gửi sai bị
//  tính một lần nhập sai — quá 5 lần liên tiếp là server khoá tài khoản.
//
//  API chạy HTTPS (UAT) nên KHÔNG cần ngoại lệ ATS — khối `NSAppTransportSecurity` đã gỡ khỏi
//  Info.plist. Quay lại host HTTP thì phải khai lại, nếu không iOS chặn ngay ở tầng mạng.
//

import Foundation
import PRM

enum LoginError: Error {
    /// Không đọc được response. Mang theo HTTP status + đoạn đầu body: thiếu chúng thì lỗi mạng chỉ
    /// hiện ra chữ "invalidResponse", không đủ để chẩn bất cứ điều gì.
    case invalidResponse(status: Int, bodyPrefix: String)
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
    func currentToken() -> String? { DemoTokenStore.token }

    /// **Chịu ngay.** Luồng đăng nhập giờ CẦN người dùng nhập OTP, nên app không tự lấy token mới
    /// được — không có mã để điền hộ.
    ///
    /// Trả `false` để SDK để lỗi `TOKEN_EXPIRED` nổi lên và bắn `onExpireToken()`; host thật sẽ đưa
    /// người dùng về màn đăng nhập. Đối ứng `DemoTokenSource.refreshToken` bên `:androidApp`.
    func refreshToken(_ onResult: @escaping (Bool) -> Void) {
        print("[Demo] SDK ăn 401 → luồng đăng nhập cần OTP, app không tự làm mới được")
        onResult(false)
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
/// Kết quả bước 1: hoặc xong luôn (hiếm), hoặc cần người dùng nhập OTP.
/// Đối ứng `OtpChallenge` bên `:androidApp`.
enum OtpChallenge {
    case token(LoginResult)
    case needOtp(requestId: String, message: String)
}

struct LoginResult {
    let accessToken: String
    let username: String   // msisdn — chỉ để hiển thị ở demo, SDK không cần
}

final class LoginService {

    static let shared = LoginService()
    private init() {}

    // MARK: - Cấu hình demo (sửa nhanh tại đây) — hardcode y theo curl BE.

    /// Cùng host với `DEMO_BASE_URL` bên `:androidApp` — môi trường UAT.
    private let endpoint = URL(string: "https://api24cdn.vtmoney.vn/uatmm/auth/v1/authn/login")!
    /// Khớp `DEMO_MSISDN` bên `:androidApp`. Đổi số thì đổi đúng dòng này.
    let msisdn = "84971410156"
    private let pin = "139200"

    /// `ephemeral` + tắt cookie + tắt cache: `URLSession` mặc định mang theo cookie, cache và
    /// header tự thêm của hệ thống, nên request thật khác lệnh `curl` mà chính nó in ra — và lệnh
    /// curl đó chạy được trong khi app thì không. Cấu hình này bỏ hết phần ngầm đó đi.
    private let session: URLSession = {
        let config = URLSessionConfiguration.ephemeral
        config.httpCookieStorage = nil
        config.httpShouldSetCookies = false
        config.urlCache = nil
        config.requestCachePolicy = .reloadIgnoringLocalCacheData
        // Chặn URLSession tự khai `Accept-Encoding: gzip, deflate, br` và `Accept-Language` theo
        // locale máy — hai header duy nhất còn lại mà curl không gửi.
        config.httpAdditionalHeaders = [
            "Accept": "*/*",
            "Accept-Encoding": "identity",
            "Accept-Language": "vi"
        ]
        return URLSession(configuration: config)
    }()

    // MARK: - Public

    /// Đăng nhập 2 bước, trả `accessToken` + `username` (msisdn). `completion` chạy trên MAIN thread.
    /// Bước 1 — xin OTP. Server gửi mã về [msisdn] và trả `requestId` để bước 2 dùng lại.
    ///
    /// Gọi **đúng một lượt** mỗi lần người dùng bấm: mỗi lượt là một lần gửi OTP và server đếm số
    /// lần không hoàn tất. Không tự thử lại ở đây.
    ///
    /// Soi gương `LoginService.requestOtp()` bên `:androidApp`.
    /// - Parameter previousRequestId: `requestId` còn giữ từ lần xin trước, nếu có.
    func requestOtp(previousRequestId: String? = nil,
                    completion: @escaping (Result<OtpChallenge, Error>) -> Void) {
        post(requestId: "", otp: "") { [weak self] result in
            guard let self else { return }
            print("[Demo] Bước 1 response: \(result)")
            switch result {
            case .failure(let error):
                self.finishChallenge(completion, .failure(error))

            case .success(let response):
                if response.status?.code == "00",
                   let token = response.data?.accessToken, !token.isEmpty {
                    let username = response.data?.username ?? self.msisdn
                    self.finishChallenge(completion, .success(.token(
                        LoginResult(accessToken: token, username: username))))
                    return
                }

                // Mã khác AUT0014 → hỏng thật, ném câu `displayMessage` của server.
                guard response.status?.code == "AUT0014" else {
                    self.finishChallenge(completion, .failure(LoginError.server(
                        code: response.status?.code ?? "?",
                        message: response.describe())))
                    return
                }

                // AUT0014 nghĩa là "cần OTP" — mã ĐÃ được gửi về máy. Nhưng server chỉ cấp
                // `requestId` MỚI khi chưa có OTP nào còn hiệu lực; xin lại quá sớm thì nó trả
                // AUT0014 kèm `requestId` RỖNG.
                //
                // Rỗng KHÔNG phải lỗi: mã cũ vẫn dùng được. Nên rơi về `requestId` đang giữ thay vì
                // báo hỏng — bản trước vứt nó đi rồi bắt người dùng bấm "Gửi lại OTP" mãi mãi, vì
                // càng bấm càng chắc chắn rỗng.
                let fresh = response.data?.requestId
                guard let requestId = (fresh?.isEmpty == false ? fresh : previousRequestId),
                      !requestId.isEmpty else {
                    self.finishChallenge(completion, .failure(LoginError.server(
                        code: "AUT0014",
                        message: response.describe())))
                    return
                }
                self.finishChallenge(completion, .success(.needOtp(
                    requestId: requestId, message: response.describe())))
            }
        }
    }

    /// Bước 2 — gửi OTP người dùng vừa nhập, lấy access token.
    func submitOtp(requestId: String, otp: String,
                   completion: @escaping (Result<LoginResult, Error>) -> Void) {
        post(requestId: requestId, otp: otp) { [weak self] result in
            guard let self else { return }
            print("[Demo] Bước 2 response: \(result)")
            switch result {
            case .failure(let error):
                self.finish(completion, .failure(error))

            case .success(let response):
                guard response.status?.code == "00" else {
                    self.finish(completion, .failure(LoginError.server(
                        code: response.status?.code ?? "?",
                        message: response.describe())))
                    return
                }
                guard let token = response.data?.accessToken, !token.isEmpty else {
                    self.finish(completion, .failure(LoginError.missingToken))
                    return
                }
                let username = response.data?.username ?? self.msisdn
                self.finish(completion, .success(LoginResult(accessToken: token, username: username)))
            }
        }
    }

    // MARK: - Private

    private func post(requestId: String, otp: String, completion: @escaping (Result<LoginResponse, Error>) -> Void) {
        var request = URLRequest(url: endpoint)
        request.httpMethod = "POST"
        request.httpShouldHandleCookies = false
        headers().forEach { request.setValue($0.value, forHTTPHeaderField: $0.key) }
        request.httpBody = try? JSONSerialization.data(withJSONObject: body(requestId: requestId, otp: otp))

        print(Self.curlCommand(for: request))

        session.dataTask(with: request) { data, response, error in
            // Lỗi tầng mạng (ATS chặn, không có DNS, timeout…) — giữ nguyên `error` của URLSession,
            // nó đã nói rõ hơn bất cứ thứ gì mình bọc lại.
            if let error { return completion(.failure(error)) }

            let status = (response as? HTTPURLResponse)?.statusCode ?? -1

            // In NGUYÊN VĂN body server trả về. `print` của `Result` đã giải mã chỉ cho thấy thứ
            // Codable dựng được — nếu một trường bị rơi thì đúng chỗ đó là chỗ không nhìn thấy.
            // Đây là cách duy nhất đối chiếu được iOS với curl.
            print("[Demo] HTTP \(status) body: \(String(data: data ?? Data(), encoding: .utf8) ?? "(rỗng)")")

            guard let data, !data.isEmpty else {
                return completion(.failure(LoginError.invalidResponse(
                    status: status, bodyPrefix: "(body rỗng)")))
            }
            guard let decoded = try? JSONDecoder().decode(LoginResponse.self, from: data) else {
                let text = String(data: data.prefix(200), encoding: .utf8) ?? "(không đọc được UTF-8)"
                return completion(.failure(LoginError.invalidResponse(
                    status: status, bodyPrefix: text)))
            }
            completion(.success(decoded))
        }.resume()
    }

    /// Đúng bộ header của `:androidApp` (`LoginApiService.@Headers`). Bản trước gửi thêm
    /// `device-id`/`publicKey`/`channel`… — bộ rút gọn này đã kiểm bằng curl là server chấp nhận,
    /// và giữ hai nền tảng gửi cùng một thứ thì lỗi ở đâu cũng tái hiện được ở bên kia.
    private func headers() -> [String: String] {
        [
            "Content-Type": "application/json",
            "imei": "1231241",
            "Product": "VIETTELPAY",
            "Authority-Party": "APP",
            "app-version": "8.8.59"
        ]
    }

    /// Đúng body của `:androidApp` (`LoginRequest`).
    ///
    /// `otp` **RỖNG ở bước 1** — bước đó chỉ để server gửi mã về máy. Mã thật do người dùng nhập
    /// rồi truyền vào ở bước 2. Trước đây gắn cứng `"1111"`; mã đó không còn đúng, và mỗi lượt gửi
    /// sai bị tính một lần nhập sai — quá 5 lần liên tiếp là server khoá tài khoản một phút.
    private func body(requestId: String, otp: String) -> [String: Any] {
        [
            "msisdn": msisdn,
            "username": msisdn,
            "userType": "msisdn",
            "pin": pin,
            "loginType": "BASIC",
            "notifyToken": "1234567899",
            "requestId": requestId,
            "typeOs": "ios",
            "otp": otp,
            "imei": "1234567891"
        ]
    }

    /// `x-request-id` dạng timestamp `yyyyMMddHHmmss` (mới mỗi lần gọi, tránh server dedup).

    /// In request thành một lệnh `curl` copy-dán được — đối ứng `CurlLoggingInterceptor` bên
    /// `:androidApp`, thứ iOS vốn thiếu.
    ///
    /// Đây là cách DUY NHẤT đối chiếu được "thứ tôi tưởng iOS gửi" với "thứ iOS thật sự gửi":
    /// body và header ở code có thể đúng mà URLSession vẫn thêm/đổi gì đó trên đường đi.
    /// Dán lệnh này vào terminal — chạy ra kết quả khác app tức là khác biệt nằm ở tầng dưới.
    private static func curlCommand(for request: URLRequest) -> String {
        var parts = ["[Demo] curl -X \(request.httpMethod ?? "POST")"]
        for (key, value) in request.allHTTPHeaderFields ?? [:] {
            parts.append("  -H '\(key): \(value)'")
        }
        if let body = request.httpBody, let text = String(data: body, encoding: .utf8) {
            parts.append("  -d '\(text)'")
        }
        parts.append("  '\(request.url?.absoluteString ?? "")'")
        return parts.joined(separator: " \\\n")
    }

    private func finish(_ completion: @escaping (Result<LoginResult, Error>) -> Void,
                        _ result: Result<LoginResult, Error>) {
        DispatchQueue.main.async { completion(result) }
    }

    private func finishChallenge(_ completion: @escaping (Result<OtpChallenge, Error>) -> Void,
                                 _ result: Result<OtpChallenge, Error>) {
        DispatchQueue.main.async { completion(result) }
    }
}

// MARK: - DTO response

private nonisolated struct LoginResponse: Decodable {
    /// Câu để hiện cho người dùng. `displayMessage` của server nói đúng chuyện đang xảy ra (vd
    /// "Bạn đã nhập sai/không nhập mã OTP quá 5 lần liên tiếp. Vui lòng thực hiện lại sau 1 phút")
    /// — thứ mà thông báo tự soạn ở client không đoán nổi.
    func describe() -> String {
        status?.displayMessage ?? status?.message ?? "Mã lỗi \(status?.code ?? "(không rõ)")"
    }

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
