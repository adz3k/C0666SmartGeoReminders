package com.example.smartgeoreminders

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.smartgeoreminders.network.NetworkModule
import kotlinx.coroutines.launch
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerTestScreen(
    reminders: List<ReminderEntity>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var postcode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var resolvedLat by remember { mutableStateOf<Double?>(null) }
    var resolvedLng by remember { mutableStateOf<Double?>(null) }

    var onlyActive by remember { mutableStateOf(true) }

    val alreadyNotifiedIds = remember { mutableStateListOf<Long>() }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    var notifGranted by remember { mutableStateOf(hasNotificationPermission()) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifGranted = granted
    }

    fun requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifGranted) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val triggered = remember(reminders, resolvedLat, resolvedLng, onlyActive) {
        val lat = resolvedLat
        val lng = resolvedLng
        if (lat == null || lng == null) return@remember emptyList()

        val source = if (onlyActive) reminders.filter { it.isActive } else reminders

        source.mapNotNull { r ->
            val rLat = r.latitude
            val rLng = r.longitude
            if (rLat == null || rLng == null) return@mapNotNull null

            val dist = distanceMeters(lat, lng, rLat, rLng)
            if (dist <= r.radiusMeters) Pair(r, dist) else null
        }.sortedBy { it.second }
    }

    LaunchedEffect(Unit) {
        notifGranted = hasNotificationPermission()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test trigger (postcode)") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifGranted) {
                Card {
                    Column(Modifier.padding(12.dp)) {
                        Text("Notifications are off. Enable them to see reminder alerts.")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { requestNotifPermission() }) {
                            Text("Allow notifications")
                        }
                    }
                }
            }

            Text(
                "Enter a postcode to simulate your current location and trigger reminders.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = postcode,
                onValueChange = { postcode = it; errorMsg = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Current postcode") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                singleLine = true,
                supportingText = {
                    errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    enabled = !isLoading,
                    onClick = {
                        val cleaned = postcode.trim()
                        if (cleaned.isEmpty()) {
                            errorMsg = "Enter a postcode."
                            return@Button
                        }

                        isLoading = true
                        errorMsg = null

                        scope.launch {
                            try {
                                val res = NetworkModule.postcodeApi.lookup(cleaned)
                                val lat = res.result?.latitude
                                val lng = res.result?.longitude

                                if (lat == null || lng == null) {
                                    errorMsg = "Postcode not found."
                                    resolvedLat = null
                                    resolvedLng = null
                                } else {
                                    resolvedLat = lat
                                    resolvedLng = lng
                                    alreadyNotifiedIds.clear()
                                }
                            } catch (_: Exception) {
                                errorMsg = "Network error. Check internet and try again."
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                ) {
                    Text(if (isLoading) "Checking..." else "Check + Trigger")
                }

                OutlinedButton(
                    enabled = !isLoading,
                    onClick = {
                        postcode = ""
                        errorMsg = null
                        resolvedLat = null
                        resolvedLng = null
                        alreadyNotifiedIds.clear()
                    }
                ) { Text("Clear") }
            }

            if (resolvedLat != null && resolvedLng != null) {
                Text(
                    "Resolved to: ${"%.5f".format(resolvedLat)} , ${"%.5f".format(resolvedLng)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Show active only")
                Switch(checked = onlyActive, onCheckedChange = { onlyActive = it })
            }

            HorizontalDivider()

            Text("Triggered reminders", style = MaterialTheme.typography.titleMedium)

            if (triggered.isEmpty()) {
                Text("No reminders triggered.", style = MaterialTheme.typography.bodyMedium)
            } else {
                triggered.forEach { (r, dist) ->
                    LaunchedEffect(resolvedLat, resolvedLng, r.id, dist, notifGranted) {
                        if (!alreadyNotifiedIds.contains(r.id) && notifGranted && NotificationUtils.canPostNotifications(context)) {
                            alreadyNotifiedIds.add(r.id)

                            val msg =
                                "Postcode: ${r.postcode ?: "N/A"} • Distance: ${dist.roundToInt()}m • Radius: ${r.radiusMeters}m"

                            NotificationUtils.send(
                                context = context,
                                title = "Reminder triggered: ${r.title}",
                                body = msg,
                                notificationId = r.id.toInt()
                            )
                        }
                    }

                    Card {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(r.title, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text("Postcode: ${r.postcode ?: "N/A"}")
                            Text("Radius: ${r.radiusMeters}m")
                            Text("Distance: ${dist.roundToInt()}m")
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

private fun distanceMeters(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Double {
    val earthRadiusMeters = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusMeters * c
}
