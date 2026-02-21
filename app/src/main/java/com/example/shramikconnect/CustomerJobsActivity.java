package com.example.shramikconnect;

import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;

public class CustomerJobsActivity extends AppCompatActivity {

    private static final String TAG = "CustomerJobsActivity";
    ListView listView;
    ArrayList<String> jobList;
    ArrayAdapter<String> adapter;

    DatabaseReference jobsRef, ratingsRef, usersRef;
    String customerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_jobs);

        listView = findViewById(R.id.listCompletedJobs); // Note: Keeping the ID for compatibility
        jobList = new ArrayList<>();

        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, jobList);

        listView.setAdapter(adapter);

        customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        jobsRef = FirebaseDatabase.getInstance().getReference("Jobs");
        ratingsRef = FirebaseDatabase.getInstance().getReference("Ratings");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        loadAllJobs();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String data = jobList.get(position);
            String[] parts = data.split("\n");
            if (parts.length > 0) {
                String jobIdLine = parts[parts.length - 1];
                if (jobIdLine.startsWith("ID: ")) {
                    String jobId = jobIdLine.substring(4);
                    
                    // Find workerId for this jobId from database to show rating dialog if completed
                    jobsRef.child(jobId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String status = snapshot.child("status").getValue(String.class);
                            String workerId = snapshot.child("workerId").getValue(String.class);
                            if ("completed".equals(status) && workerId != null) {
                                showRatingDialog(jobId, workerId);
                            } else if ("pending".equals(status)) {
                                Toast.makeText(CustomerJobsActivity.this, "Job is still pending approval", Toast.LENGTH_SHORT).show();
                            } else if ("accepted".equals(status)) {
                                Toast.makeText(CustomerJobsActivity.this, "Job in progress", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
        });
    }

    private void loadAllJobs() {
        jobsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                jobList.clear();
                for(DataSnapshot ds : snapshot.getChildren()) {
                    String custIdFromDb = ds.child("customerId").getValue(String.class);

                    if(customerId.equals(custIdFromDb)) {
                        String jobId = ds.getKey();
                        String workerId = ds.child("workerId").getValue(String.class);
                        String profession = ds.child("skill").getValue(String.class);
                        String status = ds.child("status").getValue(String.class);
                        String amount = String.valueOf(ds.child("amount").getValue());

                        String displayStatus = status;
                        if ("accepted".equalsIgnoreCase(status)) {
                            displayStatus = "Approved";
                        } else if ("pending".equalsIgnoreCase(status)) {
                            displayStatus = "Pending";
                        } else if ("completed".equalsIgnoreCase(status)) {
                            displayStatus = "Completed";
                        }

                        jobList.add("Profession: " + (profession != null ? profession : "N/A") + 
                                   "\nStatus: " + displayStatus + 
                                   "\nAmount: ₹" + amount + 
                                   "\nID: " + jobId);
                    }
                }
                adapter.notifyDataSetChanged();
                if (jobList.isEmpty()) {
                    Toast.makeText(CustomerJobsActivity.this, "No job requests found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading jobs: " + error.getMessage());
            }
        });
    }

    private void showRatingDialog(String jobId, String workerId) {
        final RatingBar ratingBar = new RatingBar(this);
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1);
        ratingBar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(android.view.Gravity.CENTER);
        container.addView(ratingBar);

        new android.app.AlertDialog.Builder(this)
                .setTitle("Rate Worker")
                .setMessage("Please rate your experience with this worker.")
                .setView(container)
                .setPositiveButton("Submit", (dialog, which) -> {
                    int stars = (int) ratingBar.getRating();
                    if(stars > 0){
                        submitRating(jobId, workerId, stars);
                    } else {
                        Toast.makeText(this, "Please select at least 1 star", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitRating(String jobId, String workerId, int stars) {
        String ratingId = ratingsRef.child(workerId).push().getKey();
        if (ratingId == null) return;

        HashMap<String, Object> map = new HashMap<>();
        map.put("customerId", customerId);
        map.put("jobId", jobId);
        map.put("stars", stars);

        ratingsRef.child(workerId).child(ratingId).setValue(map)
                .addOnSuccessListener(aVoid -> {
                    updateWorkerAverage(workerId);
                    Toast.makeText(CustomerJobsActivity.this, "Rating Submitted", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateWorkerAverage(String workerId){
        ratingsRef.child(workerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int total = 0;
                int count = 0;
                for(DataSnapshot ds : snapshot.getChildren()){
                    Integer stars = ds.child("stars").getValue(Integer.class);
                    if (stars != null) {
                        total += stars;
                        count++;
                    }
                }
                if (count > 0) {
                    double average = (double) total / count;
                    usersRef.child(workerId).child("rating").setValue(average);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
