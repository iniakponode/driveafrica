package com.uoa.driveafrica.presentation.daappnavigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun RowScope.DaAppNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier=Modifier,
    enabled: Boolean,
    alwaysShowLabel: Boolean=true,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit)?= null,
    selectedIcon: @Composable () -> Unit= icon,
){
    NavigationBarItem(
        selected=selected,
        onClick=onClick,
        modifier=modifier,
        enabled=enabled,
        alwaysShowLabel=alwaysShowLabel,
        icon= if(selected) selectedIcon else icon,
        label=label,
        colors=NavigationBarItemDefaults.colors(
            selectedIconColor = DaAppBarItemDefaults.navigationSelectedItemColor(),
            unselectedIconColor = DaAppBarItemDefaults.navigationContentColors(),
            selectedTextColor = DaAppBarItemDefaults.navigationSelectedItemColor(),
            unselectedTextColor = DaAppBarItemDefaults.navigationContentColors(),
            indicatorColor = DaAppBarItemDefaults.navigationIndicatorColor(),
        )
    )

}

object DaAppBarItemDefaults{
    @Composable
    fun navigationContentColors()=MaterialTheme.colorScheme.onSurfaceVariant

    @Composable
    fun navigationSelectedItemColor()=MaterialTheme.colorScheme.onPrimary

    @Composable
    fun navigationIndicatorColor()=MaterialTheme.colorScheme.primaryContainer
}