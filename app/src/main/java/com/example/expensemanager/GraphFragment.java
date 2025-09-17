package com.expensemanager;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;

public class GraphFragment extends Fragment {
    private DatabaseHelper db;
    private LinearLayout chartContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph, container, false);
        chartContainer = view.findViewById(R.id.chart_container);
        db = new DatabaseHelper(getContext());
        displayAllYearCharts();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        displayAllYearCharts();
    }

    private void displayAllYearCharts() {
        chartContainer.removeAllViews();

        List<String> years = db.getAllYears();
        if (years.isEmpty()) {
            TextView noData = new TextView(getContext());
            noData.setText("No transactions to display");
            noData.setTextSize(16);
            noData.setGravity(Gravity.CENTER);
            noData.setTextColor(Color.parseColor("#111111"));
            noData.setPadding(0, 32, 0, 32);
            chartContainer.addView(noData);
            return;
        }

        for (int i = 0; i < years.size(); i++) {
            String year = years.get(i);

            // Year heading
            TextView yearLabel = new TextView(getContext());
            yearLabel.setText(year);
            yearLabel.setTextSize(22);
            yearLabel.setTextColor(Color.BLACK);
            yearLabel.setGravity(Gravity.CENTER);
            yearLabel.setTypeface(yearLabel.getTypeface(), android.graphics.Typeface.BOLD);
            yearLabel.setPadding(0, 32, 0, 16);
            chartContainer.addView(yearLabel);

            // Chart for this year
            BarChart chart = new BarChart(getContext());
            LinearLayout.LayoutParams chartParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                600 // Height in pixels (adjust as needed)
            );
            chart.setLayoutParams(chartParams);
            setupChartForYear(chart, year);
            chartContainer.addView(chart);

            // Divider between years
            if (i < years.size() - 1) {
                View divider = new View(getContext());
                LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    4 // Thickness in px (adjust as needed)
                );
                dividerParams.setMargins(0, 32, 0, 32);
                divider.setLayoutParams(dividerParams);
                divider.setBackgroundColor(Color.BLACK);
                chartContainer.addView(divider);
            }
        }
    }

    private void setupChartForYear(BarChart chart, String year) {
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
