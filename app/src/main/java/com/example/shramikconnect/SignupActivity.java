package com.example.shramikconnect;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignupActivity extends AppCompatActivity {

    TextInputEditText etName, etPhone, etPassword;
    RadioGroup roleGroup;
    Button btnRegister;

    DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        roleGroup = findViewById(R.id.roleGroup);
        btnRegister = findViewById(R.id.btnRegister);

        databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {

        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        int selectedRoleId = roleGroup.getCheckedRadioButtonId();

        // Validation
        if (name.isEmpty()) {
            etName.setError("Enter your name");
            return;
        }

        if (phone.length() != 10) {
            etPhone.setError("Enter valid 10-digit phone number");
            return;
        }

        if (selectedRoleId == -1) {
            Toast.makeText(this, "Please select role", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        RadioButton selectedRole = findViewById(selectedRoleId);
        String role = selectedRole.getText().toString().trim();

        btnRegister.setEnabled(false); // prevent double click

        String userId = databaseReference.push().getKey();

        if (userId == null) {
            Toast.makeText(this, "Something went wrong. Try again.", Toast.LENGTH_SHORT).show();
            btnRegister.setEnabled(true);
            return;
        }

        User user = new User(name, phone, role, password);

        databaseReference.child(userId).setValue(user)
                .addOnCompleteListener(task -> {

                    btnRegister.setEnabled(true);

                    if (task.isSuccessful()) {

                        Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show();

                        // 🔥 REDIRECTION LOGIC
                        if (role.equalsIgnoreCase("Worker")) {
                            Intent intent = new Intent(SignupActivity.this, WorkerDashboardActivity.class);
                            intent.putExtra("name", name);
                            startActivity(intent);
                        } else {
                            Intent intent = new Intent(SignupActivity.this, CustomerDashboardActivity.class);
                            intent.putExtra("name", name);
                            startActivity(intent);
                        }

                        finish();

                    } else {
                        Toast.makeText(this,
                                "Failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}