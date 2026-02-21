package com.example.shramikconnect;

import android.content.Intent;
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
    ArrayList<String> jobListDisplay;
    ArrayList<String> workerIdList;
    ArrayAdapter<String> adapter;

    DatabaseReference jobsRef, usersRef, ratingsRef;
    String customerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_jobs);

        listView = findViewById(R.id.listCompletedJobs);
        jobListDisplay = new ArrayList<>();
        workerIdList = new ArrayList<>();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, jobListDisplay);
        listView.setAdapter(adapter);

        customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        jobsRef = FirebaseDatabase.getInstance().getReference("Jobs");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        ratingsRef = FirebaseDatabase.getInstance().getReference("Ratings");

        loadAllJobs();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedWorkerId = workerIdList.get(position);
            Intent intent = new Intent(CustomerJobsActivity.this, WorkerProfileActivity.class);
            intent.putExtra("workerId", selectedWorkerId);
            startActivity(intent);
        });
    }

    private void loadAllJobs() {
        jobsRef.orderByChild("customerId").equalTo(customerId)
            .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                jobListDisplay.clear();
                workerIdList.clear();
                if (!snapshot.exists()) {
                    Toast.makeText(CustomerJobsActivity.this, "No job requests found", Toast.LENGTH_SHORT).show();
                    return;
                }

                for(DataSnapshot ds : snapshot.getChildren()) {
                    String workerId = ds.child("workerId").getValue(String.class);
                    if (workerId == null) continue;

                    usersRef.child(workerId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                            String workerName = userSnapshot.child("name").getValue(String.class);
                            if (workerName == null) workerName = "Unknown Worker";
                            
                            String profession = ds.child("skill").getValue(String.class);
                            String status = ds.child("status").getValue(String.class);
                            String amount = String.valueOf(ds.child("amount").getValue());

                            String displayStatus = "";
                            if ("accepted".equalsIgnoreCase(status)) displayStatus = "Approved";
                            else if ("pending".equalsIgnoreCase(status)) displayStatus = "Pending";
                            else if ("completed".equalsIgnoreCase(status)) displayStatus = "Completed";

                            jobListDisplay.add("Worker: " + workerName + 
                                       "\nProfession: " + (profession != null ? profession : "N/A") + 
                                       "\nStatus: " + displayStatus + 
                                       "\nAmount: ₹" + amount);
                            workerIdList.add(workerId);
                            adapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading jobs: " + error.getMessage());
            }
        });
    }
}
