package com.expensemanager;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * Receives alarms for EMI / BC reminders and shows a notification.
 * Registered in AndroidManifest.xml.
 */
public class EmiReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "emi_bc_reminder_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Read extras from PendingIntent
        String type = intent.getStringExtra("type");          // "BC" or "EMI"
        String schemeId = intent.getStringExtra("schemeId");  // scheme id
        int hour24 = intent.getIntExtra("hour24", 7);
        int minute = intent.getIntExtra("minute", 0);

        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");

        if (title == null || title.trim().isEmpty()) {
            title = "Payment Reminder";
        }
        if (message == null || message.trim().isEmpty()) {
            message = "Your installment is due.";
        }

        createChannelIfNeeded(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)  // ensure this icon exists
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)                  // user tap dismisses it
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        int notificationId = (int) System.currentTimeMillis();
        manager.notify(notificationId, builder.build());

        // Schedule next month's reminder for this scheme (if more installments left)
        if (type != null && schemeId != null) {
            ReminderHelper.scheduleSchemeReminder(context, type, schemeId, hour24, minute);
        }
    }

    private void createChannelIfNeeded(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = "EMI & BC Reminders";
            String description = "Notifications for monthly EMI and BC installments";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel =
                    new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager manager =
                    context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
