package com.ssafy.mobile.core.permission

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 카메라 및 마이크 권한 요청을 통합 관리하는 핸들러.
 * Activity에서 초기화하여 사용합니다.
 */
class PermissionHandler(
    private val activity: ComponentActivity,
) {
    private val requiredPermissions =
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        )

    private val _permissionState =
        MutableStateFlow<PermissionRequestState>(PermissionRequestState.Idle)
    val permissionState: StateFlow<PermissionRequestState> = _permissionState.asStateFlow()

    /** 권한 요청 런처 등록 */
    private val requestPermissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                _permissionState.value = PermissionRequestState.Granted
            } else {
                // 권한 거부 시, '다시 묻지 않음' 상태인지 확인
                val shouldShowRationale =
                    requiredPermissions.any {
                        activity.shouldShowRequestPermissionRationale(it)
                    }
                _permissionState.value =
                    if (shouldShowRationale) {
                        PermissionRequestState.Denied
                    } else {
                        PermissionRequestState.PermanentlyDenied
                    }
            }
        }

    /** 현재 권한 상태 체크 및 요청 */
    fun checkAndRequestPermissions() {
        val allGranted =
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
            }

        if (allGranted) {
            _permissionState.value = PermissionRequestState.Granted
        } else {
            _permissionState.value = PermissionRequestState.ShouldRequest
            requestPermissionLauncher.launch(requiredPermissions)
        }
    }

    /** 설정창으로 이동 (영구 거부 시 사용) */
    fun openAppSettings() {
        val intent =
            android.content.Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.fromParts("package", activity.packageName, null),
            )
        activity.startActivity(intent)
    }
}
