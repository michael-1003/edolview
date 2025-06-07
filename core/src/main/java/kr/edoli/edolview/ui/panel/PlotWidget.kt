package kr.edoli.edolview.ui.panel

import com.badlogic.gdx.graphics.Color
import kr.edoli.edolview.ui.VGWidget
import kr.edoli.edolview.ui.vg.SimpleVG

class PlotWidget : VGWidget() {
    var data: FloatArray? = null
    var minValue = 0f
    var maxValue = 1f
    var channels = 1

    override fun drawVG(vg: SimpleVG) {
        val data = this.data
        if (data == null || data.isEmpty()) {
            return
        }

        val scale = height / (maxValue - minValue)
        val size = data.size / channels
        val step = width / (size - 1)

        val colors = if (channels == 1) {
            arrayOf(Color.GRAY)
        } else {
            arrayOf(
                Color.RED,
                Color.GREEN,
                Color.BLUE,
                Color.GRAY
            )
        }

        for (c in 0 until channels) {
            vg.beginPath()
            vg.setStrokeColor(colors[c])

            var offsetX = 0f
            vg.moveTo(offsetX, (data[c] - minValue) * scale)

            for (i in 1 until size) {
                val curY = (data[c + i * channels] - minValue) * scale
                vg.lineTo(offsetX + step, curY)
                offsetX += step
            }

            vg.strokePath()
        }
    }
}