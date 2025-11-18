package com.ilhanakd.aiphotovideoenhancer.ui.video

import android.content.ContentResolver
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilhanakd.aiphotovideoenhancer.domain.model.EnhanceProfile
import com.ilhanakd.aiphotovideoenhancer.domain.model.ProcessingState
import com.ilhanakd.aiphotovideoenhancer.domain.usecase.EnhanceVideoUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoEnhanceViewModel(private val useCase: EnhanceVideoUseCase) : ViewModel() {

    private val _selectedUri = MutableStateFlow<Uri?>(null)
    val selectedUri: StateFlow<Uri?> = _selectedUri

    private val _processing = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val processing: StateFlow<ProcessingState> = _processing

    private val _selectedProfile = MutableStateFlow(EnhanceProfile.SOFT_CLEAN)
    val selectedProfile: StateFlow<EnhanceProfile> = _selectedProfile

    private val _videoInfo = MutableStateFlow<String?>(null)
    val videoInfo: StateFlow<String?> = _videoInfo

    fun selectProfile(profile: EnhanceProfile) {
        _selectedProfile.value = profile
    }

    fun updateUri(contentResolver: ContentResolver, uri: Uri, formatString: (Long, Int, Int, Long) -> String) {
        _selectedUri.value = uri
        viewModelScope.launch {
            val info = withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                val descriptor = contentResolver.openFileDescriptor(uri, "r")
                if (descriptor != null) {
                    retriever.setDataSource(descriptor.fileDescriptor)
                }
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val size = descriptor?.statSize ?: 0L
                descriptor?.close()
                retriever.release()
                formatString(durationMs, width, height, size)
            }
            _videoInfo.value = info
        }
    }

    fun enhance() {
        val uri = _selectedUri.value ?: return
        _processing.value = ProcessingState.Processing(0)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val output = useCase(uri, _selectedProfile.value) { progress, eta ->
                    _processing.value = ProcessingState.Processing(progress, eta)
                }
                _processing.value = ProcessingState.Completed(output)
            } catch (e: Exception) {
                _processing.value = ProcessingState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun reset() {
        _processing.value = ProcessingState.Idle
    }
}
