package com.ttcn.prm.ui.feature.promotion.mypromotion

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ttcn.prm.R
import com.ttcn.prm.databinding.BottomSheetServiceSelectorBinding
import com.ttcn.prm.ui.base.PromotionToastGate
import com.ttcn.prm.ui.feature.promotion.mypromotion.adapter.ServiceSelectorAdapter

internal class ServiceSelectorBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetServiceSelectorBinding? = null
    private val binding get() = requireNotNull(_binding)

    private var services: List<ServiceSelectorUiItem> = emptyList()
    private var onServiceSelected: ((ServiceSelectorUiItem) -> Unit)? = null

    private val adapter = ServiceSelectorAdapter(VISIBLE_ITEM_COUNT) { service ->
        onServiceSelected?.invoke(service)
        dismiss()
    }

    override fun getTheme(): Int = R.style.PRMBaseBottomSheetDialog

    /** Ép LIGHT (force-dark=false) như [com.ttcn.prm.ui.base.PRMBaseFragment] — bottom sheet không qua base. */
    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val base = super.onGetLayoutInflater(savedInstanceState)
        val themed = ContextThemeWrapper(requireContext(), R.style.PRMForceLight)
        return base.cloneInContext(themed)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetServiceSelectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvServices.apply {
            // **Một hàng, vuốt ngang** — TLNV MOB_002 item #6: "Danh sách hiển thị trên 1 dòng, màn
            // hình hiển thị tối đa 3 dịch vụ, cho phép vuốt sang trái/phải để xem thêm".
            // Bề rộng item do adapter chia lúc `onCreateViewHolder` (xem lý do ở đó).
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = this@ServiceSelectorBottomSheet.adapter
            itemAnimator = null
        }
        adapter.submitList(services)
        val hasServices = services.isNotEmpty()
        binding.rvServices.isVisible = hasServices
        binding.tvEmpty.isVisible = !hasServices
    }

    /**
     * Chiều cao sheet — **đối ứng `computedCardHeight()` bên iOS** (trần 60% màn hình + lưới cuộn được).
     *
     * Hai thứ phải chỉnh, vì mặc định của `BottomSheetDialogFragment` hỏng với lưới nhiều hàng:
     * - **Mở ở COLLAPSED** với peek auto (~9/16 bề ngang, cỡ 230dp) → từ 2 hàng (4 dịch vụ) là hàng
     *   dưới đã khuất, user tưởng bị cắt. → `skipCollapsed` + mở thẳng EXPANDED.
     * - **Không có trần**: content cao hơn màn hình thì phần dư tràn xuống dưới và không cuộn tới được
     *   (kéo trong lưới = kéo sheet xuống). → `maxHeight` ép sheet đo lại theo AT_MOST, RecyclerView
     *   nhận phần còn lại và tự cuộn (layout đã bỏ `nestedScrollingEnabled="false"`).
     */
    override fun onStart() {
        super.onStart()
        val behavior = (dialog as? BottomSheetDialog)?.behavior ?: return
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.maxHeight = (resources.displayMetrics.heightPixels * MAX_HEIGHT_RATIO).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        onServiceSelected = null
    }

    companion object {
        /** Số dịch vụ nhìn thấy cùng lúc trên 1 hàng; dư ra thì vuốt ngang. Khớp iOS `visibleItemCount`. */
        private const val VISIBLE_ITEM_COUNT = 3

        /** Trần chiều cao sheet theo màn hình — khớp `maxHeightRatio` của iOS. */
        private const val MAX_HEIGHT_RATIO = 0.6f
        const val TAG = "prm_service_selector"

        fun newInstance(
            services: List<ServiceSelectorUiItem>,
            onServiceSelected: (ServiceSelectorUiItem) -> Unit,
        ): ServiceSelectorBottomSheet = ServiceSelectorBottomSheet().apply {
            this.services = services
            this.onServiceSelected = onServiceSelected
        }

        /**
         * Lối vào DUY NHẤT của bottom sheet — mọi màn (Ưu đãi của tôi, Tìm kiếm, Chi tiết) gọi qua
         * đây để hai luật sau không phải lặp lại ở từng màn:
         *
         * 1. **Đúng 1 dịch vụ → chọn thẳng, không mở sheet** (TLNV MOB_002 control #5): bắt user mở
         *    sheet rồi bấm lại đúng cái đó là thừa. 0 dịch vụ vẫn mở (sheet hiện "Không có dịch vụ
         *    thoả mãn") để user biết vì sao không đi tiếp được.
         * 2. **Toast xác nhận** khi đã chọn — tạm thời, để hai nền tảng cùng phản hồi giống nhau
         *    trong lúc chưa có đích điều hướng thật (`ServiceSelected` bên VM vẫn là TODO).
         *
         * Đối ứng `ServiceSelectorBottomSheet.present(from:services:onServiceSelected:)` bên iOS.
         */
        fun present(
            host: Fragment,
            services: List<ServiceSelectorUiItem>,
            onServiceSelected: (ServiceSelectorUiItem) -> Unit,
        ) {
            val notify: (ServiceSelectorUiItem) -> Unit = { service ->
                val name = service.serviceName.ifBlank { service.serviceCode }
                PromotionToastGate.showAlways(
                    host.requireContext(),
                    host.getString(R.string.prm_service_selected, name),
                )
                onServiceSelected(service)
            }
            val only = services.singleOrNull()
            if (only != null) {
                notify(only)
                return
            }
            if (host.childFragmentManager.findFragmentByTag(TAG) != null) return
            newInstance(services, notify).show(host.childFragmentManager, TAG)
        }
    }
}
