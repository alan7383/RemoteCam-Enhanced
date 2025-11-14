// app/src/main/java/com/samsung/android/scan3d/CameraState.kt

package com.samsung.android.scan3d

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ViewState(
    var preview: Boolean,
    var stream: Boolean,
    var cameraId: String,
    var resolutionIndex: Int?,
    var quality: Int,
    var flash: Boolean
) : Parcelable