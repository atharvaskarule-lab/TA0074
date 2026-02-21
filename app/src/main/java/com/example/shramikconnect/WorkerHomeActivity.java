package com.example.shramikconnect;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class WorkerHomeActivity extends AppCompatActivity {

    TextView tvWelcome, tvRating, tvEarnings;
    Switch switchAvailability;
    Button btnViewJobs, btnEditProfile, btnLogout;

    FirebaseAuth mAuth;
    DatabaseReference databaseReference;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_home);

        tvWelcome = findViewById(R.id.tvWelcome);
        tvRating = findViewById(R.id.tvRating);
        tvEarnings = findViewById(R.id.tvEarnings);
        switchAvailability = findViewById(R.id.switchAvailability);
        btnViewJobs = findViewById(R.id.btnViewJobs);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogout = findViewById(R.id.btnLogout);

        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userId);

        loadWorkerData();

        switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String status = isChecked ? "Available" : "Not Available";
            databaseReference.child("availability").setValue(status);
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)));

        btnViewJobs.setOnClickListener(v ->
                startActivity(new Intent(this, WorkerJobsActivity.class)));
    }

    private void loadWorkerData() {

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                String name = snapshot.child("name").getValue(String.class);
                String rating = String.valueOf(snapshot.child("rating").getValue());
                String earnings = String.valueOf(snapshot.child("earnings").getValue());
                String availability = snapshot.child("availability").getValue(String.class);

                tvWelcome.setText("Welcome, " + name);
                tvRating.setText("Rating: " + rating + " ⭐");
                tvEarnings.setText("Total Earnings: ₹" + earnings);

                switchAvailability.setChecked("Available".equals(availability));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(WorkerHomeActivity.this,
                        "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }
}