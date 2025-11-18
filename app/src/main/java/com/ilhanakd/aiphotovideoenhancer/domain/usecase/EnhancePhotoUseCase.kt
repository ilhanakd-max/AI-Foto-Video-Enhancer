package com.ilhanakd.aiphotovideoenhancer.domain.usecase

import android.graphics.Bitmap
import com.ilhanakd.aiphotovideoenhancer.domain.model.EnhanceParams
import com.ilhanakd.aiphotovideoenhancer.ml.PhotoEnhancerModelRunner

class EnhancePhotoUseCase(private val modelRunner: PhotoEnhancerModelRunner) {
    suspend operator fun invoke(bitmap: Bitmap, params: EnhanceParams): Bitmap {
        return modelRunner.enhancePhoto(bitmap, params)
    }
}
