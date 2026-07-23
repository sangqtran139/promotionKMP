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

    /// Load ảnh từ URL (URLSession + cache RAM). Tự cancel request cũ nếu có.
    /// Gọi trên main thread; callback cập nhật ảnh cũng về main thread.
    ///
    /// Placeholder: nếu truyền `placeholder` thì hiển thị ảnh đó lúc chờ; nếu không, phủ
    /// nền xám `remotePlaceholderColor` cho vùng ảnh. Nền xám được giữ khi URL rỗng/không
    /// hợp lệ hoặc khi tải lỗi, và được xóa khi ảnh thật về.
    func setImage(urlString: String?, placeholder: UIImage? = nil) {
        self.currentImageTask?.cancel()
        self.currentImageTask = nil

        // Trạng thái chờ/lỗi: có placeholder riêng → dùng nó; không thì phủ nền xám.
        applyPlaceholderState(placeholder)

        guard let urlString = urlString,
              let url = URL(string: urlString),
              url.scheme?.hasPrefix("http") == true else {
            return // URL rỗng/không hợp lệ → giữ nền xám.
        }

        if let cached = RemoteImageCache.shared.image(for: url) {
            applyLoadedImage(cached)
            return
        }

        // Đang tải: giữ nền xám đã set ở trên cho tới khi ảnh về (hoặc lỗi → vẫn xám).

        let task = URLSession.shared.dataTask(with: url) { [weak self] data, _, _ in
            guard let data = data, let image = UIImage(data: data) else { return } // lỗi → giữ nền xám
            RemoteImageCache.shared.set(image, for: url)
            DispatchQueue.main.async {
                self?.applyLoadedImage(image)
            }
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

    /// Ảnh thật đã về: hiện ảnh + bỏ nền xám (để ảnh trong suốt hiển thị đúng).
    private func applyLoadedImage(_ image: UIImage) {
        self.image = image
        self.backgroundColor = .clear
    }
}
