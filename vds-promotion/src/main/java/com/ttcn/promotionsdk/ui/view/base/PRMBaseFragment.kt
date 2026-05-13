package com.vds.vdsinappmessage.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel

abstract class PRMBaseFragment<VM : ViewModel, DB : ViewDataBinding> :
    Fragment() {

    protected abstract val viewModel: VM

    protected lateinit var binding: DB

    abstract val layoutId: Int

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(
            inflater,
            layoutId,
            container,
            false
        )

        binding.lifecycleOwner = viewLifecycleOwner

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupUI()
        init()
    }

    open fun setupObservers() {}

    open fun setupUI() {}

    abstract fun init()

    open fun onBackFragment() {

        val manager = requireActivity().supportFragmentManager

        if (manager.backStackEntryCount > 1) {
            manager.popBackStack()
        } else {
            requireActivity().finish()
        }
    }

    protected fun showToast(message: CharSequence?) {

        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Add / Replace Fragment
     */
    protected fun replaceFragment(
        fragment: Fragment,
        @IdRes containerId: Int,
        addToBackStack: Boolean = true
    ) {

        requireActivity()
            .supportFragmentManager
            .beginTransaction()
            .replace(containerId, fragment)
            .apply {

                if (addToBackStack) {
                    addToBackStack(
                        fragment::class.java.simpleName
                    )
                }

            }
            .commit()
    }

    protected fun addFragment(
        fragment: Fragment,
        addToBackStack: Boolean = true
    ) {

        val containerId = (requireView().parent as ViewGroup).id
        val tag = fragment::class.java.simpleName

        requireActivity()
            .supportFragmentManager
            .beginTransaction()
            .add(containerId, fragment, tag)
            .apply {

                if (addToBackStack) {
                    addToBackStack(tag)
                }

            }
            .commit()
    }

}