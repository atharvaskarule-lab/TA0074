package com.example.shramikconnect;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.messaging.FirebaseMessaging;

public class LoginActivity extends AppCompatActivity {

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

//    @Override
//    public void onStart() {
//        super.onStart();
//        // Check if user is signed in (non-null) and update UI accordingly.
//        FirebaseUser currentUser = mAuth.getCurrentUser();
//        if(currentUser != null){
//            redirectUser(currentUser.getUid());
//        }
//    }

    private void loginUser() {

        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(phone) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter Phone & Password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert phone to email format
        String fakeEmail = phone + "@shramik.com";

        mAuth.signInWithEmailAndPassword(fakeEmail, password)
                .addOnCompleteListener(authTask -> {

                    if (authTask.isSuccessful()) {

                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null) return;

                        String userId = user.getUid();

                        // Save FCM Token
                        FirebaseMessaging.getInstance().getToken()
                                .addOnCompleteListener(tokenTask -> {
                                    if (tokenTask.isSuccessful()) {
                                        String token = tokenTask.getResult();
                                        databaseReference.child(userId)
                                                .child("fcmToken")
                                                .setValue(token);
                                    }
                                });

                        redirectUser(userId);

                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Invalid Phone or Password",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void redirectUser(String userId) {

        databaseReference.child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        String role = snapshot.child("role").getValue(String.class);

                        if ("Worker".equals(role)) {
                            startActivity(new Intent(LoginActivity.this,
                                    WorkerHomeActivity.class));
                        } else {
                            startActivity(new Intent(LoginActivity.this,
                                    CustomerHomeActivity.class));
                        }

                        finish();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(LoginActivity.this,
                                "Database Error", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}