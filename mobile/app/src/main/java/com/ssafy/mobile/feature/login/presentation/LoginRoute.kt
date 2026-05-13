package com.ssafy.mobile.feature.login.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.navercorp.nid.NaverIdLoginSDK
import com.ssafy.mobile.core.ui.components.AppCard
import com.ssafy.mobile.core.ui.components.AppPrimaryButton
import com.ssafy.mobile.core.ui.components.AppSecondaryButton
import com.ssafy.mobile.core.ui.components.AppTextField
import com.ssafy.mobile.core.ui.feedback.AppErrorText

@Composable
fun LoginRoute(
    onNavigateToHome: () -> Unit,
    onNavigateToChildSelect: () -> Unit,
    onNavigateToSignup: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val emailError by viewModel.emailError.collectAsStateWithLifecycle()
    val passwordError by viewModel.passwordError.collectAsStateWithLifecycle()
    val naverLoginLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val token = NaverIdLoginSDK.getAccessToken()
            if (token.isNullOrBlank()) {
                val errorCode = NaverIdLoginSDK.getLastErrorCode().code
                if (errorCode != NAVER_USER_CANCEL_CODE) {
                    val errorDescription = NaverIdLoginSDK.getLastErrorDescription().orEmpty()
                    viewModel.onNaverLoginError(
                        errorDescription.ifBlank {
                            "네이버 로그인에 실패했습니다."
                        },
                    )
                }
            } else {
                viewModel.loginWithNaverToken(token)
            }
        }

    LaunchedEffect(Unit) {
        viewModel.initializeNaverSdk(context)
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is LoginUiState.Success -> {
                if (state.hasActiveChild) {
                    onNavigateToHome()
                } else {
                    onNavigateToChildSelect()
                }
            }
            else -> Unit
        }
    }

    LoginScreen(
        email = email,
        password = password,
        emailError = emailError,
        passwordError = passwordError,
        uiState = uiState,
        isNaverLoginEnabled = viewModel.isNaverConfigValid(),
        onEmailChanged = viewModel::onEmailChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onLoginClick = viewModel::login,
        onSignupClick = onNavigateToSignup,
        onNaverLoginClick = {
            if (viewModel.isNaverConfigValid()) {
                NaverIdLoginSDK.authenticate(context, naverLoginLauncher)
            } else {
                viewModel.onNaverLoginError("네이버 로그인 설정을 확인해 주세요.")
            }
        },
        modifier = modifier,
    )
}

@Composable
@Suppress("LongParameterList")
private fun LoginScreen(
    email: String,
    password: String,
    emailError: String?,
    passwordError: String?,
    uiState: LoginUiState,
    isNaverLoginEnabled: Boolean,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClick: () -> Unit,
    onSignupClick: () -> Unit,
    onNaverLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLoading = uiState is LoginUiState.Loading

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
                    .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            LoginHeader()

            Spacer(modifier = Modifier.height(28.dp))

            AppCard(modifier = Modifier.fillMaxWidth()) {
                LoginInputFields(
                    email = email,
                    password = password,
                    emailError = emailError,
                    passwordError = passwordError,
                    isLoading = isLoading,
                    onEmailChanged = onEmailChanged,
                    onPasswordChanged = onPasswordChanged,
                    onLoginClick = onLoginClick,
                )

                AppErrorText(
                    message = (uiState as? LoginUiState.Error)?.message.orEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                LoginActionButtons(
                    isLoading = isLoading,
                    isNaverLoginEnabled = isNaverLoginEnabled,
                    onLoginClick = onLoginClick,
                    onSignupClick = onSignupClick,
                    onNaverLoginClick = onNaverLoginClick,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "보호자 계정으로 아이의 학습 기록과 프로필을 관리할 수 있어요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LoginHeader() {
    Text(
        text = "SUDA",
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Black,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "보호자 로그인",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "아이의 오늘 학습을 확인하고 이어갈 준비를 해요.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
@Suppress("LongParameterList")
private fun LoginInputFields(
    email: String,
    password: String,
    emailError: String?,
    passwordError: String?,
    isLoading: Boolean,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClick: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    AppTextField(
        value = email,
        onValueChange = onEmailChanged,
        label = "이메일",
        placeholder = "example@email.com",
        isError = emailError != null,
        supportingText = emailError,
        enabled = !isLoading,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
        keyboardActions =
            KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
        modifier = Modifier.fillMaxWidth(),
    )

    AppTextField(
        value = password,
        onValueChange = onPasswordChanged,
        label = "비밀번호",
        isError = passwordError != null,
        supportingText = passwordError,
        enabled = !isLoading,
        visualTransformation =
            if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
        keyboardActions =
            KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onLoginClick()
                },
            ),
        trailingIcon = {
            TextButton(onClick = { passwordVisible = !passwordVisible }) {
                Text(if (passwordVisible) "숨기기" else "보기")
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun LoginActionButtons(
    isLoading: Boolean,
    isNaverLoginEnabled: Boolean,
    onLoginClick: () -> Unit,
    onSignupClick: () -> Unit,
    onNaverLoginClick: () -> Unit,
) {
    AppPrimaryButton(
        text = "로그인",
        onClick = onLoginClick,
        enabled = !isLoading,
        loading = isLoading,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(8.dp))

    AppSecondaryButton(
        text = "회원가입",
        onClick = onSignupClick,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(16.dp))

    NaverLoginButton(
        onClick = onNaverLoginClick,
        enabled = !isLoading && isNaverLoginEnabled,
    )
}

@Composable
private fun NaverLoginButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        shape = MaterialTheme.shapes.small,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = naverGreen,
                contentColor = Color.White,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
    ) {
        Text(
            text = "네이버로 로그인",
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private const val NAVER_USER_CANCEL_CODE = "user_cancel"

@Suppress("MagicNumber")
private val naverGreen = Color(0xFF03C75A)
