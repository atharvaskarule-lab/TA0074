package com.example.shramikconnect;

import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;

public class WorkerJobsActivity extends AppCompatActivity {

    private static final String TAG = "WorkerJobsActivity";
    ListView listViewJobs;
    ArrayList<String> jobList;
    ArrayAdapter<String> adapter;

    DatabaseReference jobsRef, usersRef, notificationsRef;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_jobs);

        listViewJobs = findViewById(R.id.listViewJobs);
        jobList = new ArrayList<>();

        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, jobList);
        listViewJobs.setAdapter(adapter);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            finish();
            return;
        }

        jobsRef = FirebaseDatabase.getInstance().getReference("Jobs");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        notificationsRef = FirebaseDatabase.getInstance().getReference("Notifications");

        loadJobs();

        listViewJobs.setOnItemClickListener((parent, view, position, id) -> {
            try {
                String jobData = jobList.get(position);
                String jobId = jobData.split("::")[0].trim();
                showActionDialog(jobId);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing job item", e);
            }
        });
    }

    private void loadJobs() {
        jobsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                jobList.clear();
                for(DataSnapshot ds : snapshot.getChildren()) {
                    String workerId = ds.child("workerId").getValue(String.class);

                    if(userId.equals(workerId)) {
                        String jobId = ds.getKey();
                        String skill = ds.child("skill").getValue(String.class);
                        String status = ds.child("status").getValue(String.class);
                        
                        Object amountObj = ds.child("amount").getValue();
                        String amount = (amountObj != null) ? String.valueOf(amountObj) : "0";

                        jobList.add(jobId + ":: " +
                                (skill != null ? skill : "Job") + " | ₹" + amount + " | " + (status != null ? status : "pending"));
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        });
    }

    private void showActionDialog(String jobId) {
        String[] options = {"Accept Job", "Mark as Completed", "Cancel"};

        new android.app.AlertDialog.Builder(this)
                .setTitle("Select Action")
                .setItems(options, (dialog, which) -> {
                    if(which == 0){
                        acceptJob(jobId);
                    } else if (which == 1) {
                        jobsRef.child(jobId).child("status").setValue("completed");
                        updateEarnings(jobId);
                        Toast.makeText(this, "Job Completed", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void acceptJob(String jobId) {
        jobsRef.child(jobId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String customerId = snapshot.child("customerId").getValue(String.class);
                    String skill = snapshot.child("skill").getValue(String.class);

                    // Update job status
                    jobsRef.child(jobId).child("status").setValue("accepted")
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(WorkerJobsActivity.this, "Job Accepted", Toast.LENGTH_SHORT).show();
                                if (customerId != null) {
                                    sendNotificationToCustomer(customerId, skill);
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendNotificationToCustomer(String customerId, String skill) {
        String notificationId = notificationsRef.push().getKey();
        if (notificationId == null) return;

        HashMap<String, Object> notifMap = new HashMap<>();
        notifMap.put("to", customerId);
        notifMap.put("title", "Job Accepted!");
        notifMap.put("message", "A worker has accepted your request for " + skill);
        notifMap.put("timestamp", ServerValue.TIMESTAMP);
        notifMap.put("read", false);

        notificationsRef.child(notificationId).setValue(notifMap);
    }

    private void updateEarnings(String jobId) {
        jobsRef.child(jobId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object amountObj = snapshot.child("amount").getValue();
                int amount = 0;
                try {
                    if (amountObj != null) {
                        amount = Integer.parseInt(String.valueOf(amountObj));
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid amount format", e);
                }

                final int finalAmount = amount;
                usersRef.child(userId).child("earnings").runTransaction(new Transaction.Handler() {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                        Integer currentEarnings = currentData.getValue(Integer.class);
                        if (currentEarnings == null) {
                            currentData.setValue(finalAmount);
                        } else {
                            currentData.setValue(currentEarnings + finalAmount);
                        }
                        return Transaction.success(currentData);
                    }

                    @Override
                    public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                        if (error != null) {
                            Log.e(TAG, "Earnings update failed", error.toException());
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
