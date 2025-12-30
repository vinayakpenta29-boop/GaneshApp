package com.expensemanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class EmiStore {

    // Callback used when user selects an EMI scheme
    public interface OnEmiSelectedListener {
        void onEmiSelected(String emiId);
    }

    // One EMI scheme
    public static class EmiScheme {
        public String name;
        public int months;
        public String startDate;              // dd/MM/yyyy
        public List<String> scheduleDates = new ArrayList<>();

        // Unique id: key + "|" + name
        public String id = "";

        // How many installments are paid
        public int paidCount = 0;

        // Installment type and amounts (same idea as BC)
        public String installmentType = "NONE";  // FIXED / RANDOM / NONE
        public int fixedAmount = 0;
        public List<Integer> monthlyAmounts = new ArrayList<>();

        // which tab owns this scheme: "INCOME" or "EXPENSE"
        public String ownerTab = "EXPENSE";

        // Per‑installment flags to track which dates are paid
        public List<Boolean> paidFlags = new ArrayList<>();

        // NEW: whether reminder is enabled for this scheme
        public boolean reminderEnabled = false;
    }

    private static final String PREFS_NAME = "ExpenseManagerPrefs";
    private static final String EMI_KEY = "emi_data_json";

    // key = account name or "_GLOBAL_"
    private static final HashMap<String, ArrayList<EmiScheme>> emiMap = new HashMap<>();

    public static HashMap<String, ArrayList<EmiScheme>> getEmiMap() {
        return emiMap;
    }

    /** Get all EMI schemes as a flat list (used by delete dialog) */
    public static List<EmiScheme> getAllSchemes() {
        List<EmiScheme> all = new ArrayList<>();
        for (String key : emiMap.keySet()) {
            ArrayList<EmiScheme> list = emiMap.get(key);
            if (list != null) {
                all.addAll(list);
            }
        }
        return all;
    }

    // get schemes only for a given owner tab ("INCOME" or "EXPENSE")
    public static List<EmiScheme> getSchemesForOwner(String ownerTab) {
        List<EmiScheme> result = new ArrayList<>();
        if (TextUtils.isEmpty(ownerTab)) return result;
        for (String key : emiMap.keySet()) {
            ArrayList<EmiScheme> list = emiMap.get(key);
            if (list == null) continue;
            for (EmiScheme s : list) {
                if (ownerTab.equals(s.ownerTab)) {
                    result.add(s);
                }
            }
        }
        return result;
    }

    public static void addScheme(String key, EmiScheme scheme) {
        ArrayList<EmiScheme> list = emiMap.get(key);
        if (list == null) {
            list = new ArrayList<>();
            emiMap.put(key, list);
        }
        if (TextUtils.isEmpty(scheme.id)) {
            scheme.id = key + "|" + scheme.name;
        }
        // ensure paidFlags matches schedule size
        if (scheme.paidFlags == null) {
            scheme.paidFlags = new ArrayList<>();
        }
        while (scheme.paidFlags.size() < scheme.scheduleDates.size()) {
            scheme.paidFlags.add(false);
        }
        // ownerTab must be set by caller (IncomeFragment / ExpensesFragment)
        list.add(scheme);
    }

    public static void removeScheme(String key, EmiScheme scheme) {
        if (scheme == null) return;
        ArrayList<EmiScheme> list = emiMap.get(key);
        if (list != null) {
            list.remove(scheme);
            if (list.isEmpty()) {
                emiMap.remove(key);
            }
        }
    }

    public static void removeSchemeById(String emiId) {
        if (TextUtils.isEmpty(emiId)) return;
        for (String key : new ArrayList<>(emiMap.keySet())) {
            ArrayList<EmiScheme> list = emiMap.get(key);
            if (list == null) continue;
            for (int i = list.size() - 1; i >= 0; i--) {
                EmiScheme s = list.get(i);
                if (emiId.equals(s.id)) {
                    list.remove(i);
                }
            }
            if (list.isEmpty()) {
                emiMap.remove(key);
            }
        }
    }

    // NEW: find a scheme anywhere in the map by its id
    public static EmiScheme findSchemeById(String emiId) {
        if (TextUtils.isEmpty(emiId)) return null;
        for (String key : emiMap.keySet()) {
            ArrayList<EmiScheme> list = emiMap.get(key);
            if (list == null) continue;
            for (EmiScheme s : list) {
                if (emiId.equals(s.id)) {
                    return s;
                }
            }
        }
        return null;
    }

    /**
     * Mark one EMI installment done based on month/year from the entry.
     * month and year are strings from the Income/Expenses EditTexts.
     */
    public static void markEmiInstallmentDone(String emiId, String month, String year) {
        if (TextUtils.isEmpty(emiId) || TextUtils.isEmpty(month) || TextUtils.isEmpty(year)) return;
        EmiScheme s = findSchemeById(emiId);
        if (s == null) return;

        if (s.paidFlags == null) {
            s.paidFlags = new ArrayList<>();
        }
        while (s.paidFlags.size() < s.scheduleDates.size()) {
            s.paidFlags.add(false);
        }

        for (int i = 0; i < s.scheduleDates.size(); i++) {
            String d = s.scheduleDates.get(i);
            if (matchesMonthYear(d, month, year) && !s.paidFlags.get(i)) {
                s.paidFlags.set(i, true);
                if (s.paidCount < s.months) {
                    s.paidCount++;
                }
                break;
            }
        }
    }

    /**
     * Used when deleting an entry: unmark installment whose date has same month/year.
     */
    public static void unmarkEmiInstallment(String emiId, String month, String year) {
        if (TextUtils.isEmpty(emiId) || TextUtils.isEmpty(month) || TextUtils.isEmpty(year)) return;
        EmiScheme s = findSchemeById(emiId);
        if (s == null) return;

        if (s.paidFlags == null) {
            s.paidFlags = new ArrayList<>();
        }
        while (s.paidFlags.size() < s.scheduleDates.size()) {
            s.paidFlags.add(false);
        }

        for (int i = 0; i < s.scheduleDates.size(); i++) {
            String d = s.scheduleDates.get(i);
            if (matchesMonthYear(d, month, year) && s.paidFlags.get(i)) {
                s.paidFlags.set(i, false);
                if (s.paidCount > 0) {
                    s.paidCount--;
                }
                break;
            }
        }
    }

    /**
     * Helper: check if stored date string has same month/year as user input.
     * Assumes scheduleDates stored as "dd-MM-yyyy" or "dd/MM/yyyy".
     * Works for both "2" and "02" entered in the Month box.
     */
    private static boolean matchesMonthYear(String dateStr, String month, String year) {
        if (TextUtils.isEmpty(dateStr) || TextUtils.isEmpty(month) || TextUtils.isEmpty(year)) {
            return false;
        }
        String[] parts = dateStr.split("[-/]");
        if (parts.length < 3) return false;

        try {
            int mScheme = Integer.parseInt(parts[1]); // "12" -> 12, "02" -> 2
            int yScheme = Integer.parseInt(parts[2]);
            int mInput = Integer.parseInt(month);
            int yInput = Integer.parseInt(year);
            return mScheme == mInput && yScheme == yInput;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void save(Context context) {
        JSONObject root = new JSONObject();
        try {
            for (String key : emiMap.keySet()) {
                JSONArray arr = new JSONArray();
                ArrayList<EmiScheme> list = emiMap.get(key);
                if (list == null) continue;
                for (EmiScheme s : list) {
                    JSONObject o = new JSONObject();
                    o.put("name", s.name);
                    o.put("months", s.months);
                    o.put("startDate", s.startDate);

                    if (TextUtils.isEmpty(s.id)) {
                        s.id = key + "|" + s.name;
                    }
                    o.put("id", s.id);

                    JSONArray dates = new JSONArray();
                    for (String d : s.scheduleDates) {
                        dates.put(d);
                    }
                    o.put("schedule", dates);

                    o.put("paidCount", s.paidCount);
                    o.put("installmentType", s.installmentType);
                    o.put("fixedAmount", s.fixedAmount);

                    JSONArray amts = new JSONArray();
                    for (int a : s.monthlyAmounts) {
                        amts.put(a);
                    }
                    o.put("monthlyAmounts", amts);

                    // persist owner tab
                    o.put("ownerTab", s.ownerTab);

                    // persist paidFlags
                    JSONArray flags = new JSONArray();
                    if (s.paidFlags != null) {
                        for (boolean f : s.paidFlags) {
                            flags.put(f);
                        }
                    }
                    o.put("paidFlags", flags);

                    // NEW: save reminder flag
                    o.put("reminderEnabled", s.reminderEnabled);

                    arr.put(o);
                }
                root.put(key, arr);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(EMI_KEY, root.toString()).apply();
    }

    public static void load(Context context) {
        emiMap.clear();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(EMI_KEY, "");
        if (TextUtils.isEmpty(json)) return;

        try {
            JSONObject root = new JSONObject(json);
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONArray arr = root.getJSONArray(key);
                ArrayList<EmiScheme> list = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    EmiScheme s = new EmiScheme();
                    s.name = o.optString("name");
                    s.months = o.optInt("months");
                    s.startDate = o.optString("startDate");
                    s.id = o.optString("id", key + "|" + s.name);

                    s.scheduleDates = new ArrayList<>();
                    JSONArray dates = o.optJSONArray("schedule");
                    if (dates != null) {
                        for (int j = 0; j < dates.length(); j++) {
                            s.scheduleDates.add(dates.getString(j));
                        }
                    }

                    s.paidCount = o.optInt("paidCount", 0);
                    s.installmentType = o.optString("installmentType", "NONE");
                    s.fixedAmount = o.optInt("fixedAmount", 0);
                    s.monthlyAmounts = new ArrayList<>();
                    JSONArray amts = o.optJSONArray("monthlyAmounts");
                    if (amts != null) {
                        for (int j = 0; j < amts.length(); j++) {
                            s.monthlyAmounts.add(amts.getInt(j));
                        }
                    }

                    // load owner tab (default EXPENSE for old data)
                    s.ownerTab = o.optString("ownerTab", "EXPENSE");

                    // load paidFlags if present; otherwise infer from paidCount
                    s.paidFlags = new ArrayList<>();
                    JSONArray flags = o.optJSONArray("paidFlags");
                    if (flags != null) {
                        for (int j = 0; j < flags.length(); j++) {
                            s.paidFlags.add(flags.getBoolean(j));
                        }
                    } else {
                        for (int j = 0; j < s.scheduleDates.size(); j++) {
                            s.paidFlags.add(j < s.paidCount);
                        }
                    }

                    // NEW: load reminder flag (default false for old data)
                    s.reminderEnabled = o.optBoolean("reminderEnabled", false);

                    list.add(s);
                }
                emiMap.put(key, list);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
