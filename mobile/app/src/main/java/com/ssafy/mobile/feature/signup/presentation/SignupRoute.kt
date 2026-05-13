package com.ssafy.mobile.feature.signup.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.ssafy.mobile.core.ui.components.AppCard
import com.ssafy.mobile.core.ui.components.AppPrimaryButton
import com.ssafy.mobile.core.ui.components.AppSecondaryButton
import com.ssafy.mobile.core.ui.components.AppTextField
import com.ssafy.mobile.core.ui.feedback.AppErrorText

@Composable
fun SignupRoute(
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val confirmPassword by viewModel.confirmPassword.collectAsStateWithLifecycle()
    val name by viewModel.name.collectAsStateWithLifecycle()
    val emailError by viewModel.emailError.collectAsStateWithLifecycle()
    val passwordError by viewModel.passwordError.collectAsStateWithLifecycle()
    val confirmPasswordError by viewModel.confirmPasswordError.collectAsStateWithLifecycle()
    val nameError by viewModel.nameError.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is SignupUiState.Success) {
            onNavigateToLogin()
        }
    }

    SignupScreen(
        email = email,
        password = password,
        confirmPassword = confirmPassword,
        name = name,
        emailError = emailError,
        passwordError = passwordError,
        confirmPasswordError = confirmPasswordError,
        nameError = nameError,
        uiState = uiState,
        onEmailChanged = viewModel::onEmailChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onConfirmPasswordChanged = viewModel::onConfirmPasswordChanged,
        onNameChanged = viewModel::onNameChanged,
        onSignupClick = viewModel::signup,
        onNavigateToLogin = onNavigateToLogin,
        modifier = modifier,
    )
}

@Composable
@Suppress("LongParameterList")
private fun SignupScreen(
    email: String,
    password: String,
    confirmPassword: String,
    name: String,
    emailError: String?,
    passwordError: String?,
    confirmPasswordError: String?,
    nameError: String?,
    uiState: SignupUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onNameChanged: (String) -> Unit,
    onSignupClick: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLoading = uiState is SignupUiState.Loading

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
            SignupHeader()

            Spacer(modifier = Modifier.height(28.dp))

            AppCard(modifier = Modifier.fillMaxWidth()) {
                SignupInputFields(
                    email = email,
                    password = password,
                    confirmPassword = confirmPassword,
                    name = name,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError,
                    nameError = nameError,
                    isLoading = isLoading,
                    onEmailChanged = onEmailChanged,
                    onPasswordChanged = onPasswordChanged,
                    onConfirmPasswordChanged = onConfirmPasswordChanged,
                    onNameChanged = onNameChanged,
                    onSignupClick = onSignupClick,
                )

                AppErrorText(
                    message = (uiState as? SignupUiState.Error)?.message.orEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                SignupActionButtons(
                    isLoading = isLoading,
                    onSignupClick = onSignupClick,
                    onNavigateToLogin = onNavigateToLogin,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "가입 후 로그인하면 아이 프로필을 만들고 학습을 시작할 수 있어요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SignupHeader() {
    Text(
        text = "SUDA",
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Black,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "보호자 회원가입",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "보호자 정보를 등록하고 아이의 학습 여정을 준비해요.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
@Suppress("LongParameterList", "LongMethod")
private fun SignupInputFields(
    email: String,
    password: String,
    confirmPassword: String,
    name: String,
    emailError: String?,
    passwordError: String?,
    confirmPasswordError: String?,
    nameError: String?,
    isLoading: Boolean,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onNameChanged: (String) -> Unit,
    onSignupClick: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

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
        value = name,
        onValueChange = onNameChanged,
        label = "보호자 이름",
        placeholder = "이름을 입력해 주세요",
        isError = nameError != null,
        supportingText = nameError,
        enabled = !isLoading,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Text,
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
                imeAction = ImeAction.Next,
            ),
        keyboardActions =
            KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
        trailingIcon = {
            TextButton(onClick = { passwordVisible = !passwordVisible }) {
                Text(if (passwordVisible) "숨기기" else "보기")
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )

    AppTextField(
        value = confirmPassword,
        onValueChange = onConfirmPasswordChanged,
        label = "비밀번호 확인",
        isError = confirmPasswordError != null,
        supportingText = confirmPasswordError,
        enabled = !isLoading,
        visualTransformation =
            if (confirmPasswordVisible) {
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
                    onSignupClick()
                },
            ),
        trailingIcon = {
            TextButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                Text(if (confirmPasswordVisible) "숨기기" else "보기")
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SignupActionButtons(
    isLoading: Boolean,
    onSignupClick: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    AppPrimaryButton(
        text = "회원가입",
        onClick = onSignupClick,
        enabled = !isLoading,
        loading = isLoading,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(8.dp))

    AppSecondaryButton(
        text = "로그인으로 돌아가기",
        onClick = onNavigateToLogin,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth(),
    )
}
