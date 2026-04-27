package com.ssafy.mobile.core.permission

/**
 * 권한 요청 흐름의 상태를 나타내는 클래스.
 * B 파트가 권한 요청 분기 처리 시 ViewModel State로 사용합니다. (COMM_002)
 */
sealed interface PermissionRequestState {
    /** 권한 상태를 아직 확인하지 않은 초기 상태 */
    data object Idle : PermissionRequestState

    /** 권한 요청 다이얼로그를 표시해야 하는 상태 */
    data object ShouldRequest : PermissionRequestState

    /** 모든 필요 권한이 허용된 상태 */
    data object Granted : PermissionRequestState

    /** 권한이 거부되어 설정창 이동 안내를 표시해야 하는 상태 */
    data object Denied : PermissionRequestState

    /** 권한이 영구 거부(다시 묻지 않음)되어 설정창으로만 해결 가능한 상태 */
    data object PermanentlyDenied : PermissionRequestState
}
