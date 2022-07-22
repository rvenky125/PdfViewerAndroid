package com.famas.pdfviewer.pdftools

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun File.renderPdf(lastLoadedPageIndex: Int = -1, resources: Resources): RenderData {
    val bitmaps = mutableListOf<Bitmap>()
    var pagesCount = 0
    withContext(Dispatchers.IO) {
        try {
            val fileDescriptor =
                ParcelFileDescriptor.open(this@renderPdf, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)

            pagesCount = pdfRenderer.pageCount

            val lowerIndex = lastLoadedPageIndex + 1
            val higherIndex = lowerIndex + 3

            Log.d(
                "myTag",
                "pages count:$pagesCount lower index:$lowerIndex higherIndex:$higherIndex"
            )
            for (i in lowerIndex..higherIndex) {
                val page = pdfRenderer.openPage(i).apply {
                    Log.d("myTag", "Creating page with index: $i")
                    val bitmap =
                        Bitmap.createBitmap(
                            (this.width / 72) * resources.displayMetrics.densityDpi,
                            (this.height / 72) * resources.displayMetrics.densityDpi,
                            Bitmap.Config.ARGB_8888
                        )
                    render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmaps.add(bitmap)
                }
                page.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return RenderData(bitmaps, pagesCount)
}


data class RenderData(
    val bitmaps: List<Bitmap>,
    val pagesCount: Int
)