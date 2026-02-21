package com.example.shramikconnect;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class CustomerDashboardActivity extends AppCompatActivity {

    TextView tvWelcome;
    Button btnSearchWorker, btnMyJobs, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_dashboard);

        tvWelcome = findViewById(R.id.tvWelcome);
        btnSearchWorker = findViewById(R.id.btnSearchWorker);
        btnMyJobs = findViewById(R.id.btnMyJobs);
        btnLogout = findViewById(R.id.btnLogout);

        // Get name from Signup
        String name = getIntent().getStringExtra("name");
        tvWelcome.setText("Welcome, " + name);

        // Search Worker Button


        // My Jobs Button
        btnMyJobs.setOnClickListener(v -> {
            // Future: Open MyJobsActivity
        });

        // Logout Button
        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerDashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}