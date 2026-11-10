package com.qrify.app;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {

    public static final int TAB_HOME = 0;
    public static final int TAB_MYQR = 1;
    public static final int TAB_SCAN = 2;
    public static final int TAB_HISTORY = 3;
    public static final int TAB_SETTINGS = 4;

    private static final String STATE_TAB = "tab";

    private int current = -1;
    private ImageView[] icons;
    private TextView[] labels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        icons = new ImageView[]{
                findViewById(R.id.tab_home_icon), findViewById(R.id.tab_myqr_icon), null,
                findViewById(R.id.tab_history_icon), findViewById(R.id.tab_settings_icon)};
        labels = new TextView[]{
                findViewById(R.id.tab_home_label), findViewById(R.id.tab_myqr_label), null,
                findViewById(R.id.tab_history_label), findViewById(R.id.tab_settings_label)};

        findViewById(R.id.tab_home).setOnClickListener(v -> select(TAB_HOME));
        findViewById(R.id.tab_myqr).setOnClickListener(v -> select(TAB_MYQR));
        findViewById(R.id.tab_scan).setOnClickListener(v -> select(TAB_SCAN));
        findViewById(R.id.tab_history).setOnClickListener(v -> select(TAB_HISTORY));
        findViewById(R.id.tab_settings).setOnClickListener(v -> select(TAB_SETTINGS));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (current != TAB_HOME) {
                    select(TAB_HOME);
                } else {
                    finish();
                }
            }
        });

        int start = savedInstanceState != null ? savedInstanceState.getInt(STATE_TAB, TAB_HOME) : TAB_HOME;
        select(start);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_TAB, current);
    }

    public void select(int tab) {
        if (tab == current) return;
        current = tab;
        Fragment f;
        switch (tab) {
            case TAB_MYQR:
                f = new MyQrFragment();
                break;
            case TAB_SCAN:
                f = new ScanFragment();
                break;
            case TAB_HISTORY:
                f = new HistoryFragment();
                break;
            case TAB_SETTINGS:
                f = new SettingsFragment();
                break;
            case TAB_HOME:
            default:
                f = new HomeFragment();
                break;
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.container, f).commitAllowingStateLoss();
        updateNav();
    }

    private void updateNav() {
        int active = ContextCompat.getColor(this, R.color.primary);
        int inactive = ContextCompat.getColor(this, R.color.nav_inactive);
        for (int i = 0; i < icons.length; i++) {
            if (icons[i] == null) continue;
            int col = i == current ? active : inactive;
            icons[i].setColorFilter(col);
            labels[i].setTextColor(col);
            labels[i].setTypeface(null, i == current ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
        findViewById(R.id.nav).setVisibility(View.VISIBLE);
    }
}
