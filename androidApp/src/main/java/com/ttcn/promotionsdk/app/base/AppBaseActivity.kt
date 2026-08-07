package com.ttcn.promotionsdk.app.base

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

/**
 * Base Activity **của app demo**, không phải của SDK.
 *
 * SDK chỉ phơi ra `com.ttcn.prm.entry.*`; `PRMBaseActivity` là nội bộ và host không với tới được.
 * App demo đóng vai host thật nên tự dựng base của mình — vài chục dòng, đúng thứ nó cần.
 */
abstract class AppBaseActivity<VB : ViewBinding> : AppCompatActivity() {

    private var _binding: VB? = null

    protected val binding: VB
        get() = requireNotNull(_binding) {
            "Binding is only valid after onCreate and before onDestroy."
        }

    abstract fun inflateBinding(layoutInflater: LayoutInflater): VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = inflateBinding(layoutInflater)
        setContentView(binding.root)
        setupUI()
        observeData()
    }

    open fun setupUI() {}
    open fun observeData() {}

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
