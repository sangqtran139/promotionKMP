package com.ttcn.promotionsdk.app.headless

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.ttcn.promotionsdk.app.databinding.FragmentHeadlessDemoBinding
import com.ttcn.prm.ui.base.PRMBaseFragment
import kotlinx.coroutines.launch

/**
 * Màn demo headless — soi gương `DemoHeadlessViewController.swift` bên iOS: cùng 5 nút, cùng thứ tự,
 * cùng chuỗi log. Sửa một bên thì sửa cả hai.
 */
class DemoHeadlessFragment : PRMBaseFragment<FragmentHeadlessDemoBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentHeadlessDemoBinding.inflate(inflater, container, false)

    private val viewModel: DemoHeadlessViewModel by viewModels()
    private val logLines = mutableListOf<String>()

    override fun setupUI() {
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnClearLog.setOnClickListener { clearLog() }

        binding.btnSearchVouchers.setOnClickListener {
            viewModel.searchVouchers()
        }
        binding.btnFindEligible.setOnClickListener {
            viewModel.findEligible()
        }
        binding.btnGetDetail.setOnClickListener {
            viewModel.getVoucherDetail()
        }
        binding.btnValidate.setOnClickListener {
            viewModel.validateDiscounts()
        }
        binding.btnCreateRedemption.setOnClickListener {
            viewModel.createRedemption()
        }
    }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { loading ->
                binding.btnSearchVouchers.isEnabled = !loading
                binding.btnFindEligible.isEnabled = !loading
                binding.btnGetDetail.isEnabled = !loading
                binding.btnValidate.isEnabled = !loading
                binding.btnCreateRedemption.isEnabled = !loading
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.log.collect { line ->
                appendLog(line)
            }
        }
    }

    private fun appendLog(line: String) {
        logLines.add(line)
        binding.tvLog.text = logLines.joinToString("\n")
        binding.scrollLog.post { binding.scrollLog.fullScroll(View.FOCUS_DOWN) }
    }

    private fun clearLog() {
        logLines.clear()
        binding.tvLog.text = ""
    }

    companion object {
        fun newInstance() = DemoHeadlessFragment()
    }
}
