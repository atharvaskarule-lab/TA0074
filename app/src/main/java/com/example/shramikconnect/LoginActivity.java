package com.example.shramikconnect;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.messaging.FirebaseMessaging;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    EditText etPhone, etPassword;
    Button btnLogin;
    TextView tvSignup;

    FirebaseAuth mAuth;
    DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignup = findViewById(R.id.tvSignup);

        btnLogin.setOnClickListener(v -> loginUser());

        tvSignup.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignupActivity.class))
        );
    }

    private void loginUser() {
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(phone) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter Phone & Password", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        String fakeEmail = phone + "@shramik.com";

        mAuth.signInWithEmailAndPassword(fakeEmail, password)
                .addOnCompleteListener(authTask -> {
                    btnLogin.setEnabled(true);
                    if (authTask.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            Log.d(TAG, "Login successful for UID: " + userId);
                            
                            // Update FCM Token (Optional but good)
                            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
                                databaseReference.child(userId).child("fcmToken").setValue(token);
                            });

                            redirectUser(userId);
                        }
                    } else {
                        String error = authTask.getException() != null ? authTask.getException().getMessage() : "Invalid credentials";
                        Log.e(TAG, "Login failed: " + error);
                        Toast.makeText(LoginActivity.this, "Login Failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void redirectUser(String userId) {
        databaseReference.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String role = snapshot.child("role").getValue(String.class);
                    Intent intent;
                    if ("Worker".equals(role)) {
                        intent = new Intent(LoginActivity.this, WorkerHomeActivity.class);
                    } else {
                        intent = new Intent(LoginActivity.this, CustomerHomeActivity.class);
                    }
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "User data not found in database", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LoginActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
