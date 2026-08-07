package com.ttcn.promotionsdk.app.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

/**
 * Base Fragment **của app demo**, không phải của SDK — xem [AppBaseActivity].
 *
 * Chỉ giữ đúng ba thứ màn demo cần: binding theo vòng đời view, hook [setupUI]/[observeData], và
 * hai tiện ích điều hướng/toast.
 */
abstract class AppBaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null

    protected val binding: VB
        get() = requireNotNull(_binding) {
            "Binding is only valid between onCreateView and onDestroyView."
        }

    abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = inflateBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    open fun setupUI() {}
    open fun observeData() {}

    protected fun showToast(message: CharSequence?) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    /** Chồng [fragment] lên container đang chứa màn này. */
    protected fun addFragment(fragment: Fragment, addToBackStack: Boolean = true) {
        val containerId = (view?.parent as? ViewGroup)?.id
        if (containerId == null || containerId == View.NO_ID) {
            showToast("Cannot navigate: no valid container found.")
            return
        }
        val tag = fragment::class.java.simpleName
        parentFragmentManager.beginTransaction()
            .add(containerId, fragment, tag)
            .apply { if (addToBackStack) addToBackStack(tag) }
            .commit()
    }
}
