package kr.edoli.edolview.ui.panel.histogram

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import kr.edoli.edolview.ui.UIRes
import kr.edoli.edolview.ui.VGWidget
import kr.edoli.edolview.ui.vg.SimpleVG
import kr.edoli.edolview.util.Histogram

class HistogramWidget : VGWidget() {
    val histograms = ArrayList<Histogram>()
    val isShow = ArrayList<Boolean>()
    val colors = arrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.GRAY)

    override fun drawVG(vg: SimpleVG) {
        while (isShow.size < histograms.size) {
            isShow.add(true)
        }


        histograms.forEachIndexed { histIndex, hist ->
            vg.beginPath()
            vg.moveTo(0f, 0f)

            if (!isShow[histIndex]) {
                return@forEachIndexed
            }

            val num = hist.n
            val barWidth = width / num
            val freq = hist.freq

            vg.setFillColor(colors[histIndex])

            var lastH = -1f

            freq.forEachIndexed { index, v ->
                val offset = barWidth * index
                val h = (height * v) / hist.maxFreq

                if (lastH != h) {
                    vg.lineTo(offset, h)
                }
                vg.lineTo(offset + barWidth, h)
            }

            vg.lineTo(width, 0f)
            vg.fill()
        }
    }
}