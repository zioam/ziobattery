package ziobattery

import android.app.*
import android.content.*
import android.os.*
import androidx.core.app.NotificationCompat

class BatteryService : Service() {

    private var targetLevel = 80
    private var receiverRegistered = false
    private val channelId = "battery_service_channel"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        targetLevel = intent?.getIntExtra("target", 80) ?: 80

        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle("Battery Alarm Active")
            .setContentText("Monitoring battery level…")
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(1, notification)
        }


        if (!receiverRegistered) {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            registerReceiver(batteryReceiver, filter)
            receiverRegistered = true
        }

        return START_STICKY
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: return
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            if (charging && level >= targetLevel) {
                triggerAlarm()
                stopSelf()
            }
        }
    }

    private fun triggerAlarm() {
        val alarmChannelId = "battery_alarm"
        createAlarmChannel(alarmChannelId)

        val notification = NotificationCompat.Builder(this, alarmChannelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Battery Target Reached!")
            .setContentText("Battery is now at $targetLevel%")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(Notification.DEFAULT_ALL)
            .setCategory(Notification.CATEGORY_ALARM)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(2, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                "Battery Monitoring Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createAlarmChannel(id: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmChannel = NotificationChannel(
                id,
                "Battery Alarm",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(alarmChannel)
        }
    }

    override fun onDestroy() {
        if (receiverRegistered) {
            unregisterReceiver(batteryReceiver)
            receiverRegistered = false
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}
