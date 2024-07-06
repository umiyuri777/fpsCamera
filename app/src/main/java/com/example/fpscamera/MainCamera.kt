import android.Manifest
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainCamera() {
    val permissionState: PermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    if(permissionState.status.isGranted){
        CametaStert()
    }else{
        NoPermission(onRequestPermission = permissionState::launchPermissionRequest)
    }
}

@Composable
fun NoPermission(onRequestPermission:() -> Unit){
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick =  onRequestPermission ) {
            Text(text = "カメラの許可を与えてください")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CametaStert(){
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture =  remember {
        ImageCapture.Builder()
            .setFlashMode(ImageCapture.FLASH_MODE_AUTO) // フラッシュモードを設定
            .setTargetAspectRatio(AspectRatio.RATIO_16_9) // ターゲットのアスペクト比を設定3) // ターゲットのアスペクト比を設定
            .build()
    }
    val previewView = PreviewView(context).apply {
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        scaleType = PreviewView.ScaleType.FILL_CENTER
    }
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    DisposableEffect(Unit) {
        val cameraProvider = cameraProviderFuture.get()
        Log.d("cameraProvider",cameraProvider.toString())
        val preview = androidx.camera.core.Preview.Builder().build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview.setSurfaceProvider(previewView.surfaceProvider)
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )
        onDispose {
            cameraProvider.unbindAll()
        }
    }
    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = { takePicture(context, imageCapture) }) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Clothesadd")
        }
    }) {paddingValues: PaddingValues ->
        AndroidView(factory = {previewView},
            modifier = Modifier.padding(paddingValues))
    }
}

fun takePicture(context: android.content.Context, imageCapture: ImageCapture) {
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/fpsCamera")
    }
//    val photoDirectory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString())
//    if (!photoDirectory.exists()) {
//        photoDirectory.mkdirs() // Create the directory if it does not exist
//    }
//    println(photoDirectory)
//    val photoFile = File(
//        photoDirectory,
//        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
//            .format(System.currentTimeMillis()) + ".jpg"
//    )
    val outputOptions = ImageCapture.OutputFileOptions
        .Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exception: ImageCaptureException) {
                Log.e("TakePicture", "Photo capture failed: ${exception.message}", exception)
                Toast.makeText(context, "撮影に失敗しました : ${exception.message}", Toast.LENGTH_SHORT).show()
            }

            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = outputFileResults.savedUri ?: Uri.EMPTY
                val msg = "撮影しました!: $savedUri"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                Log.d("TakePicture", msg)
            }
        }
    )
}