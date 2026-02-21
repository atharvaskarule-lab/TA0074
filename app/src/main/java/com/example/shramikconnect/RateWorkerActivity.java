package com.example.shramikconnect;

import android.os.Bundle;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class RateWorkerActivity extends AppCompatActivity {

    private TextView tvWorkerName;
    private RatingBar ratingBar;
    private Button btnSubmitRating;

    private DatabaseReference workerRef;
    private String workerId;
    private String workerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate_worker);

        tvWorkerName = findViewById(R.id.tvWorkerName);
        ratingBar = findViewById(R.id.ratingBar);
        btnSubmitRating = findViewById(R.id.btnSubmitRating);

        workerId = getIntent().getStringExtra("workerId");
        workerName = getIntent().getStringExtra("workerName");

        workerRef = FirebaseDatabase.getInstance().getReference("Users").child(workerId);

        tvWorkerName.setText("Rate " + workerName);

        btnSubmitRating.setOnClickListener(v -> submitRating());
    }

    private void submitRating() {
        final float rating = ratingBar.getRating();
        if (rating == 0) {
            Toast.makeText(this, "Please provide a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        workerRef.child("ratings").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long numberOfRatings = dataSnapshot.getChildrenCount();
                float totalRating = 0;
                for (DataSnapshot ratingSnapshot : dataSnapshot.getChildren()) {
                    totalRating += ratingSnapshot.getValue(Float.class);
                }

                float newAverageRating = (totalRating + rating) / (numberOfRatings + 1);

                workerRef.child("rating").setValue(newAverageRating);
                workerRef.child("ratings").push().setValue(rating);

                Toast.makeText(RateWorkerActivity.this, "Rating submitted!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(RateWorkerActivity.this, "Failed to submit rating.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
