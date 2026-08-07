package com.ttcn.prm.ui.base

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.annotation.StyleRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.viewbinding.ViewBinding
import com.ttcn.prm.R
import com.ttcn.prm.ui.utils.extension.screenWidth

/**
 * Base cho dialog dùng chung của SDK — song sinh của [PRMBaseFragment] nhưng cho [DialogFragment].
 * Tự set theme không tiêu đề, nền trong suốt, dim nền, và ép LIGHT như các màn SDK khác.
 */
internal abstract class PRMBaseDialog<VB : ViewBinding> : DialogFragment() {

    private var _binding: VB? = null

    protected val binding: VB
        get() = requireNotNull(_binding) {
            "Binding is only valid between onCreateView and onDestroyView."
        }

    abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    var cancelOnTouchOutside: Boolean = true
    var disableBack: Boolean = false
    var extraTag: String = ""
    var positionDialog: Int = Gravity.CENTER

    @StyleRes
    var animationId: Int = 0

    private var onDismissListener: (() -> Unit)? = null

    fun doOnDismiss(listener: () -> Unit) {
        onDismissListener = listener
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.invoke()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.PRMBaseTheme_Dialog)
    }

    /** Ép LIGHT như [PRMBaseFragment.onGetLayoutInflater] — dialog không đi qua base fragment. */
    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val base = super.onGetLayoutInflater(savedInstanceState)
        val themed = ContextThemeWrapper(requireContext(), R.style.PRMForceLight)
        return base.cloneInContext(themed)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCanceledOnTouchOutside(cancelOnTouchOutside)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(dialog.context.screenWidth(), ViewGroup.LayoutParams.WRAP_CONTENT)

        val windowParams = dialog.window?.attributes
        windowParams?.dimAmount = 0.6f
        windowParams?.flags?.let {
            windowParams?.flags = it.or(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
        windowParams?.gravity = positionDialog
        if (animationId != 0) {
            windowParams?.windowAnimations = animationId
        }
        dialog.window?.attributes = windowParams
        isCancelable = !disableBack
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = inflateBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    override fun show(manager: FragmentManager, tag: String?) {
        val finalTag = "${javaClass.simpleName}$extraTag"
        if (!manager.isDestroyed) {
            runCatching {
                manager.beginTransaction()
                    .add(this, finalTag)
                    .commitAllowingStateLoss()
            }
        }
    }

    /** Gọi khi view của dialog vừa dựng xong — nơi bind dữ liệu. */
    abstract fun init()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
