package com.goalnudge.app.domain.guard

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lacak apakah ada kamera yang sedang dipakai (oleh app manapun), supaya overlay tidak
 * muncul menimpa layar kamera — PLAN.md §4 "Deteksi konteks".
 */
@Singleton
class CameraUsageObserver @Inject constructor(@ApplicationContext context: Context) {

    private val unavailableCameraIds = mutableSetOf<String>()
    private val _isCameraInUse = MutableStateFlow(false)
    val isCameraInUse: StateFlow<Boolean> = _isCameraInUse

    init {
        runCatching {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            manager.registerAvailabilityCallback(
                object : CameraManager.AvailabilityCallback() {
                    override fun onCameraAvailable(cameraId: String) {
                        unavailableCameraIds.remove(cameraId)
                        _isCameraInUse.value = unavailableCameraIds.isNotEmpty()
                    }

                    override fun onCameraUnavailable(cameraId: String) {
                        unavailableCameraIds.add(cameraId)
                        _isCameraInUse.value = true
                    }
                },
                Handler(Looper.getMainLooper())
            )
        }
    }
}
