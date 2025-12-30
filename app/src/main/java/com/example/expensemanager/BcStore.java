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

public class BcStore {

    // Callback used when user selects a BC scheme from a dialog
    public interface OnBcSelectedListener {
        void onBcSelected(String bcId);
    }

    public static class BcScheme {
        public String name;
        public int months;
        public String startDate;
        public List<String> scheduleDates = new ArrayList<>();
        public String id = "";
        public int paidCount = 0;
        public String installmentType = "NONE";
        public int fixedAmount = 0;
        public List<Integer> monthlyAmounts = new ArrayList<>();

        // which tab owns this scheme: "INCOME" or "EXPENSE"
        public String ownerTab = "INCOME";

        // optional: flags per installment if you already use them elsewhere
        public List<Boolean> paidFlags = new ArrayList<>();

        // NEW: whether reminder is enabled for this scheme
        public boolean reminderEnabled = false;
    }

    private static final String PREFS_NAME = "ExpenseManagerPrefs";
    private static final String BC_KEY = "bc_data_json";
    private static final HashMap<String, ArrayList<BcScheme>> bcMap = new HashMap<>();

    public static HashMap<String, ArrayList<BcScheme>> getBcMap() {
        return bcMap;
    }

    /** Get all BC schemes as a flat list (used by delete dialog) */
    public static List<BcScheme> getAllSchemes() {
        List<BcScheme> all = new ArrayList<>();
        for (String key : bcMap.keySet()) {
            ArrayList<BcScheme> list = bcMap.get(key);
            if (list != null) {
                all.addAll(list);
            }
        }
        return all;
    }

    // Get schemes only for a given owner tab ("INCOME" or "EXPENSE")
    public static List<BcScheme> getSchemesForOwner(String ownerTab) {
        List<BcScheme> result = new ArrayList<>();
        if (TextUtils.isEmpty(ownerTab)) return result;
        for (String key : bcMap.keySet()) {
            ArrayList<BcScheme> list = bcMap.get(key);
            if (list == null) continue;
            for (BcScheme s : list) {
                if (ownerTab.equals(s.ownerTab)) {
                    result.add(s);
                }
            }
        }
        return result;
    }

    public static void addScheme(String key, BcScheme scheme) {
        ArrayList<BcScheme> list = bcMap.get(key);
        if (list == null) {
            list = new ArrayList<>();
            bcMap.put(key, list);
        }
        if (TextUtils.isEmpty(scheme.id)) {
            scheme.id = key + "|" + scheme.name;
        }
        // ensure paidFlags list matches schedule size
        if (scheme.paidFlags == null) {
            scheme.paidFlags = new ArrayList<>();
        }
        while (scheme.paidFlags.size() < scheme.scheduleDates.size()) {
            scheme.paidFlags.add(false);
        }
        // ownerTab must be set by caller (IncomeFragment / ExpensesFragment)
        list.add(scheme);
    }

    public static void removeScheme(String key, BcScheme scheme) {
        if (scheme == null) return;
        ArrayList<BcScheme> list = bcMap.get(key);
        if (list != null) {
            list.remove(scheme);
            if (list.isEmpty()) {
                bcMap.remove(key);
            }
        }
    }

    /** Remove by id anywhere in map */
    public static void removeSchemeById(String bcId) {
        if (TextUtils.isEmpty(bcId)) return;
        for (String key : new ArrayList<>(bcMap.keySet())) {
            ArrayList<BcScheme> list = bcMap.get(key);
            if (list == null) continue;
            for (int i = list.size() - 1; i >= 0; i--) {
                BcScheme s = list.get(i);
                if (bcId.equals(s.id)) {
                    list.remove(i);
                }
            }
            if (list.isEmpty()) {
                bcMap.remove(key);
            }
        }
    }

    // find a scheme anywhere in the map by its id
    public static BcScheme findSchemeById(String bcId) {
        if (TextUtils.isEmpty(bcId)) return null;
        for (String key : bcMap.keySet()) {
            ArrayList<BcScheme> list = bcMap.get(key);
            if (list == null) continue;
            for (BcScheme s : list) {
                if (bcId.equals(s.id)) {
                    return s;
                }
            }
        }
        return null;
    }

    /**
     * Mark one installment done based on month/year.
     * month, year come from Income/Expenses EditTexts (both as strings).
     */
    public static void markBcInstallmentDone(String bcId, String month, String year) {
        if (TextUtils.isEmpty(bcId) || TextUtils.isEmpty(month) || TextUtils.isEmpty(year)) return;
        BcScheme s = findSchemeById(bcId);
        if (s == null) return;

        // ensure paidFlags list is initialized and aligned with scheduleDates
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
     * Used when deleting an entry: unmark the installment whose date has same month/year.
     */
    public static void unmarkBcInstallment(String bcId, String month, String year) {
        if (TextUtils.isEmpty(bcId) || TextUtils.isEmpty(month) || TextUtils.isEmpty(year)) return;
        BcScheme s = findSchemeById(bcId);
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
            for (String key : bcMap.keySet()) {
                JSONArray arr = new JSONArray();
                ArrayList<BcScheme> list = bcMap.get(key);
                if (list == null) continue;
                for (BcScheme s : list) {
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

                    // persist paidFlags to keep checkbox state
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
        prefs.edit().putString(BC_KEY, root.toString()).apply();
    }

    public static void load(Context context) {
        bcMap.clear();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(BC_KEY, "");
        if (TextUtils.isEmpty(json)) return;

        try {
            JSONObject root = new JSONObject(json);
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONArray arr = root.getJSONArray(key);
                ArrayList<BcScheme> list = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    BcScheme s = new BcScheme();
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

                    // load owner tab (default INCOME for old data)
                    s.ownerTab = o.optString("ownerTab", "INCOME");

                    // load paidFlags if present; otherwise initialize from paidCount
                    s.paidFlags = new ArrayList<>();
                    JSONArray flags = o.optJSONArray("paidFlags");
                    if (flags != null) {
                        for (int j = 0; j < flags.length(); j++) {
                            s.paidFlags.add(flags.getBoolean(j));
                        }
                    } else {
                        // backward compatibility: mark first paidCount installments as true
                        for (int j = 0; j < s.scheduleDates.size(); j++) {
                            s.paidFlags.add(j < s.paidCount);
                        }
                    }

                    // NEW: load reminder flag (default false for old data)
                    s.reminderEnabled = o.optBoolean("reminderEnabled", false);

                    list.add(s);
                }
                bcMap.put(key, list);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
