package com.uoa.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController

//@Composable
//fun AppBaseUI() {
//    // set up the base UI for the app with the app bar and the navigation drawer
//    // to be used in the main activity
//    val tabs = remember { BottomTabs.values() }
//    val navController = rememberNavController()
//    ProvideWindowInsets {
//        val scaffoldState = rememberScaffoldState()
//        Scaffold(
//            scaffoldState = scaffoldState,
//            topBar = {
//                AppTopBar()
//            },
//            drawerContent = {
//                AppDrawer()
//            },
//            content = {
//                AppNavHost()
//            }
//        )
//    }
//
//}
