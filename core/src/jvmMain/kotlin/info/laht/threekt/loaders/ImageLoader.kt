package info.laht.threekt.loaders

import info.laht.threekt.textures.Image
import kotlinx.io.core.IoBuffer
import org.lwjgl.BufferUtils
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO

actual object ImageLoader {

    private val cache = WeakHashMap<String, Image>()

    @JvmOverloads
    actual fun load(path: String, flipY: Boolean): Image {

        val file = File(path)
        if (!file.exists()) {
            throw NoSuchFileException(file)
        }
        val isJpg = file.name.endsWith(".jpg", true) || file.name.endsWith(".jpeg", true)

        return cache.getOrPut(file.absolutePath) {

            var img = ImageIO.read(file)
            if (flipY) {
                img = createFlipped(img)
            }

            val res = img.width * img.height
            val pixels = IntArray(res)
            img.getRGB(0, 0, img.width, img.height, pixels, 0, img.width)

            val buffer = IoBuffer(BufferUtils.createByteBuffer(pixels.size * (if (isJpg) 3 else 4)))
            for (y in 0 until img.height) {
                for (x in 0 until img.width) {
                    val pixel = pixels[y * img.width + x]
                    buffer.writeByte((pixel shr 16 and 0xFF).toByte()) // Red component
                    buffer.writeByte((pixel shr 8 and 0xFF).toByte()) // Green component
                    buffer.writeByte((pixel and 0xFF).toByte()) // Blue component

                    if (!isJpg) {
                        buffer.writeByte((pixel shr 24 and 0xFF).toByte()) // Alpha component. Only for RGBA
                    }

                }
            }

            Image(img.width, img.height, buffer)
        }
    }

    actual fun clearCache() {
        cache.clear()
    }

    private fun createFlipped(image: BufferedImage): BufferedImage {
        val at = AffineTransform()
        at.concatenate(AffineTransform.getScaleInstance(1.0, -1.0))
        at.concatenate(AffineTransform.getTranslateInstance(0.0, (-image.height).toDouble()))
        return createTransformed(image, at)
    }

    private fun createTransformed(
            image: BufferedImage, at: AffineTransform
    ): BufferedImage {
        val newImage = BufferedImage(
                image.width, image.height,
                BufferedImage.TYPE_INT_ARGB
        )
        val g = newImage.createGraphics()
        g.transform(at)
        g.drawImage(image, 0, 0, null)
        g.dispose()
        return newImage
    }

}
