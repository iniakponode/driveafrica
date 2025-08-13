package com.uoa.safedriveafrica

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.uoa.safedriveafrica.presentation.daappnavigation.DAAppNavHost
import com.uoa.safedriveafrica.presentation.daappnavigation.TopLevelDestinations
import com.uoa.safedriveafrica.presentation.daappnavigation.DaAppNavigationBarItem

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DAApp(appState: DAAppState) {
    val snackbarHostState= remember { SnackbarHostState() }

    val isOffline by appState.isOffline.collectAsStateWithLifecycle()

//    If Users internet is not connected
    val conMessage= "No internet connection on your phone"

    LaunchedEffect(isOffline) {
        if (isOffline) {
            val result = snackbarHostState.showSnackbar(
                message = "No internet connection",
                duration = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.Dismissed) {
                // Additional action on dismiss if needed
                return@LaunchedEffect
            }
        } else {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = "You are back online",
                duration = SnackbarDuration.Short
            )
        }
    }


    Scaffold(
        topBar = { DATopBar(appState) },
        bottomBar = { DABottomBar(appState.topLevelDestinations,
            appState::navigateToTopLevelDestination,
            appState.currentDestination
            ) },
        content = { padding ->
            DAContent(padding, appState, snackbarHostState = snackbarHostState)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DATopBar(appState: DAAppState) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = com.uoa.core.R.drawable.sda_2),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(id = com.uoa.dbda.R.string.app_name))
            }
        },
        navigationIcon = {
            if (appState.canNavigateBack()) {
                IconButton(onClick = { appState.navController.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Back")
                }
            }
        }
    )
}

@Composable
fun DABottomBar(destinations:List<TopLevelDestinations>,
                onNavigateToDestination: (TopLevelDestinations) -> Unit,
                currentDestination: NavDestination?
) {
    NavigationBar {
        destinations.forEach{ destination->
            val selected=currentDestination.isTopLevelDestinationInHierarchy(destination)
            DaAppNavigationBarItem(
                selected=selected,
                onClick={onNavigateToDestination(destination)},

                icon={
                    Icon(
                        painterResource(destination.unselectedIconResId),
                        contentDescription=null
                    )
                },
                selectedIcon={
                    Icon(
                        painterResource(destination.selectedIcon),
                        contentDescription=null
                    )
                },

                label={
                    Text(
                        text=stringResource(id=destination.titleTextId)
                    )
                },
                enabled=true,
            )
        }
    }

}



@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun DAContent(padding: PaddingValues, appState: DAAppState, snackbarHostState: SnackbarHostState) {
    Box(modifier = Modifier.padding(padding)) {
        DAAppNavHost(appState = appState, onShowSnackbar = { message, action ->
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = action,
                duration = SnackbarDuration.Short,
            ) == SnackbarResult.ActionPerformed
        }, modifier = Modifier.fillMaxSize())
    }
}

private fun NavDestination?.isTopLevelDestinationInHierarchy(
    topLevelDestinations: TopLevelDestinations
) =
    this?.hierarchy?.any {
        it.route?.startsWith(topLevelDestinations.route) == true
    } ?: false

