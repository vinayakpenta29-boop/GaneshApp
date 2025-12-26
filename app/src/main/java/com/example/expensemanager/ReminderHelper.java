package com.expensemanager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

/**
 * Helper for scheduling BC / EMI installment reminders.
 * Call ReminderHelper.scheduleMonthlyReminder(...) after saving an installment.
 */
public class ReminderHelper {

    /**
     * Schedule a reminder for the NEXT month on the same day at 9:00 AM.
     *
     * @param context  Context (use requireContext() / getApplicationContext()).
     * @param title    Notification title.
     * @param message  Notification message.
     * @param year     Current installment year (e.g. "2025").
     * @param month    Current installment month 1‑12 as String (e.g. "3" for March).
     */
    public static void scheduleMonthlyReminder(Context context,
                                               String title,
                                               String message,
                                               String year,
                                               String month) {

        int y, m;
        try {
            y = Integer.parseInt(year);
            m = Integer.parseInt(month);   // 1‑12
        } catch (NumberFormatException e) {
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, y);
        cal.set(Calendar.MONTH, m - 1);   // Calendar months are 0‑based
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

        // Move to next month, same day (or last valid day)
        cal.add(Calendar.MONTH, 1);
        cal.set(Calendar.DAY_OF_MONTH,
                Math.min(dayOfMonth, cal.getActualMaximum(Calendar.DAY_OF_MONTH)));
        cal.set(Calendar.HOUR_OF_DAY, 20);
        cal.set(Calendar.MINUTE, 17);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Intent intent = new Intent(context, EmiReminderReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("message", message);

        // Unique request code (good enough for this use-case)
        int requestCode = (int) System.currentTimeMillis();

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
