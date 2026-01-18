# Mobile App Fleet Integration Guide

**Version:** 1.0  
**Last Updated:** January 17, 2026  
**Target:** Android/iOS Mobile App Developers

This guide covers how to implement fleet driver management in the Safe Drive Africa mobile app, including driver registration, fleet joining, and onboarding workflows.

---

## Table of Contents

1. [Overview](#overview)
2. [Authentication](#authentication)
3. [Three Integration Flows](#three-integration-flows)
4. [API Endpoints Reference](#api-endpoints-reference)
5. [Implementation Guide](#implementation-guide)
6. [Error Handling](#error-handling)
7. [Best Practices](#best-practices)
8. [Future Enhancements](#future-enhancements)

---

## Overview

The fleet management system supports three distinct driver onboarding flows:

1. **Email Invitation Flow** - Driver invited by email, gets instant fleet access
2. **Invite Code Flow** - Driver uses shared code, requires fleet manager approval
3. **Independent-to-Fleet Flow** - Existing driver joins a fleet later

All flows use JWT Bearer token authentication after initial registration/login.

---

## Authentication

### Registration & Login

**Base URL:** `https://api.safedriveafrica.com`

#### Register New Driver
```http
POST /api/auth/driver/register
Content-Type: application/json

{
  "email": "driver@example.com",
  "password": "securePassword123"
}
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer",
  "driver_profile_id": "924788d5-ce4d-4557-ab75-f52a27db2588",
  "email": "driver@example.com",
  "fleet_assignment": {
    "id": "uuid",
    "fleet_id": "4a44c621-d10c-4271-916c-3d3aae087b24",
    "fleet_name": "Bayelsa State Transport Company",
    "vehicle_group_id": "uuid",
    "assigned_at": "2026-01-17T10:00:00Z"
  },
  "fleet_status": {
    "status": "assigned",
    "fleet": {
      "id": "4a44c621-d10c-4271-916c-3d3aae087b24",
      "name": "Bayelsa State Transport Company"
    },
    "vehicle_group": {
      "id": "uuid",
      "name": "Cars"
    },
    "vehicle": null,
    "pending_request": null
  }
}
```

**Key Fields:**
- `access_token` - JWT token for subsequent API calls
- `fleet_assignment` - Present if driver was invited by email (instant access)
- `fleet_status.status` - Values: `"assigned"`, `"pending"`, `"none"`

**Store the JWT token securely** - It's required for all authenticated endpoints.

#### Login Existing Driver
```http
POST /api/auth/driver/login
Content-Type: application/json

{
  "email": "driver@example.com",
  "password": "securePassword123"
}
```

**Response:** Same structure as registration response.

---

## Three Integration Flows

### Flow 1: Email Invitation (Instant Access)

**Scenario:** Fleet manager sends email invitation to specific driver.

```
┌─────────────┐         ┌──────────────┐         ┌─────────┐
│Fleet Manager│         │    Driver    │         │   API   │
└──────┬──────┘         └──────┬───────┘         └────┬────┘
       │                       │                      │
       │ Send email invite     │                      │
       │───────────────────────┼─────────────────────>│
       │                       │                      │
       │                       │ Register with email  │
       │                       │─────────────────────>│
       │                       │                      │
       │                       │  Auto-assign fleet   │
       │                       │<─────────────────────│
       │                       │                      │
       │                       │ fleet_assignment ✓   │
       │                       │ status: "assigned"   │
       │                       │                      │
       │                  Go to Home Screen           │
       │                       │                      │
```

**Mobile Implementation:**

```kotlin
suspend fun registerDriver(email: String, password: String): RegistrationFlow {
    val response = apiService.register(
        RegisterRequest(email, password)
    )
    
    // Save JWT token
    secureStorage.saveToken(response.access_token)
    
    // Check fleet assignment
    return when {
        response.fleet_assignment != null -> {
            // Email invitation - instant access
            RegistrationFlow.DirectToHome(
                fleetName = response.fleet_assignment.fleet_name
            )
        }
        else -> {
            // No fleet assignment - show invite code input
            RegistrationFlow.NeedsInviteCode
        }
    }
}
```

---

### Flow 2: Invite Code (Requires Approval)

**Scenario:** Fleet manager creates shareable code, driver submits join request.

```
┌─────────────┐         ┌──────────────┐         ┌─────────┐
│Fleet Manager│         │    Driver    │         │   API   │
└──────┬──────┘         └──────┬───────┘         └────┬────┘
       │                       │                      │
       │ Create invite code    │                      │
       │ "BAYE-7EOBSF"         │                      │
       │───────────────────────┼─────────────────────>│
       │                       │                      │
       │ Share code (SMS/etc)  │                      │
       │──────────────────────>│                      │
       │                       │                      │
       │                       │ Register (any email) │
       │                       │─────────────────────>│
       │                       │                      │
       │                       │ fleet_assignment=null│
       │                       │<─────────────────────│
       │                       │                      │
       │                       │ Enter invite code    │
       │                       │ POST /join-fleet     │
       │                       │─────────────────────>│
       │                       │                      │
       │                       │ Request created      │
       │                       │ status: "pending"    │
       │                       │<─────────────────────│
       │                       │                      │
       │                   Show waiting screen        │
       │                   Poll fleet status          │
       │                       │                      │
       │ Review & approve      │                      │
       │───────────────────────┼─────────────────────>│
       │                       │                      │
       │                       │ Poll: GET status     │
       │                       │─────────────────────>│
       │                       │                      │
       │                       │ status: "assigned" ✓ │
       │                       │<─────────────────────│
       │                       │                      │
       │                  Navigate to Home Screen     │
       │                       │                      │
```

**Mobile Implementation:**

```kotlin
// Step 1: After registration with no fleet_assignment
fun showInviteCodeScreen() {
    // Show input dialog for invite code
}

// Step 2: Submit invite code
suspend fun joinFleetWithCode(inviteCode: String): JoinFleetResult {
    try {
        val response = apiService.joinFleet(
            authorization = "Bearer ${getToken()}",
            body = JoinFleetRequest(invite_code = inviteCode)
        )
        
        // Start polling for status changes
        startFleetStatusPolling()
        
        return JoinFleetResult.Success(
            fleetName = response.fleet_name,
            requestId = response.request_id
        )
        
    } catch (e: HttpException) {
        return when (e.code()) {
            404 -> JoinFleetResult.InvalidCode
            409 -> JoinFleetResult.AlreadyInFleet
            else -> JoinFleetResult.Error(e.message())
        }
    }
}

// Step 3: Poll for approval
suspend fun pollFleetStatus() {
    while (true) {
        delay(5000) // Poll every 5 seconds
        
        val status = apiService.getFleetStatus(
            authorization = "Bearer ${getToken()}"
        )
        
        when (status.status) {
            "assigned" -> {
                // Approved! Navigate to home
                navigateToHome(status.fleet)
                break
            }
            "pending" -> {
                // Still waiting
                continue
            }
            "none" -> {
                // Rejected or cancelled
                showRejectionMessage()
                break
            }
        }
    }
}
```

---

### Flow 3: Independent Driver Joins Fleet Later

**Scenario:** Driver registered without fleet, uses app independently, then decides to join fleet.

```
┌──────────────┐         ┌─────────┐
│    Driver    │         │   API   │
└──────┬───────┘         └────┬────┘
       │                      │
       │ Register (no code)   │
       │─────────────────────>│
       │                      │
       │ fleet_assignment=null│
       │<─────────────────────│
       │                      │
  Skip invite code            │
  Go to Home Screen           │
       │                      │
  Use app for weeks/months    │
  (Upload trips as independent)
       │                      │
  Decide to join fleet        │
       │                      │
  Settings > Join Fleet       │
       │                      │
       │ Enter invite code    │
       │ POST /join-fleet     │
       │─────────────────────>│
       │                      │
       │ Request created      │
       │ status: "pending"    │
       │<─────────────────────│
       │                      │
  Show pending status         │
  Continue using app          │
       │                      │
  (Fleet manager approves)    │
       │                      │
       │ Poll: GET status     │
       │─────────────────────>│
       │                      │
       │ status: "assigned" ✓ │
       │<─────────────────────│
       │                      │
  Show "Now in fleet!" banner │
       │                      │
```

**Mobile Implementation:**

```kotlin
// Settings Screen - Fleet Status Section
@Composable
fun FleetStatusSection(viewModel: SettingsViewModel) {
    val fleetStatus by viewModel.fleetStatus.collectAsState()
    
    when (fleetStatus.status) {
        "none" -> {
            // Independent driver - show join button
            Button(onClick = { viewModel.showJoinFleetDialog() }) {
                Text("Join Fleet")
            }
        }
        
        "pending" -> {
            // Request pending
            Column {
                Text("Join request pending")
                Text("Fleet: ${fleetStatus.pending_request?.fleet_name}")
                Text("Requested: ${formatDate(fleetStatus.pending_request?.requested_at)}")
                
                Button(onClick = { viewModel.cancelJoinRequest() }) {
                    Text("Cancel Request")
                }
            }
        }
        
        "assigned" -> {
            // In fleet
            Column {
                Text("Fleet: ${fleetStatus.fleet?.name}")
                Text("Vehicle Group: ${fleetStatus.vehicle_group?.name ?: "Not assigned"}")
                // Optional: Add "Leave Fleet" button
            }
        }
    }
}

// Join Fleet Dialog
suspend fun onJoinFleetClicked(inviteCode: String) {
    // Same logic as Flow 2
    val result = joinFleetWithCode(inviteCode)
    
    when (result) {
        is JoinFleetResult.Success -> {
            showMessage("Request sent to ${result.fleetName}")
            startFleetStatusPolling()
        }
        is JoinFleetResult.InvalidCode -> {
            showError("Invalid invite code")
        }
        is JoinFleetResult.AlreadyInFleet -> {
            showError("You're already in a fleet")
        }
        is JoinFleetResult.Error -> {
            showError(result.message)
        }
    }
}
```

---

## API Endpoints Reference

### 1. Register Driver

**Endpoint:** `POST /api/auth/driver/register`

**Request:**
```json
{
  "email": "driver@example.com",
  "password": "password123"
}
```

**Success Response (200):**
```json
{
  "access_token": "jwt_token_here",
  "token_type": "bearer",
  "driver_profile_id": "uuid",
  "email": "driver@example.com",
  "fleet_assignment": null,
  "fleet_status": {
    "status": "none",
    "fleet": null,
    "vehicle_group": null,
    "vehicle": null,
    "pending_request": null
  }
}
```

**Error Responses:**
- `400 Bad Request` - Invalid email or password format
- `409 Conflict` - Email already registered

---

### 2. Login Driver

**Endpoint:** `POST /api/auth/driver/login`

**Request:**
```json
{
  "email": "driver@example.com",
  "password": "password123"
}
```

**Success Response (200):** Same as registration

**Error Responses:**
- `401 Unauthorized` - Invalid credentials

---

### 3. Join Fleet with Invite Code

**Endpoint:** `POST /api/driver/join-fleet`

**Headers:**
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request:**
```json
{
  "invite_code": "BAYE-7EOBSF"
}
```

**Success Response (200):**
```json
{
  "message": "Join request created successfully",
  "request_id": "uuid",
  "fleet_name": "Bayelsa State Transport Company"
}
```

**Error Responses:**
- `401 Unauthorized` - Invalid or expired JWT token
- `404 Not Found` - Invite code doesn't exist or expired
- `409 Conflict` - Driver already in a fleet
- `410 Gone` - Invite code has been revoked

---

### 4. Get Fleet Status

**Endpoint:** `GET /api/driver/fleet-status`

**Headers:**
```
Authorization: Bearer <jwt_token>
```

**Success Response (200) - No Fleet:**
```json
{
  "status": "none",
  "fleet": null,
  "vehicle_group": null,
  "vehicle": null,
  "pending_request": null
}
```

**Success Response (200) - Pending Request:**
```json
{
  "status": "pending",
  "fleet": null,
  "vehicle_group": null,
  "vehicle": null,
  "pending_request": {
    "id": "uuid",
    "fleet_name": "Bayelsa State Transport Company",
    "requested_at": "2026-01-17T10:00:00Z"
  }
}
```

**Success Response (200) - Assigned to Fleet:**
```json
{
  "status": "assigned",
  "fleet": {
    "id": "4a44c621-d10c-4271-916c-3d3aae087b24",
    "name": "Bayelsa State Transport Company"
  },
  "vehicle_group": {
    "id": "uuid",
    "name": "Cars"
  },
  "vehicle": null,
  "pending_request": null
}
```

**Status Values:**
- `"none"` - Driver not in any fleet, no pending requests
- `"pending"` - Join request submitted, awaiting fleet manager approval
- `"assigned"` - Driver is member of a fleet

**Error Responses:**
- `401 Unauthorized` - Invalid or expired JWT token
- `403 Forbidden` - Not a driver (admin/researcher accessing)

---

### 5. Cancel Join Request

**Endpoint:** `POST /api/driver/cancel-join-request`

**Headers:**
```
Authorization: Bearer <jwt_token>
```

**No request body required**

**Success Response (200):**
```json
{
  "message": "Join request cancelled successfully"
}
```

**Error Responses:**
- `401 Unauthorized` - Invalid JWT token
- `404 Not Found` - No pending request to cancel

---

## Implementation Guide

### Initial Setup

#### 1. Store JWT Token Securely

```kotlin
// Use Android Keystore or EncryptedSharedPreferences
class SecureTokenStorage(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun saveToken(token: String) {
        sharedPreferences.edit().putString("jwt_token", token).apply()
    }
    
    fun getToken(): String? {
        return sharedPreferences.getString("jwt_token", null)
    }
    
    fun clearToken() {
        sharedPreferences.edit().remove("jwt_token").apply()
    }
}
```

#### 2. Add Authorization Header to All API Calls

```kotlin
class AuthInterceptor(private val tokenStorage: SecureTokenStorage) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStorage.getToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}

// Add to OkHttpClient
val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(AuthInterceptor(tokenStorage))
    .build()
```

---

### Registration Flow Implementation

```kotlin
sealed class RegistrationResult {
    data class DirectToHome(val fleetName: String) : RegistrationResult()
    object NeedsInviteCode : RegistrationResult()
    data class Error(val message: String) : RegistrationResult()
}

class AuthViewModel(
    private val apiService: ApiService,
    private val tokenStorage: SecureTokenStorage
) : ViewModel() {
    
    suspend fun register(email: String, password: String): RegistrationResult {
        return try {
            val response = apiService.register(
                RegisterRequest(email, password)
            )
            
            // Save JWT token
            tokenStorage.saveToken(response.access_token)
            
            // Check fleet status
            when {
                response.fleet_assignment != null -> {
                    // Email invitation - instant access
                    RegistrationResult.DirectToHome(
                        fleetName = response.fleet_assignment.fleet_name
                    )
                }
                else -> {
                    // No fleet - show invite code input
                    RegistrationResult.NeedsInviteCode
                }
            }
            
        } catch (e: HttpException) {
            when (e.code()) {
                409 -> RegistrationResult.Error("Email already registered")
                else -> RegistrationResult.Error("Registration failed: ${e.message()}")
            }
        } catch (e: Exception) {
            RegistrationResult.Error("Network error: ${e.message}")
        }
    }
}
```

---

### Fleet Joining Implementation

```kotlin
sealed class JoinFleetResult {
    data class Success(val fleetName: String, val requestId: String) : JoinFleetResult()
    object InvalidCode : JoinFleetResult()
    object AlreadyInFleet : JoinFleetResult()
    data class Error(val message: String) : JoinFleetResult()
}

class FleetViewModel(
    private val apiService: ApiService,
    private val tokenStorage: SecureTokenStorage
) : ViewModel() {
    
    private val _fleetStatus = MutableStateFlow<FleetStatus?>(null)
    val fleetStatus: StateFlow<FleetStatus?> = _fleetStatus.asStateFlow()
    
    private var pollingJob: Job? = null
    
    // Join fleet with invite code
    suspend fun joinFleet(inviteCode: String): JoinFleetResult {
        return try {
            val response = apiService.joinFleet(
                JoinFleetRequest(invite_code = inviteCode.uppercase())
            )
            
            // Start polling for status changes
            startPolling()
            
            JoinFleetResult.Success(
                fleetName = response.fleet_name,
                requestId = response.request_id
            )
            
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> JoinFleetResult.InvalidCode
                409 -> JoinFleetResult.AlreadyInFleet
                410 -> JoinFleetResult.InvalidCode // Revoked
                else -> JoinFleetResult.Error(e.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            JoinFleetResult.Error(e.message ?: "Network error")
        }
    }
    
    // Get current fleet status
    suspend fun checkFleetStatus() {
        try {
            val status = apiService.getFleetStatus()
            _fleetStatus.value = status
            
            // Stop polling if assigned or no request
            if (status.status != "pending") {
                stopPolling()
            }
        } catch (e: Exception) {
            Log.e("FleetViewModel", "Error checking fleet status", e)
        }
    }
    
    // Start polling for status changes
    fun startPolling() {
        stopPolling() // Cancel existing job
        
        pollingJob = viewModelScope.launch {
            while (isActive) {
                checkFleetStatus()
                delay(5000) // Poll every 5 seconds
            }
        }
    }
    
    // Stop polling
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
    
    // Cancel join request
    suspend fun cancelJoinRequest(): Boolean {
        return try {
            apiService.cancelJoinRequest()
            checkFleetStatus()
            true
        } catch (e: Exception) {
            Log.e("FleetViewModel", "Error cancelling request", e)
            false
        }
    }
}
```

---

### UI Implementation

#### Registration Screen

```kotlin
@Composable
fun RegistrationScreen(
    viewModel: AuthViewModel,
    onRegistrationSuccess: (RegistrationResult) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Register", style = MaterialTheme.typography.headlineMedium)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                isLoading = true
                viewModel.viewModelScope.launch {
                    val result = viewModel.register(email, password)
                    isLoading = false
                    onRegistrationSuccess(result)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
                )
            } else {
                Text("Register")
            }
        }
    }
}
```

#### Invite Code Input Screen

```kotlin
@Composable
fun InviteCodeScreen(
    viewModel: FleetViewModel,
    onSkip: () -> Unit,
    onJoinSuccess: (String) -> Unit
) {
    var inviteCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Join a Fleet",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Enter your fleet invite code to join",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = inviteCode,
            onValueChange = { 
                inviteCode = it.uppercase()
                errorMessage = null
            },
            label = { Text("Invite Code") },
            placeholder = { Text("BAYE-7EOBSF") },
            modifier = Modifier.fillMaxWidth()
        )
        
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                isLoading = true
                viewModel.viewModelScope.launch {
                    when (val result = viewModel.joinFleet(inviteCode)) {
                        is JoinFleetResult.Success -> {
                            onJoinSuccess(result.fleetName)
                        }
                        is JoinFleetResult.InvalidCode -> {
                            errorMessage = "Invalid or expired invite code"
                        }
                        is JoinFleetResult.AlreadyInFleet -> {
                            errorMessage = "You're already in a fleet"
                        }
                        is JoinFleetResult.Error -> {
                            errorMessage = result.message
                        }
                    }
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && inviteCode.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
                )
            } else {
                Text("Join Fleet")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Skip for now")
        }
    }
}
```

#### Pending Approval Screen

```kotlin
@Composable
fun PendingApprovalScreen(
    fleetName: String,
    requestedAt: String,
    onCancel: () -> Unit,
    onCheckStatus: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.HourglassEmpty,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Request Pending",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Your request to join $fleetName is awaiting approval",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Requested: $requestedAt",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onCheckStatus,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Check Status")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel Request")
        }
    }
}
```

#### Settings Screen - Fleet Section

```kotlin
@Composable
fun FleetSettingsSection(
    fleetStatus: FleetStatus?,
    onJoinFleet: () -> Unit,
    onCancelRequest: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Fleet Membership",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            when (fleetStatus?.status) {
                "none" -> {
                    Text(
                        "You're not part of any fleet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onJoinFleet,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Join Fleet")
                    }
                }
                
                "pending" -> {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.HourglassEmpty,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Request Pending",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    fleetStatus.pending_request?.fleet_name ?: "",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = onCancelRequest,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel Request")
                        }
                    }
                }
                
                "assigned" -> {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.Green
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    fleetStatus.fleet?.name ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Vehicle Group: ${fleetStatus.vehicle_group?.name ?: "Not assigned"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
```

---

## Error Handling

### Common HTTP Error Codes

| Code | Meaning | Action |
|------|---------|--------|
| `400` | Bad Request | Show validation error to user |
| `401` | Unauthorized | Token expired, redirect to login |
| `403` | Forbidden | User doesn't have permission |
| `404` | Not Found | Resource doesn't exist (invalid code) |
| `409` | Conflict | Duplicate (email exists, already in fleet) |
| `410` | Gone | Invite code revoked |
| `500` | Server Error | Retry or show "Try again later" |

### Error Handling Implementation

```kotlin
suspend fun <T> safeApiCall(
    apiCall: suspend () -> T
): Result<T> {
    return try {
        Result.success(apiCall())
    } catch (e: HttpException) {
        when (e.code()) {
            401 -> {
                // Token expired - logout user
                tokenStorage.clearToken()
                Result.failure(Exception("Session expired. Please login again."))
            }
            404 -> Result.failure(Exception("Resource not found"))
            409 -> Result.failure(Exception("Resource already exists"))
            410 -> Result.failure(Exception("Resource no longer available"))
            in 500..599 -> Result.failure(Exception("Server error. Please try again later."))
            else -> Result.failure(Exception(e.message() ?: "Unknown error"))
        }
    } catch (e: IOException) {
        Result.failure(Exception("Network error. Check your connection."))
    } catch (e: Exception) {
        Result.failure(Exception("An unexpected error occurred"))
    }
}

// Usage
suspend fun joinFleet(code: String): JoinFleetResult {
    return when (val result = safeApiCall { apiService.joinFleet(JoinFleetRequest(code)) }) {
        is Result.Success -> JoinFleetResult.Success(result.data.fleet_name, result.data.request_id)
        is Result.Failure -> JoinFleetResult.Error(result.exception.message ?: "Unknown error")
    }
}
```

---

## Best Practices

### 1. Token Management

- **Store JWT tokens securely** using Android Keystore or EncryptedSharedPreferences
- **Include Bearer token** in Authorization header for all authenticated requests
- **Handle 401 responses** by clearing token and redirecting to login
- **Refresh token on app resume** if implementing token refresh

### 2. Fleet Status Polling

- **Poll every 5 seconds** when waiting for approval
- **Stop polling** when status changes to "assigned" or "none"
- **Cancel polling** when user navigates away from waiting screen
- **Use background service** if polling needs to continue when app is backgrounded

### 3. User Experience

- **Auto-uppercase invite codes** - Codes are case-insensitive, display as uppercase
- **Show clear status indicators** - Use icons/colors for pending/assigned states
- **Allow "Skip" option** - Independent drivers can skip fleet joining
- **Provide feedback** - Show loading states and success/error messages
- **Handle network errors gracefully** - Retry logic for failed requests

### 4. Data Upload

- **Independent drivers** can upload trips without fleet_id
- **Fleet drivers** upload same way - fleet association determined via `driver_fleet_assignments` table
- **No changes needed** to existing trip upload logic
- **Future enhancement** will add `fleet_id` directly to trips table (see below)

### 5. App Lifecycle

```kotlin
class MainActivity : ComponentActivity() {
    private val fleetViewModel: FleetViewModel by viewModels()
    
    override fun onResume() {
        super.onResume()
        // Check fleet status when app resumes
        lifecycleScope.launch {
            fleetViewModel.checkFleetStatus()
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Stop polling when app goes to background
        fleetViewModel.stopPolling()
    }
}
```

---

## Future Enhancements

### Fleet ID in Trip Data

**Current Limitation:**  
Trip data doesn't directly store `fleet_id`. Fleet association is determined by joining `trips` table with `driver_fleet_assignments` table at query time.

**Planned Enhancement:**  
Add `fleet_id` field directly to trips table for easier analytics and filtering.

**What This Means for You:**
- Current implementation works fine
- Trip uploads work identically for independent and fleet drivers
- Backend handles fleet association lookup
- Future API version will include `fleet_id` in trip responses
- **No mobile app changes required** when this is implemented

**Timeline:** To be implemented in future sprint (documented for reference only)

---

## Testing Checklist

### Registration Flow
- [ ] Register with invited email → Instant fleet access
- [ ] Register with non-invited email → Prompt for invite code
- [ ] Register with existing email → Show appropriate error

### Invite Code Flow
- [ ] Submit valid invite code → Create pending request
- [ ] Submit invalid code → Show error message
- [ ] Submit expired code → Show error message
- [ ] Submit revoked code → Show error message
- [ ] Driver already in fleet → Show conflict error

### Fleet Status
- [ ] Status polling updates UI when approved
- [ ] Status polling stops when assigned
- [ ] Cancel request works correctly
- [ ] App resume refreshes status

### Independent Driver
- [ ] Skip invite code during onboarding
- [ ] Upload trips as independent driver
- [ ] Join fleet from settings later
- [ ] Transition from independent to fleet driver

### Error Handling
- [ ] Network errors show retry option
- [ ] Token expiration logs out user
- [ ] Server errors show friendly message
- [ ] All error states handled gracefully

---

## Support & Contact

**API Base URL:** `https://api.safedriveafrica.com`

**Documentation Version:** 1.0 (January 17, 2026)

**Questions?** Refer to:
- API specification: `fleet-driver-management-api-specification.md`
- Web client guide: `fleet-driver-client-integration.md`
- Backend documentation: `backend_api.md`

For backend issues or API questions, contact the backend development team.
