package com.jder.data
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File
class ImageExporter {
    fun exportToPNG(imageBitmap: ImageBitmap, file: File): Result<Unit> = try {
        val skiaBitmap = imageBitmap.asSkiaBitmap()
        val image = Image.makeFromBitmap(skiaBitmap)
        val data = image.encodeToData(EncodedImageFormat.PNG)
        data?.let {
            file.writeBytes(it.bytes)
            Result.success(Unit)
        } ?: Result.failure(Exception("Impossibile codificare l'immagine"))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
