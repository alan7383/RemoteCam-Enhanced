package com.samsung.android.scan3d

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ViewState(
    val preview: Boolean,
    val stream: Boolean,
    val cameraId: String,
    val resolutionIndex: Int?,
    val quality: Int,
    val flash: Boolean,
    val flashLevel: Int = -1
) : Parcelable