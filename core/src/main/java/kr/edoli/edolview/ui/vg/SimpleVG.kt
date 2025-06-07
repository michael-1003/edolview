package kr.edoli.edolview.ui.vg

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.PolygonBatch
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Disposable
import kr.edoli.edolview.ui.drawLine
import kr.edoli.edolview.ui.drawPolygon
import kr.edoli.edolview.ui.drawRect
import kotlin.math.cos
import kotlin.math.sin

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
    fun fillPath()
    fun strokePath()
    fun strokeAndFillPath()

    fun line(x: Float, y: Float, x2: Float, y2: Float)
    fun rect(x: Float, y: Float, width: Float, height: Float, shapeType: ShapeType)
    fun circle(x: Float, y: Float, radius: Float, shapeType: ShapeType)
    fun ellipse(x: Float, y: Float, radiusX: Float, radiusY: Float, shapeType: ShapeType)
    fun polygon(points: FloatArray, shapeType: ShapeType)
    fun text(text: String, x: Float, y: Float, size: Float = 12f, align: Int = Align.left)

    fun setStrokeWidth(width: Float)
    fun setStrokeColor(color: Color)
    fun setFillColor(color: Color)
}

enum class ShapeType(val isFill: Boolean, val isStroke: Boolean) {
    FILL(true, false),
    STROKE(false, true),
    STROKE_AND_FILL(true, true)
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
        svgBuilder.append("""<svg width="$width" height="$height" xmlns="http://www.w3.org/2000/svg">""")
    }

    fun endSVG(): String {
        svgBuilder.append("</svg>")
        return svgBuilder.toString()
    }

    // Y 좌표를 SVG 좌표계로 변환
    private fun transformY(y: Float): Float {
        return height - y
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
        val transformStr = if (transform == null) "" else {
            val flippedMatrix = Matrix4(transform)
            // y축 스케일을 반전
            flippedMatrix.values[5] = -flippedMatrix.values[5]
            // y 위치 조정
            flippedMatrix.values[13] = height - flippedMatrix.values[13]
            " transform=\"${matrixToSVGTransform(flippedMatrix)}\""
        }
        svgBuilder.append("<g$transformStr>")
    }

    override fun endGroup() {
        svgBuilder.append("</g>")
    }

    override fun moveTo(x: Float, y: Float) {
        pathBuilder.append(" M $x ${transformY(y)}")
        pathStarted = true
    }

    override fun lineTo(x: Float, y: Float) {
        if (pathStarted) {
            pathBuilder.append(" L $x ${transformY(y)}")
        } else {
            moveTo(x, y)
        }
    }

    override fun quadraticCurveTo(cx: Float, cy: Float, x: Float, y: Float) {
        if (pathStarted) {
            pathBuilder.append(" Q $cx ${transformY(cy)}, $x ${transformY(y)}")
        } else {
            moveTo(x, y)
        }
    }

    override fun bezierCurveTo(cx1: Float, cy1: Float, cx2: Float, cy2: Float, x: Float, y: Float) {
        if (pathStarted) {
            pathBuilder.append(" C $cx1 ${transformY(cy1)}, $cx2 ${transformY(cy2)}, $x ${transformY(y)}")
        } else {
            moveTo(x, y)
        }
    }

    override fun fillPath() {
        if (!pathStarted) return

        val fillRGB = colorToRGBString(fillColor)

        svgBuilder.append("""<path d="$pathBuilder" fill="$fillRGB" ${fillOpacityString()} stroke="none" />""")
    }

    override fun strokePath() {
        if (!pathStarted) return

        val strokeRGB = colorToRGBString(strokeColor)

        svgBuilder.append("""<path d="$pathBuilder" fill="none" stroke="$strokeRGB" ${strokeOpacityString()} stroke-width="$strokeWidth" />""")
    }

    override fun strokeAndFillPath() {
        if (!pathStarted) return

        val strokeRGB = colorToRGBString(strokeColor)
        val fillRGB = colorToRGBString(fillColor)

        svgBuilder.append("""<path d="$pathBuilder" fill="$fillRGB" ${fillOpacityString()} stroke="$strokeRGB" ${strokeOpacityString()} stroke-width="$strokeWidth" />""")
    }

    private fun svgShapeTypeToSVG(shapeType: ShapeType): String {
        return when (shapeType) {
            ShapeType.FILL -> """fill="${colorToRGBString(fillColor)}" ${fillOpacityString()} stroke="none"/>"""
            ShapeType.STROKE -> """fill="none" stroke="${colorToRGBString(strokeColor)}" ${strokeOpacityString()} stroke-width="$strokeWidth"/>"""
            ShapeType.STROKE_AND_FILL -> """fill="${colorToRGBString(fillColor)}" ${fillOpacityString()} stroke="${colorToRGBString(strokeColor)}" ${strokeOpacityString()} stroke-width="$strokeWidth"/>"""
        }
    }

    override fun line(x: Float, y: Float, x2: Float, y2: Float) {
        val strokeRGB = colorToRGBString(strokeColor)
        svgBuilder.append("""<line x1="$x" y1="${transformY(y)}" x2="$x2" y2="${transformY(y2)}" stroke="$strokeRGB" ${strokeOpacityString()} stroke-width="$strokeWidth" />""")
    }

    override fun rect(x: Float, y: Float, width: Float, height: Float, shapeType: ShapeType) {
        // SVG에서 직사각형은 좌상단에서 시작하므로 y를 변환
        val transformedY = transformY(y + height) // 왼쪽 위 모서리로 변환
        svgBuilder.append("""<rect x="$x" y="$transformedY" width="$width" height="$height" """)
        svgBuilder.append(svgShapeTypeToSVG(shapeType))
    }

    override fun circle(x: Float, y: Float, radius: Float, shapeType: ShapeType) {
        // 원의 중심점 y좌표 변환
        svgBuilder.append("""<circle cx="$x" cy="${transformY(y)}" r="$radius" """)
        svgBuilder.append(svgShapeTypeToSVG(shapeType))
    }

    override fun ellipse(x: Float, y: Float, radiusX: Float, radiusY: Float, shapeType: ShapeType) {
        // 타원의 중심점 y좌표 변환
        svgBuilder.append("""<ellipse cx="$x" cy="${transformY(y)}" rx="$radiusX" ry="$radiusY" """)
        svgBuilder.append(svgShapeTypeToSVG(shapeType))
    }

    override fun polygon(points: FloatArray, shapeType: ShapeType) {
        if (points.size < 4) return

        val pointsStr = StringBuilder()
        for (i in points.indices step 2) {
            pointsStr.append("${points[i]},${transformY(points[i+1])} ")
        }

        svgBuilder.append("""<polygon points="$pointsStr" """)
        svgBuilder.append(svgShapeTypeToSVG(shapeType))
    }

    override fun text(text: String, x: Float, y: Float, size: Float, align: Int) {
        // SVG 텍스트의 y 좌표는 텍스트의 하단이 아닌 상단을 기준으로 함
        // 텍스트 높이의 대략적인 보정을 위해 사이즈의 절반 정도를 추가
        val adjustedY = transformY(y) + size

        val textAlign = when (align) {
            Align.left -> "start"
            Align.center -> "middle"
            Align.right -> "end"
            else -> "start"
        }

        svgBuilder.append("""<text x="$x" y="$adjustedY" font-size="$size" text-anchor="$textAlign" fill="${colorToRGBString(fillColor)}" ${fillOpacityString()} >""")
        svgBuilder.append(text)
        svgBuilder.append("</text>")
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

        return "rgb($r,$g,$b)"
    }

    private fun fillOpacityString(): String {
        return if (fillColor.a < 1f) " fill-opacity=\"${fillColor.a}\"" else ""
    }

    private fun strokeOpacityString(): String {
        return if (strokeColor.a < 1f) " stroke-opacity=\"${strokeColor.a}\"" else ""
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
}

// Default libGDX implementation
class GDXSimpleVG(val batch: PolygonBatch, val font: BitmapFont) : SimpleVG {
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

    override fun fillPath() {
        drawPolyline(paths, true, false)
        paths.clear()
        path = Polyline()
    }

    override fun strokePath() {
        drawPolyline(paths, false, true)
        paths.clear()
        path = Polyline()
    }

    override fun strokeAndFillPath() {
        drawPolyline(paths, true, true)
        paths.clear()
        path = Polyline()
    }

    private fun executeShapeType(shapeType: ShapeType) {
        when (shapeType) {
            ShapeType.FILL -> fillPath()
            ShapeType.STROKE -> strokePath()
            ShapeType.STROKE_AND_FILL -> strokeAndFillPath()
        }
    }

    override fun line(x: Float, y: Float, x2: Float, y2: Float) {
        batch.color = strokeColor
        batch.drawLine(x, y, x2, y2, strokeWidth)
    }

    override fun rect(x: Float, y: Float, width: Float, height: Float, shapeType: ShapeType) {
        if (shapeType.isFill) {
            batch.color = fillColor
            batch.drawRect(x, y, width, height)
        }
        if (shapeType.isStroke) {
            batch.color = strokeColor
            batch.drawLine(x, y, x + width, y, strokeWidth)
            batch.drawLine(x + width, y, x + width, y + height, strokeWidth)
            batch.drawLine(x + width, y + height, x, y + height, strokeWidth)
            batch.drawLine(x, y + height, x, y, strokeWidth)
        }
    }

    override fun circle(x: Float, y: Float, radius: Float, shapeType: ShapeType) {
        ellipse(x, y, radius, radius, shapeType)
    }

    override fun ellipse(x: Float, y: Float, radiusX: Float, radiusY: Float, shapeType: ShapeType) {
        beginPath()

        val segments = 24
        for (i in 0..segments) {
            val angle = (i.toFloat() / segments) * Math.PI.toFloat() * 2f
            val px = x + radiusX * cos(angle.toDouble()).toFloat()
            val py = y + radiusY * sin(angle.toDouble()).toFloat()

            if (i == 0) {
                moveTo(px, py)
            } else {
                lineTo(px, py)
            }
        }

        closePath()

        executeShapeType(shapeType)
    }

    override fun polygon(points: FloatArray, shapeType: ShapeType) {
        if (points.size < 4) return

        beginPath()
        moveTo(points[0], points[1])

        for (i in 2 until points.size step 2) {
            lineTo(points[i], points[i + 1])
        }

        closePath()

        executeShapeType(shapeType)
    }

    override fun text(text: String, x: Float, y: Float, size: Float, align: Int) {
        val originalColor = font.color.cpy()
        font.color = fillColor
        font.draw(batch, text, x, y, 0f, align, false)
        font.color = originalColor
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