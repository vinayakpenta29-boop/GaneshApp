package com.expensemanager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

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

        // Customize X-axis labels for months
        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{
            "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        }));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(12);

        chart.getDescription().setEnabled(false);
        chart.animateY(1000);
        chart.invalidate();

        return view;
    }
}
