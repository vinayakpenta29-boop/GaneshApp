package com.expensemanager;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class SchemeDeleteHelper {

    /**
     * Entry point called from IncomeFragment / ExpensesFragment
     * when user taps the "Delete" item in the three dots menu.
     */
    public static void showDeleteDialog(Fragment fragment) {
        Context context = fragment.requireContext();

        String[] types = {"BC", "EMI"};
        final int[] checkedIndex = {0};

        new AlertDialog.Builder(context)
                .setTitle("Delete schemes")
                .setSingleChoiceItems(types, 0, (dialog, which) -> checkedIndex[0] = which)
                .setPositiveButton("Next", (dialog, which) -> {
                    dialog.dismiss();
                    String type = types[checkedIndex[0]];
                    if ("BC".equals(type)) {
                        showMultiDeleteDialogForBc(fragment);
                    } else {
                        showMultiDeleteDialogForEmi(fragment);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Second dialog for BC: multi‑choice list of all BC schemes.
     * Selected schemes are deleted on "Delete".
     */
    private static void showMultiDeleteDialogForBc(Fragment fragment) {
        Context context = fragment.requireContext();

        // Get all BC schemes (flat list)
        List<BcStore.BcScheme> all = BcStore.getAllSchemes();
        if (all == null || all.isEmpty()) {
            Toast.makeText(context, "No BC schemes to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[all.size()];
        boolean[] checked = new boolean[all.size()];

        for (int i = 0; i < all.size(); i++) {
            BcStore.BcScheme s = all.get(i);
            // Show name; include id if you want them unique on UI
            names[i] = s.name;
            checked[i] = false;
        }

        new AlertDialog.Builder(context)
                .setTitle("Select BC schemes")
                .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> {
                    checked[which] = isChecked;
                })
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Collect ids to delete
                    List<String> idsToDelete = new ArrayList<>();
                    for (int i = 0; i < all.size(); i++) {
                        if (checked[i]) {
                            BcStore.BcScheme s = all.get(i);
                            if (s.id != null) {
                                idsToDelete.add(s.id);
                            }
                        }
                    }

                    if (idsToDelete.isEmpty()) {
                        Toast.makeText(context, "No BC scheme selected", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Remove selected schemes by id
                    for (String id : idsToDelete) {
                        BcStore.removeSchemeById(id);
                    }
                    BcStore.save(context);

                    Toast.makeText(context, "Selected BC schemes deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Second dialog for EMI: multi‑choice list of all EMI schemes.
     * Requires EmiStore to have a similar API to BcStore.
     */
    private static void showMultiDeleteDialogForEmi(Fragment fragment) {
        Context context = fragment.requireContext();

        // You must implement these in EmiStore similar to BcStore:
        //   public static List<EmiScheme> getAllSchemes()
        //   public static void removeSchemeById(String emiId)
        List<EmiStore.EmiScheme> all = EmiStore.getAllSchemes();
        if (all == null || all.isEmpty()) {
            Toast.makeText(context, "No EMI schemes to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[all.size()];
        boolean[] checked = new boolean[all.size()];

        for (int i = 0; i < all.size(); i++) {
            EmiStore.EmiScheme s = all.get(i);
            names[i] = s.name;
            checked[i] = false;
        }

        new AlertDialog.Builder(context)
                .setTitle("Select EMI schemes")
                .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> {
                    checked[which] = isChecked;
                })
                .setPositiveButton("Delete", (dialog, which) -> {
                    List<String> idsToDelete = new ArrayList<>();
                    for (int i = 0; i < all.size(); i++) {
                        if (checked[i]) {
                            EmiStore.EmiScheme s = all.get(i);
                            if (s.id != null) {
                                idsToDelete.add(s.id);
                            }
                        }
                    }

                    if (idsToDelete.isEmpty()) {
                        Toast.makeText(context, "No EMI scheme selected", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (String id : idsToDelete) {
                        EmiStore.removeSchemeById(id);
                    }
                    EmiStore.save(context);

                    Toast.makeText(context, "Selected EMI schemes deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
