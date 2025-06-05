package kr.edoli.edolview.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.PolygonBatch
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.utils.BufferUtils
import com.badlogic.gdx.utils.viewport.ScreenViewport
import kr.edoli.edolview.image.ClipboardUtils
import kr.edoli.edolview.image.ImageConvert
import kr.edoli.edolview.ui.vg.GDXSimpleVG
import kr.edoli.edolview.ui.vg.SVGSimpleVG
import kr.edoli.edolview.ui.vg.SimpleVG

abstract class VGWidget : Widget() {

    init {
        contextMenu {
            addMenu("Copy as PNG") {
                val widget = this@VGWidget
                val fboWidth = widget.width.toInt()
                val fboHeight = widget.height.toInt()

                // Create a framebuffer with the widget dimensions
                val fbo = FrameBuffer(Pixmap.Format.RGBA8888, fboWidth, fboHeight, false)

                // Begin rendering to framebuffer
                fbo.begin()

                // Get batch for drawing
                val fboBatch = PolygonSpriteBatch()
                // x, y가 0, 0이 되도록 변경// x, y가 0, 0이 되도록 변경
                val viewport = ScreenViewport().also { it.update(fboWidth, fboHeight, true) }
                val camera = viewport.camera
                fboBatch.projectionMatrix = viewport.camera.combined

                fboBatch.begin()

                // Draw the widget content
                drawVG(GDXSimpleVG(fboBatch))

                fboBatch.end()

                // Create a pixmap from framebuffer

                val texture = fbo.colorBufferTexture
                texture.bind()
                val channels = 4

                Gdx.gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1)
                val pixels = BufferUtils.newByteBuffer(texture.width * texture.height * channels)
                Gdx.gl.glReadPixels(0, 0, texture.width, texture.height, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixels)

                val error = Gdx.gl.glGetError()
                if (error != GL30.GL_NO_ERROR) {
                    Gdx.app.error("Texture dump", "Get error: $error")
                }

                pixels.position(0)
                val byteArray = ByteArray(pixels.remaining())
                val width = fbo.width
                val height = fbo.height

                for (i in 0 until height) {
                    pixels.get(byteArray, width * (height - i - 1) * channels, width * channels)
                }

                fbo.end()

                // Copy to clipboard using Gdx.app.clipboard
                ClipboardUtils.putImage(ImageConvert.byteArrayToBufferedImage(byteArray, width, height, 4))

                // Dispose resources
                fboBatch.dispose()
                fbo.dispose()
            }
            addMenu("Copy as SVG") {
                val widget = this@VGWidget
                val fboWidth = widget.width.toInt()
                val fboHeight = widget.height.toInt()

                val vg = SVGSimpleVG()

                vg.beginSVG(fboWidth, fboHeight)

                drawVG(vg)

                val svg = vg.endSVG()

                ClipboardUtils.putSVG(svg)
            }
        }
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        val oldMatrix = batch.transformMatrix.cpy()
        batch.transformMatrix = batch.transformMatrix.translate(x, y, 0f)

        val vg = GDXSimpleVG(batch as PolygonBatch)
        drawVG(vg)

        batch.transformMatrix = oldMatrix
    }

    abstract fun drawVG(vg: SimpleVG)
}