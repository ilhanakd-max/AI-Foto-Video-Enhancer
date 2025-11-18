package com.ilhanakd.aiphotovideoenhancer.domain.model

sealed class ProcessingState {
    object Idle : ProcessingState()
    data class Processing(val progress: Int = 0, val etaSeconds: Int? = null) : ProcessingState()
    data class Completed(val outputPath: String) : ProcessingState()
    data class Error(val message: String) : ProcessingState()
}
