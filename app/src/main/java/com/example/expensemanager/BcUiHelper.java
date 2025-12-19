package com.expensemanager;

import android.app.DatePickerDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.Gravity;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.graphics.Color;

import androidx.fragment.app.Fragment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import com.expensemanager.BcStore.BcScheme;

public class BcUiHelper {

    public interface OnBcAddedListener {
        void onBcAdded();
    }

    private static int dpToPx(Fragment fragment, int dp) {
        float density = fragment.requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // Show only Add BC and View BC List in popup
    public static void showBcMenu(Fragment fragment, View anchor) {
        android.widget.PopupMenu menu =
                new android.widget.PopupMenu(fragment.getContext(), anchor);
        menu.getMenu().add("Add BC");
        menu.getMenu().add("View BC List");
        menu.getMenu().add("Add EMI");
        menu.getMenu().add("View EMI List");

        menu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if ("Add BC".equals(title)) {
                showAddBcDialog(fragment, null);
            } else if ("View BC List".equals(title)) {
                showBcListDialog(fragment);
            } else if ("Add EMI".equals(title)) {
                EmiUiHelper.showAddEmiDialog(fragment, null);
            } else if ("View EMI List".equals(title)) {
                EmiUiHelper.showEmiListDialog(fragment);
            }
            return true;
        });
        menu.show();
    }

    // Dialog used when category = BC (select which BC scheme)
    public static void showSelectBcDialog(Fragment fragment,
                                          BcStore.OnBcSelectedListener listener) {
        Context ctx = fragment.requireContext();
        HashMap<String, ArrayList<BcScheme>> bcMap = BcStore.getBcMap();

        if (bcMap.isEmpty()) {
            Toast.makeText(ctx, "No BC schemes found", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> labels = new ArrayList<>();
        List<String> ids = new ArrayList<>();

        for (String key : bcMap.keySet()) {
            ArrayList<BcScheme> list = bcMap.get(key);
            if (list == null) continue;

            for (BcScheme s : list) {
                String label = ("_GLOBAL_".equals(key) || TextUtils.isEmpty(key))
                        ? s.name
                        : key + " - " + s.name;
                labels.add(label);
                ids.add(s.id); // key|name
            }
        }

        if (labels.isEmpty()) {
            Toast.makeText(ctx, "No BC schemes found", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = labels.toArray(new String[0]);
        new android.app.AlertDialog.Builder(ctx)
                .setTitle("Which BC belongs to?")
                .setItems(items, (d, which) -> {
                    String bcId = ids.get(which);
                    if (listener != null) {
                        listener.onBcSelected(bcId);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static void showAddBcDialog(Fragment fragment, OnBcAddedListener listener) {
        Context ctx = fragment.requireContext();

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(fragment, 16);
        root.setPadding(pad, pad, pad, pad);

        EditText etAccount = new EditText(ctx);
        etAccount.setHint("Account Name (optional)");
        root.addView(etAccount);

        EditText etBcName = new EditText(ctx);
        etBcName.setHint("BC Name");
        root.addView(etBcName);

        EditText etMonths = new EditText(ctx);
        etMonths.setHint("Months");
        etMonths.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        root.addView(etMonths);

        EditText etStartDate = new EditText(ctx);
        etStartDate.setHint("Start / Due Date (dd/MM/yyyy)");
        etStartDate.setFocusable(false);
        etStartDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog dp = new DatePickerDialog(
                    ctx,
                    (DatePicker view1, int year, int month, int dayOfMonth) -> {
                        String d = String.format(Locale.getDefault(),
                                "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                        etStartDate.setText(d);
                    },
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH));
            dp.show();
        });
        root.addView(etStartDate);

        TextView instLabel = new TextView(ctx);
        instLabel.setText("Installment Option");
        instLabel.setPadding(0, dpToPx(fragment, 8), 0, dpToPx(fragment, 4));
        root.addView(instLabel);

        LinearLayout instLayout = new LinearLayout(ctx);
        instLayout.setOrientation(LinearLayout.HORIZONTAL);
        instLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        int btnMargin = dpToPx(fragment, 4);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        btnParams.setMargins(btnMargin, btnMargin, btnMargin, btnMargin);

        Button btnFixed = new Button(ctx);
        btnFixed.setText("Fixed Amount");
        btnFixed.setAllCaps(false);
        btnFixed.setTextColor(Color.WHITE);
        btnFixed.setBackgroundResource(R.drawable.btn_installment_green);
        btnFixed.setLayoutParams(new LinearLayout.LayoutParams(btnParams));

        Button btnRandom = new Button(ctx);
        btnRandom.setText("Random Amount");
        btnRandom.setAllCaps(false);
        btnRandom.setTextColor(Color.WHITE);
        btnRandom.setBackgroundResource(R.drawable.btn_installment_green);
        btnRandom.setLayoutParams(new LinearLayout.LayoutParams(btnParams));

        instLayout.addView(btnRandom);
        instLayout.addView(btnFixed);
        root.addView(instLayout);

        final String[] instType = new String[]{"NONE"};
        final int[] fixedAmountHolder = new int[]{0};
        final List<Integer> randomAmountsHolder = new ArrayList<>();

        btnFixed.setOnClickListener(v2 -> {
            final EditText input = new EditText(ctx);
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            input.setHint("Installment amount");

            new android.app.AlertDialog.Builder(ctx)
                    .setTitle("Fixed Amount")
                    .setView(input)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("OK", (d, w) -> {
                        String amtStr = input.getText().toString().trim();
                        try {
                            int val = Integer.parseInt(amtStr);
                            instType[0] = "FIXED";
                            fixedAmountHolder[0] = val;
                            randomAmountsHolder.clear();
                            Toast.makeText(ctx, "Fixed installment set: ₹" + val,
                                    Toast.LENGTH_SHORT).show();
                        } catch (NumberFormatException e) {
                            Toast.makeText(ctx, "Invalid amount", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .show();
        });

        btnRandom.setOnClickListener(v2 -> {
            String monthsStr = etMonths.getText().toString().trim();
            int m;
            try {
                m = Integer.parseInt(monthsStr);
            } catch (NumberFormatException e) {
                Toast.makeText(ctx, "Enter Months first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (m <= 0) {
                Toast.makeText(ctx, "Months must be > 0", Toast.LENGTH_SHORT).show();
                return;
            }

            ScrollView scroll = new ScrollView(ctx);
            LinearLayout listLayout = new LinearLayout(ctx);
            listLayout.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(listLayout);

            EditText[] amountInputs = new EditText[m];
            for (int i = 0; i < m; i++) {
                EditText et = new EditText(ctx);
                et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                et.setHint((i + 1) + " installment amount");
                listLayout.addView(et);
                amountInputs[i] = et;
            }

            new android.app.AlertDialog.Builder(ctx)
                    .setTitle("Random Amount")
                    .setView(scroll)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("OK", (d, w) -> {
                        List<Integer> list = new ArrayList<>();
                        for (int i = 0; i < m; i++) {
                            String s = amountInputs[i].getText().toString().trim();
                            try {
                                list.add(Integer.parseInt(s));
                            } catch (NumberFormatException e) {
                                Toast.makeText(ctx,
                                        "Invalid amount at position " + (i + 1),
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        instType[0] = "RANDOM";
                        fixedAmountHolder[0] = 0;
                        randomAmountsHolder.clear();
                        randomAmountsHolder.addAll(list);
                        Toast.makeText(ctx, "Random installments set",
                                Toast.LENGTH_SHORT).show();
                    })
                    .show();
        });

        new android.app.AlertDialog.Builder(ctx)
                .setTitle("Add BC Scheme")
                .setView(root)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add", (d, w) -> {
                    String accountName = etAccount.getText().toString().trim();
                    String bcName = etBcName.getText().toString().trim();
                    String monthsStr = etMonths.getText().toString().trim();
                    String startDate = etStartDate.getText().toString().trim();

                    if (TextUtils.isEmpty(bcName) ||
                            TextUtils.isEmpty(monthsStr) ||
                            TextUtils.isEmpty(startDate)) {
                        Toast.makeText(ctx,
                                "BC Name, Months and Start Date are required",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int months;
                    try {
                        months = Integer.parseInt(monthsStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(ctx, "Invalid months", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    BcScheme scheme = new BcScheme();
                    scheme.name = bcName;
                    scheme.months = months;
                    scheme.startDate = startDate;
                    generateBcSchedule(scheme);

                    scheme.installmentType = instType[0];
                    if ("FIXED".equals(instType[0])) {
                        scheme.fixedAmount = fixedAmountHolder[0];
                        scheme.monthlyAmounts.clear();
                    } else if ("RANDOM".equals(instType[0])) {
                        scheme.fixedAmount = 0;
                        scheme.monthlyAmounts.clear();
                        scheme.monthlyAmounts.addAll(randomAmountsHolder);
                    } else {
                        scheme.fixedAmount = 0;
                        scheme.monthlyAmounts.clear();
                    }

                    String key = TextUtils.isEmpty(accountName) ? "_GLOBAL_" : accountName;
                    BcStore.addScheme(key, scheme);
                    BcStore.save(ctx);

                    if (listener != null) listener.onBcAdded();
                    Toast.makeText(ctx, "BC added", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    public static void generateBcSchedule(BcScheme scheme) {
        scheme.scheduleDates.clear();
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar c = Calendar.getInstance();
        try {
            c.setTime(df.parse(scheme.startDate));
        } catch (ParseException e) {
            return;
        }
        for (int i = 0; i < scheme.months; i++) {
            scheme.scheduleDates.add(df.format(c.getTime()));
            c.add(Calendar.MONTH, 1);
        }
    }

    public static void showBcListDialog(Fragment fragment) {
        Context ctx = fragment.requireContext();

        LinearLayout listLayout = new LinearLayout(ctx);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(fragment, 16);
        listLayout.setPadding(pad, pad, pad, pad);

        HashMap<String, ArrayList<BcScheme>> bcMap = BcStore.getBcMap();

        if (bcMap.isEmpty()) {
            TextView tv = new TextView(ctx);
            tv.setText("No BC schemes found");
            tv.setGravity(Gravity.CENTER);
            listLayout.addView(tv);
        } else {
            for (String key : bcMap.keySet()) {
                ArrayList<BcScheme> list = bcMap.get(key);
                if (list == null) continue;

                for (BcScheme scheme : list) {
                    Button btn = new Button(ctx);
                    btn.setText(scheme.name);
                    btn.setOnClickListener(v ->
                            showBcDetailsDialog(fragment, scheme)
                    );
                    listLayout.addView(btn);
                }
            }
        }

        ScrollView scroll = new ScrollView(ctx);
        scroll.addView(listLayout);

        new android.app.AlertDialog.Builder(ctx)
                .setTitle("BC List")
                .setView(scroll)
                .setPositiveButton("Close", null)
                .show();
    }

    public static void showBcDetailsDialog(Fragment fragment, BcScheme scheme) {
        Context ctx = fragment.requireContext();

        TableLayout table = new TableLayout(ctx);
        table.setStretchAllColumns(true);
        table.setShrinkAllColumns(true);
        table.setPadding(0, 0, 0, 0);

        int cellPad = dpToPx(fragment, 4);

        int headerBg = Color.parseColor("#928E85");
        int headerText = Color.BLACK;

        TableRow header = new TableRow(ctx);

        TextView hSr = createHeaderCell(ctx, "Sr.", cellPad);
        TextView hStatus = createHeaderCell(ctx, "Status", cellPad);
        TextView hDate = createHeaderCell(ctx, "Date", cellPad);
        TextView hAmt = createHeaderCell(ctx, "Amount", cellPad);

        hSr.setBackgroundColor(headerBg);
        hStatus.setBackgroundColor(headerBg);
        hDate.setBackgroundColor(headerBg);
        hAmt.setBackgroundColor(headerBg);

        hSr.setTextColor(headerText);
        hStatus.setTextColor(headerText);
        hDate.setTextColor(headerText);
        hAmt.setTextColor(headerText);

        header.addView(hSr);
        header.addView(hStatus);
        header.addView(hDate);
        header.addView(hAmt);
        table.addView(header);

        for (int i = 0; i < scheme.scheduleDates.size(); i++) {

            String date = scheme.scheduleDates.get(i);

            int amount = 0;
            if ("FIXED".equals(scheme.installmentType)) {
                amount = scheme.fixedAmount;
            } else if ("RANDOM".equals(scheme.installmentType)
                    && i < scheme.monthlyAmounts.size()) {
                amount = scheme.monthlyAmounts.get(i);
            }

            boolean done = i < scheme.paidCount;

            TableRow row = new TableRow(ctx);
            row.setPadding(0, 0, 0, 0);
            if (done) {
                row.setBackgroundResource(R.drawable.bg_row_paid);
            }

            TextView tvSr = createCell(ctx, String.valueOf(i + 1), cellPad, true);
            if (done) tvSr.setTextColor(Color.parseColor("#2E7D32"));
            row.addView(tvSr);

            TextView tvStatus = createCell(ctx, done ? "✅ " : "☐ ", cellPad, false);
            if (done) tvStatus.setTextColor(Color.parseColor("#2E7D32"));
            row.addView(tvStatus);

            TextView tvDate = createCell(ctx, date, cellPad, true);
            if (done) tvDate.setTextColor(Color.parseColor("#2E7D32"));
            row.addView(tvDate);

            TextView tvAmt = createCell(ctx, String.valueOf(amount), cellPad, true);
            if (done) tvAmt.setTextColor(Color.parseColor("#2E7D32"));
            row.addView(tvAmt);

            table.addView(row);
        }

        ScrollView scrollView = new ScrollView(ctx);
        scrollView.addView(table);
        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);

        int dialogPadding = dpToPx(fragment, 16);
        container.setPadding(dialogPadding, dialogPadding, dialogPadding, dialogPadding);
        container.setBackgroundResource(R.drawable.bg_table_container);

        container.addView(scrollView);

        new android.app.AlertDialog.Builder(ctx)
                .setTitle("BC: " + scheme.name)
                .setView(container)
                .setPositiveButton("Close", null)
                .show();
    }

    private static TextView createHeaderCell(Context ctx, String text, int pad) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(pad, pad, pad, pad);
        tv.setTextColor(Color.BLACK);
        tv.setBackgroundResource(R.drawable.table_cell_border);
        return tv;
    }

    private static TextView createCell(Context ctx, String text, int pad, boolean bold) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(pad, pad, pad, pad);
        tv.setTextColor(Color.BLACK);
        if (bold) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setBackgroundResource(R.drawable.table_cell_border);
        return tv;
    }
}
