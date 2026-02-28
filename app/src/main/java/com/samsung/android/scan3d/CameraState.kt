package com.samsung.android.scan3d

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents the complete state of the camera view and settings.
 * This class is Parcelable so it can be passed between the activity and the service via Intents.
 */
@Parcelize
data class ViewState(
    val preview: Boolean,           // Show local preview on the phone
    val stream: Boolean,            // Enable network streaming (HTTP)
    val cameraId: String,           // Selected sensor ID (e.g. "0", "1")
    val resolutionIndex: Int?,      // Index of chosen resolution in available sizes
    val quality: Int,               // JPEG compression quality (for MJPEG mode only)
    val flash: Boolean,             // Torch state (enabled/disabled)
    val flashLevel: Int = -1,       // Torch intensity (for compatible devices)

    // --- New settings for video stream ---
    val streamFormat: Int = 0,      // Stream format: 0 for MJPEG, 1 for H.264
    val h264Bitrate: Int = 5,       // Bitrate in Mbps (typically between 1 and 20)
    val h264Mode: Int = 0           // Bitrate mode: 0 for CBR (Constant), 1 for VBR (Variable)
) : Parcelable