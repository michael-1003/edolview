package kr.edoli.edolview.store

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import kr.edoli.edolview.image.ImageConvert
import kr.edoli.edolview.image.ImageSpec
import kr.edoli.edolview.image.timesAssign
import kr.edoli.edolview.util.FileUtils
import org.apache.commons.io.FilenameUtils
import org.opencv.core.Mat
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileReader
import kotlin.math.abs

/**
 * Created by daniel on 16. 11. 27.
 */
object ImageStore {

    private const val MAX_MEMORY = 8 * 1024 * 1024 * 1024L

    private val imageStoreMap = CacheBuilder.newBuilder()
            .maximumWeight(MAX_MEMORY)
            .weigher { _: String, v1: ImageSpec -> (v1.mat.total() * v1.mat.channels()).toInt() }
            .removalListener<String, ImageSpec> { it.value?.mat?.release() }
            .build(object : CacheLoader<String, ImageSpec>() {
                override fun load(key: String): ImageSpec {
                    return loadFromPath(key)
                }
            })

    private fun loadFromPath(path: String): ImageSpec {
        val ext = FilenameUtils.getExtension(path)
        val file = File(path)
        if (!file.exists()) {
            return ImageSpec(Mat())
        }

        val bytes = if (ext.lowercase() == "pfm") {
            // Remove trailing spaces when reading PFM files.
            // This is a workaround for the issue with OpenCV not handling trailing spaces correctly.
            val totalSize = file.length()
            val allBytes = ByteArray(totalSize.toInt())
            FileInputStream(file).use { fis ->
                val newLineCode = '\n'.code
                val spaceCode = ' '.code.toByte()

                var lineCount = 0
                var fisPos = 0
                var bytesPos = 0

                while (lineCount < 3) {
                    val value = fis.read()
                    if (value == newLineCode) {
                        lineCount++
                    }

                    if (value == newLineCode && bytesPos > 0 && allBytes[bytesPos - 1] == spaceCode) {
                        allBytes[bytesPos - 1] = newLineCode.toByte()
                    } else {
                        allBytes[bytesPos] = value.toByte()
                        bytesPos++
                    }
                    fisPos ++
                }
                fis.readNBytes(allBytes, bytesPos, (totalSize - fisPos).toInt())
            }
            allBytes
        } else {
            FileUtils.readFileToByteArray(file)
        }


        val lastModified = file.lastModified()
        var mat: Mat

        try {
            if (ext.lowercase() == "flo") {
                mat = ImageConvert.decodeFlo(bytes) ?: Mat()
            } else {
                mat = bytesToMat(bytes)

                if (ext.lowercase() == "pfm") {
                    val reader = BufferedReader(FileReader(path))
                    val header = reader.readLine()  // first line
                    val dimension = reader.readLine()  // second line
                    val scale = reader.readLine()  // third line

                    mat *= abs(scale.toDouble())
                }
            }
        } catch (_: Exception) {
            // Ignore error and pass empty mat
            mat = Mat()
        }

        return ImageSpec(mat, lastModified=lastModified)
    }

    fun bytesToMat(bytes: ByteArray): Mat {
        return ImageConvert.decodeBytes(bytes)
    }

    fun checkMagicNumber(bytes: ByteArray, magicNumber: ByteArray): Boolean {
        for (i in magicNumber.indices) {
            if (bytes[i] != magicNumber[i]) {
                return false
            }
        }
        return true
    }

    fun clearCache() {
        imageStoreMap.cleanUp()
        imageStoreMap.invalidateAll()
    }

    fun get(file: File): ImageSpec {
        return get(file.absolutePath, file.lastModified())
    }

    fun get(path: String, lastModified: Long): ImageSpec {
        val processedName = process(path)
        var imageSpec = imageStoreMap[processedName]

        if (imageSpec.lastModified != lastModified || imageSpec.isEmpty) {
            imageStoreMap.invalidate(processedName)
            imageSpec = imageStoreMap[processedName]
        }
        return imageSpec
    }

    private fun process(name: String): String {
        var pName = name
        while (pName.contains("//")) {
            pName = pName.replace("//", "/")
        }
        return pName
    }
}