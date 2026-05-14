package com.ttcn.promotionsdk.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class PRMBaseActivity<VB : ViewBinding> : AppCompatActivity() {

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
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    protected fun replaceFragment(
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

    protected fun removeFragment() {
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