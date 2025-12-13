# 🔌 QUICK INTEGRATION - Vehicle Detection Monitor Screen

## Add to Your App in 3 Steps

### Step 1: Add to Navigation Graph

Find your `DAAppNavHost.kt` or main navigation file and add the screen:

```kotlin
// At the top, add import:
import com.uoa.sensor.presentation.ui.navigation.vehicleDetectionMonitorScreen

// Inside your NavHost:
NavHost(
    navController = navController,
    startDestination = startDestination,
    modifier = modifier
) {
    // ... your existing screens ...
    
    homeScreen(navController)
    sensorControlScreen(navController)
    reportScreen(navController)
    
    // ADD THIS LINE:
    vehicleDetectionMonitorScreen()  // ← Vehicle Detection Monitor
}
```

### Step 2: Add Navigation Button (Choose One Location)

**Option A: Add to Home Screen**

In your home screen, add a button:

```kotlin
Button(
    onClick = { navController.navigate("vehicleDetectionMonitor") },
    modifier = Modifier.fillMaxWidth()
) {
    Icon(Icons.Default.Speed, contentDescription = null)
    Spacer(Modifier.width(8.dp))
    Text("Vehicle Detection Monitor")
}
```

**Option B: Add to Sensor Control Screen**

In `SensorActivity` or similar, add:

```kotlin
FloatingActionButton(
    onClick = { navController.navigate("vehicleDetectionMonitor") }
) {
    Icon(Icons.Default.Timeline, contentDescription = "Monitor")
}
```

**Option C: Add to Bottom Navigation**

In your bottom nav setup:

```kotlin
val items = listOf(
    // ... existing items ...
    BottomNavItem(
        route = "vehicleDetectionMonitor",
        icon = Icons.Default.Speed,
        label = "Monitor"
    )
)
```

### Step 3: Build & Test

```powershell
./gradlew clean assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Complete Example

Here's a complete example if you want to add it to your main navigation:

```kotlin
// File: app/src/main/java/.../presentation/daappnavigation/DAAppNavHost.kt

package com.uoa.safedriveafrica.presentation.daappnavigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
// ... other imports ...

// ADD THIS IMPORT:
import com.uoa.sensor.presentation.ui.navigation.vehicleDetectionMonitorScreen

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun DAAppNavHost(
    appState: DAAppState,
    onShowSnackbar: suspend (String, String?) -> Boolean,
    modifier: Modifier = Modifier,
    startDestination: String = WELCOME_ROUTE
) {
    val navController = appState.navController
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Existing screens
        welcomeScreen(navController)
        onBoardingScreen(navController)
        alcoholQuestionnaireScreen(navController)
        homeScreen(navController)
        drivingTipDetailsScreen(navController)
        sensorControlScreen(navController)
        reportScreen(navController)
        
        // ADD THIS LINE - Vehicle Detection Monitor:
        vehicleDetectionMonitorScreen()
    }
}
```

---

## How to Navigate to It

From any screen in your app:

```kotlin
// Using navController:
navController.navigate("vehicleDetectionMonitor")

// Or using the extension function:
import com.uoa.sensor.presentation.ui.navigation.navigateToVehicleDetectionMonitor
navController.navigateToVehicleDetectionMonitor()
```

---

## Example: Add Button to Home Screen

If you want to add a "Monitor" button to your home screen:

```kotlin
// In HomeScreen.kt or wherever you want the button:

@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // ... existing home screen content ...
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Vehicle Detection Monitor Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { navController.navigate("vehicleDetectionMonitor") }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Vehicle Detection Monitor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-time GPS speed and detection status",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
```

---

## That's It!

Once you add the navigation, you'll have:

- ✅ Real-time speed in mph (compare with dashboard)
- ✅ Current detection state
- ✅ Variance analysis
- ✅ GPS accuracy
- ✅ Active trip information

**Build and test it in your vehicle!** 🚗📱

---

## Troubleshooting

### "Cannot resolve symbol vehicleDetectionMonitorScreen"

**Fix**: Make sure you added the import:
```kotlin
import com.uoa.sensor.presentation.ui.navigation.vehicleDetectionMonitorScreen
```

### "Screen shows but no data updates"

**Fix**: Make sure VehicleMovementService is running. The screen gets data from the active DrivingStateManager.

### "Navigation not working"

**Fix**: Check that you added the screen to your NavHost:
```kotlin
NavHost(...) {
    // ... other screens ...
    vehicleDetectionMonitorScreen()  // Make sure this is here
}
```

---

**Ready to integrate!** Add the navigation and enjoy your real-time monitoring screen! ✨

