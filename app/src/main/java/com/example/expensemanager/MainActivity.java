package com.expensemanager;

import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set the status bar color to match your purple color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.purple_500)); // Use your color resource
        }

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        ViewPager viewPager = findViewById(R.id.view_pager);
        TabAdapter adapter = new TabAdapter(getSupportFragmentManager());
        adapter.addFragment(new IncomeFragment(), "Income");
        adapter.addFragment(new ExpensesFragment(), "Expenses");
        adapter.addFragment(new SummaryFragment(), "Summary");
        adapter.addFragment(new GraphFragment(), "Graph");
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
    }
}
