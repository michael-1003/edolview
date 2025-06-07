package kr.edoli.edolview.ui.panel.histogram

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import kr.edoli.edolview.ImContext
import kr.edoli.edolview.ui.VGWidget
import kr.edoli.edolview.ui.vg.ShapeType
import kr.edoli.edolview.ui.vg.SimpleVG
import kr.edoli.edolview.util.Histogram
import kr.edoli.edolview.util.toColorValueStr

class HistogramWidget : VGWidget() {
    val histograms = ArrayList<Histogram>()
    val isShow = ArrayList<Boolean>()
    val colors = arrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.GRAY)

    var isOver = false
    var mouseX = 0f
    var mouseY = 0f

    init {
        addListener(object : InputListener() {
            override fun mouseMoved(event: InputEvent?, x: Float, y: Float): Boolean {
                mouseX = x
                mouseY = y
                return false
            }

            override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                isOver = true
            }
            override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                if (toActor == this@HistogramWidget) {
                    return
                }
                isOver = false
            }

        })
    }

    override fun drawVG(vg: SimpleVG) {
        while (isShow.size < histograms.size) {
            isShow.add(true)
        }

        if (histograms.isEmpty()) {
            return
        }

        val num = histograms[0].n
        val barWidth = width / num

        histograms.forEachIndexed { histIndex, hist ->

            if (!isShow[histIndex]) {
                return@forEachIndexed
            }
            val freq = hist.freq


            vg.beginPath()
            vg.setFillColor(colors[histIndex])
            vg.moveTo(0f, 0f)

            var lastH = -1f
            freq.forEachIndexed { index, v ->
                val offset = barWidth * index
                val h = (height * v) / hist.maxFreq

                if (lastH != h) {
                    vg.lineTo(offset, h)
                }
                vg.lineTo(offset + barWidth, h)

                lastH = h
            }

            vg.lineTo(width, 0f)
            vg.fillPath()
        }


        if (isOver) {
            val mouseXIndex = (mouseX / barWidth).toInt()

            if (mouseXIndex < 0 || mouseXIndex >= num) {
                return
            }

            // Draw vertical line at mouseXIndex
            vg.setStrokeColor(Color.LIGHT_GRAY)
            val barX = (mouseXIndex + 0.5f) * barWidth
            vg.line(barX, 0f, barX, height)

            // Value Tooltip
            val freqWidth = 64f
            val marginFromBar = 4f
            val valueX = if (barX > width - freqWidth) barX - freqWidth - marginFromBar else barX + marginFromBar
            val valueY = mouseY.coerceIn(0f, height - 20f)
            vg.setFillColor(Color(0f, 0f, 0f, 0.75f))
            vg.rect(valueX, valueY, freqWidth, 20f, shapeType = ShapeType.STROKE_AND_FILL)

            val imageSpec = ImContext.mainImageSpec.get() ?: return
            val histValue = histograms[0].value(mouseXIndex).toColorValueStr(imageSpec, 2)
            vg.setFillColor(Color.GRAY)
            vg.text(histValue, valueX + 2f, valueY + 16f + 2f)

        }
    }
}