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
                .setSmallIcon(R.mipmap.ic_launcher)  // make sure this icon exists
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        int notificationId = (int) System.currentTimeMillis();
        manager.notify(notificationId, builder.build());
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
