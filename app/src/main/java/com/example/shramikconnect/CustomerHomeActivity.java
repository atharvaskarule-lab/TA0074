package com.example.shramikconnect;

import android.os.Bundle;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;

public class CustomerHomeActivity extends AppCompatActivity {

    Spinner spinnerSkills;
    Button btnSearch;
    ListView listWorkers;

    ArrayList<String> workerList;
    ArrayAdapter<String> adapter;

    DatabaseReference usersRef, jobsRef;
    String customerId;

    String[] skills = {"Electrician", "Plumber", "Painter", "Carpenter"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_home);

        spinnerSkills = findViewById(R.id.spinnerSkills);
        btnSearch = findViewById(R.id.btnSearch);
        listWorkers = findViewById(R.id.listWorkers);

        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        jobsRef = FirebaseDatabase.getInstance().getReference("Jobs");

        customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        workerList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, workerList);

        listWorkers.setAdapter(adapter);

        ArrayAdapter<String> skillAdapter =
                new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_dropdown_item,
                        skills);

        spinnerSkills.setAdapter(skillAdapter);

        btnSearch.setOnClickListener(v -> searchWorkers());

        listWorkers.setOnItemClickListener((parent, view, position, id) -> {

            String data = workerList.get(position);
            String workerId = data.split("::")[0];

            showBookingDialog(workerId);
        });
    }

    private void searchWorkers() {

        String selectedSkill = spinnerSkills.getSelectedItem().toString();

        usersRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        workerList.clear();

                        for(DataSnapshot ds : snapshot.getChildren()) {

                            String role = ds.child("role")
                                    .getValue(String.class);

                            String availability = ds.child("availability")
                                    .getValue(String.class);

                            String skill = ds.child("skill")
                                    .getValue(String.class);

                            if("Worker".equals(role) &&
                                    "Available".equals(availability) &&
                                    selectedSkill.equals(skill)) {

                                String workerId = ds.getKey();
                                String name = ds.child("name")
                                        .getValue(String.class);

                                workerList.add(workerId + ":: " + name);
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
        input.setHint("Enter Amount");

        new android.app.AlertDialog.Builder(this)
                .setTitle("Book Worker")
                .setView(input)
                .setPositiveButton("Confirm", (dialog, which) -> {

                    String amountStr = input.getText().toString();

                    if(!amountStr.isEmpty()) {

                        int amount = Integer.parseInt(amountStr);

                        String jobId = jobsRef.push().getKey();

                        HashMap<String, Object> jobMap =
                                new HashMap<>();

                        jobMap.put("workerId", workerId);
                        jobMap.put("customerId", customerId);
                        jobMap.put("skill",
                                spinnerSkills.getSelectedItem().toString());
                        jobMap.put("amount", amount);
                        jobMap.put("status", "pending");

                        jobsRef.child(jobId).setValue(jobMap);

                        Toast.makeText(this,
                                "Job Requested Successfully",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}