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

public class CustomerHomeActivity extends AppCompatActivity {

    private static final String TAG = "CustomerHomeActivity";
    Spinner spinnerSkills;
    Button btnSearch, btnViewMyJobs;
    ListView listWorkers;

    ArrayList<String> workerNames;
    ArrayList<String> workerIds;
    ArrayAdapter<String> adapter;

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

        workerNames = new ArrayList<>();
        workerIds = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, workerNames);

        listWorkers.setAdapter(adapter);

        ArrayAdapter<String> skillAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, skills);
        spinnerSkills.setAdapter(skillAdapter);

        btnSearch.setOnClickListener(v -> searchWorkers());
        
        btnViewMyJobs.setOnClickListener(v -> {
            startActivity(new Intent(CustomerHomeActivity.this, CustomerJobsActivity.class));
        });

        listWorkers.setOnItemClickListener((parent, view, position, id) -> {
            String selectedWorkerId = workerIds.get(position);
            Intent intent = new Intent(CustomerHomeActivity.this, WorkerProfileActivity.class);
            intent.putExtra("workerId", selectedWorkerId);
            startActivity(intent);
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
            if (manager != null) manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        try {
            NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission missing", e);
        }
    }

    private void searchWorkers() {
        String selectedSkill = spinnerSkills.getSelectedItem().toString();
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                workerNames.clear();
                workerIds.clear();
                for(DataSnapshot ds : snapshot.getChildren()) {
                    String role = ds.child("role").getValue(String.class);
                    String profession = ds.child("profession").getValue(String.class);
                    if ("Worker".equals(role) && selectedSkill.equalsIgnoreCase(profession)) {
                        String name = ds.child("name").getValue(String.class);
                        workerNames.add(name != null ? name : "Unknown");
                        workerIds.add(ds.getKey());
                    }
                }
                adapter.notifyDataSetChanged();
                if (workerNames.isEmpty()) {
                    Toast.makeText(CustomerHomeActivity.this, "No workers found", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
