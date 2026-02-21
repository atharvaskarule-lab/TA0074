package com.example.shramikconnect;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SosActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 123;

    private FusedLocationProviderClient fusedLocationClient;
    private TextView tvSosStatus;
    private CardView btnTriggerSos;
    private Button btnSafeNow;

    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos);

        tvSosStatus = findViewById(R.id.tvSosStatus);
        btnTriggerSos = findViewById(R.id.btnTriggerSos);
        btnSafeNow = findViewById(R.id.btnSafeNow);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        btnTriggerSos.setOnClickListener(v -> triggerSos());
        btnSafeNow.setOnClickListener(v -> markAsSafe());
    }

    private void triggerSos() {
        if (checkAndRequestPermissions()) {
            sendSosToAllUsers();
        }
    }

    private boolean checkAndRequestPermissions() {
        String[] permissions = {
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                sendSosToAllUsers();
            } else {
                Toast.makeText(this, "All permissions are required for the SOS feature.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void sendSosToAllUsers() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted.", Toast.LENGTH_SHORT).show();
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        String message = "Help! I am in danger. My current location is: https://www.google.com/maps?q=" + location.getLatitude() + "," + location.getLongitude();
                        getAllUserPhoneNumbers(phoneNumbers -> {
                            for (String phoneNumber : phoneNumbers) {
                                sendSms(phoneNumber, message);
                            }
                        });
                        updateSosStatus(true);
                    } else {
                        Toast.makeText(SosActivity.this, "Could not get location.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void getAllUserPhoneNumbers(OnPhoneNumbersReadyListener listener) {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> phoneNumbers = new ArrayList<>();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String phoneNumber = userSnapshot.child("phone").getValue(String.class);
                    if (phoneNumber != null && !phoneNumber.isEmpty()) {
                        phoneNumbers.add(phoneNumber);
                    }
                }
                listener.onPhoneNumbersReady(phoneNumbers);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(SosActivity.this, "Failed to get user phone numbers.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendSms(String phoneNumber, String message) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                Toast.makeText(this, "SOS message sent to " + phoneNumber, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Failed to send SOS message to " + phoneNumber, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "SMS Permission not granted.", Toast.LENGTH_SHORT).show();
        }
    }

    private void markAsSafe() {
        updateSosStatus(false);
    }

    private void updateSosStatus(boolean isSos) {
        if (isSos) {
            tvSosStatus.setText("Status: SOS Triggered!");
            tvSosStatus.setTextColor(ContextCompat.getColor(this, R.color.red));
        } else {
            tvSosStatus.setText("Status: Secure");
            tvSosStatus.setTextColor(ContextCompat.getColor(this, R.color.green));
        }
    }

    interface OnPhoneNumbersReadyListener {
        void onPhoneNumbersReady(List<String> phoneNumbers);
    }
}
