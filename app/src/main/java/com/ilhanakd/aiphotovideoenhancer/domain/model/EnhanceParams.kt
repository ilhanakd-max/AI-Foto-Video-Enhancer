package com.ilhanakd.aiphotovideoenhancer.domain.model

data class EnhanceParams(
    val sharpness: Int = 50,
    val denoise: Int = 20,
    val brightness: Float = 0f,
    val contrast: Float = 1f
)
