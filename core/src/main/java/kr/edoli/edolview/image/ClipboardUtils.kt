package kr.edoli.edolview.image

import org.opencv.core.Mat
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.*
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane


class ImageSelection(private val image: java.awt.Image) : Transferable {

    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return arrayOf(DataFlavor.imageFlavor)
    }

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
        return DataFlavor.imageFlavor.equals(flavor)
    }

    override fun getTransferData(flavor: DataFlavor): java.awt.Image {
        if (!DataFlavor.imageFlavor.equals(flavor)) {
            throw UnsupportedFlavorException(flavor)
        }
        return image
    }
}

class SVGSelection(private val svgText: String) : Transferable {

    companion object {
        private val svgInputStreamFlavor = DataFlavor("image/svg+xml;class=java.io.InputStream")
        private val flavors = arrayOf(
            svgInputStreamFlavor,
            DataFlavor.stringFlavor,
            DataFlavor.javaFileListFlavor
        )
    }

    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return flavors.clone()
    }

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
        for (f in flavors) {
            if (f.equals(flavor)) {
                return true
            }
        }
        return false
    }

    @Throws(UnsupportedFlavorException::class, IOException::class)
    override fun getTransferData(flavor: DataFlavor): Any {
        // 1) SVG 문자열 그대로 필요하다면 → stringFlavor로 제공
        if (flavor.equals(DataFlavor.stringFlavor)) {
            return svgText
        }
        // 2) SVG를 InputStream 형식(이미지로 인식하도록) 제공
        if (flavor.equals(svgInputStreamFlavor)) {
            val bytes: ByteArray = svgText.toByteArray(StandardCharsets.UTF_8)
            return ByteArrayInputStream(bytes)
        }
        // 3) javaFileListFlavor: 임시 .svg 파일을 만들어 FileList로 넘겨줌
        if (flavor.equals(DataFlavor.javaFileListFlavor)) {
            // 임시 파일 생성
            val tempFile = File.createTempFile("temp_svg_clipboard", ".svg")
            tempFile.deleteOnExit()
            Files.write(tempFile.toPath(), svgText.toByteArray(StandardCharsets.UTF_8))
            val fileList: List<File> = Collections.singletonList(tempFile)
            return fileList
        }
        throw UnsupportedFlavorException(flavor)
    }
}

object ClipboardUtils {
    fun putSVG(svgData: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = SVGSelection(svgData)
        clipboard.setContents(selection, null)
    }

    fun putImage(mat: Mat) {
        val bufferedImage = try {
            ImageConvert.matToBufferedImage(mat)
        } catch (ex: Exception) {
            Thread {
                JOptionPane.showMessageDialog(null, ex.message, "Image conversion error", JOptionPane.ERROR_MESSAGE)
            }.start()
            null
        }
        if (bufferedImage != null) {
            putImage(bufferedImage)
        }
    }

    fun putImage(buffered: BufferedImage) {
        val imgSel = ImageSelection(buffered)

        // Clipboard는 기본적으로 png, jpeg 모두 생성하려고 함.
        // channel 갯수가 4개 일때는 png는 생성되지만 jpeg에서는 에러가 발생
        // 해당 에러는 무시 가능
        Toolkit.getDefaultToolkit().systemClipboard.setContents(imgSel, null)
    }

    fun hasImage(): Boolean {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val content = clipboard.getContents(null)
        if (!content.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            return false
        }
        return true
    }

    fun processClipboard(
            imageHandler: ((Transferable?) -> Unit)?,
            fileHandler: ((Transferable?) -> Unit)?,
            stringHandler: ((Transferable?) -> Unit)?
    ) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val content = clipboard.getContents(null)
        if (content.isDataFlavorSupported(DataFlavor.imageFlavor) && imageHandler != null) {
            imageHandler(content)
        } else if (content.isDataFlavorSupported(DataFlavor.javaFileListFlavor) && fileHandler != null) {
            fileHandler(content)
        } else if (content.isDataFlavorSupported(DataFlavor.stringFlavor) && stringHandler != null) {
            stringHandler(content)
        }
    }

    fun getImage(): Image? {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val content = clipboard.getContents(null)
        if (!content.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            return null
        }
        return content.getTransferData(DataFlavor.imageFlavor) as Image?
    }

    fun showClipboardImage() {
        val image = getImage()
        if (image != null) {
            showImage(image)
        }
    }

    fun showImage(image: Image) {
        val frame = JFrame()
        frame.title = "Cropped"
        frame.add(JLabel().apply {
            icon = ImageIcon(image)
        })
        frame.pack()
        frame.isVisible = true
    }

    fun putString(text: String) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }

    fun getString(): String {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val content = clipboard.getContents(null)
        return content.getTransferData(DataFlavor.stringFlavor) as String? ?: ""
    }

    @Suppress("UNCHECKED_CAST")
    fun getFileList(): List<File> {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val content = clipboard.getContents(null)
        return content.getTransferData(DataFlavor.javaFileListFlavor) as List<File>? ?: listOf()
    }
}