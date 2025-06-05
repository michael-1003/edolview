package kr.edoli.edolview.ui.vg

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.PolygonBatch
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Disposable
import kr.edoli.edolview.ui.drawLine
import kr.edoli.edolview.ui.drawPolygon
import kr.edoli.edolview.ui.drawRect

/**
 * Vector graphics drawing class built on libGDX that supports switching backends
 * to enable SVG export functionality.
 */

// Backend interface
interface SimpleVG {
    fun beginPath()
    fun closePath()
    fun beginGroup(transform: Matrix4? = null)
    fun endGroup()
    fun moveTo(x: Float, y: Float)
    fun lineTo(x: Float, y: Float)
    fun quadraticCurveTo(cx: Float, cy: Float, x: Float, y: Float)
    fun bezierCurveTo(cx1: Float, cy1: Float, cx2: Float, cy2: Float, x: Float, y: Float)
    fun rect(x: Float, y: Float, width: Float, height: Float)
    fun circle(x: Float, y: Float, radius: Float)
    fun ellipse(x: Float, y: Float, radiusX: Float, radiusY: Float)
    fun polygon(points: FloatArray)
    fun fill()
    fun stroke()
    fun strokeAndFill()
    fun setStrokeWidth(width: Float)
    fun setStrokeColor(color: Color)
    fun setFillColor(color: Color)
}

class Polyline {
    val points = mutableListOf<Vector2>()
    var closed = false

    fun add(x: Float, y: Float) {
        points.add(Vector2(x, y))
    }

    fun add(vec: Vector2) {
        points.add(vec)
    }

    fun close() {
        closed = true
    }

    fun clear() {
        points.clear()
        closed = false
    }

    fun toArray(): FloatArray {
        val array = FloatArray(points.size * 2)
        for (i in points.indices) {
            array[i * 2] = points[i].x
            array[i * 2 + 1] = points[i].y
        }
        return array
    }

    fun iterLines(): Iterator<Pair<Vector2, Vector2>> {
        return object : Iterator<Pair<Vector2, Vector2>> {
            private var index = 0

            override fun hasNext(): Boolean {
                return if (closed) {
                    index < points.size
                } else {
                    index < points.size - 1
                }
            }

            override fun next(): Pair<Vector2, Vector2> {
                if (!hasNext()) throw NoSuchElementException()
                val start = points[index]
                val end = if (index + 1 < points.size) points[index + 1] else points[0]
                index++
                return Pair(start, end)
            }
        }
    }
}

class SVGSimpleVG : SimpleVG, Disposable {
    private val svgBuilder = StringBuilder()
    private val pathBuilder = StringBuilder()
    private var strokeWidth = 1f
    private var strokeColor = Color.BLACK
    private var fillColor = Color.WHITE
    private var pathStarted = false

    // Overall SVG metadata
    private var width = 0
    private var height = 0

    fun beginSVG(width: Int, height: Int) {
        this.width = width
        this.height = height
        svgBuilder.clear()
        svgBuilder.append("""<svg width="$width" height="$height" xmlns="http://www.w3.org/2000/svg"><g transform="scale(1, -1) translate(0, -$height)">""")
    }

    fun endSVG(): String {
        svgBuilder.append("</g></svg>")
        return svgBuilder.toString()
    }

    override fun beginPath() {
        pathBuilder.clear()
        pathStarted = false
    }

    override fun closePath() {
        if (pathStarted) {
            pathBuilder.append(" Z")
        }
    }

    override fun beginGroup(transform: Matrix4?) {
        val transformStr = if (transform == null) "" else " transform=\"${matrixToSVGTransform(transform)}\""
        svgBuilder.append("<g$transformStr>")
    }

    override fun endGroup() {
        svgBuilder.append("</g>")
    }

    override fun moveTo(x: Float, y: Float) {
        pathBuilder.append(" M $x $y")
        pathStarted = true
    }

    override fun lineTo(x: Float, y: Float) {
        if (pathStarted) {
            pathBuilder.append(" L $x $y")
        } else {
            moveTo(x, y)
        }
    }

    override fun quadraticCurveTo(cx: Float, cy: Float, x: Float, y: Float) {
        if (pathStarted) {
            pathBuilder.append(" Q $cx $cy, $x $y")
        } else {
            moveTo(x, y)
        }
    }

    override fun bezierCurveTo(cx1: Float, cy1: Float, cx2: Float, cy2: Float, x: Float, y: Float) {
        if (pathStarted) {
            pathBuilder.append(" C $cx1 $cy1, $cx2 $cy2, $x $y")
        } else {
            moveTo(x, y)
        }
    }

    override fun rect(x: Float, y: Float, width: Float, height: Float) {
        // In SVG we can use the rect element directly
        val strokeRGB = colorToRGBString(strokeColor)
        val fillRGB = colorToRGBString(fillColor)

        svgBuilder.append("""<rect x="$x" y="$y" width="$width" height="$height" """)
        svgBuilder.append("""stroke="$strokeRGB" stroke-width="$strokeWidth" fill="$fillRGB" />""")
    }

    override fun circle(x: Float, y: Float, radius: Float) {
        // Use SVG circle element
        val strokeRGB = colorToRGBString(strokeColor)
        val fillRGB = colorToRGBString(fillColor)

        svgBuilder.append("""<circle cx="$x" cy="$y" r="$radius" """)
        svgBuilder.append("""stroke="$strokeRGB" stroke-width="$strokeWidth" fill="$fillRGB" />""")
    }

    override fun ellipse(x: Float, y: Float, radiusX: Float, radiusY: Float) {
        // Use SVG ellipse element
        val strokeRGB = colorToRGBString(strokeColor)
        val fillRGB = colorToRGBString(fillColor)

        svgBuilder.append("""<ellipse cx="$x" cy="$y" rx="$radiusX" ry="$radiusY" """)
        svgBuilder.append("""stroke="$strokeRGB" stroke-width="$strokeWidth" fill="$fillRGB" />""")
    }

    override fun polygon(points: FloatArray) {
        if (points.size < 4) return

        val pointsStr = StringBuilder()
        for (i in 0 until points.size step 2) {
            pointsStr.append("${points[i]},${points[i+1]} ")
        }

        val strokeRGB = colorToRGBString(strokeColor)
        val fillRGB = colorToRGBString(fillColor)

        svgBuilder.append("""<polygon points="$pointsStr" """)
        svgBuilder.append("""stroke="$strokeRGB" stroke-width="$strokeWidth" fill="$fillRGB" />""")
    }

    override fun fill() {
        if (!pathStarted) return

        val fillRGB = colorToRGBString(fillColor)

        svgBuilder.append("""<path d="$pathBuilder" fill="$fillRGB" stroke="none" />""")
    }

    override fun stroke() {
        if (!pathStarted) return

        val strokeRGB = colorToRGBString(strokeColor)

        svgBuilder.append("""<path d="$pathBuilder" fill="none" stroke="$strokeRGB" stroke-width="$strokeWidth" />""")
    }

    override fun strokeAndFill() {
        if (!pathStarted) return

        val strokeRGB = colorToRGBString(strokeColor)
        val fillRGB = colorToRGBString(fillColor)

        svgBuilder.append("""<path d="$pathBuilder" fill="$fillRGB" stroke="$strokeRGB" stroke-width="$strokeWidth" />""")
    }

    override fun setStrokeWidth(width: Float) {
        this.strokeWidth = width
    }

    override fun setStrokeColor(color: Color) {
        this.strokeColor = color
    }

    override fun setFillColor(color: Color) {
        this.fillColor = color
    }

    private fun colorToRGBString(color: Color): String {
        val r = (color.r * 255).toInt()
        val g = (color.g * 255).toInt()
        val b = (color.b * 255).toInt()
        val a = color.a

        return if (a < 1f) {
            "rgba($r,$g,$b,$a)"
        } else {
            "rgb($r,$g,$b)"
        }
    }

    private fun matrixToSVGTransform(matrix: Matrix4): String {
        val values = matrix.values
        return "matrix(${values[0]},${values[1]},${values[4]},${values[5]},${values[12]},${values[13]})"
    }

    override fun dispose() {
        // Clean up any resources if needed
        svgBuilder.clear()
        pathBuilder.clear()
    }

    // Export the current SVG content to a file
    fun exportToFile(filePath: String) {
        try {
            val svgContent = endSVG()
            java.io.File(filePath).writeText(svgContent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// Default libGDX implementation
class GDXSimpleVG(val batch: PolygonBatch) : SimpleVG {
    private var path = Polyline()
    private val paths = mutableListOf<Polyline>()
    private val groupTransformStack = mutableListOf<Matrix4>()

    private var strokeWidth = 1f
    private var strokeColor = Color.BLACK
    private var fillColor = Color.WHITE


    override fun beginPath() {
        path.clear()
    }

    override fun closePath() {
        path.close()
    }

    override fun beginGroup(transform: Matrix4?) {
        groupTransformStack.add(batch.transformMatrix)
        if (transform != null) {
            batch.transformMatrix = transform
        }
    }

    override fun endGroup() {
        val lastGroupTransform = groupTransformStack.removeLast()
        batch.transformMatrix = lastGroupTransform
    }

    override fun moveTo(x: Float, y: Float) {
        path = Polyline()
        paths.add(path)

        path.add(Vector2(x, y))
    }

    override fun lineTo(x: Float, y: Float) {
        path.add(Vector2(x, y))
    }

    override fun quadraticCurveTo(cx: Float, cy: Float, x: Float, y: Float) {
        // Approximate with line segments
        val steps = 10
        val start = path.points.lastOrNull() ?: return

        for (i in 1..steps) {
            val t = i.toFloat() / steps
            val u = 1 - t
            val px = u * u * start.x + 2 * u * t * cx + t * t * x
            val py = u * u * start.y + 2 * u * t * cy + t * t * y
            lineTo(px, py)
        }
    }

    override fun bezierCurveTo(cx1: Float, cy1: Float, cx2: Float, cy2: Float, x: Float, y: Float) {
        // Approximate with line segments
        val steps = 10
        val start = path.points.lastOrNull() ?: return

        for (i in 1..steps) {
            val t = i.toFloat() / steps
            val u = 1 - t
            val px = u * u * u * start.x + 3 * u * u * t * cx1 + 3 * u * t * t * cx2 + t * t * t * x
            val py = u * u * u * start.y + 3 * u * u * t * cy1 + 3 * u * t * t * cy2 + t * t * t * y
            lineTo(px, py)
        }
    }

    override fun rect(x: Float, y: Float, width: Float, height: Float) {
        batch.drawRect(x, y, width, height)
    }

    override fun circle(x: Float, y: Float, radius: Float) {
        ellipse(x, y, radius, radius)
    }

    override fun ellipse(x: Float, y: Float, radiusX: Float, radiusY: Float) {
        beginPath()

        val segments = 24
        for (i in 0..segments) {
            val angle = (i.toFloat() / segments) * Math.PI.toFloat() * 2f
            val px = x + radiusX * Math.cos(angle.toDouble()).toFloat()
            val py = y + radiusY * Math.sin(angle.toDouble()).toFloat()

            if (i == 0) {
                moveTo(px, py)
            } else {
                lineTo(px, py)
            }
        }

        closePath()
    }

    override fun polygon(points: FloatArray) {
        if (points.size < 4) return

        beginPath()
        moveTo(points[0], points[1])

        for (i in 2 until points.size step 2) {
            lineTo(points[i], points[i + 1])
        }

        closePath()
    }

    override fun fill() {
        drawPolyline(paths, true, false)
        paths.clear()
        path = Polyline()
    }

    override fun stroke() {
        drawPolyline(paths, false, true)
        paths.clear()
        path = Polyline()
    }

    override fun strokeAndFill() {
        drawPolyline(paths, true, true)
        paths.clear()
        path = Polyline()
    }

    override fun setStrokeWidth(width: Float) {
        strokeWidth = width
    }

    override fun setStrokeColor(color: Color) {
        strokeColor = color
    }

    override fun setFillColor(color: Color) {
        fillColor = color
    }

    private fun drawPolyline(polylines: List<Polyline>, filled: Boolean, stroke: Boolean) {
        for (polyline in polylines) {
            if (polyline.points.size < 2) return

            if (filled && polyline.points.size > 2) {
                batch.color = fillColor
                val vertices = polyline.toArray()
                batch.drawPolygon(vertices)
            }
            if (stroke) {
                batch.color = strokeColor
                for (line in polyline.iterLines()) {
                    batch.drawLine(line.first.x, line.first.y, line.second.x, line.second.y, strokeWidth)
                }
            }
            
        }
    }
}