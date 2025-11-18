package com.ilhanakd.aiphotovideoenhancer.domain.usecase

import android.net.Uri
import com.ilhanakd.aiphotovideoenhancer.domain.model.EnhanceProfile
import com.ilhanakd.aiphotovideoenhancer.ml.VideoEnhancerModelRunner

class EnhanceVideoUseCase(private val modelRunner: VideoEnhancerModelRunner) {
    suspend operator fun invoke(uri: Uri, profile: EnhanceProfile, onProgress: (Int, Int?) -> Unit): String {
        return modelRunner.enhanceVideo(uri, profile, onProgress)
    }
}
