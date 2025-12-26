package com.expensemanager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.expensemanager.BcStore.BcScheme;
import com.expensemanager.EmiStore.EmiScheme;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Helper for scheduling BC / EMI installment reminders.
 * Called from ReminderUiHelper after user selects scheme + time,
 * and from EmiReminderReceiver to schedule the next cycle.
 */
public class ReminderHelper {

    private static final String DATE_FORMAT = "dd/MM/yyyy";

    /**
     * Schedule a reminder for the selected scheme.
     * Alarm time = NEXT due date at given hour:minute.
     *
     * @param context  Context.
     * @param type     "BC" or "EMI".
     * @param schemeId Scheme id (BcScheme.id or EmiScheme.id).
     * @param hour24   0–23.
     * @param minute   0–59.
     */
    public static void scheduleSchemeReminder(Context context,
                                              String type,
                                              String schemeId,
                                              int hour24,
                                              int minute) {

        if (context == null || TextUtils.isEmpty(type) || TextUtils.isEmpty(schemeId)) {
            return;
        }

        String nextDueDate = getNextDueDate(type, schemeId);
        if (TextUtils.isEmpty(nextDueDate)) {
            // No more installments; nothing to schedule
            return;
        }

        SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        try {
            // Set to due date
            cal.setTime(df.parse(nextDueDate));
        } catch (ParseException e) {
            return;
        }

        // Fire ON due date at chosen time
        cal.set(Calendar.HOUR_OF_DAY, hour24);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // If time is already in the past for this cycle, skip
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            return;
        }

        String title = ("BC".equals(type) ? "BC Reminder" : "EMI Reminder");
        String message = "Installment due on " + nextDueDate;

        Intent intent = new Intent(context, EmiReminderReceiver.class);
        intent.putExtra("type", type);
        intent.putExtra("schemeId", schemeId);
        intent.putExtra("hour24", hour24);
        intent.putExtra("minute", minute);
        intent.putExtra("title", title);
        intent.putExtra("message", message);

        // Stable requestCode per scheme so you overwrite previous alarm for that scheme
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

    /**
     * Find the next unpaid due date for the given scheme.
     * Uses scheduleDates[paidCount] if available.
     */
    private static String getNextDueDate(String type, String schemeId) {
        if ("BC".equals(type)) {
            for (java.util.Map.Entry<String, java.util.ArrayList<BcScheme>> entry
                    : BcStore.getBcMap().entrySet()) {
                for (BcScheme s : entry.getValue()) {
                    if (schemeId.equals(s.id)) {
                        if (s.paidCount >= 0 && s.paidCount < s.scheduleDates.size()) {
                            return s.scheduleDates.get(s.paidCount);
                        }
                        return null;
                    }
                }
            }
        } else if ("EMI".equals(type)) {
            for (java.util.Map.Entry<String, java.util.ArrayList<EmiScheme>> entry
                    : EmiStore.getEmiMap().entrySet()) {
                for (EmiScheme s : entry.getValue()) {
                    if (schemeId.equals(s.id)) {
                        if (s.paidCount >= 0 && s.paidCount < s.scheduleDates.size()) {
                            return s.scheduleDates.get(s.paidCount);
                        }
                        return null;
                    }
                }
            }
        }
        return null;
    }
}
