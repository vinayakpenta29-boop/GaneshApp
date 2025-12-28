package com.expensemanager;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows a list of income/expense entries with checkboxes and deletes selected ones.
 */
public class EntryDeleteHelper {

    public static void showDeleteEntriesDialog(Fragment fragment) {
        Context ctx = fragment.requireContext();
        DatabaseHelper db = new DatabaseHelper(ctx);

        // Load all transactions (you can filter by month/year if you want)
        List<Transaction> all = db.getAllTransactions();

        if (all.isEmpty()) {
            Toast.makeText(ctx, "No entries to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build display strings like "2025-12-27  |  EXPENSE  |  Note"
        List<String> labels = new ArrayList<>();
        for (Transaction t : all) {
            String label = t.year + "-" + t.month + "  |  " + t.type.toUpperCase()
                    + "  |  " + (t.note == null ? "" : t.note);
            labels.add(label);
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(ctx, android.R.layout.simple_list_item_multiple_choice, labels);

        android.view.View root = android.view.LayoutInflater.from(ctx)
                .inflate(R.layout.dialog_delete_entries, null, false);
        ListView lv = root.findViewById(R.id.lv_entries);
        lv.setAdapter(adapter);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        new AlertDialog.Builder(ctx)
                .setTitle("Delete entries")
                .setView(root)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, w) -> {
                    // Collect checked positions
                    android.util.SparseBooleanArray checked = lv.getCheckedItemPositions();
                    List<Long> idsToDelete = new ArrayList<>();

                    for (int i = 0; i < all.size(); i++) {
                        if (checked.get(i)) {
                            Transaction t = all.get(i);
                            idsToDelete.add(t.id);

                            // If this entry is for BC / EMI, roll back corresponding installment
                            if ("BC".equalsIgnoreCase(t.category) && t.note != null) {
                                String schemeId = extractSchemeIdFromNote(t.note);
                                if (schemeId != null) {
                                    BcStore.unmarkBcInstallment(schemeId, t.month, t.year);
                                }
                            } else if ("EMI".equalsIgnoreCase(t.category) && t.note != null) {
                                String schemeId = extractSchemeIdFromNote(t.note);
                                if (schemeId != null) {
                                    EmiStore.unmarkEmiInstallment(schemeId, t.month, t.year);
                                }
                            }
                        }
                    }

                    if (idsToDelete.isEmpty()) {
                        Toast.makeText(ctx, "No entries selected", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Persist updated BC / EMI state
                    BcStore.save(ctx);
                    EmiStore.save(ctx);

                    // Delete from DB
                    for (Long id : idsToDelete) {
                        db.deleteTransactionById(id);
                    }

                    Toast.makeText(ctx,
                            "Deleted " + idsToDelete.size() + " entries",
                            Toast.LENGTH_SHORT).show();

                    // Ask Income/Expenses fragments to refresh if you use FragmentResult as before
                    androidx.fragment.app.FragmentManager fm = fragment.getParentFragmentManager();
                    android.os.Bundle b = new android.os.Bundle();
                    b.putBoolean("refresh", true);
                    fm.setFragmentResult("refresh_summary", b);
                })
                .show();
    }

    /**
     * Note format for BC/EMI entries is:
     *   schemeId + "||" + userNote
     * This helper returns the schemeId part, or null if not present.
     */
    private static String extractSchemeIdFromNote(String note) {
        if (note == null) return null;
        int idx = note.indexOf("||");
        if (idx <= 0) return null;           // no delimiter or empty id
        return note.substring(0, idx);
    }
}
