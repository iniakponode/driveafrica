package com.uoa.driverprofile.presentation.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.uoa.core.model.DrivingTip
import java.util.UUID

@Composable
fun TipList(
    tips: List<DrivingTip>,
    source: String,
    onDrivingTipClick: (UUID) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tips.forEach { tip ->
            TipCard(
                tip = tip,
                source = source,
                onClick = { onDrivingTipClick(tip.tipId) }
            )
        }
    }
}