package com.expensemanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
    private List<Transaction> items;

    public TransactionAdapter(List<Transaction> items) {
        this.items = items;
    }

    public void setItems(List<Transaction> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Transaction item = items.get(position);
        holder.tvAmount.setText(String.valueOf(item.amount));
        holder.tvNote.setText(item.note);
        holder.tvDate.setText(item.date);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAmount, tvNote, tvDate;

        ViewHolder(View itemView) {
            super(itemView);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvNote = itemView.findViewById(R.id.tv_note);
            tvDate = itemView.findViewById(R.id.tv_date);
        }
    }
}
