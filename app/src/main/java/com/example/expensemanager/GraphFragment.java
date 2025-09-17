package com.expensemanager;

import android.graphics.Color;
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
import java.util.Calendar;
import java.util.List;

public class GraphFragment extends Fragment {
    private DatabaseHelper db;
    private BarChart chart;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph, container, false);
        chart = view.findViewById(R.id.monthly_chart);

        db = new DatabaseHelper(getContext());
        updateChartForLatestYear();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateChartForLatestYear();
    }

    private void updateChartForLatestYear() {
        List<String> years = db.getAllYears();
        String yearToShow = years.isEmpty() ?
                String.valueOf(Calendar.getInstance().get(Calendar.YEAR)) :
                years.get(years.size() - 1);
        updateChartForYear(yearToShow);
    }

    private void updateChartForYear(String year) {
        ArrayList<BarEntry> incomeEntries = new ArrayList<>();
        ArrayList<BarEntry> expenseEntries = new ArrayList<>();
        ArrayList<String> monthLabels = new ArrayList<>();

        db.getGroupedMonthlyEntries(incomeEntries, expenseEntries, monthLabels, year);

        BarDataSet incomeSet = new BarDataSet(incomeEntries, "Income");
        incomeSet.setColor(Color.GREEN);

        BarDataSet expenseSet = new BarDataSet(expenseEntries, "Expense");
        expenseSet.setColor(Color.RED);

        BarData barData = new BarData(incomeSet, expenseSet);
        barData.setBarWidth(0.3f);

        chart.setData(barData);
        chart.getXAxis().setAxisMinimum(0);
        chart.groupBars(0, 0.2f, 0.05f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(monthLabels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(monthLabels.size());

        chart.getDescription().setEnabled(false);
        chart.animateY(1000);
        chart.invalidate();
    }
}
