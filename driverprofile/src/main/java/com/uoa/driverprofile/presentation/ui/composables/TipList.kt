package com.uoa.driverprofile.presentation.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tips) { tip ->
            TipCard(
                tip = tip,
                source = source,
                onClick = { onDrivingTipClick(tip.tipId) }
            )
        }
    }
}