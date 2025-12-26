package com.expensemanager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import java.util.Calendar;

/**
 * TEMP version of ReminderHelper just to test that alarms + notifications fire.
 * It always schedules a reminder 2 minutes from now.
 */
public class ReminderHelper {

    /**
     * Schedule a test reminder 2 minutes from now.
     *
     * @param context  Context.
     * @param type     "BC" or "EMI" (used only for title).
     * @param schemeId Scheme id (only used for stable requestCode).
     * @param hour24   ignored in test.
     * @param minute   ignored in test.
     */
    public static void scheduleSchemeReminder(Context context,
                                              String type,
                                              String schemeId,
                                              int hour24,
                                              int minute) {

        if (context == null || TextUtils.isEmpty(type) || TextUtils.isEmpty(schemeId)) {
            return;
        }

        // === TEST: 2 minutes from now ===
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, 2);

        String title = ("BC".equals(type) ? "BC Reminder (TEST)" : "EMI Reminder (TEST)");
        String message = "Test reminder scheduled 2 minutes from now.";

        Intent intent = new Intent(context, EmiReminderReceiver.class);
        intent.putExtra("type", type);
        intent.putExtra("schemeId", schemeId);
        intent.putExtra("hour24", cal.get(Calendar.HOUR_OF_DAY));
        intent.putExtra("minute", cal.get(Calendar.MINUTE));
        intent.putExtra("title", title);
        intent.putExtra("message", message);

        int requestCode = schemeId.hashCode();

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    cal.getTimeInMillis(),
                    pi
            );
        }
    }
}
