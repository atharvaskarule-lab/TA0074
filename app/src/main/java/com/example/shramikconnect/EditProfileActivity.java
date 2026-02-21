package com.example.shramikconnect;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Arrays;

public class EditProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    
    EditText etName, etPhone, etEmail, etAddress;
    Button btnSave;
    Spinner spinnerProfession;
    LinearLayout workerRoleLayout;
    ImageView ivEditProfilePic;
    FloatingActionButton fabChangePhoto;

    FirebaseAuth mAuth;
    DatabaseReference databaseReference;
    StorageReference storageReference;
    String userId;
    Uri imageUri;

    String[] professions = {"Electrician", "Mistri", "Carpenter", "Painter", "Plumber", "Labour", "Other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etAddress = findViewById(R.id.etAddress);
        btnSave = findViewById(R.id.btnSave);
        spinnerProfession = findViewById(R.id.spinnerProfession);
        workerRoleLayout = findViewById(R.id.workerRoleLayout);
        ivEditProfilePic = findViewById(R.id.ivEditProfilePic);
        fabChangePhoto = findViewById(R.id.fabChangePhoto);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }
        userId = mAuth.getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        storageReference = FirebaseStorage.getInstance().getReference("ProfilePictures");

        // Setup Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, professions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProfession.setAdapter(adapter);

        loadProfileData();

        fabChangePhoto.setOnClickListener(v -> openGallery());
        btnSave.setOnClickListener(v -> saveProfileData());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            ivEditProfilePic.setImageURI(imageUri);
        }
    }

    private void loadProfileData() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String address = snapshot.child("address").getValue(String.class);
                    String role = snapshot.child("role").getValue(String.class);
                    String profession = snapshot.child("profession").getValue(String.class);
                    String imageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                    etName.setText(name);
                    etPhone.setText(phone);
                    etEmail.setText(email);
                    etAddress.setText(address);

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(EditProfileActivity.this).load(imageUrl).into(ivEditProfilePic);
                    }

                    if ("Worker".equals(role)) {
                        workerRoleLayout.setVisibility(View.VISIBLE);
                        if (profession != null) {
                            int spinnerPosition = Arrays.asList(professions).indexOf(profession);
                            if (spinnerPosition != -1) {
                                spinnerProfession.setSelection(spinnerPosition);
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveProfileData() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        if (name.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Name and Address are required", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        if (imageUri != null) {
            uploadImageAndSaveData(name, email, address);
        } else {
            updateDatabase(name, email, address, null);
        }
    }

    private void uploadImageAndSaveData(String name, String email, String address) {
        StorageReference fileRef = storageReference.child(userId + ".jpg");
        fileRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
            updateDatabase(name, email, address, uri.toString());
        })).addOnFailureListener(e -> {
            btnSave.setEnabled(true);
            btnSave.setText("Save Profile");
            Toast.makeText(EditProfileActivity.this, "Image Upload Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void updateDatabase(String name, String email, String address, String imageUrl) {
        databaseReference.child("name").setValue(name);
        databaseReference.child("email").setValue(email);
        databaseReference.child("address").setValue(address);

        if (imageUrl != null) {
            databaseReference.child("profileImageUrl").setValue(imageUrl);
        }

        if (workerRoleLayout.getVisibility() == View.VISIBLE) {
            databaseReference.child("profession").setValue(spinnerProfession.getSelectedItem().toString());
        }

        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
        finish();
    }
}
