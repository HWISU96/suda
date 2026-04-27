package com.ssafy.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.ssafy.mobile.core.navigation.MobileNavHost
import com.ssafy.mobile.core.permission.PermissionGuide
import com.ssafy.mobile.core.permission.PermissionHandler
import com.ssafy.mobile.core.permission.PermissionRequestState
import com.ssafy.mobile.core.ui.theme.MobileTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var permissionHandler: PermissionHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionHandler = PermissionHandler(this)
        // 앱 시작 시 권한 체크 및 요청
        permissionHandler.checkAndRequestPermissions()

        enableEdgeToEdge()
        setContent {
            val lifecycleOwner = LocalLifecycleOwner.current

            // 사용자가 설정 화면에서 돌아왔을 때를 대비하여 ON_RESUME 시점에 권한 재확인
            DisposableEffect(lifecycleOwner) {
                val observer =
                    LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            permissionHandler.checkAndRequestPermissions()
                        }
                    }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            MobileTheme {
                val permissionState by permissionHandler.permissionState
                    .collectAsStateWithLifecycle()

                Box(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    MobileNavHost(navController = navController)

                    // 권한이 거부되었거나 영구 거부된 경우 안내 화면 표시
                    when (permissionState) {
                        is PermissionRequestState.Denied -> {
                            PermissionGuide(
                                title = "필수 권한 안내",
                                description = "원활한 서비스 이용을 위해 카메라와 마이크 권한이 필요합니다.",
                                onOpenSettings = { permissionHandler.checkAndRequestPermissions() },
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                        is PermissionRequestState.PermanentlyDenied -> {
                            PermissionGuide(
                                title = "권한 설정 필요",
                                description = "카메라 및 마이크 권한이 영구적으로 거부되었습니다. 설정에서 직접 권한을 허용해주세요.",
                                onOpenSettings = { permissionHandler.openAppSettings() },
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                        else -> { /* Idle, ShouldRequest, Granted 상태에서는 아무것도 표시하지 않음 */ }
                    }
                }
            }
        }
    }
}
