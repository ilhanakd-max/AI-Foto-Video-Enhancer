package com.ilhanakd.aiphotovideoenhancer.ui.photo

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilhanakd.aiphotovideoenhancer.domain.model.EnhanceParams
import com.ilhanakd.aiphotovideoenhancer.domain.model.ProcessingState
import com.ilhanakd.aiphotovideoenhancer.domain.usecase.EnhancePhotoUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class PhotoEnhanceViewModel(private val useCase: EnhancePhotoUseCase) : ViewModel() {

    private val _params = MutableStateFlow(EnhanceParams())
    val params: StateFlow<EnhanceParams> = _params

    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val processingState: StateFlow<ProcessingState> = _processingState

    private val _original = MutableStateFlow<Bitmap?>(null)
    val original: StateFlow<Bitmap?> = _original

    private val _enhanced = MutableStateFlow<Bitmap?>(null)
    val enhanced: StateFlow<Bitmap?> = _enhanced

    fun updateParams(sharpness: Int? = null, denoise: Int? = null, brightness: Float? = null, contrast: Float? = null) {
        _params.value = _params.value.copy(
            sharpness = sharpness ?: _params.value.sharpness,
            denoise = denoise ?: _params.value.denoise,
            brightness = brightness ?: _params.value.brightness,
            contrast = contrast ?: _params.value.contrast
        )
    }

    fun loadImage(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                val input: InputStream? = contentResolver.openInputStream(uri)
                input.use { BitmapFactory.decodeStream(it) }
            }
            _original.value = bitmap
            _enhanced.value = null
        }
    }

    fun enhance() {
        val bitmap = _original.value ?: return
        _processingState.value = ProcessingState.Processing(progress = 0)
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val result = useCase(bitmap, _params.value)
                _enhanced.value = result
                _processingState.value = ProcessingState.Completed("")
            } catch (e: Exception) {
                _processingState.value = ProcessingState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetProcessing() {
        _processingState.value = ProcessingState.Idle
    }
}
