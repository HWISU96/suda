package com.ssafy.mobile.core.ui.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 네트워크 오프라인 상태를 화면 상단에 알리는 배너 피드백 컴포넌트. (COMM_004)
 */
@Composable
fun AppOfflineBanner(modifier: Modifier = Modifier) {
    Text(
        text = "네트워크 연결이 불안정합니다.",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onError,
        textAlign = TextAlign.Center,
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.error)
                .padding(vertical = 6.dp),
    )
}
