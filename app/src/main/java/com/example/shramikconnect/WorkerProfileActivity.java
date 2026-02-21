package com.example.shramikconnect;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.HashMap;

public class WorkerProfileActivity extends AppCompatActivity {

    ImageView ivWorkerProfilePic;
    TextView tvWorkerName, tvWorkerProfession, tvWorkerPhone, tvWorkerEmail, tvWorkerAddress;
    RatingBar rbWorkerRating;
    Button btnBookWorker;

    DatabaseReference workerRef, jobsRef;
    String workerId, customerId, profession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_profile);

        ivWorkerProfilePic = findViewById(R.id.ivWorkerProfilePic);
        tvWorkerName = findViewById(R.id.tvWorkerName);
        tvWorkerProfession = findViewById(R.id.tvWorkerProfession);
        tvWorkerPhone = findViewById(R.id.tvWorkerPhone);
        tvWorkerEmail = findViewById(R.id.tvWorkerEmail);
        tvWorkerAddress = findViewById(R.id.tvWorkerAddress);
        rbWorkerRating = findViewById(R.id.rbWorkerRating);
        btnBookWorker = findViewById(R.id.btnBookWorker);

        workerId = getIntent().getStringExtra("workerId");
        customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        workerRef = FirebaseDatabase.getInstance().getReference("Users").child(workerId);
        jobsRef = FirebaseDatabase.getInstance().getReference("Jobs");

        loadWorkerData();

        btnBookWorker.setOnClickListener(v -> showBookingDialog());
    }

    private void loadWorkerData() {
        workerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    profession = snapshot.child("profession").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String address = snapshot.child("address").getValue(String.class);
                    String imageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                    Double rating = snapshot.child("rating").getValue(Double.class);

                    tvWorkerName.setText(name);
                    tvWorkerProfession.setText(profession != null ? profession : "N/A");
                    tvWorkerPhone.setText(phone);
                    tvWorkerEmail.setText(email != null && !email.isEmpty() ? email : "Not Provided");
                    tvWorkerAddress.setText(address != null && !address.isEmpty() ? address : "Not Provided");
                    rbWorkerRating.setRating(rating != null ? rating.floatValue() : 0f);

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(WorkerProfileActivity.this).load(imageUrl).into(ivWorkerProfilePic);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showBookingDialog() {
        final EditText input = new EditText(this);
        input.setHint("Enter Booking Amount");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new android.app.AlertDialog.Builder(this)
                .setTitle("Hire Worker")
                .setMessage("Enter the amount you want to offer for this job.")
                .setView(input)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String amountStr = input.getText().toString();
                    if(!amountStr.isEmpty()) {
                        saveJobRequest(amountStr);
                    } else {
                        Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveJobRequest(String amount) {
        String jobId = jobsRef.push().getKey();
        if (jobId == null) return;

        HashMap<String, Object> jobMap = new HashMap<>();
        jobMap.put("workerId", workerId);
        jobMap.put("customerId", customerId);
        jobMap.put("skill", profession);
        jobMap.put("amount", amount);
        jobMap.put("status", "pending");
        jobMap.put("timestamp", ServerValue.TIMESTAMP);

        jobsRef.child(jobId).setValue(jobMap).addOnSuccessListener(aVoid -> {
            Toast.makeText(WorkerProfileActivity.this, "Job Requested Successfully", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
