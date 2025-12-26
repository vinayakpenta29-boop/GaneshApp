package com.expensemanager;

import android.app.TimePickerDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.expensemanager.BcStore.BcScheme;
import com.expensemanager.EmiStore.EmiScheme;

public class ReminderUiHelper {

    /**
     * Entry point from ExpensesFragment.
     */
    public static void showReminderDialog(Fragment fragment) {
        Context ctx = fragment.requireContext();

        LayoutInflater inflater = LayoutInflater.from(ctx);
        View root = inflater.inflate(R.layout.dialog_expense_reminder, null, false);
        // If you don't want a layout file, you can also build the view in code.
        // For simplicity assume dialog_expense_reminder has:
        // - Switch with id switch_enable_reminder
        // - RadioGroup with two RadioButtons: rb_bc and rb_emi
        // - TextView tv_selected_scheme

        Switch switchEnable = root.findViewById(R.id.switch_enable_reminder);
        RadioGroup rgType = root.findViewById(R.id.rg_type);
        RadioButton rbBc = root.findViewById(R.id.rb_bc);
        RadioButton rbEmi = root.findViewById(R.id.rb_emi);
        TextView tvScheme = root.findViewById(R.id.tv_selected_scheme);

        rgType.setVisibility(View.GONE);
        tvScheme.setVisibility(View.GONE);

        final String[] selectedType = new String[]{null};      // "BC" or "EMI"
        final String[] selectedSchemeId = new String[]{null};
        final String[] selectedSchemeName = new String[]{null};

        switchEnable.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked) {
                rgType.setVisibility(View.VISIBLE);
            } else {
                rgType.setVisibility(View.GONE);
                tvScheme.setVisibility(View.GONE);
                selectedType[0] = null;
                selectedSchemeId[0] = null;
                selectedSchemeName[0] = null;
            }
        });

        rgType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_bc) {
                selectedType[0] = "BC";
                pickBcScheme(fragment, tvScheme, selectedSchemeId, selectedSchemeName);
            } else if (checkedId == R.id.rb_emi) {
                selectedType[0] = "EMI";
                pickEmiScheme(fragment, tvScheme, selectedSchemeId, selectedSchemeName);
            }
        });

        new android.app.AlertDialog.Builder(ctx)
                .setTitle("EMI Reminder")
                .setView(root)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Set", (d, w) -> {
                    if (!switchEnable.isChecked()) {
                        Toast.makeText(ctx, "Reminder is OFF", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (selectedType[0] == null || TextUtils.isEmpty(selectedSchemeId[0])) {
                        Toast.makeText(ctx, "Select BC/EMI scheme first", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // After dialog OK, open time picker
                    openTimePickerAndSchedule(ctx, selectedType[0], selectedSchemeId[0], selectedSchemeName[0]);
                })
                .show();
    }

    private static void pickBcScheme(Fragment fragment,
                                     TextView tvScheme,
                                     String[] selectedSchemeId,
                                     String[] selectedSchemeName) {
        Context ctx = fragment.requireContext();
        List<BcScheme> schemes = BcStore.getSchemesForOwner("EXPENSE");
        if (schemes.isEmpty()) {
            Toast.makeText(ctx, "No BC schemes found", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> labels = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (BcScheme s : schemes) {
            labels.add(s.name);
            ids.add(s.id);
        }

        String[] items = labels.toArray(new String[0]);
        new android.app.AlertDialog.Builder(ctx)
                .setTitle("Select BC Scheme")
                .setItems(items, (dialog, which) -> {
                    selectedSchemeId[0] = ids.get(which);
                    selectedSchemeName[0] = labels.get(which);
                    tvScheme.setText("Selected: " + selectedSchemeName[0]);
                    tvScheme.setVisibility(View.VISIBLE);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void pickEmiScheme(Fragment fragment,
                                      TextView tvScheme,
                                      String[] selectedSchemeId,
                                      String[] selectedSchemeName) {
        Context ctx = fragment.requireContext();
        List<EmiScheme> schemes = EmiStore.getSchemesForOwner("EXPENSE");
        if (schemes.isEmpty()) {
            Toast.makeText(ctx, "No EMI schemes found", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> labels = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (EmiScheme s : schemes) {
            labels.add(s.name);
            ids.add(s.id);
        }

        String[] items = labels.toArray(new String[0]);
        new android.app.AlertDialog.Builder(ctx)
                .setTitle("Select EMI Scheme")
                .setItems(items, (dialog, which) -> {
                    selectedSchemeId[0] = ids.get(which);
                    selectedSchemeName[0] = labels.get(which);
                    tvScheme.setText("Selected: " + selectedSchemeName[0]);
                    tvScheme.setVisibility(View.VISIBLE);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void openTimePickerAndSchedule(Context ctx,
                                                  String type,
                                                  String schemeId,
                                                  String schemeName) {

        Calendar now = Calendar.getInstance();
        int initHour = 7; // default 7:00 AM
        int initMinute = 0;

        TimePickerDialog dialog = new TimePickerDialog(
                ctx,
                (view, hourOfDay, minute) -> {
                    // hourOfDay is 0–23 even in 12h mode; use directly
                    ReminderHelper.scheduleSchemeReminder(
                            ctx,
                            type,
                            schemeId,
                            hourOfDay,
                            minute
                    );
                    Toast.makeText(
                            ctx,
                            "Reminder set for " + schemeName,
                            Toast.LENGTH_SHORT
                    ).show();
                },
                initHour,
                initMinute,
                false  // 12-hour view with AM/PM
        );
        dialog.setTitle("Select Reminder Time");
        dialog.show();
    }
}
