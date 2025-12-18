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
    }

    private static final String PREFS_NAME = "ExpenseManagerPrefs";
    private static final String BC_KEY = "bc_data_json";
    private static final HashMap<String, ArrayList<BcScheme>> bcMap = new HashMap<>();

    public static HashMap<String, ArrayList<BcScheme>> getBcMap() {
        return bcMap;
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

    // Increase paidCount so next checkbox is ticked in View BC List
    public static void markBcInstallmentDone(String bcId, String unusedDate) {
        if (TextUtils.isEmpty(bcId)) return;
        for (String key : bcMap.keySet()) {
            ArrayList<BcScheme> list = bcMap.get(key);
            if (list == null) continue;
            for (BcScheme s : list) {
                if (bcId.equals(s.id)) {
                    if (s.paidCount < s.months) {
                        s.paidCount++;
                    }
                    return;
                }
            }
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

                    list.add(s);
                }
                bcMap.put(key, list);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
