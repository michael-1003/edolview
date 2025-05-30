package kr.edoli.edolview.image

import com.badlogic.gdx.math.Rectangle
import kr.edoli.edolview.geom.Point2D
import kr.edoli.edolview.util.set
import org.opencv.core.*

operator fun Mat.get(rowRange: IntRange, colRange: IntRange): Mat {
    return submat(rowRange.cv, colRange.cv)
}

operator fun Mat.get(rect: Rect): Mat {
    return submat(rect)
}

operator fun Mat.get(row: Int, col: Int, channel: Int): Double {
    return get(row, col)[channel]
}

operator fun Mat.get(point: Point): DoubleArray {
    return get(point.y.toInt(), point.x.toInt())
}

fun Mat.contains(rect: Rect): Boolean {
    return rect.x >= 0 && rect.y >= 0 &&
            rect.x + rect.width <= this.width() &&
            rect.y + rect.height <= this.height()
}

fun Mat.contains(point: Point): Boolean {
    return point.x >= 0 && point.y >= 0 &&
            point.x < this.width() && point.y < this.height()
}
/* Plus */

operator fun Mat.plus(other: Mat): Mat {
    // check dimension and type of two matrices
    if (this.type() != other.type()) {
        throw Exception()
    }
    if (this.rows() != other.rows() || this.cols() != other.cols()) {
        throw Exception()
    }

    val newMat = Mat(this.rows(), this.cols(), this.type())
    Core.add(this, other, newMat)
    return newMat
}

operator fun Mat.plus(value: Double): Mat {
    val newMat = Mat(this.rows(), this.cols(), this.type())
    Core.add(this, Scalar(DoubleArray(newMat.channels()) { value }), newMat)
    return newMat
}

operator fun Double.plus(mat: Mat): Mat {
    val newMat = Mat(mat.rows(), mat.cols(), mat.type())
    Core.add(mat, Scalar(DoubleArray(newMat.channels()) { this }), newMat)
    return newMat
}

operator fun Mat.plusAssign(other: Mat) {
    if (this.type() != other.type()) {
        throw Exception()
    }
    if (this.rows() != other.rows() || this.cols() != other.cols()) {
        throw Exception()
    }

    Core.add(this, other, this)
}

operator fun Mat.plusAssign(value: Double) {
    Core.add(this, Scalar(DoubleArray(channels()) { value }), this)
}

/* Minus */

operator fun Mat.minus(other: Mat): Mat {
    // check dimension and type of two matrices
    if (this.type() != other.type()) {
        throw Exception()
    }
    if (this.rows() != other.rows() || this.cols() != other.cols()) {
        throw Exception()
    }

    val newMat = Mat(this.rows(), this.cols(), this.type())
    Core.subtract(this, other, newMat)
    return newMat
}

operator fun Mat.minus(value: Double): Mat {
    val newMat = Mat(this.rows(), this.cols(), this.type())
    Core.subtract(this, Scalar(DoubleArray(newMat.channels()) { value }), newMat)
    return newMat
}

operator fun Double.minus(mat: Mat): Mat {
    val newMat = Mat(mat.rows(), mat.cols(), mat.type(), Scalar(DoubleArray(mat.channels()) { this }))
    Core.subtract(newMat, mat, newMat)
    return newMat
}

operator fun Mat.unaryMinus(): Mat {
    val newMat = Mat(this.rows(), this.cols(), this.type(), Scalar(DoubleArray(channels()) { 0.0 }))
    Core.subtract(newMat, this, newMat)
    return newMat
}

operator fun Mat.minusAssign(other: Mat) {
    if (this.type() != other.type()) {
        throw Exception()
    }
    if (this.rows() != other.rows() || this.cols() != other.cols()) {
        throw Exception()
    }

    Core.subtract(this, other, this)
}

operator fun Mat.minusAssign(value: Double) {
    Core.subtract(this, Scalar(DoubleArray(channels()) { value }), this)
}

/* Times */

operator fun Mat.times(other: Mat): Mat {
    // check dimension and type of two matrices
    if (this.type() != other.type()) {
        throw Exception()
    }
    if (this.rows() != other.rows() || this.cols() != other.cols()) {
        throw Exception()
    }

    val newMat = Mat(this.rows(), this.cols(), this.type())
    Core.multiply(this, other, newMat)
    return newMat
}

operator fun Mat.times(value: Double): Mat {
    val newMat = Mat(this.rows(), this.cols(), this.type())
    Core.multiply(this, Scalar(DoubleArray(newMat.channels()) { value }), newMat)
    return newMat
}

operator fun Double.times(mat: Mat): Mat {
    val newMat = Mat(mat.rows(), mat.cols(), mat.type())
    Core.multiply(mat, Scalar(DoubleArray(newMat.channels()) { this }), newMat)
    return newMat
}

operator fun Mat.timesAssign(other: Mat) {
    if (this.type() != other.type()) {
        throw Exception()
    }
    if (this.rows() != other.rows() || this.cols() != other.cols()) {
        throw Exception()
    }

    Core.multiply(this, other, this)
}

operator fun Mat.timesAssign(value: Double) {
    Core.multiply(this, Scalar(DoubleArray(channels()) { value }), this)
}

/* Divide */

operator fun Mat.div(other: Mat): Mat {
    // check dimension and type of two matrices
    if (this.type() != other.type()) {
        throw Exception()
    }
    if (this.rows() != other.rows() || this.cols() != other.cols()) {
        throw Exception()
    }

    val newMat = Mat(this.rows(), this.cols(), this.type())
    Core.divide(this, other, newMat)
    return newMat
}

operator fun Mat.div(value: Double): Mat {
    val newMat = Mat(this.rows(), this.cols(), this.type())
    Core.divide(this, Scalar(DoubleArray(newMat.channels()) { value }), newMat)
    return newMat
}

operator fun Double.div(mat: Mat): Mat {
    val newMat = Mat(mat.rows(), mat.cols(), mat.type(), Scalar(DoubleArray(mat.channels()) { this }))
    Core.divide(newMat, mat, newMat)
    return newMat
}

operator fun Mat.divAssign(other: Mat) {
    if (this.type() != other.type()) {
        throw Exception()
    }
    if (this.rows() != other.rows() || this.cols() != other.cols()) {
        throw Exception()
    }

    Core.divide(this, other, this)
}

operator fun Mat.divAssign(value: Double) {
    Core.divide(this, Scalar(DoubleArray(channels()) { value }), this)
}

/* Other */

fun Mat.pow(value: Double): Mat {
    val newMat = Mat(rows(), cols(), type())
    Core.pow(this, value, newMat)
    return newMat
}

fun Mat.powAssign(value: Double) {
    Core.pow(this, value, this)
}


fun Mat.split(): List<Mat> {
    if (this.channels() == 1) {
        return listOf(this)
    }
    val matArray = Array(this.channels()) { Mat() }.toList()
    Core.split(this, matArray)

    return matArray
}

fun Mat.sum(): DoubleArray {
    return Core.sumElems(this).`val`
}

fun Mat.min(): Double {
    val matArray = Array(this.channels()) { Mat() }.toList()
    Core.split(this, matArray)

    var min = Double.MAX_VALUE

    for (mat in matArray) {
        val result = Core.minMaxLoc(mat)
        val tmpMin = result.minVal

        if (tmpMin < min) min = tmpMin
    }

    return min
}


fun Mat.max(): Double {
    val matArray = Array(this.channels()) { Mat() }.toList()
    Core.split(this, matArray)

    var max = Double.MIN_VALUE

    for (mat in matArray) {
        val result = Core.minMaxLoc(mat)
        val tmpMax = result.maxVal

        if (tmpMax > max) max = tmpMax
    }

    return max
}


fun Mat.minMax(): Pair<Double, Double> {
    val numChannel = this.channels()

    if (numChannel == 1) {
        val result = Core.minMaxLoc(this)
        return Pair(result.minVal, result.maxVal)
    } else {
        val matArray = Array(numChannel) { Mat() }.toList()
        Core.split(this, matArray)

        var min = Double.MAX_VALUE
        var max = Double.MIN_VALUE

        for (mat in matArray) {
            val result = Core.minMaxLoc(mat)
            val tmpMin = result.minVal
            val tmpMax = result.maxVal

            if (tmpMin < min) min = tmpMin
            if (tmpMax > max) max = tmpMax
        }

        return Pair(min, max)
    }
}

fun Mat.normalize(): Mat {
    val (min, max) = this.minMax()
    return (this - min) / (max - min)
}

fun Mat.bound(): Rect {
    return Rect(0, 0, width(), height())
}


fun Mat.typeMax(): Double {
    return when (type()) {
        CvType.CV_8U -> return 255.0
        CvType.CV_16U -> return 65535.0
        CvType.CV_32S -> return Int.MAX_VALUE.toDouble()
        CvType.CV_8UC3 -> return 255.0
        CvType.CV_16UC3 -> return 65535.0
        CvType.CV_32SC3 -> return Int.MAX_VALUE.toDouble()
        CvType.CV_8UC4 -> return 255.0
        CvType.CV_16UC4 -> return 65535.0
        CvType.CV_32SC4 -> return Int.MAX_VALUE.toDouble()
        else -> -1.0
    }
}

fun Mat.bitsPerPixel(): Int {
    return when (depth()) {
        CvType.CV_8U -> 8
        CvType.CV_8S -> 8
        CvType.CV_16U -> 16
        CvType.CV_16S -> 16
        CvType.CV_32S -> 32
        CvType.CV_32F -> 32
        CvType.CV_64F -> 64
        CvType.CV_16F -> 16
        else -> -1
    }
}

val Mat.nbytes: Int
    get() = (this.rows() * this.cols() * this.elemSize()).toInt()

inline fun <T> Mat.toArray(generateArray: (arraySize: Int) -> T): T {
    val channels = this.channels()
    val arraySize = (this.total() * channels).toInt()
    return generateArray(arraySize)
}

fun Mat.toByteArray(): ByteArray {
    return toArray { arraySize ->
        val rawData = ByteArray(arraySize)
        this.get(0, 0, rawData)
        rawData
    }
}

fun Mat.toFloatArray(): FloatArray {
    return toArray { arraySize ->
        val rawData = FloatArray(arraySize)
        this.get(0, 0, rawData)
        rawData
    }
}

fun Mat.toDoubleArray(): DoubleArray {
    return toArray { arraySize ->
        val rawData = DoubleArray(arraySize)
        this.get(0, 0, rawData)
        rawData
    }
}

fun Mat.toIntArray(): IntArray {
    return toArray { arraySize ->
        val rawData = IntArray(arraySize)
        this.get(0, 0, rawData)
        rawData
    }
}

fun Mat.toShortArray(): ShortArray {
    return toArray { arraySize ->
        val rawData = ShortArray(arraySize)
        this.get(0, 0, rawData)
        rawData
    }
}

/* Type conversion */

val IntRange.cv: Range
    get() = Range(this.first, this.last)

val Rectangle.cv: Rect
    get() = Rect(x.toInt(), y.toInt(), width.toInt(), height.toInt())

val Point2D.cv: Point
    get() = Point(x, y)

fun Rect.set(rectangle: Rectangle): Rect {
    return this.set(rectangle.x.toInt(), rectangle.y.toInt(), rectangle.width.toInt(), rectangle.height.toInt())
}

fun Rect.set(rect: Rect): Rect {
    return this.set(rect.x, rect.y, rect.width, rect.height)
}

