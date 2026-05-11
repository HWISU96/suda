package com.ssafy.mobile

import androidx.lifecycle.ViewModel
import com.ssafy.mobile.core.auth.AuthSessionManager
import com.ssafy.mobile.core.auth.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val authSessionManager: AuthSessionManager,
    ) : ViewModel() {
        val authState: StateFlow<AuthState> = authSessionManager.authState
    }
