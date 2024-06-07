package com.uoa.driveafrica.ui.components.nav

// scaffold for the home screen with bottom navigation bar and top app bar
// import TopHomeScreenBar and BottomHomeScreenBar
import com.uoa.sensor.presentation.viewModel.TripViewModel
import android.os.Build
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.uoa.driveafrica.ui.components.TopNavBar
import com.uoa.driveafrica.ui.components.screens.DrivingTips
import com.uoa.driveafrica.ui.components.screens.Home
import com.uoa.driveafrica.ui.components.screens.Reports
import com.uoa.sensor.presentation.ui.SensorControlScreen
//import com.uoa.sensor.presentation.ui.SensorDataCollectionScreen
import com.uoa.sensor.presentation.viewModel.SensorViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation() {
        val navController: NavHostController = rememberNavController()
        val sensorViewModel: SensorViewModel = viewModel()
        val tripViewModel: TripViewModel = viewModel()
        Scaffold(
            topBar = {
                     TopNavBar {
                            navController.popBackStack()
                     }

            },
            bottomBar = {
                NavigationBar{
                    val navBackStackEntry = navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry.value?.destination?.route

                    navItems.forEach { navItem ->
                       NavigationBarItem(
                           selected =currentRoute?.let { navItem.route == it } == true,
                           onClick = { navController.navigate(navItem.route){
                                 popUpTo(navController.graph.findStartDestination().id)
                                 launchSingleTop = true
                                    restoreState = true
                           } },
                           label = { Text(text = navItem.title) },
                           icon = { Icon(imageVector = navItem.icon, contentDescription =navItem.title )})
                    }
                }

            }
        ) { paddingValues ->
            NavHost(navController = navController, startDestination = ScreenType.HOME.name, modifier = Modifier.padding(paddingValues)) {
                composable(ScreenType.HOME.name) {
                    Column(modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)) {
                        Home()
                    }
                }
                composable(ScreenType.REPORTS.name) {
                    Column(modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)) {
                        Reports()
                    }
                }
                composable(ScreenType.DRIVING_TIPS.name) {
                    Column(modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)) {
                        DrivingTips()
                    }
                }
                composable(ScreenType.Record_Trip.name) {
                    Column(modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)) {
                        SensorControlScreen(sensorViewModel, tripViewModel)
                    }
                }
            }
        }
    }