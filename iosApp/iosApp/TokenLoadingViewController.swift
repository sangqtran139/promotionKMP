//
//  TokenLoadingViewController.swift
//  PromotionSDKDemo
//
//  Cổng khởi động của app demo — SOI GƯƠNG `PromotionTokenLoadingFragment` bên Android:
//  lấy token đăng nhập (progress bar) → init SDK → bơm context demo → mới vào màn chính.
//  Login lỗi thì hiện lý do + nút "Thử lại", KHÔNG đi tiếp bằng token rỗng.
//
//  Cổng này chặn màn chính cho tới khi `PromotionSDK.initialize` xong — SDK khởi tạo bất đồng bộ
//  sau login, vào sớm thì màn không mở được và widget rỗng.
//

import UIKit
import PromotionKit

final class TokenLoadingViewController: UIViewController {

    // MARK: - Demo data (giả lập host cung cấp) — đối ứng `initSdk`/`updateDemoContext` bên Android

    /// Base URL Promotion BFF — theo **môi trường đang build** (scheme `iosApp-Staging` /
    /// `iosApp-Uat` / `iosApp-Product`). Xem [DemoEnvironment]; đối ứng `BuildConfig.DEMO_BASE_URL`
    /// bên `:androidApp`.
    private static let baseUrl = DemoEnvironment.baseUrl

    /// Danh mục dịch vụ HOST cung cấp (cho bottom sheet "Chọn dịch vụ") — đối ứng `demoServices` bên Android.
    ///
    /// `productId` phải khớp **`applicableProducts.productId`** của voucher (`sku` KHÔNG được dùng
    /// để so — xem `servicesForApplicableProducts`), nên ở đây là UUID chứ không phải mã "P-FOOD-001".
    ///
    /// Một `productId` gắn nhiều SKU (vd `…0011` = BH 2 chiều + gói doanh nghiệp, `…0003` = V120 + V90)
    /// nhưng list bị `distinctBy { productId }` → khai **một dòng mỗi productId**, tên gộp các SKU.
    /// Khai theo từng SKU thì dòng thứ hai bị loại âm thầm.
    private let demoServices: [PromotionAvailableService] = [
        PromotionAvailableService(productId: "P-ALC-001", productName: "Data Viettel MIMAX125 - không giới hạn", skuSourceId: "TELCO",     iconUrl: "https://picsum.photos/seed/mimax125/96"),
        PromotionAvailableService(productId: "P-BILL-001", productName: "Gói cước V120 / V90",                    skuSourceId: "TELCO",     iconUrl: "https://picsum.photos/seed/goicuoc/96"),
        PromotionAvailableService(productId: "P-FOOD-001", productName: "BH xe máy Vespa 1 năm",                  skuSourceId: "INSURANCE", iconUrl: "https://picsum.photos/seed/vespa/96"),
        PromotionAvailableService(productId: "P-FOOD-002", productName: "BH ô tô",       skuSourceId: "INSURANCE", iconUrl: "https://picsum.photos/seed/bhoto/96"),
        PromotionAvailableService(productId: "P-FOOD-003", productName: "Combo đồ uống đóng chai",                skuSourceId: "BEVERAGE",  iconUrl: "https://picsum.photos/seed/douong/96")
    ]

    // MARK: - UI

    private let progressBar: UIProgressView = {
        let v = UIProgressView(progressViewStyle: .bar)
        v.translatesAutoresizingMaskIntoConstraints = false
        v.progressTintColor = .systemBlue
        v.trackTintColor = .secondarySystemFill
        return v
    }()

    private let spinner: UIActivityIndicatorView = {
        let v = UIActivityIndicatorView(style: .large)
        v.translatesAutoresizingMaskIntoConstraints = false
        v.hidesWhenStopped = true
        return v
    }()

    private let statusLabel: UILabel = {
        let l = UILabel()
        l.translatesAutoresizingMaskIntoConstraints = false
        l.textAlignment = .center
        l.numberOfLines = 0
        l.font = .systemFont(ofSize: 15)
        l.textColor = .secondaryLabel
        return l
    }()

    private lazy var retryButton: UIButton = {
        let b = UIButton(type: .system)
        b.translatesAutoresizingMaskIntoConstraints = false
        b.setTitle("Thử lại", for: .normal)
        b.setTitleColor(.white, for: .normal)
        b.backgroundColor = .systemBlue
        b.layer.cornerRadius = 10
        b.titleLabel?.font = .systemFont(ofSize: 16, weight: .medium)
        b.isHidden = true
        b.addTarget(self, action: #selector(primaryAction), for: .touchUpInside)
        return b
    }()

    /// Ô nhập số điện thoại. Điền sẵn số của lượt đăng nhập gần nhất ([DemoLoginStore]) — lần đầu
    /// thì trống. Đối ứng `edtMsisdn` bên `:androidApp`.
    private lazy var msisdnField: UITextField = {
        let f = UITextField()
        f.translatesAutoresizingMaskIntoConstraints = false
        f.borderStyle = .roundedRect
        f.textAlignment = .center
        f.keyboardType = .phonePad
        f.textContentType = .telephoneNumber
        f.placeholder = "Số điện thoại (84… hoặc 09…)"
        f.font = .systemFont(ofSize: 17)
        return f
    }()

    /// Ô nhập PIN. `isSecureTextEntry` để che ký tự; KHÔNG lưu lại giá trị này — xem [DemoLoginStore].
    /// Đối ứng `edtPin` bên `:androidApp`.
    private lazy var pinField: UITextField = {
        let f = UITextField()
        f.translatesAutoresizingMaskIntoConstraints = false
        f.borderStyle = .roundedRect
        f.textAlignment = .center
        f.keyboardType = .numberPad
        f.isSecureTextEntry = true
        // `.oneTimeCode` cho ô OTP thì đúng, nhưng ở ô PIN nó khiến iOS gợi ý mã SMS vào nhầm chỗ.
        f.textContentType = .password
        f.placeholder = "Mã PIN"
        f.font = .systemFont(ofSize: 17)
        // Nút con mắt hiện/ẩn PIN. Đối ứng `app:endIconMode="password_toggle"` của
        // `TextInputLayout` bên `:androidApp` — iOS không có sẵn, phải tự gắn vào `rightView`.
        f.rightView = self.pinRevealButton
        f.rightViewMode = .always
        return f
    }()

    /// Con mắt của ô PIN. `SF Symbol` đổi theo trạng thái nên người dùng biết bấm sẽ ra gì.
    private lazy var pinRevealButton: UIButton = {
        let b = UIButton(type: .system)
        b.setImage(UIImage(systemName: "eye.slash"), for: .normal)
        b.tintColor = .secondaryLabel
        // Chừa lề phải để icon không dính sát viền bo của `roundedRect`.
        b.frame = CGRect(x: 0, y: 0, width: 40, height: 44)
        b.addTarget(self, action: #selector(togglePinVisibility), for: .touchUpInside)
        return b
    }()

    /// Hiện/ẩn PIN.
    ///
    /// Gán lại `text` sau khi lật `isSecureTextEntry` là **bắt buộc**: UIKit xoá sạch nội dung ở lần
    /// gõ kế tiếp sau khi đổi cờ này (hành vi cũ của `UITextField`, để tránh lộ mật khẩu đã nhập).
    /// Thiếu hai dòng đó thì bấm con mắt xong gõ tiếp là mất hết những gì đã nhập.
    @objc private func togglePinVisibility() {
        pinField.isSecureTextEntry.toggle()
        let name = pinField.isSecureTextEntry ? "eye.slash" : "eye"
        pinRevealButton.setImage(UIImage(systemName: name), for: .normal)
        let saved = pinField.text
        pinField.text = ""
        pinField.text = saved
    }

    /// Chạy progress bar "vô định" (UIProgressView không có chế độ indeterminate như Android).
    /// Ô nhập OTP — ẩn cho tới khi server đã gửi mã (bước 1 xong). `textContentType = .oneTimeCode`
    /// để iOS gợi ý mã vừa nhận từ tin nhắn ngay trên bàn phím.
    private lazy var otpField: UITextField = {
        let f = UITextField()
        f.translatesAutoresizingMaskIntoConstraints = false
        f.borderStyle = .roundedRect
        f.textAlignment = .center
        f.keyboardType = .numberPad
        f.textContentType = .oneTimeCode
        f.placeholder = "Nhập mã OTP"
        f.font = .systemFont(ofSize: 18, weight: .medium)
        return f
    }()

    /// `nil` = chưa xin OTP hoặc bước 1 vừa hỏng; khác nil = đã có mã, bấm "Xác nhận OTP" là gọi
    /// bước 2 với chính `requestId` này.
    private var otpRequestId: String?

    /// Bộ đăng nhập của lượt xin OTP **đang chờ**, chụp lại đúng lúc bấm "Gửi OTP".
    ///
    /// Không đọc lại từ ô lúc bấm "Xác nhận OTP": server gắn mã với đúng cặp msisdn+PIN đã tạo ra
    /// `requestId`, mà giữa hai lần bấm người dùng hoàn toàn có thể sửa ô số. Gửi lệch là mất một
    /// lượt trong hạn 5 lần sai mà không hiểu vì sao. Đối ứng `pendingCredentials` bên `:androidApp`.
    private var pendingCredentials: (msisdn: String, pin: String)?

    /// Nút gọi API lần 2. Tách khỏi [retryButton] để **luôn** có đường xác nhận OTP, kể cả khi
    /// bước 1 vừa hỏng — trước đây ô và nút cùng bị ẩn ở nhánh lỗi nên không còn thao tác nào.
    private lazy var confirmButton: UIButton = {
        let b = UIButton(type: .system)
        b.translatesAutoresizingMaskIntoConstraints = false
        b.setTitle("Xác nhận OTP", for: .normal)
        b.setTitleColor(.white, for: .normal)
        b.backgroundColor = .systemGreen
        b.layer.cornerRadius = 10
        b.titleLabel?.font = .systemFont(ofSize: 16, weight: .semibold)
        b.addTarget(self, action: #selector(confirmOtp), for: .touchUpInside)
        return b
    }()

    private var progressTimer: Timer?

    /// Constraint dọc của cả khối nội dung. Bàn phím số không có nút Done, nên nếu nó che mất
    /// "Xác nhận OTP" thì người dùng kẹt — phải đẩy nội dung lên thay vì trông chờ họ vuốt.
    private var contentCenterY: NSLayoutConstraint!

    // MARK: - Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "Demo PromotionSDK"
        view.backgroundColor = .systemBackground
        setupLayout()
        observeKeyboard()
        view.addGestureRecognizer(
            UITapGestureRecognizer(target: self, action: #selector(dismissKeyboard))
        )

        // Điền sẵn DUY NHẤT số của lượt gần nhất (rỗng nếu chưa từng). PIN luôn để trống:
        // `DemoLoginStore` cố ý không lưu nó xuống đĩa, và không còn hằng demo nào để điền hộ.
        msisdnField.text = DemoLoginStore.lastMsisdn
        // Chặn nhập quá độ dài — UIKit không có `maxLength`, phải qua delegate.
        pinField.delegate = self
        otpField.delegate = self
        statusLabel.text = "Nhập số điện thoại và mã PIN rồi bấm Gửi OTP"
        retryButton.setTitle("Gửi OTP", for: .normal)
        retryButton.isHidden = false

        // KHÔNG tự `askOtp()` khi mở màn nữa. Số điện thoại giờ do người dùng nhập, mà tự gửi thì:
        //  - mỗi lần mở app là một lượt OTP về số đang điền sẵn, kể cả khi định đăng nhập số khác;
        //  - server đếm số lượt không hoàn tất, quá 5 lần liên tiếp là khoá tài khoản một phút.
        // Đối ứng `PromotionTokenLoadingFragment.onViewCreated` bên `:androidApp`.
    }

    private func observeKeyboard() {
        let center = NotificationCenter.default
        center.addObserver(self, selector: #selector(keyboardWillChange),
                           name: UIResponder.keyboardWillShowNotification, object: nil)
        center.addObserver(self, selector: #selector(keyboardWillChange),
                           name: UIResponder.keyboardWillHideNotification, object: nil)
    }

    /// Đẩy khối nội dung lên vừa đủ để nút "Xác nhận OTP" nằm trên mép bàn phím.
    @objc private func keyboardWillChange(_ note: Notification) {
        guard let frame = note.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect else { return }
        let duration = note.userInfo?[UIResponder.keyboardAnimationDurationUserInfoKey] as? Double ?? 0.25

        let showing = note.name == UIResponder.keyboardWillShowNotification
        if showing {
            // Khoảng cách từ đáy nút tới mép trên bàn phím; âm nghĩa là đang bị che.
            let keyboardTop = view.bounds.height - frame.height
            let overlap = confirmButton.frame.maxY - keyboardTop + 16
            contentCenterY.constant = overlap > 0 ? -60 - overlap : -60
        } else {
            contentCenterY.constant = -60
        }

        UIView.animate(withDuration: duration) { self.view.layoutIfNeeded() }
    }

    @objc private func dismissKeyboard() {
        view.endEditing(true)
    }

    deinit {
        progressTimer?.invalidate()
        NotificationCenter.default.removeObserver(self)
    }

    private func setupLayout() {
        view.addSubview(progressBar)
        view.addSubview(spinner)
        view.addSubview(statusLabel)
        view.addSubview(msisdnField)
        view.addSubview(pinField)
        view.addSubview(otpField)
        view.addSubview(confirmButton)
        view.addSubview(retryButton)

        contentCenterY = spinner.centerYAnchor.constraint(equalTo: view.centerYAnchor, constant: -60)

        NSLayoutConstraint.activate([
            spinner.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            contentCenterY,

            progressBar.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 40),
            progressBar.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -40),
            progressBar.topAnchor.constraint(equalTo: spinner.bottomAnchor, constant: 24),

            statusLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 24),
            statusLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -24),
            statusLabel.topAnchor.constraint(equalTo: progressBar.bottomAnchor, constant: 20),

            msisdnField.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 60),
            msisdnField.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -60),
            msisdnField.topAnchor.constraint(equalTo: statusLabel.bottomAnchor, constant: 16),
            msisdnField.heightAnchor.constraint(equalToConstant: 44),

            pinField.leadingAnchor.constraint(equalTo: msisdnField.leadingAnchor),
            pinField.trailingAnchor.constraint(equalTo: msisdnField.trailingAnchor),
            pinField.topAnchor.constraint(equalTo: msisdnField.bottomAnchor, constant: 10),
            pinField.heightAnchor.constraint(equalToConstant: 44),

            otpField.leadingAnchor.constraint(equalTo: msisdnField.leadingAnchor),
            otpField.trailingAnchor.constraint(equalTo: msisdnField.trailingAnchor),
            otpField.topAnchor.constraint(equalTo: pinField.bottomAnchor, constant: 10),
            otpField.heightAnchor.constraint(equalToConstant: 44),

            confirmButton.leadingAnchor.constraint(equalTo: otpField.leadingAnchor),
            confirmButton.trailingAnchor.constraint(equalTo: otpField.trailingAnchor),
            confirmButton.topAnchor.constraint(equalTo: otpField.bottomAnchor, constant: 12),
            confirmButton.heightAnchor.constraint(equalToConstant: 44),

            retryButton.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            retryButton.topAnchor.constraint(equalTo: confirmButton.bottomAnchor, constant: 12),
            retryButton.widthAnchor.constraint(equalToConstant: 140),
            retryButton.heightAnchor.constraint(equalToConstant: 44)
        ])
    }

    // MARK: - Login (đối ứng `PromotionTokenLoadingFragment.startLogin`)

    /// Một nút hai vai: đang chờ OTP thì xác thực, còn lại thì xin mã mới.
    /// Nút "Gửi OTP" / "Gửi lại OTP" — gọi API lần 1.
    @objc private func primaryAction() {
        askOtp()
    }

    /// Nút "Xác nhận OTP" — gọi API lần 2 với `requestId` lấy từ lần 1.
    @objc private func confirmOtp() {
        guard let requestId = otpRequestId else {
            statusLabel.text = "Chưa có requestId — bấm \"Gửi OTP\" trước để server gửi mã."
            return
        }
        verifyOtp(requestId: requestId)
    }

    /// Bước 1 — xin OTP. Server gửi mã về số của tài khoản demo.
    ///
    /// Bấm một lần là một lần gửi OTP, và server đếm số lần không hoàn tất (quá 5 lần liên tiếp thì
    /// khoá một phút). Vì vậy không tự gọi lại ở bất kỳ nhánh lỗi nào.
    /// Soi gương `askOtp()` bên `PromotionTokenLoadingFragment`.
    private func askOtp() {
        // Gõ `09…` hay `84…` đều nhận — chuẩn hoá về dạng server đòi (`84…`) ngay tại đây, và ghi
        // ngược lại vào ô để người dùng thấy đúng số sắp được gửi đi.
        let msisdn = LoginService.normalizeMsisdn(msisdnField.text ?? "")
        let pin = (pinField.text ?? "").trimmingCharacters(in: .whitespaces)
        guard !msisdn.isEmpty, !pin.isEmpty else {
            statusLabel.text = "Nhập đủ số điện thoại và mã PIN rồi bấm Gửi OTP"
            return
        }
        msisdnField.text = msisdn

        // Đổi số so với lượt trước → `requestId` cũ thuộc về tài khoản khác, dùng lại là gửi OTP của
        // người này sang phiên của người kia. Vứt đi để bước 1 xin phiên mới hẳn.
        if pendingCredentials?.msisdn != msisdn { otpRequestId = nil }
        pendingCredentials = (msisdn: msisdn, pin: pin)

        // GIỮ `otpRequestId` cũ (cùng số), không xoá: nếu server không cấp mã mới (OTP trước còn
        // hiệu lực) thì cái đang giữ vẫn là đường duy nhất để gọi bước 2.
        statusLabel.text = "Đang gửi OTP tới \(msisdn)"
        setLoading(true)
        view.endEditing(true)

        LoginService.shared.requestOtp(
            msisdn: msisdn,
            pin: pin,
            previousRequestId: otpRequestId
        ) { [weak self] result in
            guard let self else { return }
            // Nhớ số SAU khi server nhận — số gõ sai thì không đáng điền sẵn cho lần sau.
            if case .success = result { DemoLoginStore.lastMsisdn = msisdn }
            switch result {
            case .success(.token(let login)):
                self.finishLogin(login)

            case .success(.needOtp(let requestId, let message)):
                self.otpRequestId = requestId
                self.statusLabel.text = message
                self.setLoading(false)
                self.otpField.text = ""
                self.otpField.becomeFirstResponder()
                self.retryButton.setTitle("Gửi lại OTP", for: .normal)
                self.retryButton.isHidden = false

            case .failure(let error):
                self.showFailure(error)
            }
        }
    }

    /// Bước 2 — gửi mã người dùng vừa nhập.
    private func verifyOtp(requestId: String) {
        let otp = (otpField.text ?? "").trimmingCharacters(in: .whitespaces)
        guard !otp.isEmpty else {
            statusLabel.text = "Nhập mã OTP đã nhận rồi bấm Xác nhận OTP"
            return
        }
        guard let credentials = pendingCredentials else {
            statusLabel.text = "Bấm \"Gửi OTP\" trước để server gửi mã."
            return
        }

        statusLabel.text = "Đang xác thực OTP"
        setLoading(true)
        view.endEditing(true)

        LoginService.shared.submitOtp(
            requestId: requestId,
            otp: otp,
            msisdn: credentials.msisdn,
            pin: credentials.pin
        ) { [weak self] result in
            guard let self else { return }
            switch result {
            case .success(let login):
                self.finishLogin(login)

            case .failure(let error):
                // Ở LẠI màn nhập OTP: mã có thể chỉ gõ nhầm, xin mã mới là tốn thêm một lượt
                // trong hạn 5 lần. Người dùng sửa rồi bấm lại.
                print("[Demo] Xác thực OTP lỗi: \(error)")
                self.statusLabel.text = Self.describe(error)
                self.setLoading(false)
                self.retryButton.setTitle("Gửi lại OTP", for: .normal)
                self.retryButton.isHidden = false
            }
        }
    }

    /// Soi gương `finishLogin` bên `PromotionTokenLoadingFragment`: báo thành công rồi tự vào màn
    /// chính sau nửa giây.
    ///
    /// Bản trước chặn bằng một alert khoe token và bắt bấm OK — Android không có bước đó, và nó
    /// nằm chắn giữa luồng đăng nhập nên hai bên chạy khác hẳn nhau. Token vẫn in ra console.
    private func finishLogin(_ login: LoginResult) {
        DemoTokenStore.token = login.accessToken
        print("[Demo] accessToken: \(login.accessToken)")

        initSdk()
        updateDemoContext()

        statusLabel.text = "Lấy token thành công"
        setLoading(false)
        view.endEditing(true)

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
            self?.navigateToLauncher()
        }
    }

    private func showFailure(_ error: Error) {
        print("[Demo] Login lỗi: \(error)")
        // KHÔNG xoá `otpRequestId`: bước 1 hỏng không làm mã đã gửi mất hiệu lực.
        statusLabel.text = Self.describe(error)
        setLoading(false)
        // "Gửi lại OTP" chỉ đúng khi đã từng có mã được gửi. Từ khi PIN do người dùng nhập, bước 1
        // còn hỏng vì sai PIN — lúc đó chưa có OTP nào để mà "gửi lại", và chữ đó khiến người dùng
        // tưởng mã đã về máy rồi ngồi chờ tin nhắn không bao giờ tới.
        retryButton.setTitle(otpRequestId == nil ? "Gửi OTP" : "Gửi lại OTP", for: .normal)
        retryButton.isHidden = false
    }

    /// `LoginError.server` đã mang sẵn `displayMessage` của server — hiện nguyên văn, đừng bọc thêm.
    private static func describe(_ error: Error) -> String {
        switch error {
        case LoginError.server(_, let message):
            return message
        case LoginError.invalidResponse(let status, let bodyPrefix):
            return "Server trả dữ liệu lạ (HTTP \(status))\n\(bodyPrefix)"
        case let urlError as URLError:
            // `.code.rawValue` là mã NSURLError — tra được thẳng, vd -1200 = lỗi TLS/ATS,
            // -1009 = mất mạng, -1001 = timeout.
            return "Lỗi mạng \(urlError.code.rawValue): \(urlError.localizedDescription)"
        default:
            return "Lấy token thất bại\n\(error)"
        }
    }

    /// Gọi THẲNG PromotionSDK — không qua wrapper.
    /// **Lúc nào vào app cũng initialize() lại**, lần nào cũng áp đủ cấu hình host truyền (kể cả
    /// baseUrl/environment/language). Đối ứng `PromotionTokenLoadingFragment.initSdk` bên Android.
    ///
    /// `tokenSource` là cách DUY NHẤT token đi vào SDK — không có tham số `accessToken` nào nữa. SDK
    /// gọi `currentToken()` ở mỗi request, nên app đổi token lúc nào cũng được mà không phải báo gì.
    ///
    /// `DemoTokenSource` (ở LoginService.swift) cài đặt cả hai hàm: `currentToken()` cho mọi request,
    /// và `refreshToken(_:)` cho lúc SDK ăn 401. Nó là singleton sống bằng tuổi process — SDK giữ
    /// object này tới tận `release()` nên KHÔNG được capture view controller này.
    private func initSdk() {
        PromotionSDK.initialize(
            tokenSource: DemoTokenSource.shared,
            baseUrl: Self.baseUrl,
            availableServices: demoServices,
            callback: DemoPromotionCallback.shared,
        )
    }

    private func updateDemoContext() {
        PromotionSDK.updateOrderInfo(
            orderId: "ORD-DEMO-001",
            productId: "TKBAOVIET",
            orderValue: "500000",
            metaData: "channel=MOBILE_APP",
            productName: "Tài khoản Bảo Việt",
            productCategory: "INSURANCE",
            quantity: 1,
            unitPrice: "500000"
        )
    }

    private func navigateToLauncher() {
        // Thay root của navigation → không back ngược lại được màn loading (giống `replace` Android).
        navigationController?.setViewControllers([ViewController()], animated: true)
    }

    // MARK: - Helpers

    private func setLoading(_ loading: Bool) {
        retryButton.isHidden = true
        confirmButton.isEnabled = !loading
        if loading {
            spinner.startAnimating()
            startProgressAnimation()
        } else {
            spinner.stopAnimating()
            stopProgressAnimation(completed: true)
        }
    }

    private func startProgressAnimation() {
        progressBar.isHidden = false
        progressBar.setProgress(0, animated: false)
        progressTimer?.invalidate()
        progressTimer = Timer.scheduledTimer(withTimeInterval: 0.15, repeats: true) { [weak self] _ in
            guard let self else { return }
            // Bò dần tới 90% rồi dừng — hoàn tất khi login trả về.
            let next = self.progressBar.progress + 0.02
            self.progressBar.setProgress(min(next, 0.9), animated: true)
        }
    }

    private func stopProgressAnimation(completed: Bool) {
        progressTimer?.invalidate()
        progressTimer = nil
        progressBar.setProgress(completed ? 1 : 0, animated: true)
    }
}

// MARK: - UITextFieldDelegate — giới hạn độ dài ô PIN / OTP

extension TokenLoadingViewController: UITextFieldDelegate {

    /// UIKit không có `maxLength` như `android:maxLength`, nên phải tự chặn ở đây.
    ///
    /// Tính độ dài **sau khi thay** (`replacingCharacters`) chứ không phải `text.count + string.count`:
    /// cách sau sai ở mọi thao tác không phải gõ thêm — xoá, chọn một đoạn rồi dán đè, dán nhiều ký
    /// tự — và sẽ chặn nhầm cả phím xoá khi ô đã đầy.
    func textField(_ textField: UITextField,
                   shouldChangeCharactersIn range: NSRange,
                   replacementString string: String) -> Bool {
        let limit: Int
        switch textField {
        case pinField: limit = LoginService.pinLength
        case otpField: limit = LoginService.otpLength
        default: return true
        }
        let current = textField.text ?? ""
        guard let r = Range(range, in: current) else { return true }
        return current.replacingCharacters(in: r, with: string).count <= limit
    }
}
