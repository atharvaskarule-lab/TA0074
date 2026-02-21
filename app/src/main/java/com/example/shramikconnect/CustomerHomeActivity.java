package com.example.shramikconnect;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;

public class CustomerHomeActivity extends AppCompatActivity {

    private static final String TAG = "CustomerHomeActivity";
    Spinner spinnerSkills;
    Button btnSearch, btnViewMyJobs;
    ListView listWorkers;

    ArrayList<User> workerList;
    WorkerAdapter adapter;

    DatabaseReference usersRef, jobsRef, notificationsRef;
    String customerId;

    String[] skills = {"Electrician", "Mistri", "Carpenter", "Painter", "Plumber", "Labour", "Other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_home);

        spinnerSkills = findViewById(R.id.spinnerSkills);
        btnSearch = findViewById(R.id.btnSearch);
        btnViewMyJobs = findViewById(R.id.btnViewMyJobs);
        listWorkers = findViewById(R.id.listWorkers);

        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        jobsRef = FirebaseDatabase.getInstance().getReference("Jobs");
        notificationsRef = FirebaseDatabase.getInstance().getReference("Notifications");

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            listenForNotifications();
        }

        workerList = new ArrayList<>();
        adapter = new WorkerAdapter(this, workerList);

        listWorkers.setAdapter(adapter);

        ArrayAdapter<String> skillAdapter =
                new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_dropdown_item,
                        skills);

        spinnerSkills.setAdapter(skillAdapter);

        btnSearch.setOnClickListener(v -> searchWorkers());
        
        btnViewMyJobs.setOnClickListener(v -> {
            startActivity(new Intent(CustomerHomeActivity.this, CustomerJobsActivity.class));
        });

        listWorkers.setOnItemClickListener((parent, view, position, id) -> {
            User worker = workerList.get(position);
            showBookingDialog(worker.uid);
        });
    }

    private void listenForNotifications() {
        notificationsRef.orderByChild("to").equalTo(customerId)
            .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Boolean read = ds.child("read").getValue(Boolean.class);
                    if (read != null && !read) {
                        String title = ds.child("title").getValue(String.class);
                        String message = ds.child("message").getValue(String.class);
                        showLocalNotification(title, message);

                        // Mark as read
                        ds.getRef().child("read").setValue(true);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showLocalNotification(String title, String message) {
        String channelId = "customer_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Customer Notifications", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
    }

    private void searchWorkers() {
        String selectedSkill = spinnerSkills.getSelectedItem().toString();
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                workerList.clear();
                for(DataSnapshot ds : snapshot.getChildren()) {
                    if ("Worker".equals(ds.child("role").getValue(String.class)) && 
                        selectedSkill.equalsIgnoreCase(ds.child("profession").getValue(String.class))) {
                            String workerId = ds.getKey();
                            String name = ds.child("name").getValue(String.class);
                            String phone = ds.child("phone").getValue(String.class);
                            String role = ds.child("role").getValue(String.class);

                            workerList.add(new User(workerId, name, phone, role));
                    }
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showBookingDialog(String workerId) {
        final EditText input = new EditText(this);
        input.setHint("Enter Booking Amount");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new android.app.AlertDialog.Builder(this)
                .setTitle("Book Worker")
                .setView(input)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String amountStr = input.getText().toString();
                    if(!amountStr.isEmpty()) {
                        String jobId = jobsRef.push().getKey();
                        if (jobId == null) return;
                        HashMap<String, Object> jobMap = new HashMap<>();
                        jobMap.put("workerId", workerId);
                        jobMap.put("customerId", customerId);
                        jobMap.put("skill", spinnerSkills.getSelectedItem().toString());
                        jobMap.put("amount", amountStr);
                        jobMap.put("status", "pending");
                        jobMap.put("timestamp", ServerValue.TIMESTAMP);
                        jobsRef.child(jobId).setValue(jobMap);
                        Toast.makeText(this, "Job Requested Successfully", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
