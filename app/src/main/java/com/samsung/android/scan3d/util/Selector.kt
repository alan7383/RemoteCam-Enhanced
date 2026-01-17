package com.samsung.android.scan3d.util

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Parcelable
import android.util.Log
import kotlinx.parcelize.Parcelize
import java.lang.Exception
import kotlin.math.atan
import kotlin.math.roundToInt

object Selector {
    @Parcelize
    data class SensorDesc(val title: String, val cameraId: String, val format: Int) : Parcelable

    private fun lensOrientationString(value: Int) = when (value) {
        CameraCharacteristics.LENS_FACING_BACK -> "Back"
        CameraCharacteristics.LENS_FACING_FRONT -> "Front"
        CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
        else -> "Unknown"
    }

    @SuppressLint("InlinedApi")
    fun enumerateCameras(cameraManager: CameraManager): List<SensorDesc> {
        val availableCameras: MutableList<SensorDesc> = mutableListOf()

        val cameraIds = try {
            cameraManager.cameraIdList.toList()
        } catch (e: Exception) {
            Log.e("SELECTOR", "Fatal error getting camera ID list", e)
            return emptyList()
        }

        // On parcourt TOUTES les caméras que le système nous donne, sans aucun filtre préalable.
        for (id in cameraIds) {
            try {
                val characteristics = cameraManager.getCameraCharacteristics(id)

                val orientation = lensOrientationString(
                    characteristics.get(CameraCharacteristics.LENS_FACING)!!
                )

                val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                val apertures = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
                val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)

                val title: String

                // On essaie de construire le nom technique. Si des infos manquent, on met un nom de secours.
                if (focalLengths != null && focalLengths.isNotEmpty() &&
                    apertures != null && apertures.isNotEmpty() &&
                    sensorSize != null) {

                    val foaclmm = focalLengths[0]
                    val foc = ("${foaclmm}mm").padEnd(6, ' ')
                    val ape = ("f${apertures[0]}").padEnd(4, ' ')
                    val vfov = ("${(2.0 * (180.0 / Math.PI) * atan(sensorSize.height / (2.0 * foaclmm))).roundToInt()}°").padEnd(4, ' ')

                    title = "vfov:$vfov $foc $ape $orientation"
                } else {
                    title = "Camera ID: $id ($orientation)"
                }

                // On ajoute la caméra si on n'a pas déjà une autre avec exactement le même titre.
                if (!availableCameras.any { it.title == title }) {
                    availableCameras.add(
                        SensorDesc(title, id, ImageFormat.JPEG)
                    )
                }

            } catch (e: Exception) {
                // Si on n'arrive pas à lire les infos d'une caméra, on l'ignore silencieusement.
                // C'est souvent une caméra système ou virtuelle non destinée aux apps tierces.
                Log.w("SELECTOR", "Could not process camera $id, skipping. Error: ${e.message}")
            }
        }

        // On trie la liste finale par ID de caméra pour un ordre cohérent.
        return availableCameras.sortedBy { it.cameraId.toIntOrNull() ?: Int.MAX_VALUE }
    }
}