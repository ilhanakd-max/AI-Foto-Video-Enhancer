package com.ilhanakd.aiphotovideoenhancer.ml

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import com.ilhanakd.aiphotovideoenhancer.data.processing.ImageProcessor
import com.ilhanakd.aiphotovideoenhancer.domain.model.EnhanceProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.max

class VideoEnhancerModelRunner(private val context: Context) {

    suspend fun enhanceVideo(uri: Uri, profile: EnhanceProfile, onProgress: (Int, Int?) -> Unit): String =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val retriever = MediaMetadataRetriever()
            val inputDescriptor = resolver.openFileDescriptor(uri, "r")
                ?: throw IllegalArgumentException("Invalid uri")
            retriever.setDataSource(inputDescriptor.fileDescriptor)
            val durationMs = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                ?: 0L)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1280
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 720

            val targetWidth: Int
            val targetHeight: Int
            if (max(width, height) > 1280) {
                val scale = 1280f / max(width, height)
                targetWidth = (width * scale).toInt()
                targetHeight = (height * scale).toInt()
            } else {
                targetWidth = width
                targetHeight = height
            }

            val outputDir = context.getExternalFilesDir(null) ?: context.filesDir
            val outputFile = File(outputDir, "enhanced_${System.currentTimeMillis()}.mp4")

            val extractor = MediaExtractor()
            extractor.setDataSource(inputDescriptor.fileDescriptor)
            val audioTrackIndexInput = selectTrack(extractor, isAudio = true)
            val audioFormat = if (audioTrackIndexInput >= 0) extractor.getTrackFormat(audioTrackIndexInput) else null

            val encoderFormat = MediaFormat.createVideoFormat("video/avc", targetWidth, targetHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE, targetWidth * targetHeight * 4)
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            val encoder = MediaCodec.createEncoderByType("video/avc")
            encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
            val bufferInfo = MediaCodec.BufferInfo()

            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var muxerStarted = false
            var videoTrackIndex = -1
            var audioTrackIndex = -1
            audioFormat?.let { format ->
                audioTrackIndex = muxer.addTrack(format)
            }

            val totalFrames = if (durationMs > 0) (durationMs / FRAME_INTERVAL_MS).toInt().coerceAtLeast(1) else 1
            var processedFrames = 0

            for (frameIndex in 0..totalFrames) {
                val timeUs = frameIndex * FRAME_INTERVAL_MS * 1000
                val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST) ?: continue
                val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
                val enhanced = ImageProcessor.applyEnhancements(
                    scaled,
                    profile.sharpness,
                    profile.denoise,
                    profile.brightness,
                    profile.contrast
                )
                val yuvData = convertBitmapToYUV(enhanced)
                queueFrame(encoder, bufferInfo, muxer, yuvData, timeUs, { format ->
                    if (videoTrackIndex == -1) {
                        videoTrackIndex = muxer.addTrack(format)
                    }
                    if (!muxerStarted && videoTrackIndex >= 0) {
                        muxer.start()
                        muxerStarted = true
                    }
                }) { trackIndex, info, buffer ->
                    val targetTrack = if (trackIndex == -1) videoTrackIndex else trackIndex
                    if (muxerStarted && targetTrack >= 0 && buffer != null && info.size > 0) {
                        muxer.writeSampleData(targetTrack, buffer, info)
                    }
                }
                bitmap.recycle()
                scaled.recycle()
                enhanced.recycle()
                processedFrames++
                val progress = (processedFrames * 100 / totalFrames).coerceIn(0, 100)
                val remaining = ((totalFrames - processedFrames) * FRAME_INTERVAL_MS / 1000).toInt()
                onProgress(progress, remaining)
            }

            signalEndOfStream(encoder)
            drainEncoder(encoder, bufferInfo, muxer, videoTrackIndex) { trackIndex, info, buffer ->
                if (muxerStarted && trackIndex >= 0 && buffer != null && info.size > 0) {
                    muxer.writeSampleData(trackIndex, buffer, info)
                }
            }

            if (audioTrackIndex >= 0 && muxerStarted) {
                copyAudio(extractor, audioTrackIndexInput, muxer, audioTrackIndex)
            }

            encoder.stop()
            encoder.release()
            muxer.stop()
            muxer.release()
            retriever.release()
            extractor.release()
            inputDescriptor.close()
            return@withContext outputFile.absolutePath
        }

    private fun selectTrack(extractor: MediaExtractor, isAudio: Boolean): Int {
        for (index in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(index)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (isAudio && mime.startsWith("audio")) return index
            if (!isAudio && mime.startsWith("video")) return index
        }
        return -1
    }

    private fun queueFrame(
        encoder: MediaCodec,
        bufferInfo: MediaCodec.BufferInfo,
        muxer: MediaMuxer,
        data: ByteArray,
        presentationTimeUs: Long,
        onFormat: (MediaFormat) -> Unit,
        writeSample: (Int, MediaCodec.BufferInfo, ByteBuffer?) -> Unit
    ) {
        val inputIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
        if (inputIndex >= 0) {
            val inputBuffer = encoder.getInputBuffer(inputIndex)
            inputBuffer?.clear()
            inputBuffer?.put(data)
            encoder.queueInputBuffer(inputIndex, 0, data.size, presentationTimeUs, 0)
        }
        drainEncoder(encoder, bufferInfo, muxer, -1, onFormat = onFormat, writeSample = writeSample)
    }

    private fun drainEncoder(
        encoder: MediaCodec,
        bufferInfo: MediaCodec.BufferInfo,
        muxer: MediaMuxer,
        videoTrack: Int,
        onFormat: (MediaFormat) -> Unit = {},
        writeSample: (Int, MediaCodec.BufferInfo, ByteBuffer?) -> Unit = { _, _, _ -> }
    ) {
        loop@ while (true) {
            val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            when {
                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> break@loop
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val format = encoder.outputFormat
                    onFormat(format)
                }
                outputIndex >= 0 -> {
                    val outputBuffer = encoder.getOutputBuffer(outputIndex)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size != 0) {
                        writeSample(videoTrack, bufferInfo, outputBuffer)
                    }
                    encoder.releaseOutputBuffer(outputIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        break@loop
                    }
                }
            }
        }
    }

    private fun signalEndOfStream(encoder: MediaCodec) {
        val inputIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
        if (inputIndex >= 0) {
            encoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        }
    }

    private fun convertBitmapToYUV(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val frameSize = width * height
        val yuv = ByteArray(frameSize * 3 / 2)
        var yIndex = 0
        var uvIndex = frameSize
        val pixels = IntArray(frameSize)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        for (j in 0 until height) {
            for (i in 0 until width) {
                val color = pixels[j * width + i]
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF
                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                yuv[yIndex++] = y.toByte()
                if (j % 2 == 0 && i % 2 == 0 && uvIndex + 1 < yuv.size) {
                    yuv[uvIndex++] = u.toByte()
                    yuv[uvIndex++] = v.toByte()
                }
            }
        }
        return yuv
    }

    private fun copyAudio(extractor: MediaExtractor, audioTrackInput: Int, muxer: MediaMuxer, audioTrackOut: Int) {
        extractor.selectTrack(audioTrackInput)
        val bufferSize = 1024 * 1024
        val buffer = ByteBuffer.allocate(bufferSize)
        val info = MediaCodec.BufferInfo()
        while (true) {
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) break
            info.offset = 0
            info.size = sampleSize
            info.presentationTimeUs = extractor.sampleTime
            info.flags = extractor.sampleFlags
            muxer.writeSampleData(audioTrackOut, buffer, info)
            extractor.advance()
        }
    }

    companion object {
        private const val FRAME_INTERVAL_MS = 33L
        private const val TIMEOUT_US = 10_000L
        private const val TAG = "VideoEnhancerModelRunner"
    }
}
