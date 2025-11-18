package com.ilhanakd.aiphotovideoenhancer.ui.photo

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ilhanakd.aiphotovideoenhancer.R
import com.ilhanakd.aiphotovideoenhancer.domain.model.ProcessingState
import com.ilhanakd.aiphotovideoenhancer.ui.components.BeforeAfterSlider
import com.ilhanakd.aiphotovideoenhancer.ui.components.PrimaryButton
import com.ilhanakd.aiphotovideoenhancer.domain.usecase.EnhancePhotoUseCase
import com.ilhanakd.aiphotovideoenhancer.ml.PhotoEnhancerModelRunner

@Composable
fun PhotoEnhanceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: PhotoEnhanceViewModel = viewModel(factory = PhotoEnhanceViewModelFactory(context))
    val params by viewModel.params.collectAsState()
    val original by viewModel.original.collectAsState()
    val enhanced by viewModel.enhanced.collectAsState()
    val state by viewModel.processingState.collectAsState()

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.loadImage(context.contentResolver, it) }
    }

    val shareIntent = remember { mutableStateOf<Intent?>(null) }

    shareIntent.value?.let { intent ->
        LaunchedEffect(intent) {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_image)))
            shareIntent.value = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.enhance_photo)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            if (original == null) {
                Text(text = stringResource(id = R.string.select_image))
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryButton(text = stringResource(id = R.string.pick_image)) {
                    pickImageLauncher.launch("image/*")
                }
            } else {
                BeforeAfterSlider(
                    before = original?.asImageBitmap(),
                    after = (enhanced ?: original)?.asImageBitmap(),
                    beforeLabel = stringResource(id = R.string.before),
                    afterLabel = stringResource(id = R.string.after),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                SliderSection(
                    title = stringResource(id = R.string.sharpness),
                    value = params.sharpness.toFloat(),
                    onValueChange = { viewModel.updateParams(sharpness = it.toInt()) }
                )
                SliderSection(
                    title = stringResource(id = R.string.denoise),
                    value = params.denoise.toFloat(),
                    onValueChange = { viewModel.updateParams(denoise = it.toInt()) }
                )
                SliderSection(
                    title = stringResource(id = R.string.brightness),
                    value = params.brightness,
                    valueRange = -0.5f..0.5f,
                    onValueChange = { viewModel.updateParams(brightness = it) }
                )
                SliderSection(
                    title = stringResource(id = R.string.contrast),
                    value = params.contrast,
                    valueRange = 0.5f..1.8f,
                    onValueChange = { viewModel.updateParams(contrast = it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (state is ProcessingState.Processing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryButton(text = stringResource(id = R.string.enhance), modifier = Modifier.weight(1f)) {
                        viewModel.enhance()
                    }
                    if (enhanced != null) {
                        Button(modifier = Modifier.weight(1f), onClick = {
                            val uri = saveBitmap(context, enhanced!!)
                            shareIntent.value = Intent(Intent.ACTION_SEND).apply {
                                type = "image/*"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        }) {
                            Text(text = stringResource(id = R.string.share))
                        }
                    }
                }
            }
        }
    }

    when (state) {
        is ProcessingState.Completed -> {
            AlertDialog(
                onDismissRequest = viewModel::resetProcessing,
                confirmButton = {
                    Button(onClick = viewModel::resetProcessing) {
                        Text(text = stringResource(id = R.string.ok))
                    }
                },
                title = { Text(text = stringResource(id = R.string.completed)) },
                text = {
                    val savedUri = enhanced?.let { saveBitmap(context, it) }
                    Text(text = stringResource(id = R.string.save_success))
                    savedUri?.let {
                        shareIntent.value = Intent(Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, it)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    }
                }
            )
        }
        is ProcessingState.Error -> {
            AlertDialog(
                onDismissRequest = viewModel::resetProcessing,
                confirmButton = {
                    Button(onClick = viewModel::resetProcessing) {
                        Text(text = stringResource(id = R.string.ok))
                    }
                },
                title = { Text(text = stringResource(id = R.string.error_processing)) },
                text = { Text(text = (state as ProcessingState.Error).message) }
            )
        }
        else -> Unit
    }
}

@Composable
private fun SliderSection(title: String, value: Float, valueRange: ClosedFloatingPointRange<Float> = 0f..100f, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(activeTrackColor = MaterialTheme.colorScheme.primary)
        )
    }
}

private fun saveBitmap(context: Context, bitmap: Bitmap): Uri? {
    val fileName = "photo_${System.currentTimeMillis()}.jpg"
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = android.content.ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AIPhotoVideoEnhancer")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(it, values, null, null)
        }
        uri
    } else {
        @Suppress("DEPRECATION")
        val directory = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val values = android.content.ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        val uri = context.contentResolver.insert(directory, values)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
        }
        uri
    }
}

class PhotoEnhanceViewModelFactory(private val context: Context) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        val useCase = EnhancePhotoUseCase(PhotoEnhancerModelRunner(context))
        return PhotoEnhanceViewModel(useCase) as T
    }
}
