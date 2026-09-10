package com.ttcn.prm.ui.feature.promotiondetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ttcn.prm.databinding.PrmFragmentContentDetailOfferPrmBinding
import com.ttcn.prm.ui.base.PRMBaseFragment
import com.ttcn.promotionsdk.presentation.promotiondetail.wrapPromotionHtml

internal class PrmContentDetailOfferFragment :
    PRMBaseFragment<PrmFragmentContentDetailOfferPrmBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): PrmFragmentContentDetailOfferPrmBinding {
        return PrmFragmentContentDetailOfferPrmBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        val htmlContent = arguments?.getString(ARG_CONTENT).orEmpty()
        initWebView()
        // Bọc HTML bằng hàm DÙNG CHUNG ở promotionLogic (iOS nạp đúng chuỗi này vào WKWebView).
        binding.wvContent.loadDataWithBaseURL(
            null,
            wrapPromotionHtml(htmlContent),
            MIME_TYPE_HTML,
            ENCODING_UTF8,
            null,
        )
    }

    /**
     * Nội dung tab là **HTML do backend trả** (mô tả / hướng dẫn của campaign). Nó chỉ cần render
     * text + ảnh, nên WebView bị siết xuống mức tối thiểu:
     *
     * | Cờ | Vì sao tắt |
     * |---|---|
     * | `javaScriptEnabled` | Bật là cho script trong nội dung của bên thứ ba chạy trong app host. Không có `addJavascriptInterface` nên không chạm được native, nhưng vẫn thừa quyền — nội dung ưu đãi không cần JS. |
     * | `allowFileAccess` / `allowContentAccess` | Không nạp `file://` hay `content://` bao giờ (chỉ `loadDataWithBaseURL(null, …)`), nên đây là quyền cấp không dùng. |
     * | `domStorageEnabled` | Chỉ có nghĩa khi có JS. |
     *
     * Đối ứng `PromotionDetailViewController.makeContentWebView()` bên iOS — sửa một bên thì sửa cả hai.
     */
    private fun initWebView() {
        with(binding.wvContent.settings) {
            allowFileAccess = false
            allowContentAccess = false
            javaScriptEnabled = false
            domStorageEnabled = false
            defaultFontSize = 14
        }

        binding.wvContent.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest,
            ): Boolean = true
        }
    }

    companion object {
        private const val ARG_CONTENT = "prm_detail_html_content"
        private const val MIME_TYPE_HTML = "text/html"
        private const val ENCODING_UTF8 = "UTF-8"

        fun newInstance(content: String): PrmContentDetailOfferFragment {
            return PrmContentDetailOfferFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CONTENT, content)
                }
            }
        }
    }
}
