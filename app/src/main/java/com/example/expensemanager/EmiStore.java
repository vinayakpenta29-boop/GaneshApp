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
    }

    private static final String PREFS_NAME = "ExpenseManagerPrefs";
    private static final String EMI_KEY = "emi_data_json";

    // key = account name or "_GLOBAL_"
    private static final HashMap<String, ArrayList<EmiScheme>> emiMap = new HashMap<>();

    public static HashMap<String, ArrayList<EmiScheme>> getEmiMap() {
        return emiMap;
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

    // Increase paidCount so next EMI checkbox is ticked
    public static void markEmiInstallmentDone(String emiId, String unusedDate) {
        if (TextUtils.isEmpty(emiId)) return;
        for (String key : emiMap.keySet()) {
            ArrayList<EmiScheme> list = emiMap.get(key);
            if (list == null) continue;
            for (EmiScheme s : list) {
                if (emiId.equals(s.id)) {
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

                    list.add(s);
                }
                emiMap.put(key, list);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
