package com.example.shramikconnect;

import android.os.Bundle;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;

public class CustomerJobsActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<String> jobList;
    ArrayAdapter<String> adapter;

    DatabaseReference jobsRef, ratingsRef, usersRef;
    String customerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_jobs);

        listView = findViewById(R.id.listCompletedJobs);
        jobList = new ArrayList<>();

        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, jobList);

        listView.setAdapter(adapter);

        customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        jobsRef = FirebaseDatabase.getInstance().getReference("Jobs");
        ratingsRef = FirebaseDatabase.getInstance().getReference("Ratings");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        loadCompletedJobs();

        listView.setOnItemClickListener((parent, view, position, id) -> {

            String data = jobList.get(position);
            String jobId = data.split("::")[0];
            String workerId = data.split("::")[1];

            showRatingDialog(jobId, workerId);
        });
    }

    private void loadCompletedJobs() {

        jobsRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        jobList.clear();

                        for(DataSnapshot ds : snapshot.getChildren()) {

                            String custId = ds.child("customerId")
                                    .getValue(String.class);
                            String status = ds.child("status")
                                    .getValue(String.class);

                            if(customerId.equals(custId)
                                    && "completed".equals(status)) {

                                String jobId = ds.getKey();
                                String workerId = ds.child("workerId")
                                        .getValue(String.class);

                                jobList.add(jobId + "::" + workerId);
                            }
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void showRatingDialog(String jobId, String workerId) {

        final RatingBar ratingBar = new RatingBar(this);
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1);

        new android.app.AlertDialog.Builder(this)
                .setTitle("Rate Worker")
                .setView(ratingBar)
                .setPositiveButton("Submit", (dialog, which) -> {

                    int stars = (int) ratingBar.getRating();

                    if(stars > 0){
                        submitRating(jobId, workerId, stars);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitRating(String jobId,
                              String workerId,
                              int stars) {

        String ratingId = ratingsRef.child(workerId)
                .push().getKey();

        HashMap<String, Object> map = new HashMap<>();
        map.put("customerId", customerId);
        map.put("jobId", jobId);
        map.put("stars", stars);

        ratingsRef.child(workerId)
                .child(ratingId)
                .setValue(map);

        updateWorkerAverage(workerId);

        Toast.makeText(this,
                "Rating Submitted",
                Toast.LENGTH_SHORT).show();
    }

    private void updateWorkerAverage(String workerId){

        ratingsRef.child(workerId)
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot) {

                                int total = 0;
                                int count = 0;

                                for(DataSnapshot ds : snapshot.getChildren()){
                                    int stars = ds.child("stars")
                                            .getValue(Integer.class);
                                    total += stars;
                                    count++;
                                }

                                double average =
                                        (double) total / count;

                                usersRef.child(workerId)
                                        .child("rating")
                                        .setValue(average);
                            }

                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error) {}
                        });
    }
}