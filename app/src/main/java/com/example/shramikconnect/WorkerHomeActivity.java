package com.example.shramikconnect;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class WorkerHomeActivity extends AppCompatActivity {

    private static final String TAG = "WorkerHomeActivity";
    TextView tvWelcome, tvRating, tvEarnings;
    Switch switchAvailability;
    Button btnViewJobs, btnEditProfile, btnLogout, btnSos, btnViewChats;

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
        btnSos = findViewById(R.id.btnSos);
        btnViewChats = findViewById(R.id.btnViewChats);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // 1. Session Check
        if (currentUser == null) {
            Log.e(TAG, "No user logged in. Redirecting to Login.");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        userId = currentUser.getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userId);

        loadWorkerData();

        switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String status = isChecked ? "Available" : "Not Available";
            databaseReference.child("availability").setValue(status);
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)));

        btnViewJobs.setOnClickListener(v ->
                startActivity(new Intent(this, WorkerJobsActivity.class)));

        btnSos.setOnClickListener(v -> 
                startActivity(new Intent(this, SosActivity.class)));

        btnViewChats.setOnClickListener(v -> 
                startActivity(new Intent(this, WorkerChatsActivity.class)));
    }

    private void loadWorkerData() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // 2. Safe Data Retrieval with Default Values
                    String name = snapshot.child("name").getValue(String.class);
                    if (name == null) name = "Worker";

                    Object ratingObj = snapshot.child("rating").getValue();
                    String rating = (ratingObj != null) ? String.valueOf(ratingObj) : "0.0";

                    Object earningsObj = snapshot.child("earnings").getValue();
                    String earnings = (earningsObj != null) ? String.valueOf(earningsObj) : "0";

                    String availability = snapshot.child("availability").getValue(String.class);
                    if (availability == null) availability = "Not Available";

                    tvWelcome.setText("Welcome, " + name);
                    tvRating.setText("Rating: " + rating + " ⭐");
                    tvEarnings.setText("Total Earnings: ₹" + earnings);

                    // Prevent triggering the listener when setting initial state
                    switchAvailability.setOnCheckedChangeListener(null);
                    switchAvailability.setChecked("Available".equalsIgnoreCase(availability));
                    
                    // Re-attach listener
                    switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        String status = isChecked ? "Available" : "Not Available";
                        databaseReference.child("availability").setValue(status);
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        });
    }
}
