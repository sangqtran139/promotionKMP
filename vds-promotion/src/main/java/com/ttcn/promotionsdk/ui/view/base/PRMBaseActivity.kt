package com.vds.vdsinappmessage.base

import android.os.Bundle
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel

abstract class PRMBaseActivity<VM : ViewModel, DB : ViewDataBinding> :
    AppCompatActivity() {

    /**
     * Optional ViewModel
     */
    protected open val viewModel: VM? = null

    protected lateinit var binding: DB

    abstract val layoutId: Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(
            this,
            layoutId
        )

        binding.lifecycleOwner = this

        setupObservers()
        setupUI()
        init()
    }

    open fun setupObservers() {}

    open fun setupUI() {}

    abstract fun init()

    protected fun showToast(message: CharSequence?) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Add Fragment
     */
    protected fun addFragment(
        fragment: Fragment,
        @IdRes containerId: Int,
        addToBackStack: Boolean = true
    ) {

        supportFragmentManager
            .beginTransaction()
            .replace(containerId, fragment)
            .apply {

                if (addToBackStack) {
                    addToBackStack(fragment::class.java.simpleName)
                }

            }
            .commit()
    }

    /**
     * Remove Fragment
     */
    protected fun removeFragment() {

        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            finish()
        }
    }
}