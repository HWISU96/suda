package com.ssafy.mobile.core.ui.feedback

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

/**
 * 에러 상태를 텍스트로만 표시하는 순수 피드백 컴포넌트.
 * 버튼 등 인터랙션이 필요하면 상위 화면에서 별도 구성합니다.
 */
@Composable
fun AppErrorText(
    message: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}
