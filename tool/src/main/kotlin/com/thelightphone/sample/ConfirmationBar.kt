package com.thelightphone.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.lightClickable

/** Bottom confirmation bar matching LightOS's native style: CLOSE icon to cancel, "CONFIRM" to confirm. */
@Composable
fun ConfirmationBar(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LightIcon(
            icon = LightIcons.CLOSE,
            modifier = Modifier.lightClickable { onCancel() },
        )
        LightText(
            text = "CONFIRM",
            variant = LightTextVariant.Copy,
            modifier = Modifier.lightClickable { onConfirm() },
        )
    }
}