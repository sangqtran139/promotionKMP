//
//  RemoteImageCache.swift
//  PRMFoundation
//
//  Tầng lấy **dữ liệu** ảnh: RAM → đĩa → mạng, có gộp request trùng.
//  Phần decode và gán vào view nằm ở `UIImageView+Remote.swift`.
//

import UIKit
import CryptoKit

/// Cache ảnh dùng chung cho toàn SDK: **RAM + đĩa**, và **gộp** các request trùng URL.
///
/// Trước đây chỉ có `NSCache` (RAM). Hai hệ quả thấy rõ trong luồng thật của SDK này:
///
/// - **Không có đĩa** → mỗi lần mở lại app là tải lại toàn bộ logo voucher. Danh sách ưu đãi có
///   hàng chục logo, và chúng gần như không đổi giữa hai phiên. `NSCache` còn bị hệ điều hành xoá
///   bất cứ lúc nào khi thiếu bộ nhớ, nên ngay trong một phiên cũng không chắc còn.
/// - **Không gộp request** → cùng một logo xuất hiện ở 5 cell (voucher cùng merchant) là 5 lượt
///   `URLSessionDataTask` song song cho cùng một URL, 5 lần tải cùng số byte.
///
/// **Không tự viết eviction cho RAM**: `NSCache` đã tự nhả khi máy thiếu bộ nhớ. Phần đĩa thì có
/// (xem [trimIfNeeded]) vì file trên đĩa không ai dọn hộ.
public final class RemoteImageCache {

    public static let shared = RemoteImageCache()

    // MARK: - RAM

    /// `totalCostLimit` được đặt tường minh — `NSCache` mặc định **không giới hạn** gì cả, nó chỉ
    /// nhả khi hệ điều hành báo thiếu bộ nhớ. Với ảnh động thì đó là quá muộn: một GIF đã decode
    /// chiếm vài chục MB (mỗi frame là một bitmap), cuộn qua chục cái là app host bị kill trước khi
    /// cảnh báo bộ nhớ kịp tới. Đối ứng Glide bên Android, thứ tự quản bộ nhớ theo kích thước màn.
    private let memory: NSCache<NSURL, UIImage> = {
        let cache = NSCache<NSURL, UIImage>()
        cache.totalCostLimit = 32 * 1024 * 1024   // 32 MB pixel đã decode
        cache.countLimit = 120                    // chặn cả trường hợp nhiều ảnh nhỏ
        return cache
    }()

    func image(for url: URL) -> UIImage? { memory.object(forKey: url as NSURL) }

    /// `cost` = số byte pixel ước lượng, **nhân số frame** với ảnh động — nếu không thì một GIF 40
    /// frame bị tính bằng một ảnh tĩnh và `totalCostLimit` không chặn được gì.
    func set(_ image: UIImage, for url: URL) {
        memory.setObject(image, forKey: url as NSURL, cost: image.prmEstimatedBytes)
    }

    // MARK: - Đĩa

    /// `Library/Caches` chứ không phải `Documents`: iOS được phép xoá thư mục này khi máy hết chỗ,
    /// và nó **không** vào iCloud backup. Ảnh tải lại được nên đó đúng là chỗ của nó — để ở
    /// `Documents` là chiếm dung lượng backup của người dùng bằng thứ có thể tải lại.
    private let diskDirectory: URL?

    /// Hàng đợi riêng cho I/O đĩa: đọc/ghi file **không** được chạy trên main thread (cuộn giật),
    /// và `.utility` để không tranh CPU với việc dựng UI.
    private let ioQueue = DispatchQueue(label: "com.vtm.prm.imagecache.io", qos: .utility)

    /// Dung lượng tối đa của cache đĩa. Vượt thì xoá file **cũ nhất theo lần truy cập gần nhất**.
    private let maxDiskBytes = 50 * 1024 * 1024

    private init() {
        let caches = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first
        diskDirectory = caches?.appendingPathComponent("PRMRemoteImage", isDirectory: true)
        if let dir = diskDirectory {
            try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        }
        ioQueue.async { [weak self] in self?.trimIfNeeded() }
    }

    /// Tên file = SHA256 của URL. **Không** dùng `hashValue` của Swift: nó có seed ngẫu nhiên mỗi
    /// lần chạy app, nên file ghi phiên này sẽ không bao giờ tìm thấy ở phiên sau — cache đĩa thành
    /// vô dụng theo cách rất khó nhận ra. Cũng không dùng chính URL làm tên: có `/`, `?`, và dài
    /// quá giới hạn tên file.
    private func diskPath(for url: URL) -> URL? {
        guard let dir = diskDirectory else { return nil }
        let digest = SHA256.hash(data: Data(url.absoluteString.utf8))
        let name = digest.map { String(format: "%02x", $0) }.joined()
        return dir.appendingPathComponent(name)
    }

    private func diskData(for url: URL) -> Data? {
        guard let path = diskPath(for: url) else { return nil }
        guard let data = try? Data(contentsOf: path), !data.isEmpty else { return nil }
        // Chạm `modificationDate` để [trimIfNeeded] biết file này còn được dùng — nếu không, ảnh
        // dùng hằng ngày vẫn bị xoá chỉ vì nó được TẢI VỀ lâu rồi.
        try? FileManager.default.setAttributes([.modificationDate: Date()], ofItemAtPath: path.path)
        return data
    }

    private func storeOnDisk(_ data: Data, for url: URL) {
        guard let path = diskPath(for: url) else { return }
        try? data.write(to: path, options: .atomic)
    }

    /// Xoá bớt file cũ khi vượt [maxDiskBytes]. Chạy **một lần mỗi phiên**, ở `init`.
    ///
    /// Xoá theo `modificationDate` tăng dần (LRU xấp xỉ) chứ không xoá sạch: xoá sạch thì lần mở app
    /// sau lại tải lại toàn bộ, tức là mất đúng cái lợi vừa thêm vào.
    private func trimIfNeeded() {
        guard let dir = diskDirectory else { return }
        let keys: [URLResourceKey] = [.fileSizeKey, .contentModificationDateKey]
        guard let files = try? FileManager.default.contentsOfDirectory(
            at: dir, includingPropertiesForKeys: keys
        ) else { return }

        let sized = files.compactMap { url -> (URL, Int, Date)? in
            guard let values = try? url.resourceValues(forKeys: Set(keys)),
                  let size = values.fileSize,
                  let date = values.contentModificationDate else { return nil }
            return (url, size, date)
        }
        var total = sized.reduce(0) { $0 + $1.1 }
        guard total > maxDiskBytes else { return }

        for (url, size, _) in sized.sorted(by: { $0.2 < $1.2 }) {
            try? FileManager.default.removeItem(at: url)
            total -= size
            if total <= maxDiskBytes { break }
        }
    }

    // MARK: - Gộp request trùng

    private let lock = NSLock()
    private var inFlight: [URL: [(Data?) -> Void]] = [:]

    /// Lấy **dữ liệu thô** của ảnh: đĩa trước, không có thì mạng.
    ///
    /// [completion] chạy trên hàng đợi nền (không phải main) — nơi gọi tự hop nếu cần đụng UI. Cố ý:
    /// phần decode ảnh động rất nặng, hop về main rồi decode ở đó là làm giật cuộn.
    ///
    /// **Gộp**: gọi n lần cho cùng URL trong lúc lượt đầu chưa xong → đúng **một** request mạng, cả
    /// n `completion` cùng nhận kết quả. Vì vậy hàm này **không** trả về task để cancel: huỷ theo
    /// một nơi gọi sẽ cắt luôn của n-1 nơi còn lại. Chống ảnh nhảy khi cell tái sử dụng đã có cách
    /// khác và đúng hơn — đối chiếu URL lúc gán (xem `UIImageView.currentImageURL`).
    func loadData(for url: URL, completion: @escaping (Data?) -> Void) {
        lock.lock()
        if inFlight[url] != nil {
            inFlight[url]?.append(completion)
            lock.unlock()
            return
        }
        inFlight[url] = [completion]
        lock.unlock()

        ioQueue.async { [weak self] in
            guard let self else { return completion(nil) }

            if let cached = self.diskData(for: url) {
                self.finish(url, with: cached)
                return
            }

            URLSession.shared.dataTask(with: url) { [weak self] data, response, error in
                guard let self else { return }
                if error != nil {
                    self.finish(url, with: nil)
                    return
                }
                if let http = response as? HTTPURLResponse, !(200...299).contains(http.statusCode) {
                    self.finish(url, with: nil)
                    return
                }
                guard let data, !data.isEmpty else {
                    self.finish(url, with: nil)
                    return
                }
                self.ioQueue.async { self.storeOnDisk(data, for: url) }
                self.finish(url, with: data)
            }.resume()
        }
    }

    private func finish(_ url: URL, with data: Data?) {
        lock.lock()
        let waiters = inFlight.removeValue(forKey: url) ?? []
        lock.unlock()
        for waiter in waiters { waiter(data) }
    }

    /// Xoá toàn bộ cache (RAM + đĩa). Dùng ở `PromotionSDK.release()` và trong test.
    public func clear() {
        memory.removeAllObjects()
        ioQueue.async { [weak self] in
            guard let dir = self?.diskDirectory else { return }
            try? FileManager.default.removeItem(at: dir)
            try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        }
    }
}

private extension UIImage {
    /// Số byte pixel ước lượng: `w × h × 4 × scale² × số frame`.
    ///
    /// Dùng cho `NSCache.cost`. Không có cách lấy dung lượng thật của một `UIImage` mà không đụng
    /// `CGImage` của từng frame, nên ước lượng theo kích thước điểm ảnh là đủ đúng cho việc xếp hạng.
    var prmEstimatedBytes: Int {
        let frames = max(images?.count ?? 1, 1)
        let pixels = size.width * scale * size.height * scale
        return Int(pixels * 4) * frames
    }
}
