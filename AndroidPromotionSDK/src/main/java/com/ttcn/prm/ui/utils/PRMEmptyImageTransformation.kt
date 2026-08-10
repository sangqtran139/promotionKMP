package com.ttcn.prm.ui.utils

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import timber.log.Timber
import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * Đổi **ảnh rỗng** thành ô màu placeholder.
 *
 * BFF ảnh promotion trả `HTTP 200` + **PNG 1×1 trong suốt** (70 byte, RGBA 0,0,0,0) khi voucher không
 * có ảnh thật — chứ không phải 404. Glide decode "thành công" ảnh đó rồi `CircleCrop` phóng lên thành
 * một vòng tròn trong suốt, còn ở banner thì ra ô trắng. Cả hai đều lệch với iOS (nền xám placeholder).
 *
 * Chạy **trước** `CircleCrop`/scale nên vẫn thấy kích thước gốc 1×1 (sau khi crop thì ảnh đã bị phóng
 * to, không còn phân biệt được nữa). Trả về ô 1×1 màu placeholder → bước sau phóng lên thành đúng
 * mảng xám như iOS.
 *
 * Luật "1×1 = rỗng" phải trùng iOS `UIImage.prmIsRenderable` — đổi bên này thì đổi cả bên kia.
 */
internal class PRMEmptyImageTransformation(
    @ColorInt private val fillColor: Int,
) : BitmapTransformation() {

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int,
    ): Bitmap {
        // Ảnh thật: trả nguyên input — Glide hiểu đây là no-op và không tạo resource mới.
        if (toTransform.width > 1 || toTransform.height > 1) return toTransform

        // Không log thì "ảnh không lên" bên Android là hộp đen: ô xám do server trả ảnh rỗng nhìn y hệt
        // ô xám do tải lỗi. Đối ứng log `[PRMRemoteImage] ảnh rỗng …` bên iOS.
        if (isPromotionSdkDebug()) {
            Timber.tag(PRM_IMAGE_LOG_TAG).w(
                "ảnh rỗng %d×%d — server không có ảnh thật, hiện nền placeholder thay vì ô trong suốt",
                toTransform.width,
                toTransform.height,
            )
        }

        return pool.get(1, 1, Bitmap.Config.ARGB_8888).apply { eraseColor(fillColor) }
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID_BYTES)
        messageDigest.update(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(fillColor).array())
    }

    override fun equals(other: Any?): Boolean =
        other is PRMEmptyImageTransformation && other.fillColor == fillColor

    override fun hashCode(): Int = ID.hashCode() * 31 + fillColor

    private companion object {
        const val ID = "com.ttcn.prm.ui.utils.PRMEmptyImageTransformation"
        val ID_BYTES: ByteArray = ID.toByteArray(Key.CHARSET)
    }
}
