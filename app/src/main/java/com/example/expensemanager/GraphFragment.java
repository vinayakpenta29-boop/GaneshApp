package com.expensemanager;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.TextView;

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
    private Spinner spinnerYear;
    private String selectedYear = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph, container, false);
        chart = view.findViewById(R.id.monthly_chart);
        spinnerYear = view.findViewById(R.id.spinner_year);

        db = new DatabaseHelper(getContext());
        setupYearSpinner();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        setupYearSpinner();
    }

    private void setupYearSpinner() {
        List<String> years = db.getAllYears();
        if (years.isEmpty()) {
            years.add(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
            requireContext(), // use requireContext() for Fragment
            android.R.layout.simple_spinner_item,
            years
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                if (v instanceof TextView) ((TextView) v).setTextColor(Color.BLACK); // force black text
                return v;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                if (v instanceof TextView) ((TextView) v).setTextColor(Color.BLACK); // force black text
                return v;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(adapter);

        // Properly restore selection
        int selIndex = years.contains(selectedYear) ? years.indexOf(selectedYear) : years.size() - 1;
        spinnerYear.setSelection(selIndex, false);

        spinnerYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                selectedYear = years.get(position);
                updateChartForYear(selectedYear);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Initial chart update (in case setOnItemSelectedListener not triggered)
        selectedYear = years.get(spinnerYear.getSelectedItemPosition());
        updateChartForYear(selectedYear);
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
