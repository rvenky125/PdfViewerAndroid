@file:OptIn(ExperimentalComposeUiApi::class)

package com.famas.pdfviewer

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.famas.pdfviewer.pdftools.renderPdf
import com.famas.pdfviewer.ui.theme.PdfViewerTheme
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    @SuppressLint("MutableCollectionMutableState")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PdfViewerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val (file, setFile) = remember { mutableStateOf<File?>(null) }
                    var pagesCount by remember { mutableStateOf(0) }
                    val registerForPdfFile = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) {
                        val tempFile = File(cacheDir, "sample.pdf")
                        if (!tempFile.exists()) {
                            tempFile.createNewFile()
                        }
                        val inputStream = contentResolver.openInputStream(
                            it ?: return@rememberLauncherForActivityResult
                        )
                            ?: return@rememberLauncherForActivityResult
                        inputStream.use { input ->
                            tempFile.outputStream().use { out ->
                                input.copyTo(out)
                            }
                        }
                        setFile(tempFile)
                    }

                    val lazyListState = rememberLazyListState()
                    var bitmaps by remember { mutableStateOf<List<Bitmap?>>(emptyList()) }
                    var lastLoadedPageIndex by remember { mutableStateOf(-1) }
                    var currentPage by remember { mutableStateOf(0) }
                    val scale = remember { mutableStateOf(1f) }
                    var offset by remember { mutableStateOf(Offset.Zero) }
                    val scaleAnimatable = animateFloatAsState(targetValue = scale.value)
                    val coroutine = rememberCoroutineScope()

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Zoomable(state = rememberZoomableState(), modifier = Modifier.weight(1f)) {
                            LazyColumn(state = lazyListState) {
                                itemsIndexed(bitmaps) { i, item ->
                                    item?.let {
                                        Card {
                                            Image(
                                                bitmap = it.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    } ?: Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(500.dp)
                                            .padding(vertical = 8.dp)
                                            .background(Color.Gray)
                                    )
                                    currentPage = i
                                }
                            }
                        }

                        Button(onClick = { registerForPdfFile.launch("application/pdf") }) {
                            Text(text = "Open Pdf")
                        }

                        LaunchedEffect(
                            key1 = currentPage,
                            key2 = file,
                            block = {
                                Log.d("myTag", "executed launched effect currentPage:$currentPage")
                                if (currentPage >= lastLoadedPageIndex - 2) {
                                    val data = file?.renderPdf(lastLoadedPageIndex, resources)
                                    if (bitmaps.isEmpty()) {
                                        bitmaps =
                                            MutableList(data?.pagesCount ?: 0) { null }.toList()
                                    }
                                    bitmaps = bitmaps.mapIndexed { index, bitmap ->
                                        when {
                                            index <= lastLoadedPageIndex -> bitmap
                                            else -> if (index < (data?.bitmaps?.size
                                                    ?: (0 - 1))
                                            ) {
                                                lastLoadedPageIndex = index
                                                data?.bitmaps?.get(index - lastLoadedPageIndex)
                                            } else bitmap
                                        }
                                    }
                                }
                            })
                    }
                }
            }
        }
    }
}