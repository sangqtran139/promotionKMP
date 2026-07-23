package com.ttcn.prm.core.utils

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.CharacterStyle

class PRMSimpleSpanBuilder {

    private data class SpanSection(
        val text: String,
        val startIndex: Int,
        val styles: Array<out CharacterStyle>
    ) {
        fun apply(spanStringBuilder: SpannableStringBuilder) {
            styles.forEach { style ->
                spanStringBuilder.setSpan(
                    style,
                    startIndex,
                    startIndex + text.length,
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    private val spanSections = mutableListOf<SpanSection>()
    private val stringBuilder = StringBuilder()

    fun append(text: String, vararg styles: CharacterStyle): PRMSimpleSpanBuilder {
        if (styles.isNotEmpty()) {
            spanSections.add(
                SpanSection(
                    text = text,
                    startIndex = stringBuilder.length,
                    styles = styles
                )
            )
        }

        stringBuilder.append(text)
        return this
    }

    fun appendWithSemiColon(text: String, vararg styles: CharacterStyle): PRMSimpleSpanBuilder {
        return append("$text;", *styles)
    }

    fun appendWithSpace(text: String, vararg styles: CharacterStyle): PRMSimpleSpanBuilder {
        return append("$text ", *styles)
    }

    fun appendWithHash(text: String, vararg styles: CharacterStyle): PRMSimpleSpanBuilder {
        return append("$text#", *styles)
    }

    fun appendWithLineBreak(text: String, vararg styles: CharacterStyle): PRMSimpleSpanBuilder {
        return append("$text\n", *styles)
    }

    fun build(): SpannableStringBuilder {
        val spannableStringBuilder = SpannableStringBuilder(stringBuilder.toString())

        spanSections.forEach { section ->
            section.apply(spannableStringBuilder)
        }

        return spannableStringBuilder
    }

    override fun toString(): String {
        return stringBuilder.toString()
    }
}