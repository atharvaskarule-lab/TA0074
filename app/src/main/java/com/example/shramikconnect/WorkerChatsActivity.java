package com.example.shramikconnect;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class WorkerChatsActivity extends AppCompatActivity {

    private ListView listCustomers;
    private ArrayList<String> customerList;
    private ArrayAdapter<String> adapter;

    private DatabaseReference messagesRef;
    private DatabaseReference usersRef;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_chats);

        listCustomers = findViewById(R.id.listCustomers);
        customerList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, customerList);
        listCustomers.setAdapter(adapter);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        messagesRef = FirebaseDatabase.getInstance().getReference("Messages");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        loadCustomers();

        listCustomers.setOnItemClickListener((parent, view, position, id) -> {
            String customerName = customerList.get(position);
            usersRef.orderByChild("name").equalTo(customerName).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        String customerId = userSnapshot.getKey();
                        Intent intent = new Intent(WorkerChatsActivity.this, ChatActivity.class);
                        intent.putExtra("receiverId", customerId);
                        startActivity(intent);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        });
    }

    private void loadCustomers() {
        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Set<String> customerIds = new HashSet<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Message message = snapshot.getValue(Message.class);
                    if (message != null) {
                        if (message.getSenderId().equals(currentUserId)) {
                            customerIds.add(message.getReceiverId());
                        } else if (message.getReceiverId().equals(currentUserId)) {
                            customerIds.add(message.getSenderId());
                        }
                    }
                }
                loadCustomerNames(customerIds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadCustomerNames(Set<String> customerIds) {
        customerList.clear();
        for (String customerId : customerIds) {
            usersRef.child(customerId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String customerName = dataSnapshot.getValue(String.class);
                    if (customerName != null) {
                        customerList.add(customerName);
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }
}
