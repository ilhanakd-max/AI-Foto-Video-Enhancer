package com.ilhanakd.aiphotovideoenhancer.ui.video

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ilhanakd.aiphotovideoenhancer.R
import com.ilhanakd.aiphotovideoenhancer.domain.model.EnhanceProfile
import com.ilhanakd.aiphotovideoenhancer.domain.model.ProcessingState
import com.ilhanakd.aiphotovideoenhancer.domain.usecase.EnhanceVideoUseCase
import com.ilhanakd.aiphotovideoenhancer.ml.VideoEnhancerModelRunner

@Composable
fun VideoEnhanceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: VideoEnhanceViewModel = viewModel(factory = VideoEnhanceViewModelFactory(context))
    val selectedUri by viewModel.selectedUri.collectAsState()
    val processing by viewModel.processing.collectAsState()
    val profile by viewModel.selectedProfile.collectAsState()
    val videoInfo by viewModel.videoInfo.collectAsState()

    val pickVideoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            viewModel.updateUri(context.contentResolver, it) { durationMs, width, height, size ->
                val minutes = durationMs / 1000 / 60
                val seconds = (durationMs / 1000 % 60)
                val humanSize = "${size / (1024 * 1024)} MB"
                context.getString(R.string.video_info, String.format("%02d:%02d", minutes, seconds), width, height, humanSize)
            }
        }
    }

    val shareIntent = remember { mutableStateOf<Intent?>(null) }
    shareIntent.value?.let {
        LaunchedEffect(it) {
            context.startActivity(Intent.createChooser(it, context.getString(R.string.share_video)))
            shareIntent.value = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.enhance_video)) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
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
            if (selectedUri == null) {
                Text(text = stringResource(id = R.string.select_video))
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { pickVideoLauncher.launch("video/*") }) {
                    Text(text = stringResource(id = R.string.pick_video))
                }
            } else {
                videoInfo?.let { Text(text = it) }
                Spacer(modifier = Modifier.height(12.dp))
                ProfileSelector(profile, onSelect = { viewModel.selectProfile(it) })
                Spacer(modifier = Modifier.height(12.dp))
                if (processing is ProcessingState.Processing) {
                    val progress = (processing as ProcessingState.Processing).progress / 100f
                    LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
                    (processing as ProcessingState.Processing).etaSeconds?.let {
                        Text(text = stringResource(id = R.string.eta_label, it))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = viewModel::enhance, enabled = processing !is ProcessingState.Processing) {
                    Text(text = stringResource(id = R.string.enhance))
                }
            }
        }
    }

    when (processing) {
        is ProcessingState.Completed -> {
            val path = (processing as ProcessingState.Completed).outputPath
            val uri = saveVideo(context, path)
            AlertDialog(
                onDismissRequest = viewModel::reset,
                confirmButton = {
                    Button(onClick = viewModel::reset) { Text(text = stringResource(id = R.string.ok)) }
                },
                title = { Text(text = stringResource(id = R.string.completed)) },
                text = { Text(text = stringResource(id = R.string.save_success)) }
            )
            uri?.let {
                shareIntent.value = Intent(Intent.ACTION_SEND).apply {
                    type = "video/*"
                    putExtra(Intent.EXTRA_STREAM, it)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
        }
        is ProcessingState.Error -> {
            AlertDialog(
                onDismissRequest = viewModel::reset,
                confirmButton = { Button(onClick = viewModel::reset) { Text(text = stringResource(id = R.string.ok)) } },
                title = { Text(text = stringResource(id = R.string.error_processing)) },
                text = { Text(text = (processing as ProcessingState.Error).message) }
            )
        }
        else -> Unit
    }
}

@Composable
private fun ProfileSelector(selected: EnhanceProfile, onSelect: (EnhanceProfile) -> Unit) {
    Column {
        ProfileOption(title = stringResource(id = R.string.profile_soft), selected = selected == EnhanceProfile.SOFT_CLEAN) {
            onSelect(EnhanceProfile.SOFT_CLEAN)
        }
        ProfileOption(title = stringResource(id = R.string.profile_sharp), selected = selected == EnhanceProfile.STRONG_SHARP) {
            onSelect(EnhanceProfile.STRONG_SHARP)
        }
        ProfileOption(title = stringResource(id = R.string.profile_night), selected = selected == EnhanceProfile.NIGHT_BOOST) {
            onSelect(EnhanceProfile.NIGHT_BOOST)
        }
    }
}

@Composable
private fun ProfileOption(title: String, selected: Boolean, onSelect: () -> Unit) {
    androidx.compose.material3.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(text = title, modifier = Modifier.padding(start = 8.dp))
    }
}

private fun saveVideo(context: Context, filePath: String): Uri? {
    val file = java.io.File(filePath)
    val values = android.content.ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/AIPhotoVideoEnhancer")
    }
    val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
    uri?.let {
        context.contentResolver.openOutputStream(it)?.use { output ->
            file.inputStream().use { input -> input.copyTo(output) }
        }
    }
    return uri
}

class VideoEnhanceViewModelFactory(private val context: Context) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        val useCase = EnhanceVideoUseCase(VideoEnhancerModelRunner(context))
        return VideoEnhanceViewModel(useCase) as T
    }
}
