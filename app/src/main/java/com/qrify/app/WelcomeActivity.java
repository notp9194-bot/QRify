package com.qrify.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Prefs.welcomed(this)) {
            startMain();
            return;
        }
        setContentView(R.layout.activity_welcome);
        findViewById(R.id.btn_start).setOnClickListener(v -> {
            Prefs.setWelcomed(this);
            startMain();
        });
    }

    private void startMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
