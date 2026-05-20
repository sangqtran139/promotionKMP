package com.ttcn.promotionsdk.app.theme

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import com.ttcn.promotionsdk.app.R
import com.ttcn.promotionsdk.app.databinding.FragmentThemePreviewBinding
import com.ttcn.promotionsdk.app.databinding.ItemThemeColorTokenBinding
import com.ttcn.promotionsdk.app.databinding.ItemThemePreviewCardBinding
import com.ttcn.promotionsdk.app.databinding.ItemThemeSliderTokenBinding
import com.ttcn.promotionsdk.databinding.ItemChoosePromotionBinding
import com.ttcn.promotionsdk.ui.entry.PromotionTheme
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.PromotionItem
import com.ttcn.promotionsdk.ui.theme.PromotionListItemTheme
import com.ttcn.promotionsdk.ui.theme.PromotionThemeDisplay
import com.ttcn.promotionsdk.ui.theme.PromotionThemeDisplay.Defaults
import com.ttcn.promotionsdk.ui.theme.TabUnderlineTheme
import com.ttcn.promotionsdk.ui.theme.toToken
import com.ttcn.promotionsdk.ui.utils.enum.PRMSearchType
import com.ttcn.promotionsdk.ui.utils.extension.TokenColorParser
import com.ttcn.promotionsdk.ui.utils.extension.TokenDrawableFactory
import com.ttcn.promotionsdk.ui.utils.view.PRMButton
import com.ttcn.promotionsdk.ui.utils.view.PRMEndowView
import com.ttcn.promotionsdk.ui.utils.view.PRMSearchField
import kotlin.math.roundToInt
import com.ttcn.promotionsdk.R as SdkR

class ThemePreviewFragment : Fragment() {

    private var _binding: FragmentThemePreviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var preferenceManager: ThemePreferenceManager
    private lateinit var sdkDefaults: Defaults
    private lateinit var themeDisplay: Defaults

    private val colorFields = mutableMapOf<String, () -> String?>()
    private val colorSetters = mutableMapOf<String, (String?) -> Unit>()
    private val sliderFields = mutableMapOf<String, () -> Float?>()
    private val sliderSetters = mutableMapOf<String, (Float?) -> Unit>()
    private var cardBuildIndex = 0

    private var headerTopPadding = 0
    private var footerBottomPadding = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentThemePreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferenceManager = ThemePreferenceManager(requireContext())
        sdkDefaults = PromotionTheme.loadDisplayDefaults(requireContext())
        reloadDisplayValues()

        headerTopPadding = binding.headerContainer.paddingTop
        footerBottomPadding = binding.footer.paddingBottom
        applyWindowInsets()

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnReset.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.prm_theme_reset_success),
                Toast.LENGTH_SHORT,
            ).show()
            onReset()
        }
        binding.btnApply.setOnClickListener { onApply() }

        registerFields()
        buildCards()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.headerContainer.updatePadding(top = headerTopPadding + statusBars.top)
            binding.footer.updatePadding(bottom = footerBottomPadding + navBars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun reloadDisplayValues() {
        val saved = preferenceManager.load()
        themeDisplay = PromotionTheme.mergeDisplayWithSaved(requireContext(), sdkDefaults, saved)
    }

    private fun registerFields() {
        colorFields.clear()
        colorSetters.clear()
        sliderFields.clear()
        sliderSetters.clear()

        fun regColor(key: String, getter: () -> String?, setter: (String?) -> Unit) {
            colorFields[key] = getter
            colorSetters[key] = setter
        }

        fun regSlider(key: String, getter: () -> Float?, setter: (Float?) -> Unit) {
            sliderFields[key] = getter
            sliderSetters[key] = setter
        }

        val b = { themeDisplay.button }
        regColor("button.backgroundColor", { b().backgroundColor }) {
            themeDisplay = themeDisplay.copy(button = b().copy(backgroundColor = it))
        }
        regColor("button.textColor", { b().textColor }) {
            themeDisplay = themeDisplay.copy(button = b().copy(textColor = it))
        }
        regColor("button.shadowColor", { b().shadowColor }) {
            themeDisplay = themeDisplay.copy(button = b().copy(shadowColor = it))
        }
        regSlider("button.cornerRadius", { b().cornerRadius }) {
            themeDisplay = themeDisplay.copy(button = b().copy(cornerRadius = it))
        }

        val s = { themeDisplay.searchBar }
        regColor("search.borderColor", { s().borderColor }) {
            themeDisplay = themeDisplay.copy(searchBar = s().copy(borderColor = it))
        }
        regColor("search.hintTextColor", { s().hintTextColor }) {
            themeDisplay = themeDisplay.copy(searchBar = s().copy(hintTextColor = it))
        }
        regColor("search.textColor", { s().textColor }) {
            themeDisplay = themeDisplay.copy(searchBar = s().copy(textColor = it))
        }
        regColor("search.iconColor", { s().iconColor }) {
            themeDisplay = themeDisplay.copy(searchBar = s().copy(iconColor = it))
        }
        regSlider("search.cornerRadius", { s().cornerRadius }) {
            themeDisplay = themeDisplay.copy(searchBar = s().copy(cornerRadius = it))
        }

        val l = { themeDisplay.listItem }
        regColor("list.linkTextColor", { l().linkTextColor }) {
            themeDisplay = themeDisplay.copy(listItem = l().copy(linkTextColor = it))
        }
        regColor("list.usedBadgeTextColor", { l().usedBadgeTextColor }) {
            themeDisplay = themeDisplay.copy(listItem = l().copy(usedBadgeTextColor = it))
        }
        regColor("list.usedBadgeBackgroundColor", { l().usedBadgeBackgroundColor }) {
            themeDisplay = themeDisplay.copy(listItem = l().copy(usedBadgeBackgroundColor = it))
        }
        regColor("list.radioButtonStrokeColor", { l().radioButtonStrokeColor }) {
            themeDisplay = themeDisplay.copy(listItem = l().copy(radioButtonStrokeColor = it))
        }
        regColor("list.radioButtonSelectedStrokeColor", { l().radioButtonSelectedStrokeColor }) {
            themeDisplay =
                themeDisplay.copy(listItem = l().copy(radioButtonSelectedStrokeColor = it))
        }

        val t = { themeDisplay.tabChip }
        regColor("tabChip.activeBackgroundColor", { t().activeBackgroundColor }) {
            themeDisplay = themeDisplay.copy(tabChip = t().copy(activeBackgroundColor = it))
        }
        regColor("tabChip.inactiveBackgroundColor", { t().inactiveBackgroundColor }) {
            themeDisplay = themeDisplay.copy(tabChip = t().copy(inactiveBackgroundColor = it))
        }
        regColor("tabChip.activeTextColor", { t().activeTextColor }) {
            themeDisplay = themeDisplay.copy(tabChip = t().copy(activeTextColor = it))
        }
        regColor("tabChip.inactiveTextColor", { t().inactiveTextColor }) {
            themeDisplay = themeDisplay.copy(tabChip = t().copy(inactiveTextColor = it))
        }
        regSlider("tabChip.cornerRadius", { t().cornerRadius }) {
            themeDisplay = themeDisplay.copy(tabChip = t().copy(cornerRadius = it))
        }

        val u = { themeDisplay.tabUnderline }
        regColor("tabUnderline.indicatorColor", { u().indicatorColor }) {
            themeDisplay = themeDisplay.copy(tabUnderline = u().copy(indicatorColor = it))
        }
        regColor("tabUnderline.activeTextColor", { u().activeTextColor }) {
            themeDisplay = themeDisplay.copy(tabUnderline = u().copy(activeTextColor = it))
        }
        regColor("tabUnderline.inactiveTextColor", { u().inactiveTextColor }) {
            themeDisplay = themeDisplay.copy(tabUnderline = u().copy(inactiveTextColor = it))
        }
        regColor("tabUnderline.backgroundColor", { u().backgroundColor }) {
            themeDisplay = themeDisplay.copy(tabUnderline = u().copy(backgroundColor = it))
        }

        val d = { themeDisplay.discountBadge }
        regColor("discount.availableTextColor", { d().availableTextColor }) {
            themeDisplay = themeDisplay.copy(discountBadge = d().copy(availableTextColor = it))
        }
        regColor("discount.unavailableTextColor", { d().unavailableTextColor }) {
            themeDisplay = themeDisplay.copy(discountBadge = d().copy(unavailableTextColor = it))
        }
        regColor("discount.availableBackgroundColor", { d().availableBackgroundColor }) {
            themeDisplay =
                themeDisplay.copy(discountBadge = d().copy(availableBackgroundColor = it))
        }
        regColor("discount.unavailableBackgroundColor", { d().unavailableBackgroundColor }) {
            themeDisplay =
                themeDisplay.copy(discountBadge = d().copy(unavailableBackgroundColor = it))
        }
        regColor("discount.actionTextColor", { d().actionTextColor }) {
            themeDisplay = themeDisplay.copy(discountBadge = d().copy(actionTextColor = it))
        }
    }

    private fun buildCards() {
        binding.cardsContainer.removeAllViews()
        cardBuildIndex = 0

        addCard(
            titleRes = R.string.prm_theme_section_button,
            tokenCount = 4,
            iconRes = R.drawable.prm_ic_theme_button,
            fields = listOf(
                TokenField.Color("button.backgroundColor", R.string.prm_theme_lbl_background_color),
                TokenField.Color("button.textColor", R.string.prm_theme_lbl_text_color),
                TokenField.Color("button.shadowColor", R.string.prm_theme_lbl_shadow_color),
                TokenField.Slider("button.cornerRadius", R.string.prm_theme_lbl_corner_radius),
            ),
            onPreview = { showButtonPreview() },
        )

        addCard(
            titleRes = R.string.prm_theme_section_search_bar,
            tokenCount = 5,
            iconRes = SdkR.drawable.prm_ic_search_endow,
            fields = listOf(
                TokenField.Color("search.borderColor", R.string.prm_theme_lbl_border_color),
                TokenField.Color("search.hintTextColor", R.string.prm_theme_lbl_hint_text_color),
                TokenField.Color("search.textColor", R.string.prm_theme_lbl_text_color),
                TokenField.Color("search.iconColor", R.string.prm_theme_lbl_icon_color),
                TokenField.Slider("search.cornerRadius", R.string.prm_theme_lbl_corner_radius),
            ),
            onPreview = { showSearchPreview() },
        )

        addCard(
            titleRes = R.string.prm_theme_section_list_item,
            tokenCount = 5,
            iconRes = R.drawable.prm_ic_theme_button,
            fields = listOf(
                TokenField.Color("list.linkTextColor", R.string.prm_theme_lbl_link_text_color),
                TokenField.Color(
                    "list.usedBadgeTextColor",
                    R.string.prm_theme_lbl_used_badge_text_color
                ),
                TokenField.Color(
                    "list.usedBadgeBackgroundColor",
                    R.string.prm_theme_lbl_used_badge_background_color
                ),
                TokenField.Color(
                    "list.radioButtonStrokeColor",
                    R.string.prm_theme_lbl_radio_stroke_color
                ),
                TokenField.Color(
                    "list.radioButtonSelectedStrokeColor",
                    R.string.prm_theme_lbl_radio_selected_fill,
                ),
            ),
            onPreview = { showListItemPreview() },
        )

        addCard(
            titleRes = R.string.prm_theme_section_tab_chip,
            tokenCount = 5,
            iconRes = R.drawable.prm_ic_theme_tab,
            fields = listOf(
                TokenField.Color(
                    "tabChip.activeBackgroundColor",
                    R.string.prm_theme_lbl_active_background_color
                ),
                TokenField.Color(
                    "tabChip.inactiveBackgroundColor",
                    R.string.prm_theme_lbl_inactive_background_color
                ),
                TokenField.Color(
                    "tabChip.activeTextColor",
                    R.string.prm_theme_lbl_active_text_color
                ),
                TokenField.Color(
                    "tabChip.inactiveTextColor",
                    R.string.prm_theme_lbl_inactive_text_color
                ),
                TokenField.Slider("tabChip.cornerRadius", R.string.prm_theme_lbl_corner_radius),
            ),
            onPreview = { showTabChipPreview() },
        )

        addCard(
            titleRes = R.string.prm_theme_section_tab_underline,
            tokenCount = 4,
            iconRes = R.drawable.prm_ic_theme_tab,
            fields = listOf(
                TokenField.Color(
                    "tabUnderline.indicatorColor",
                    R.string.prm_theme_lbl_indicator_color
                ),
                TokenField.Color(
                    "tabUnderline.activeTextColor",
                    R.string.prm_theme_lbl_active_text_color
                ),
                TokenField.Color(
                    "tabUnderline.inactiveTextColor",
                    R.string.prm_theme_lbl_inactive_text_color
                ),
                TokenField.Color(
                    "tabUnderline.backgroundColor",
                    R.string.prm_theme_lbl_background_tab
                ),
            ),
            onPreview = { showTabUnderlinePreview() },
        )

        addCard(
            titleRes = R.string.prm_theme_section_discount_badge,
            tokenCount = 5,
            iconRes = R.drawable.prm_ic_theme_discount,
            fields = listOf(
                TokenField.Color(
                    "discount.availableTextColor",
                    R.string.prm_theme_lbl_available_text_color
                ),
                TokenField.Color(
                    "discount.unavailableTextColor",
                    R.string.prm_theme_lbl_unavailable_text_color
                ),
                TokenField.Color(
                    "discount.availableBackgroundColor",
                    R.string.prm_theme_lbl_available_background_color
                ),
                TokenField.Color(
                    "discount.unavailableBackgroundColor",
                    R.string.prm_theme_lbl_unavailable_background_color
                ),
                TokenField.Color(
                    "discount.actionTextColor",
                    R.string.prm_theme_lbl_action_text_color
                ),
            ),
            onPreview = { showEndowPreview() },
        )
    }

    private sealed class TokenField {
        abstract val key: String

        data class Color(override val key: String, @param:StringRes val labelResId: Int) :
            TokenField()

        data class Slider(override val key: String, @param:StringRes val labelResId: Int) :
            TokenField()
    }

    private fun addCard(
        @StringRes titleRes: Int,
        tokenCount: Int,
        iconRes: Int,
        fields: List<TokenField>,
        onPreview: () -> Unit,
    ) {
        val inflater = LayoutInflater.from(requireContext())
        val cardBinding =
            ItemThemePreviewCardBinding.inflate(inflater, binding.cardsContainer, false)
        val marginTop = if (cardBuildIndex == 0) 0 else {
            resources.getDimensionPixelSize(R.dimen.prm_theme_card_margin_top)
        }
        val initiallyExpanded = cardBuildIndex == 0
        cardBuildIndex++

        cardBinding.txtCardTitle.text = getString(
            R.string.prm_theme_card_title_format,
            getString(titleRes),
            tokenCount,
        )
        cardBinding.imgGroupIcon.setImageResource(iconRes)
        cardBinding.btnPreview.setOnClickListener {
            syncFieldsFromViews()
            onPreview()
        }

        var expanded = initiallyExpanded
        updateCardExpandedState(cardBinding, expanded)

        cardBinding.cardHeader.setOnClickListener {
            expanded = !expanded
            updateCardExpandedState(cardBinding, expanded)
        }

        fields.forEachIndexed { index, field ->
            val row = when (field) {
                is TokenField.Color -> createColorRow(inflater, field.key, field.labelResId)
                is TokenField.Slider -> createSliderRow(inflater, field.key, field.labelResId)
            }
            if (index == fields.lastIndex) {
                row.findViewById<View>(R.id.viewDivider)?.visibility = View.GONE
            }
            cardBinding.tokenContainer.addView(row)
        }

        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        lp.topMargin = marginTop
        cardBinding.root.layoutParams = lp
        binding.cardsContainer.addView(cardBinding.root)
    }

    private fun updateCardExpandedState(card: ItemThemePreviewCardBinding, expanded: Boolean) {
        card.cardBody.isVisible = expanded
        card.imgChevron.setImageResource(
            if (expanded) R.drawable.prm_ic_chevron_up else R.drawable.prm_ic_chevron_down,
        )
        if (!expanded) {
            val collapsedHeight =
                resources.getDimensionPixelSize(R.dimen.prm_theme_card_collapsed_height)
            val padding = resources.getDimensionPixelSize(R.dimen.prm_theme_card_padding) * 2
            card.cardHeader.minimumHeight = (collapsedHeight - padding).coerceAtLeast(0)
        } else {
            card.cardHeader.minimumHeight = 0
        }
    }

    private fun createColorRow(
        inflater: LayoutInflater,
        key: String,
        @StringRes labelResId: Int
    ): View {
        val row = ItemThemeColorTokenBinding.inflate(inflater, binding.cardsContainer, false)
        val initialHex = colorFields[key]?.invoke().orEmpty()
        row.txtTokenLabel.text = getString(labelResId)
        row.edtColorValue.setText(initialHex)
        row.edtColorValue.tag = key
        updatePaletteSwatch(row.viewColorSwatch, row.imgPalette, initialHex)

        row.edtColorValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val raw = s?.toString().orEmpty()
                if (raw.isBlank()) {
                    updatePaletteSwatch(row.viewColorSwatch, row.imgPalette, null)
                    return
                }
                updatePaletteSwatch(row.viewColorSwatch, row.imgPalette, normalizeHex(raw))
            }
        })

        row.edtColorValue.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                commitColorFromField(key, row.edtColorValue, row.viewColorSwatch, row.imgPalette)
            }
        }
        row.edtColorValue.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                commitColorFromField(key, row.edtColorValue, row.viewColorSwatch, row.imgPalette)
                row.edtColorValue.clearFocus()
                true
            } else {
                false
            }
        }
        row.btnPalette.setOnClickListener {
            showColorPickerDialog(
                key,
                labelResId,
                row.edtColorValue,
                row.viewColorSwatch,
                row.imgPalette
            )
        }
        return row.root
    }

    private fun commitColorFromField(
        key: String,
        editText: EditText,
        swatch: View?,
        paletteIcon: View?,
    ) {
        val raw = editText.text.toString()
        if (raw.isBlank()) {
            colorSetters[key]?.invoke(null)
            if (swatch != null && paletteIcon != null) updatePaletteSwatch(
                swatch,
                paletteIcon,
                null
            )
            return
        }
        val normalized = normalizeHex(raw)
        if (normalized != null) {
            editText.setText(normalized)
            colorSetters[key]?.invoke(normalized)
            if (swatch != null && paletteIcon != null) updatePaletteSwatch(
                swatch,
                paletteIcon,
                normalized
            )
        }
    }

    private fun findColorRowContainer(editText: EditText): ViewGroup? {
        var current: View? = editText.parent as? View
        while (current is ViewGroup) {
            if (current.findViewById<View>(R.id.viewColorSwatch) != null) return current
            current = current.parent as? View
        }
        return null
    }

    private fun normalizeHex(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        val withHash = if (trimmed.startsWith("#")) trimmed else "#$trimmed"
        if (TokenColorParser.parse(withHash) == null) return null
        return withHash.uppercase()
    }

    private fun updatePaletteSwatch(swatch: View, paletteIcon: View, hex: String?) {
        val color = hex?.let { TokenColorParser.parse(it) }
        if (color != null) {
            swatch.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
                val strokePx = (1 * resources.displayMetrics.density).toInt().coerceAtLeast(1)
                setStroke(
                    strokePx,
                    ContextCompat.getColor(requireContext(), R.color.prm_theme_card_stroke)
                )
            }
            swatch.visibility = View.VISIBLE
            paletteIcon.visibility = View.GONE
        } else {
            swatch.visibility = View.GONE
            paletteIcon.visibility = View.VISIBLE
        }
    }

    private fun showColorPickerDialog(
        key: String,
        @StringRes labelResId: Int,
        targetEditText: EditText,
        swatch: View,
        paletteIcon: View,
    ) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_theme_color_picker, null, false)

        val txtPickerTokenLabel = dialogView.findViewById<TextView>(R.id.txtPickerTokenLabel)
        val preview = dialogView.findViewById<View>(R.id.viewPickerPreview)
        val edtHex = dialogView.findViewById<EditText>(R.id.edtPickerHex)
        val seekRed = dialogView.findViewById<SeekBar>(R.id.seekRed)
        val seekGreen = dialogView.findViewById<SeekBar>(R.id.seekGreen)
        val seekBlue = dialogView.findViewById<SeekBar>(R.id.seekBlue)
        val txtRedCurrent = dialogView.findViewById<TextView>(R.id.txtRedCurrent)
        val txtGreenCurrent = dialogView.findViewById<TextView>(R.id.txtGreenCurrent)
        val txtBlueCurrent = dialogView.findViewById<TextView>(R.id.txtBlueCurrent)

        txtPickerTokenLabel.text = getString(labelResId)

        var includeAlpha = false
        var currentArgb = Color.BLACK
        val initial = normalizeHex(targetEditText.text.toString())
        if (initial != null) {
            TokenColorParser.parse(initial)?.let { parsed ->
                currentArgb = parsed
                includeAlpha = initial.length == 9
            }
        }

        fun refreshFromArgb(argb: Int) {
            currentArgb = argb
            preview.setBackgroundColor(argb)
            seekRed.progress = Color.red(argb)
            seekGreen.progress = Color.green(argb)
            seekBlue.progress = Color.blue(argb)
            txtRedCurrent.text = Color.red(argb).toString()
            txtGreenCurrent.text = Color.green(argb).toString()
            txtBlueCurrent.text = Color.blue(argb).toString()
            val hexText =
                if (includeAlpha && Color.alpha(argb) != 255) {
                    String.format("#%08X", argb)
                } else {
                    String.format("#%06X", (0xFFFFFF and argb))
                }
            edtHex.setText(hexText)
        }

        fun bindSeekListeners() {
            val listener = object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    val alpha = 255
                    refreshFromArgb(
                        Color.argb(
                            alpha,
                            seekRed.progress,
                            seekGreen.progress,
                            seekBlue.progress
                        )
                    )
                }

                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            }
            seekRed.setOnSeekBarChangeListener(listener)
            seekGreen.setOnSeekBarChangeListener(listener)
            seekBlue.setOnSeekBarChangeListener(listener)
        }

        refreshFromArgb(currentArgb)
        bindSeekListeners()

        edtHex.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                normalizeHex(edtHex.text.toString())?.let { hex ->
                    includeAlpha = hex.length == 9
                    TokenColorParser.parse(hex)?.let { refreshFromArgb(it) }
                }
                true
            } else {
                false
            }
        }

        val btnCancel = dialogView.findViewById<TextView>(R.id.btnPickerCancel)
        val btnOk = dialogView.findViewById<TextView>(R.id.btnPickerOk)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnOk.setOnClickListener {
            val hex = normalizeHex(edtHex.text.toString())
            if (hex != null) {
                targetEditText.setText(hex)
                colorSetters[key]?.invoke(hex)
                updatePaletteSwatch(swatch, paletteIcon, hex)
            }
            dialog.dismiss()
        }
        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.let { window ->
            val widthPx = (resources.displayMetrics.widthPixels * 0.92f).toInt()
            window.setLayout(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun createSliderRow(
        inflater: LayoutInflater,
        key: String,
        @StringRes labelResId: Int
    ): View {
        val row = ItemThemeSliderTokenBinding.inflate(inflater, binding.cardsContainer, false)
        val minProgress = 0
        val maxProgress = 999
        val value = sliderFields[key]?.invoke()?.roundToInt() ?: 0
        val coerced = value.coerceIn(minProgress, maxProgress)

        row.txtSliderLabel.text =
            getString(R.string.prm_theme_slider_label_format, getString(labelResId))
        row.txtSliderMin.text = minProgress.toString()
        row.txtSliderMax.text = maxProgress.toString()
        fun updateCurrentDisplay(progress: Int) {
            row.txtSliderCurrent.text = getString(R.string.prm_theme_slider_value_dp, progress)
        }
        updateCurrentDisplay(coerced)

        row.seekToken.max = maxProgress
        row.seekToken.progress = coerced
        row.seekToken.tag = key
        row.seekToken.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) sliderSetters[key]?.invoke(progress.toFloat())
                    updateCurrentDisplay(progress)
                }

                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            },
        )
        return row.root
    }

    private fun showBottomSheet(@StringRes titleRes: Int, content: View) {
        val sheet = BottomSheetDialog(requireContext())
        val container = FrameLayout(requireContext()).apply {
            val pad = resources.getDimensionPixelSize(SdkR.dimen.view_size_16)
            setPadding(pad, pad, pad, pad)
            addView(content)
        }
        sheet.setTitle(
            getString(
                R.string.prm_theme_bottom_sheet_preview_format,
                getString(titleRes)
            )
        )
        sheet.setContentView(container)
        sheet.show()
    }

    private fun showButtonPreview() {
        syncFieldsFromViews()
        val button = LayoutInflater.from(requireContext())
            .inflate(R.layout.widget_preview_prm_button, null, false) as PRMButton
        button.text = getString(R.string.prm_theme_demo_btn_continue)
        val token = themeDisplay.button.toToken()
        showBottomSheet(R.string.prm_theme_sheet_component_prm_button, button)
        // Apply after attach so onAttachedToWindow does not overwrite with registry token.
        button.post { button.applyToken(token) }
    }

    private fun showSearchPreview() {
        syncFieldsFromViews()
        val token = themeDisplay.searchBar.toToken()
        val search = PRMSearchField(requireContext()).apply {
            searchType = PRMSearchType.BASIC
            hint = getString(SdkR.string.prm_search_hint)
            applyToken(token)
            viewBinding.searchInput.setText(getString(R.string.prm_theme_demo_search_text))
        }
        showBottomSheet(R.string.prm_theme_sheet_component_prm_search, search)
        search.post { search.applyToken(token) }
    }

    private fun showTabChipPreview() {
        syncFieldsFromViews()
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            val pad = resources.getDimensionPixelSize(SdkR.dimen.view_size_8)
            setPadding(pad, pad, pad, pad)
        }
        listOf(
            getString(R.string.prm_theme_demo_tab_all) to true,
            getString(R.string.prm_theme_demo_tab_expiring) to false,
        ).forEach { (title, selected) ->
            val tv = TextView(requireContext()).apply {
                text = title
                val pad = resources.getDimensionPixelSize(SdkR.dimen.view_size_12)
                setPadding(pad, pad, pad, pad)
            }
            val token = themeDisplay.tabChip
            val radius = token.cornerRadius ?: 7f
            val bg = if (selected) token.activeBackgroundColor else token.inactiveBackgroundColor
            val fg = if (selected) token.activeTextColor else token.inactiveTextColor
            val bgColor = bg?.let { TokenColorParser.parse(it) } ?: Color.GRAY
            val fgColor = fg?.let { TokenColorParser.parse(it) } ?: Color.BLACK
            tv.setTextColor(fgColor)
            tv.background = TokenDrawableFactory.roundedRect(bgColor, radius, requireContext())
            row.addView(tv)
        }
        showBottomSheet(R.string.prm_theme_sheet_component_rv_tabs, row)
    }

    private fun showTabUnderlinePreview() {
        syncFieldsFromViews()
        val tabs = TabLayout(requireContext()).apply {
            addTab(newTab().setText(getString(R.string.prm_theme_demo_detail_tab_info)))
            addTab(newTab().setText(getString(R.string.prm_theme_demo_detail_tab_guide)))
            TabUnderlineTheme.applyToken(this, themeDisplay.tabUnderline)
        }
        showBottomSheet(R.string.prm_theme_sheet_component_tabs, tabs)
    }

    private fun showEndowPreview() {
        syncFieldsFromViews()
        val token = themeDisplay.discountBadge.toToken()
        val endow = PRMEndowView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setVouchers(
                listOf(
                    PromotionItem(
                        id = "preview_valid",
                        name = getString(R.string.prm_theme_demo_voucher_available_name),
                        discount = getString(R.string.prm_theme_demo_voucher_available_discount),
                        isApplied = true,
                    ),
                    PromotionItem(
                        id = "preview_expired",
                        name = getString(R.string.prm_theme_demo_voucher_unavailable_name),
                        discount = getString(R.string.prm_theme_demo_voucher_unavailable_discount),
                        isApplied = true,
                        isExpired = true,
                    ),
                ),
            )
        }
        showBottomSheet(R.string.prm_theme_sheet_component_prm_endow, endow)
        endow.post { endow.applyToken(token) }
    }

    private fun showListItemPreview() {
        syncFieldsFromViews()
        val inflater = LayoutInflater.from(requireContext())
        val pad = resources.getDimensionPixelSize(SdkR.dimen.view_size_8)
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        val token = themeDisplay.listItem

        val activeBinding = ItemChoosePromotionBinding.inflate(inflater, container, false)
        bindListItemPreviewState(activeBinding, showExpiredBadge = false)
        PromotionListItemTheme.applyToken(activeBinding, token)
        container.addView(activeBinding.root)

        val expiredBinding = ItemChoosePromotionBinding.inflate(inflater, container, false)
        bindListItemPreviewState(expiredBinding, showExpiredBadge = true)
        PromotionListItemTheme.applyToken(expiredBinding, token)
        val expiredLp = expiredBinding.root.layoutParams as LinearLayout.LayoutParams
        expiredLp.topMargin = pad
        expiredBinding.root.layoutParams = expiredLp
        container.addView(expiredBinding.root)

        showBottomSheet(R.string.prm_theme_sheet_component_promotion_item, container)
    }

    private fun bindListItemPreviewState(
        binding: ItemChoosePromotionBinding,
        showExpiredBadge: Boolean,
    ) {
        binding.apply {
            txtVoucherName.text = getString(R.string.prm_theme_demo_list_voucher_name)
            tvContent.text = getString(R.string.prm_theme_demo_list_discount)
            tvEndDate.text = getString(R.string.prm_theme_demo_list_expiry)
            ctlNotEnoughApplyVoucher.isVisible = false
            imgCircleNotEnoughApplyVoucher.isVisible = false
            if (showExpiredBadge) {
                ctlTop.alpha = 0.6f
                txtExpired.isVisible = true
                txtExpired.text = getString(SdkR.string.prm_is_used)
                lnDetail.isVisible = false
                cbUseVoucher.isVisible = false
            } else {
                ctlTop.alpha = 1f
                txtExpired.isVisible = false
                lnDetail.isVisible = true
                cbUseVoucher.isVisible = true
                cbUseVoucher.isChecked = true
            }
        }
    }

    private fun onReset() {
        preferenceManager.clear()
        PromotionTheme.clear()
        sdkDefaults = PromotionTheme.loadDisplayDefaults(requireContext())
        themeDisplay = sdkDefaults
        buildCards()
    }

    private fun onApply() {
        syncFieldsFromViews()
        val config = PromotionThemeDisplay.configFromDisplayValues(themeDisplay, sdkDefaults)
        preferenceManager.save(config)
        PromotionTheme.configure(config)
        parentFragmentManager.popBackStack()
        Toast.makeText(
            requireActivity(),
            getString(R.string.prm_theme_apply_success),
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun syncFieldsFromViews() {
        val processedColorKeys = mutableSetOf<String>()
        val processedSliderKeys = mutableSetOf<String>()
        fun walk(group: ViewGroup) {
            for (i in 0 until group.childCount) {
                val child = group.getChildAt(i)
                val colorEdit = child.findViewById<EditText>(R.id.edtColorValue)
                if (colorEdit?.tag is String) {
                    val key = colorEdit.tag as String
                    if (processedColorKeys.add(key)) {
                        val row = findColorRowContainer(colorEdit)
                        val swatch = row?.findViewById<View>(R.id.viewColorSwatch)
                        val paletteIcon = row?.findViewById<View>(R.id.imgPalette)
                        commitColorFromField(key, colorEdit, swatch, paletteIcon)
                    }
                }
                val seekBar = child.findViewById<SeekBar>(R.id.seekToken)
                if (seekBar?.tag is String) {
                    val key = seekBar.tag as String
                    if (processedSliderKeys.add(key)) {
                        sliderSetters[key]?.invoke(seekBar.progress.toFloat())
                    }
                }
                if (child is ViewGroup) walk(child)
            }
        }
        walk(binding.cardsContainer)
    }
}
