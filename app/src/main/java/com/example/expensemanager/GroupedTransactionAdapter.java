package com.expensemanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GroupedTransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_ITEM = 1;

    private String selectedYear = "";
    private List<Object> items = new ArrayList<>();

    public void setSelectedYear(String year) {
        this.selectedYear = year;
        notifyDataSetChanged();
    }

    public void setData(Map<String, List<Transaction>> grouped) {
        items.clear();
        for (String month : grouped.keySet()) {
            items.add(month); // Header (month name)
            items.addAll(grouped.get(month)); // Transactions
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_month_header, parent, false);
            return new HeaderViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
            return new TransactionViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            String month = (String) items.get(position);
            ((HeaderViewHolder) holder).tvMonthHeader.setText(month + "-" + selectedYear);
        } else if (holder instanceof TransactionViewHolder) {
            Transaction item = (Transaction) items.get(position);
            ((TransactionViewHolder) holder).tvAmount.setText(String.valueOf(item.amount));
            ((TransactionViewHolder) holder).tvNote.setText(item.note);
            ((TransactionViewHolder) holder).tvDate.setText(item.date);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvMonthHeader;
        HeaderViewHolder(View itemView) {
            super(itemView);
            tvMonthHeader = itemView.findViewById(R.id.tv_month_header);
        }
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvAmount, tvNote, tvDate;
        TransactionViewHolder(View itemView) {
            super(itemView);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvNote = itemView.findViewById(R.id.tv_note);
            tvDate = itemView.findViewById(R.id.tv_date);
        }
    }
}
