package com.ttcn.prm.ui.utils.extension

import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.ttcn.prm.ui.utils.ViewGlobalConst
import timber.log.Timber
import java.text.Normalizer
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

fun Float.dpToPixel(): Int {
    val metrics = Resources.getSystem().displayMetrics
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this, metrics
    ).toInt()
}

fun Int.getString(context: Context): String {
    return context.resources.getString(this)
}

inline fun <T> runSafely(block: () -> T?): T? {
    return try {
        block()
    } catch (e: Exception) {
        Timber.e(e)
        null
    }
}

/**
 * Prevent null string
 */
fun String?.getText(): String {
    return this ?: ""
}

inline fun <reified T> getJson(data: T?): String {
    return if (data == null) {
        ""
    } else {
        try {
            Gson().toJson(data)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}

/**
 * Allows calls like
 *
 * `viewGroup.inflate(R.layout.foo)`
 */
fun ViewGroup.inflate(@LayoutRes layout: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layout, this, attachToRoot)
}

// defaultDisplay/getRealSize deprecated (API 30) nhưng chưa có thay thế tương thích minSdk 24.
@Suppress("DEPRECATION")
fun Context.screenWidth(): Int {
    val windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager? ?: return -1
    val point = Point()
    windowManager.defaultDisplay.getRealSize(point)
    return point.x
}

internal inline fun <reified T> Fragment.findDelegate(): T? {
    return findDelegate(this, T::class.java)
}

private fun <T> findDelegate(fragment: Fragment, clazz: Class<T>): T? {
    return try {
        val parentFragment = fragment.parentFragment
        if (parentFragment == null) {
            val activity = fragment.requireActivity()
            if (clazz.isAssignableFrom(activity.javaClass)) {
                return clazz.cast(activity)
            }
            return null
        } else if (clazz.isAssignableFrom(parentFragment.javaClass)) {
            clazz.cast(parentFragment)
        } else {
            findDelegate(parentFragment, clazz)
        }
    } catch (e: Exception) {
        null
    }
}

fun getDeviceName(): String {
    val manufacturer: String = Build.MANUFACTURER
    val model: String = Build.MODEL
    val result = if (model.startsWith(manufacturer)) {
        model.capitalize(Locale.getDefault())
    } else manufacturer.capitalize(Locale.getDefault()) + " " + model
    return if (result.length < 30) {
        result
    } else {
        result.substring(0, 30)
    }
}

fun String.unAccent(): String {
    return try {
        val nfdNormalizedString: String = Normalizer.normalize(this, Normalizer.Form.NFD)
        val pattern: Pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        pattern.matcher(nfdNormalizedString).replaceAll("")
    } catch (ex: java.lang.Exception) {
        ViewGlobalConst.EMPTY_STRING
    }
}

fun Fragment.isSafe() =
    !(this.isRemoving || this.activity == null || this.isDetached || !this.isAdded || this.view == null)

fun getCurrentTimeInMinutes(): Int {
    val cal = Calendar.getInstance()
    return cal.get(Calendar.HOUR_OF_DAY) * 3600 + cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND)
}

/**
 * Parse chuỗi thời gian thành số giây kể từ 00:00:00
 * Hỗ trợ cả 2 format: "HH:mm" và "HH:mm:ss"
 * Ví dụ:
 * - "03:00" -> 10800 giây (3*3600 + 0*60 + 0)
 * - "03:00:00" -> 10800 giây (3*3600 + 0*60 + 0)
 * - "05:00" -> 18000 giây (5*3600 + 0*60 + 0)
 * - "05:00:00" -> 18000 giây (5*3600 + 0*60 + 0)
 * - "14:30:45" -> 52245 giây
 * - "9:15" -> 33300 giây (hỗ trợ single digit hour)
 */
fun parseTimeToSeconds(timeString: String): Int? {
    return try {
        if (timeString.isEmpty()) {
            Timber.tag("DisplayTimeFrames").w("Time string can not null")
            return null
        }

        val parts = timeString.trim().split(":")

        if (parts.size != 2 && parts.size != 3) {
            Timber.tag("DisplayTimeFrames")
                .w("Invalid time format: $timeString (expected HH:mm or HH:mm:ss)")
            return null
        }

        val hour = parts[0].toIntOrNull()
        val minute = parts[1].toIntOrNull()

        val second = if (parts.size == 3) {
            parts[2].toIntOrNull()
        } else {
            0
        }

        if (hour == null || minute == null || second == null) {
            Timber.tag("DisplayTimeFrames").w("Failed to parse time components: $timeString")
            return null
        }

        if (hour !in 0..23) {
            Timber.tag("DisplayTimeFrames").w("Invalid hour: $hour (must be 0-23)")
            return null
        }

        if (minute !in 0..59) {
            Timber.tag("DisplayTimeFrames").w("Invalid minute: $minute (must be 0-59)")
            return null
        }
        if (second !in 0..59) {
            Timber.tag("DisplayTimeFrames").w("Invalid second: $second (must be 0-59)")
            return null
        }

        hour * 3600 + minute * 60 + second
    } catch (e: Exception) {
        Timber.tag("DisplayTimeFrames").e(e, "Error parsing time: $timeString")
        null
    }
}

fun String.toVoucherDisplayDate(): String {
    if (isBlank()) return ""
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        runCatching {
            OffsetDateTime.parse(this)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault()))
        }.getOrNull()?.let { return it }
    }
    val datePart = take(10)
    val parts = datePart.split("-")
    return if (parts.size == 3 && parts[0].length == 4) {
        "${parts[2]}/${parts[1]}/${parts[0]}"
    } else {
        datePart
    }
}

private var vn = arrayOf(
    "à", "á", "ả", "ã", "ạ",
    "â", "ầ", "ấ", "ẩ", "ẫ", "ậ",
    "ă", "ằ", "ắ", "ẳ", "ẵ", "ặ",
    "è", "é", "ẻ", "ẽ", "ẹ",
    "ê", "ề", "ế", "ể", "ễ", "ệ",
    "ò", "ó", "ỏ", "õ", "ọ",
    "ô", "ố", "ồ", "ổ", "ỗ", "ộ",
    "ơ", "ờ", "ớ", "ở", "ỡ", "ợ",
    "ù", "ú", "ủ", "ũ", "ụ",
    "ư", "ừ", "ứ", "ử", "ữ", "ự",
    "ì", "í", "ỉ", "ĩ", "ị", "ỳ", "ý", "ỷ", "ỹ", "ỵ", "đ"
)

private var en = arrayOf(
    "a", "a", "a", "a", "a",
    "a", "a", "a", "a", "a", "a",
    "a", "a", "a", "a", "a", "a",
    "e", "e", "e", "e", "e",
    "e", "e", "e", "e", "e", "e",
    "o", "o", "o", "o", "o",
    "o", "o", "o", "o", "o", "o",
    "o", "o", "o", "o", "o", "o",
    "u", "u", "u", "u", "u",
    "u", "u", "u", "u", "u", "u",
    "i", "i", "i", "i", "i", "y", "y", "y", "y", "y", "d"
)

private var normalizeEn = arrayOf(
    "af", "as", "ar", "ax", "aj",
    "aa", "a", "a", "a", "a", "a",
    "aw", "a", "a", "a", "a", "a",
    "ef", "es", "er", "ex", "ej",
    "e", "e", "e", "e", "e", "e",
    "of", "os", "or", "ox", "oj",
    "o", "o", "o", "o", "o", "o",
    "o", "o", "o", "o", "o", "o",
    "uf", "us", "ur", "ux", "uj",
    "u", "u", "u", "u", "u", "u",
    "if", "is", "ir", "ix", "ij", "yf", "ys", "yr", "yx", "yj", "dd"
)

/**
 * replace Accents
 */
fun String.replaceAccents(): String {
    var s = this
    vn.forEachIndexed { index, v ->
        s = s.replace(v, en[index])
        s = s.replace(uppercase(Locale.US), uppercase(Locale.US))
    }
    return s
}

inline fun <reified T : Parcelable> Bundle?.parcelable(
    key: String
): T? {

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

        this?.getParcelable(key, T::class.java)

    } else {

        @Suppress("DEPRECATION")
        this?.getParcelable(key)
    }
}

inline fun <reified T : Parcelable> Bundle?.parcelableArrayList(
    key: String
): ArrayList<T> {

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

        this?.getParcelableArrayList(key, T::class.java)

    } else {

        @Suppress("DEPRECATION")
        this?.getParcelableArrayList<T>(key)

    } ?: arrayListOf()
}