package com.ttcn.prm.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

internal abstract class PRMBaseActivity<VB : ViewBinding> : AppCompatActivity() {

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

    protected fun showToast(message: CharSequence?) {
        // Toast bị gom sau [PromotionToastGate] — mặc định TẮT (lỗi vẫn được bắt, chỉ không hiện).
        if (!PromotionToastGate.isEnabled) return
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Thay nội dung [containerId] bằng [fragment], trong FragmentManager của Activity.
     *
     * Khác [PRMBaseFragment.replaceFragment]: ở đây Activity **là** chủ container nên dùng thẳng
     * `supportFragmentManager`; bên Fragment phải bám `parentFragmentManager` của chính nó.
     */
    protected fun replaceFragment(
        fragment: Fragment,
        @IdRes containerId: Int,
        addToBackStack: Boolean = true
    ) {
        val tag = fragment::class.java.simpleName
        supportFragmentManager.beginTransaction()
            .replace(containerId, fragment, tag)
            .apply { if (addToBackStack) addToBackStack(tag) }
            .commit()
    }

    /**
     * Lùi một màn; hết màn thì đóng Activity.
     *
     * `finish()` ở đây là hợp lệ vì đây là Activity của **chính subclass gọi hàm này**, khác hẳn
     * `PRMBaseFragment.goBack()` — fragment SDK nằm trong Activity của host nên tuyệt đối không được
     * `finish()`, nó chỉ trả sự kiện về `onBackPressedDispatcher`.
     */
    protected fun goBackOrFinish() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}