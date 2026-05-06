package com.ssafy.mobile.feature.mypage.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.mobile.core.auth.AuthSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface MyPageLogoutState {
    data object Idle : MyPageLogoutState

    data object Loading : MyPageLogoutState

    data object Success : MyPageLogoutState

    data class Error(
        val message: String,
    ) : MyPageLogoutState
}

@HiltViewModel
class MyPageViewModel
    @Inject
    constructor(
        private val authSessionManager: AuthSessionManager,
    ) : ViewModel() {
        private val _logoutState = MutableStateFlow<MyPageLogoutState>(MyPageLogoutState.Idle)
        val logoutState: StateFlow<MyPageLogoutState> = _logoutState.asStateFlow()

        fun logout() {
            if (_logoutState.value is MyPageLogoutState.Loading) return

            _logoutState.value = MyPageLogoutState.Loading

            viewModelScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        authSessionManager.clearSession()
                    }
                    _logoutState.value = MyPageLogoutState.Success
                } catch (e: CancellationException) {
                    throw e
                } catch (
                    @Suppress("TooGenericExceptionCaught")
                    e: Exception,
                ) {
                    // 세션 초기화 실패 시 보안을 위해 로그를 남기고 사용자에게 안내
                    Log.e("MyPageViewModel", "Logout failed", e)
                    _logoutState.value = MyPageLogoutState.Error("로그아웃하지 못했습니다. 다시 시도해 주세요.")
                }
            }
        }

        fun resetLogoutState() {
            _logoutState.value = MyPageLogoutState.Idle
        }
    }
