//
//  UIImage+Animated.swift
//  PRMFoundation
//
//  Decode ảnh tải từ mạng, có hỗ trợ GIF/APNG động.
//

import UIKit
import ImageIO

// MARK: - Constants

/// Delay tối thiểu hợp lệ của 1 frame GIF (10ms). Nhiều GIF khai 0ms/10ms với ý "nhanh nhất có thể";
/// trình duyệt quy ước thay bằng 100ms — làm theo để tốc độ khớp Chrome/Android (Glide cũng vậy).
private let kMinFrameDelay: TimeInterval = 0.011
private let kDefaultFrameDelay: TimeInterval = 0.1

/// Trần số phần tử của mảng frame sau khi nhân bản để giữ đúng nhịp từng frame.
/// Vượt trần → rơi về nhịp đều (`tổng thời lượng / số frame`) để tránh mảng khổng lồ.
/// Nhân bản chỉ lặp lại **tham chiếu** cùng một `UIImage` nên không tốn thêm bitmap.
private let kMaxExpandedFrames = 400

/// Hạn mức RAM cho **toàn bộ** frame của một ảnh động.
///
/// Ảnh động giữ mọi frame dưới dạng bitmap đã giải nén trong RAM (khác Android: Glide stream frame
/// theo nhịp vẽ). GIF dài rất dễ nổ — GIF 249 frame ở 300×300 đã là ~90MB, mà SDK này nhúng trong app
/// ví nên không được phép ăn hết bộ nhớ của host. Vượt hạn mức → giảm kích thước từng frame khi decode
/// (đối ứng `.override()` của Glide bên Android).
private let kAnimatedBitmapBudget = 32 * 1024 * 1024

/// Sàn kích thước khi thu nhỏ frame — dưới mức này ảnh vỡ tới mức vô nghĩa, thà vượt hạn mức.
private let kMinAnimatedPixelSize = 64

public extension UIImage {

    /// **Chặng 1** — ảnh hiện được ngay, rẻ nhất có thể.
    ///
    /// Ảnh tĩnh thì đây là kết quả cuối (`isAnimated == false`). Ảnh động thì đây chỉ là **frame đầu**,
    /// gọi tiếp [prmDecoded(from:)] ở background để dựng animation rồi thay vào.
    ///
    /// Lý do tách 2 chặng: dựng animation phải giải nén **mọi** frame trước khi vẽ được gì —
    /// đo trên máy Mac: 190 ms (GIF 249 frame) tới 247 ms (GIF 90 frame @764×781), trên máy thật còn
    /// chậm hơn 2–4 lần. Trong khi frame đầu chỉ mất 5–8 ms. Glide bên Android cũng vẽ frame đầu rồi
    /// decode dần các frame sau theo nhịp animation, nên gộp 1 chặng là iOS hiện ảnh muộn hơn Android
    /// đúng bằng khoảng đó.
    ///
    /// Frame đầu decode với **cùng hạn mức kích thước** như chặng 2 → lúc thay ảnh động vào không bị
    /// "nét rồi mờ".
    static func prmQuickDecoded(from data: Data) -> (image: UIImage, isAnimated: Bool)? {
        guard let source = CGImageSourceCreateWithData(data as CFData, nil) else {
            return UIImage(data: data).map { ($0, false) }
        }
        let frameCount = CGImageSourceGetCount(source)
        guard frameCount > 1 else {
            return UIImage(data: data).map { ($0, false) }
        }
        let maxPixelSize = prmMaxPixelSize(source: source, frameCount: frameCount)
        guard let cgImage = prmFrame(source: source, index: 0, maxPixelSize: maxPixelSize) else {
            return UIImage(data: data).map { ($0, false) }
        }
        return (UIImage(cgImage: cgImage), true)
    }

    /// **Chặng 2** — decode đầy đủ. Gọi ở background, KHÔNG gọi trên main thread.
    ///
    /// - GIF/APNG **nhiều frame** → ảnh động (`UIImageView` tự chạy khi gán vào `image`).
    /// - Còn lại (PNG/JPEG/WebP/GIF 1 frame) → `UIImage(data:)` như cũ.
    ///
    /// Dùng ImageIO có sẵn trong OS — SDK không link thư viện ngoài (UIGuide §1).
    static func prmDecoded(from data: Data) -> UIImage? {
        guard let source = CGImageSourceCreateWithData(data as CFData, nil) else {
            return UIImage(data: data)
        }
        let frameCount = CGImageSourceGetCount(source)
        guard frameCount > 1 else { return UIImage(data: data) }
        return prmAnimatedImage(source: source, frameCount: frameCount) ?? UIImage(data: data)
    }

    /// Ảnh có kích thước vẽ được không.
    ///
    /// BFF ảnh của promotion trả **HTTP 200 + PNG 1×1 trong suốt** khi record không có ảnh thật
    /// (thay vì 404), nên "decode thành công" không đồng nghĩa "có gì để hiện". Coi 1×1 là ảnh rỗng
    /// để giữ nền placeholder thay vì vẽ ra một ô trong suốt.
    ///
    /// Bên Android có bản soi gương: `PRMEmptyImageTransformation` (cùng luật "1×1 = rỗng") — sửa luật
    /// ở đây thì sửa cả bên kia, không thì hai nền tảng lại lệch.
    var prmIsRenderable: Bool {
        size.width > 1 && size.height > 1
    }

    // MARK: - Private

    private static func prmAnimatedImage(source: CGImageSource, frameCount: Int) -> UIImage? {
        var frames: [UIImage] = []
        var delays: [TimeInterval] = []
        frames.reserveCapacity(frameCount)
        delays.reserveCapacity(frameCount)

        let maxPixelSize = prmMaxPixelSize(source: source, frameCount: frameCount)
        for index in 0..<frameCount {
            guard let cgImage = prmFrame(source: source, index: index, maxPixelSize: maxPixelSize) else { continue }
            frames.append(UIImage(cgImage: cgImage))
            delays.append(prmFrameDelay(source: source, index: index))
        }
        guard frames.count > 1 else { return nil }

        let duration = delays.reduce(0, +)
        let images = prmExpandedFrames(frames, delays: delays) ?? frames
        return UIImage.animatedImage(with: images, duration: duration)
    }

    /// Nhân bản frame theo nhịp chung nhỏ nhất để giữ đúng delay riêng của từng frame
    /// (`UIImage.animatedImage` chia đều thời lượng cho mọi phần tử). Trả `nil` nếu không cần/không
    /// nên nhân bản → dùng nhịp đều.
    private static func prmExpandedFrames(_ frames: [UIImage], delays: [TimeInterval]) -> [UIImage]? {
        // Delay GIF tính theo 1/100 giây → quy về centisecond rồi lấy GCD làm nhịp chung.
        let centiseconds = delays.map { max(1, Int((($0 * 100).rounded()))) }
        guard let tick = centiseconds.reduce(nil, prmGCD), tick > 0 else { return nil }

        let repeats = centiseconds.map { $0 / tick }
        let total = repeats.reduce(0, +)
        guard total > frames.count, total <= kMaxExpandedFrames else { return nil }

        var expanded: [UIImage] = []
        expanded.reserveCapacity(total)
        for (index, frame) in frames.enumerated() {
            expanded.append(contentsOf: Array(repeating: frame, count: repeats[index]))
        }
        return expanded
    }

    /// Kích thước cạnh dài tối đa cho mỗi frame để tổng bitmap nằm trong [kAnimatedBitmapBudget].
    /// `nil` = ảnh đủ nhỏ, decode nguyên bản.
    private static func prmMaxPixelSize(source: CGImageSource, frameCount: Int) -> Int? {
        guard let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any],
              let width = properties[kCGImagePropertyPixelWidth] as? Int,
              let height = properties[kCGImagePropertyPixelHeight] as? Int,
              width > 0, height > 0 else { return nil }

        let totalBytes = width * height * 4 * frameCount
        guard totalBytes > kAnimatedBitmapBudget else { return nil }

        // Diện tích phải co theo tỉ lệ budget/totalBytes → cạnh co theo căn bậc hai của tỉ lệ đó.
        let ratio = (Double(kAnimatedBitmapBudget) / Double(totalBytes)).squareRoot()
        let longestSide = Double(max(width, height))
        return max(kMinAnimatedPixelSize, Int(longestSide * ratio))
    }

    private static func prmFrame(source: CGImageSource, index: Int, maxPixelSize: Int?) -> CGImage? {
        guard let maxPixelSize = maxPixelSize else {
            return CGImageSourceCreateImageAtIndex(source, index, nil)
        }
        let options: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceThumbnailMaxPixelSize: maxPixelSize,
        ]
        return CGImageSourceCreateThumbnailAtIndex(source, index, options as CFDictionary)
            ?? CGImageSourceCreateImageAtIndex(source, index, nil)
    }

    private static func prmGCD(_ lhs: Int?, _ rhs: Int) -> Int {
        guard var a = lhs else { return rhs }
        var b = rhs
        while b != 0 { (a, b) = (b, a % b) }
        return abs(a)
    }

    /// Delay của 1 frame: đọc dictionary GIF, không có thì thử APNG, cuối cùng về mặc định 100ms.
    private static func prmFrameDelay(source: CGImageSource, index: Int) -> TimeInterval {
        guard let properties = CGImageSourceCopyPropertiesAtIndex(source, index, nil) as? [CFString: Any] else {
            return kDefaultFrameDelay
        }
        let gif = properties[kCGImagePropertyGIFDictionary] as? [CFString: Any]
        let png = properties[kCGImagePropertyPNGDictionary] as? [CFString: Any]

        let raw = (gif?[kCGImagePropertyGIFUnclampedDelayTime] as? TimeInterval)
            ?? (gif?[kCGImagePropertyGIFDelayTime] as? TimeInterval)
            ?? (png?[kCGImagePropertyAPNGUnclampedDelayTime] as? TimeInterval)
            ?? (png?[kCGImagePropertyAPNGDelayTime] as? TimeInterval)
            ?? kDefaultFrameDelay

        return raw < kMinFrameDelay ? kDefaultFrameDelay : raw
    }
}
