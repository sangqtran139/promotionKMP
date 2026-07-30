//
//  UIImageView+Remote.swift
//  Utility
//

import UIKit
import ObjectiveC

/// Cache ảnh trong RAM dùng chung cho toàn SDK.
public final class RemoteImageCache {
    public static let shared = RemoteImageCache()
    private let cache = NSCache<NSURL, UIImage>()
    private init() {}

    func image(for url: URL) -> UIImage? { cache.object(forKey: url as NSURL) }
    func set(_ image: UIImage, for url: URL) { cache.setObject(image, forKey: url as NSURL) }
}

private var taskKey: UInt8 = 0
private var urlKey: UInt8 = 0

/// Log lý do ảnh không hiện. Chỉ ở bản DEBUG — bản release không in URL ảnh của khách ra console.
private func logRemoteImageFailure(_ url: URL, _ reason: String) {
    #if DEBUG
    print("[PRMRemoteImage] \(reason) — \(url.absoluteString)")
    #endif
}

public extension UIImageView {
    /// Màu xám phủ vùng ảnh khi CHƯA tải xong hoặc tải LỖI (thay vì để trắng).
    /// Đổi 1 dòng này để áp cho toàn SDK. Mặc định `#E9E9E9` (khớp token `tokenDark10`).
    /// Các mức khác đã cân nhắc: `#F4F4F4` (tokenDark05), `#D3D3D3` (tokenDark20), `#F2F3F2` (tokenGray10).
    static var remotePlaceholderColor: UIColor = UIColor(
        red: 233.0 / 255.0, green: 233.0 / 255.0, blue: 233.0 / 255.0, alpha: 1
    )

    /// Task đang chạy gắn với image view này, để cancel khi gọi load mới (tránh ảnh nhảy khi reuse cell).
    private var currentImageTask: URLSessionDataTask? {
        get { objc_getAssociatedObject(self, &taskKey) as? URLSessionDataTask }
        set { objc_setAssociatedObject(self, &taskKey, newValue, .OBJC_ASSOCIATION_RETAIN) }
    }

    /// URL mà image view này ĐANG cần hiển thị.
    ///
    /// Cancel task là không đủ để chống ảnh nhảy khi reuse cell: ảnh động dựng animation **sau khi**
    /// request đã xong (`cancel()` lúc đó là no-op) và mất thêm vài trăm ms, đủ để cell được gán URL
    /// khác. Mọi lần gán ảnh vì thế phải đối chiếu URL còn khớp hay không.
    private var currentImageURL: URL? {
        get { objc_getAssociatedObject(self, &urlKey) as? URL }
        set { objc_setAssociatedObject(self, &urlKey, newValue, .OBJC_ASSOCIATION_RETAIN) }
    }

    /// Load ảnh từ URL (URLSession + cache RAM). Tự cancel request cũ nếu có.
    /// Gọi trên main thread; callback cập nhật ảnh cũng về main thread.
    ///
    /// Hỗ trợ **GIF/APNG động** — `UIImageView` tự chạy animation, không cần gọi thêm gì. Ảnh động đi
    /// **2 chặng**: frame đầu lên ngay (~5–8 ms), animation dựng xong ở background rồi thay vào (vài
    /// trăm ms) — xem [UIImage.prmQuickDecoded(from:)]. Ảnh tĩnh giữ nguyên hành vi cũ, 1 chặng.
    ///
    /// Placeholder: nếu truyền `placeholder` thì hiển thị ảnh đó lúc chờ; nếu không, phủ
    /// nền xám `remotePlaceholderColor` cho vùng ảnh. Nền xám được giữ khi URL rỗng/không
    /// hợp lệ hoặc khi tải lỗi, và được xóa khi ảnh thật về.
    func setImage(urlString: String?, placeholder: UIImage? = nil) {
        self.currentImageTask?.cancel()
        self.currentImageTask = nil
        self.currentImageURL = nil

        // Trạng thái chờ/lỗi: có placeholder riêng → dùng nó; không thì phủ nền xám.
        applyPlaceholderState(placeholder)

        guard let urlString = urlString,
              let url = URL(string: urlString),
              url.scheme?.hasPrefix("http") == true else {
            return // URL rỗng/không hợp lệ → giữ nền xám.
        }

        self.currentImageURL = url

        if let cached = RemoteImageCache.shared.image(for: url) {
            applyLoadedImage(cached)
            return
        }

        // Đang tải: giữ nền xám đã set ở trên cho tới khi ảnh về (hoặc lỗi → vẫn xám).
        //
        // MỌI nhánh lỗi dưới đây đều để nguyên nền xám — nhìn ngoài y như nhau, nên bắt buộc log
        // kèm lý do, không thì "ảnh không hiện" là hộp đen (phân biệt ATS chặn HTTP / 4xx / decode
        // hỏng / server trả ảnh rỗng đều cần log này).
        let task = URLSession.shared.dataTask(with: url) { [weak self] data, response, error in
            if let error = error {
                let code = (error as NSError).code
                // Cancel là chủ động (cell tái sử dụng, load URL mới) — không phải lỗi.
                if code != NSURLErrorCancelled {
                    logRemoteImageFailure(url, "network error \(code): \(error.localizedDescription)")
                }
                return
            }
            if let http = response as? HTTPURLResponse, !(200...299).contains(http.statusCode) {
                logRemoteImageFailure(url, "HTTP \(http.statusCode)")
                return
            }
            guard let data = data, !data.isEmpty else {
                logRemoteImageFailure(url, "response rỗng")
                return
            }
            // ─── Chặng 1: hiện ngay ───────────────────────────────────────────────
            guard let quick = UIImage.prmQuickDecoded(from: data) else {
                logRemoteImageFailure(url, "decode fail (\(data.count) bytes, không phải định dạng ảnh OS đọc được)")
                return
            }
            // Ảnh 1×1 (BFF trả khi record không có ảnh thật) → vẽ ra thì trong suốt, giữ nền xám còn
            // đúng hơn. KHÔNG cache để lần sau ảnh thật lên là hiện ngay.
            guard quick.image.prmIsRenderable else {
                logRemoteImageFailure(
                    url,
                    "ảnh rỗng \(Int(quick.image.size.width))×\(Int(quick.image.size.height)) — server không có ảnh thật"
                )
                return
            }

            if !quick.isAnimated {
                RemoteImageCache.shared.set(quick.image, for: url)
                DispatchQueue.main.async { self?.applyLoadedImage(quick.image, if: url) }
                return
            }

            // Ảnh động: cho frame đầu lên trước (5–8 ms) rồi mới dựng animation.
            DispatchQueue.main.async { self?.applyLoadedImage(quick.image, if: url) }

            // ─── Chặng 2: dựng animation, thay vào ────────────────────────────────
            //
            // Vẫn đang ở thread của URLSession (không phải main) nên decode nặng ở đây là an toàn.
            // Chạy tới cùng dù image view đã biến mất: data tải xong rồi, dựng nốt để cache còn dùng
            // cho lần mở sau / cell khác cùng URL. Đánh đổi: cuộn nhanh qua nhiều GIF khác nhau thì
            // tốn CPU background cho những ảnh không còn hiển thị (mỗi URL chỉ một lần mỗi phiên).
            if let cached = RemoteImageCache.shared.image(for: url) {
                DispatchQueue.main.async { self?.applyLoadedImage(cached, if: url) }
                return
            }
            guard let animated = UIImage.prmDecoded(from: data) else { return } // giữ frame đầu đang hiện
            RemoteImageCache.shared.set(animated, for: url)
            DispatchQueue.main.async { self?.applyLoadedImage(animated, if: url) }
        }
        self.currentImageTask = task
        task.resume()
    }

    /// Đặt trạng thái placeholder: có ảnh placeholder → hiện ảnh đó (nền trong suốt);
    /// không có → phủ nền xám + xóa ảnh cũ (tránh giữ ảnh của cell tái sử dụng).
    private func applyPlaceholderState(_ placeholder: UIImage?) {
        if let placeholder {
            self.image = placeholder
            self.backgroundColor = .clear
        } else {
            self.image = nil
            self.backgroundColor = UIImageView.remotePlaceholderColor
        }
    }

    /// Gán ảnh **chỉ khi** image view vẫn đang cần đúng URL đó — chống ảnh nhảy khi cell tái sử dụng
    /// giữa lúc đang decode. Xem [currentImageURL].
    private func applyLoadedImage(_ image: UIImage, if url: URL) {
        guard currentImageURL == url else { return }
        applyLoadedImage(image)
    }

    /// Ảnh thật đã về: hiện ảnh + bỏ nền xám (để ảnh trong suốt hiển thị đúng).
    private func applyLoadedImage(_ image: UIImage) {
        self.image = image
        self.backgroundColor = .clear
    }
}
