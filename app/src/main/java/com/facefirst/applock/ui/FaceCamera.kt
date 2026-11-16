package com.facefirst.applock.ui

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.facefirst.applock.face.FaceAnalyzer
import com.facefirst.applock.face.FrameObs
import java.util.concurrent.Executors

/** Ön kamera önizlemesi + kare analizi. [onFrame] ana iş parçacığında çağrılır. */
@Composable
fun FaceCamera(templates: List<FloatArray>?, modifier: Modifier = Modifier, onFrame: (FrameObs, FloatArray?) -> Unit) {
    val context = LocalContext.current
    val owner = context as LifecycleOwner
    val callback by rememberUpdatedState(onFrame)
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    DisposableEffect(templates) {
        val executor = Executors.newSingleThreadExecutor()
        val main = ContextCompat.getMainExecutor(context)
        var provider: ProcessCameraProvider? = null
        var analyzer: FaceAnalyzer? = null
        var disposed = false
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (disposed) return@addListener
            try {
                val p = future.get()
                val a = FaceAnalyzer(context.applicationContext, templates) { o, emb -> main.execute { if (!disposed) callback(o, emb) } }
                provider = p; analyzer = a
                val analysis = ImageAnalysis.Builder()
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also { it.setAnalyzer(executor, a) }
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                p.unbindAll()
                p.bindToLifecycle(owner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
            } catch (ex: Exception) {
                callback(FrameObs(error = true), null)
            }
        }, main)
        onDispose {
            disposed = true
            analyzer?.closed = true
            runCatching { provider?.unbindAll() }
            val a = analyzer
            executor.execute { runCatching { a?.close() } } // analiz bittikten sonra kapat
            executor.shutdown()
        }
    }
    AndroidView(factory = { previewView }, modifier = modifier)
}
