package com.ttcn.prm.ui.utils.extension

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import java.util.Locale

fun CharSequence.toHighlightedSpannable(
    keyword: String,
    highlightColor: Int? = null,
    isBold: Boolean = true,
): SpannableString {
    val source = toString()
    if (source.isEmpty() || keyword.isBlank()) {
        return SpannableString(source)
    }

    val ranges = findHighlightRanges(source, keyword)
    if (ranges.isEmpty()) {
        return SpannableString(source)
    }

    val spannable = SpannableString(source)
    ranges.forEach { range ->
        val start = range.first
        val end = range.last + 1
        if (isBold) {
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        if (highlightColor != null) {
            spannable.setSpan(
                ForegroundColorSpan(highlightColor),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }
    return spannable
}

internal fun findHighlightRanges(text: String, keyword: String): List<IntRange> {
    if (text.isEmpty() || keyword.isBlank()) {
        return emptyList()
    }

    val searchableText = buildSearchableText(text)
    if (searchableText.normalized.isEmpty()) {
        return emptyList()
    }

    val patterns = buildSearchPatterns(keyword)
    val claimed = BooleanArray(searchableText.normalized.length)
    val ranges = mutableListOf<IntRange>()

    patterns.forEach { pattern ->
        val normalizedPattern = normalizeForSearch(pattern)
        if (normalizedPattern.isEmpty()) {
            return@forEach
        }

        var startIndex = 0
        while (startIndex <= searchableText.normalized.length - normalizedPattern.length) {
            val matchIndex = searchableText.normalized.indexOf(
                normalizedPattern,
                startIndex,
            )
            if (matchIndex == -1) {
                break
            }

            val matchEnd = matchIndex + normalizedPattern.length
            val overlapsClaimedRange = (matchIndex until matchEnd).any { claimed[it] }
            if (!overlapsClaimedRange) {
                for (index in matchIndex until matchEnd) {
                    claimed[index] = true
                }
                val originalStart = searchableText.indexMap[matchIndex]
                val originalEnd = searchableText.indexMap[matchEnd - 1] + 1
                ranges.add(originalStart until originalEnd)
            }
            startIndex = matchIndex + 1
        }
    }

    return ranges.sortedBy { it.first }
}

private data class SearchableText(
    val original: String,
    val normalized: String,
    val indexMap: List<Int>,
)

private fun buildSearchableText(text: String): SearchableText {
    val normalized = StringBuilder()
    val indexMap = mutableListOf<Int>()

    text.forEachIndexed { originalIndex, char ->
        normalizeForSearch(char.toString()).forEach { normalizedChar ->
            normalized.append(normalizedChar)
            indexMap.add(originalIndex)
        }
    }

    return SearchableText(
        original = text,
        normalized = normalized.toString(),
        indexMap = indexMap,
    )
}

private fun buildSearchPatterns(keyword: String): List<String> {
    val trimmedKeyword = keyword.trim()
    if (trimmedKeyword.isEmpty()) {
        return emptyList()
    }

    val tokens = trimmedKeyword.split(WHITESPACE_REGEX).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) {
        return emptyList()
    }

    val patterns = linkedSetOf<String>()
    for (startIndex in tokens.indices) {
        for (endIndex in startIndex until tokens.size) {
            patterns.add(tokens.subList(startIndex, endIndex + 1).joinToString(" "))
        }
    }

    return patterns
        .distinctBy(::normalizeForSearch)
        .sortedByDescending { normalizeForSearch(it).length }
}

private fun normalizeForSearch(text: String): String {
    return text.lowercase(Locale.getDefault()).unAccent()
}

private val WHITESPACE_REGEX = Regex("\\s+")
