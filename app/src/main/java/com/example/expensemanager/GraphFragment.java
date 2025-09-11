package com.expensemanager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import java.util.ArrayList;

public class GraphFragment extends Fragment {
    private DatabaseHelper db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph, container, false);
        BarChart chart = view.findViewById(R.id.monthly_chart);

        db = new DatabaseHelper(getContext());
        ArrayList<BarEntry> entries = db.getMonthlyBarEntries();

        BarDataSet set = new BarDataSet(entries, "Income/Expense");
        BarData data = new BarData(set);

        chart.setData(data);
        chart.invalidate();

        return view;
    }
}
