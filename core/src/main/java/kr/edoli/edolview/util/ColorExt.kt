package kr.edoli.edolview.util

import com.badlogic.gdx.graphics.Color
import kr.edoli.edolview.image.ImageSpec


fun DoubleArray.toColor(): Color {
    val length = this.size
    return if (length == 4 || length == 3) {
        Color(this[0].toFloat(), this[1].toFloat(), this[2].toFloat(), 1f)
    } else if (length == 1) {
        Color(this[0].toFloat(), this[0].toFloat(), this[0].toFloat(), 1f)
    } else {
        Color.BLACK
    }
}

private fun formatDigits(value: Double, digits: Int?): String {
    return if (digits != null) {
        String.format("%.${digits}f", value)
    } else {
        value.toString()
    }
}

fun Double.toColorValueStr(imageSpec: ImageSpec, decimalDigits: Int? = null): String {
    val scale = imageSpec.typeMaxValue

    var value = if (scale > 0) this * scale else this

    return if (imageSpec.isInt) value.format(0) else formatDigits(value, decimalDigits)
}

fun DoubleArray.toColorStr(imageSpec: ImageSpec, separator: String = ", ", decimalDigits: Int? = null) =
    joinToString(separator) {
        // scale == 0 means that an opened image is not uint image
        val scale = imageSpec.typeMaxValue

        var value = it
        if (scale > 0) {
            value *= scale
        }
        if (imageSpec.isInt) value.format(0) else formatDigits(value, decimalDigits)
    }

fun Color.toFloatArray() = floatArrayOf(this.r, this.g, this.b, this.a)