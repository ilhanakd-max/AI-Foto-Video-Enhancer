package com.ilhanakd.aiphotovideoenhancer.domain.model

enum class EnhanceProfile(val sharpness: Int, val denoise: Int, val brightness: Float, val contrast: Float) {
    SOFT_CLEAN(sharpness = 30, denoise = 40, brightness = 0.1f, contrast = 1.05f),
    STRONG_SHARP(sharpness = 70, denoise = 15, brightness = 0f, contrast = 1.1f),
    NIGHT_BOOST(sharpness = 50, denoise = 50, brightness = 0.2f, contrast = 1.2f)
}
