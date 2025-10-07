package com.example.ziobattery

import android.app.*
import android.content.*
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import ziobattery.BatteryService


import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val requestCodeNotifications = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        askNotificationPermission() // 🔹 add this

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                BatteryAlarmScreen()
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    requestCodeNotifications
                )
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "battery_alarm",
                "Battery Alarm",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}

@Composable
fun BatteryAlarmScreen() {
    val context = LocalContext.current
    var batteryLevel by remember { mutableIntStateOf(0) }
    var targetLevel by remember { mutableIntStateOf(80) }
    val version = 1.1

    // Battery receiver
    LaunchedEffect(Unit) {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                batteryLevel = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
            }
        }
        context.registerReceiver(receiver, filter)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101010)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "Battery: $batteryLevel%",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Circular slider substitute (simple version using Slider)
            Text(
                text = "$targetLevel%",
                color = Color(0xFF00FFAA),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Slider(
                value = targetLevel.toFloat(),
                onValueChange = { targetLevel = it.toInt() },
                valueRange = 0f..100f,
                modifier = Modifier.width(250.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00FFAA),
                    activeTrackColor = Color(0xFF00FFAA)
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(onClick = {
                val intent = Intent(context, BatteryService::class.java)
                intent.putExtra("target", targetLevel)
                ContextCompat.startForegroundService(context, intent)
            }) {
                Text("Set")
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "ziobattery $version%",
                color = Color(0xFF00FFAA),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
