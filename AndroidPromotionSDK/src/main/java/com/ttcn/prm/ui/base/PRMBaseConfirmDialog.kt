package com.ttcn.prm.ui.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ttcn.prm.R
import com.ttcn.prm.databinding.PrmDialogConfirmBinding
import com.ttcn.prm.ui.utils.extension.retrieveColor
import com.ttcn.prm.ui.utils.extension.setFont

/**
 * Dialog xác nhận dùng chung của SDK (đồng ý / bỏ qua), gồm tiêu đề, nội dung, link phụ và
 * checkbox tuỳ chọn. Song sinh của `CEPBaseConfirmationDialog` bên `ttcn-inapp-message-android-sdk`.
 */
internal class PRMBaseConfirmDialog : PRMBaseDialog<PrmDialogConfirmBinding>() {

    private var titleText: CharSequence? = null
    private var contentText: CharSequence? = null
    private var contentLinkText: CharSequence? = null
    private var contentCheckBoxText: CharSequence? = null
    private var positiveText: CharSequence? = null
    private var negativeText: CharSequence? = null
    private var dismissOnPositive: Boolean = true
    private var positiveColor: Int = 0
    private var negativeColor: Int = 0
    private var isBoldNegativeText: Boolean = false
    private var isDelete: Boolean = false

    private var onPositive: (() -> Unit)? = null
    private var onNegative: (() -> Unit)? = null
    private var onClickLink: (() -> Unit)? = null

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): PrmDialogConfirmBinding =
        PrmDialogConfirmBinding.inflate(inflater, container, false)

    override fun init() {
        with(binding) {
            tvTitle.text = titleText
            tvContent.text = contentText

            contentLinkText?.let { linkText ->
                tvContentLink.visibility = View.VISIBLE
                tvContentLink.text = linkText
                tvContentLink.setOnClickListener {
                    onClickLink?.invoke()
                    dismissAllowingStateLoss()
                }
            } ?: run { tvContentLink.visibility = View.GONE }

            contentCheckBoxText?.let { checkBoxText ->
                layoutCheckBox.visibility = View.VISIBLE
                tvCheckBoxContent.text = checkBoxText
            } ?: run { layoutCheckBox.visibility = View.GONE }

            btnPositive.text = positiveText
            btnPositive.setOnClickListener {
                onPositive?.invoke()
                if (dismissOnPositive) {
                    dismissAllowingStateLoss()
                }
            }

            // Không có nút phụ (vd dialog thông báo đơn) → ẩn cả divider lẫn btnNegative. Chain
            // ngang 0dp/0dp trong prm_dialog_confirm.xml tự giãn btnPositive full-width khi
            // btnNegative GONE, không cần đổi constraint bằng tay.
            if (negativeText != null) {
                btnNegative.visibility = View.VISIBLE
                divider2.visibility = View.VISIBLE
                btnNegative.text = negativeText
                btnNegative.setOnClickListener {
                    onNegative?.invoke()
                    dismissAllowingStateLoss()
                }
            } else {
                btnNegative.visibility = View.GONE
                divider2.visibility = View.GONE
            }

            if (positiveColor != 0) {
                btnPositive.setTextColor(root.context.retrieveColor(positiveColor))
            }
            if (negativeColor != 0) {
                btnNegative.setTextColor(root.context.retrieveColor(negativeColor))
            }
            if (isBoldNegativeText) {
                btnNegative.setFont(R.style.PRMFontBold18)
            }
            if (isDelete) {
                btnPositive.setTextColor(root.context.retrieveColor(R.color.prm_tokenRed100))
            }
        }

        disableBack = true
    }

    override fun onDestroyView() {
        onPositive = null
        onNegative = null
        onClickLink = null
        super.onDestroyView()
    }

    companion object {

        fun newInstance(
            title: CharSequence,
            content: CharSequence,
            buttonPositive: CharSequence,
            buttonNegative: CharSequence? = null,
            contentLink: CharSequence? = null,
            contentCheckBox: CharSequence? = null,
            onPositive: (() -> Unit)? = null,
            onNegative: (() -> Unit)? = null,
            onClickLink: (() -> Unit)? = null,
            dismissOnPositive: Boolean = true,
            buttonPositiveColor: Int = 0,
            buttonNegativeColor: Int = 0,
            isBoldNegativeText: Boolean = false,
            isDelete: Boolean = false,
        ): PRMBaseConfirmDialog = PRMBaseConfirmDialog().apply {
            titleText = title
            contentText = content
            positiveText = buttonPositive
            negativeText = buttonNegative
            contentLinkText = contentLink
            contentCheckBoxText = contentCheckBox
            this.onPositive = onPositive
            this.onNegative = onNegative
            this.onClickLink = onClickLink
            this.dismissOnPositive = dismissOnPositive
            positiveColor = buttonPositiveColor
            negativeColor = buttonNegativeColor
            this.isBoldNegativeText = isBoldNegativeText
            this.isDelete = isDelete
        }
    }
}
