package com.example.shramikconnect;

import android.os.Bundle;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;

public class WorkerJobsActivity extends AppCompatActivity {

    ListView listViewJobs;
    ArrayList<String> jobList;
    ArrayAdapter<String> adapter;

    DatabaseReference jobsRef, usersRef;
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

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        jobsRef = FirebaseDatabase.getInstance().getReference("Jobs");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        loadJobs();

        listViewJobs.setOnItemClickListener((parent, view, position, id) -> {

            String jobData = jobList.get(position);
            String jobId = jobData.split("::")[0];

            showActionDialog(jobId);
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
                        int amount = ds.child("amount").getValue(Integer.class);

                        jobList.add(jobId + ":: " +
                                skill + " | ₹" + amount + " | " + status);
                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showActionDialog(String jobId) {

        String[] options = {"Accept Job", "Mark as Completed"};

        new android.app.AlertDialog.Builder(this)
                .setTitle("Select Action")
                .setItems(options, (dialog, which) -> {

                    if(which == 0){
                        jobsRef.child(jobId).child("status").setValue("accepted");
                    } else {

                        jobsRef.child(jobId).child("status")
                                .setValue("completed");

                        updateEarnings(jobId);
                    }
                })
                .show();
    }

    private void updateEarnings(String jobId) {

        jobsRef.child(jobId).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        int amount = snapshot.child("amount")
                                .getValue(Integer.class);

                        usersRef.child(userId).addListenerForSingleValueEvent(
                                new ValueEventListener() {
                                    @Override
                                    public void onDataChange(
                                            @NonNull DataSnapshot userSnap) {

                                        int currentEarnings =
                                                userSnap.child("earnings")
                                                        .getValue(Integer.class);

                                        usersRef.child(userId)
                                                .child("earnings")
                                                .setValue(currentEarnings + amount);
                                    }

                                    @Override
                                    public void onCancelled(
                                            @NonNull DatabaseError error) {}
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}