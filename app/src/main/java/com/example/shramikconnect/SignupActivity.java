package com.example.shramikconnect;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

public class SignupActivity extends AppCompatActivity {

    private static final String TAG = "SignupActivity";

    TextInputEditText etName, etPhone, etPassword;
    RadioGroup roleGroup;
    Button btnRegister;

    FirebaseAuth mAuth;
    DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        roleGroup = findViewById(R.id.roleGroup);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> {
            Log.d(TAG, "Register button clicked");
            registerUser();
        });
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
        String roleText = selectedRole.getText().toString().trim();
        
        final String role = roleText.contains("Worker") ? "Worker" : "Customer";

        btnRegister.setEnabled(false);
        Log.d(TAG, "Creating Firebase Auth user for: " + phone);

        // Firebase Auth requires an email. We'll use a fake email based on the phone number.
        String fakeEmail = phone + "@shramik.com";

        mAuth.createUserWithEmailAndPassword(fakeEmail, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Registration success, update UI with the signed-in user's information
                        Log.d(TAG, "createUserWithEmail:success");
                        String userId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                        
                        saveUserToDatabase(userId, name, phone, role, password);
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(SignupActivity.this, "Authentication failed: " + 
                                Objects.requireNonNull(task.getException()).getMessage(),
                                Toast.LENGTH_SHORT).show();
                        btnRegister.setEnabled(true);
                    }
                });
    }

    private void saveUserToDatabase(String userId, String name, String phone, String role, String password) {
        User user = new User(name, phone, role, password);

        databaseReference.child(userId).setValue(user)
                .addOnCompleteListener(task -> {
                    btnRegister.setEnabled(true);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User data saved successfully in Realtime Database");
                        Toast.makeText(SignupActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();

                        // 🔥 REDIRECTION LOGIC
                        Intent intent;
                        if ("Worker".equals(role)) {
                            intent = new Intent(SignupActivity.this, WorkerHomeActivity.class);
                        } else {
                            intent = new Intent(SignupActivity.this, CustomerHomeActivity.class);
                        }
                        intent.putExtra("name", name);
                        intent.putExtra("phone", phone);
                        startActivity(intent);
                        finish();

                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Log.e(TAG, "Database save failed: " + error);
                        Toast.makeText(SignupActivity.this, "Failed to save user data: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
