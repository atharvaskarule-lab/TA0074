package com.example.shramikconnect;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.List;

public class EditProfileActivity extends AppCompatActivity {

    EditText etName, etPhone;
    Button btnSave;
    Spinner spinnerProfession;
    LinearLayout workerRoleLayout;

    FirebaseAuth mAuth;
    DatabaseReference databaseReference;
    String userId;

    String[] professions = {"Electrician", "Mistri", "Carpenter", "Painter", "Plumber", "Labour", "Other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        btnSave = findViewById(R.id.btnSave);
        spinnerProfession = findViewById(R.id.spinnerProfession);
        workerRoleLayout = findViewById(R.id.workerRoleLayout);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }
        userId = mAuth.getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userId);

        // Setup Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, professions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProfession.setAdapter(adapter);

        loadProfileData();

        btnSave.setOnClickListener(v -> saveProfileData());
    }

    private void loadProfileData() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String role = snapshot.child("role").getValue(String.class);
                    String profession = snapshot.child("profession").getValue(String.class);

                    etName.setText(name);
                    etPhone.setText(phone);

                    // If user is a worker, show profession selection
                    if ("Worker".equals(role)) {
                        workerRoleLayout.setVisibility(View.VISIBLE);
                        if (profession != null) {
                            int spinnerPosition = Arrays.asList(professions).indexOf(profession);
                            if (spinnerPosition != -1) {
                                spinnerProfession.setSelection(spinnerPosition);
                            }
                        }
                    } else {
                        workerRoleLayout.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditProfileActivity.this, "Failed to load profile data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfileData() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseReference.child("name").setValue(name);
        databaseReference.child("phone").setValue(phone);

        // Save profession if it's a worker
        if (workerRoleLayout.getVisibility() == View.VISIBLE) {
            String selectedProfession = spinnerProfession.getSelectedItem().toString();
            databaseReference.child("profession").setValue(selectedProfession);
        }

        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
        finish();
    }
}
