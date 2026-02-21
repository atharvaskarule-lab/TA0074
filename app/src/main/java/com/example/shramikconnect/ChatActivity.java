package com.example.shramikconnect;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText etMessage;
    private Button btnSendMessage;

    private MessageAdapter messageAdapter;
    private List<Message> messageList;

    private String receiverId;
    private String senderId;
    private DatabaseReference messagesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerView = findViewById(R.id.recyclerView);
        etMessage = findViewById(R.id.etMessage);
        btnSendMessage = findViewById(R.id.btnSendMessage);

        receiverId = getIntent().getStringExtra("receiverId");
        senderId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        messagesRef = FirebaseDatabase.getInstance().getReference("Messages");

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);

        btnSendMessage.setOnClickListener(v -> sendMessage());

        readMessages();
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        if (messageText.isEmpty()) {
            return;
        }

        long timestamp = System.currentTimeMillis();
        Message message = new Message(senderId, receiverId, messageText, timestamp);

        messagesRef.push().setValue(message);
        etMessage.setText("");
    }

    private void readMessages() {
        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                messageList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Message message = snapshot.getValue(Message.class);
                    if (message != null &&
                            ((message.getSenderId().equals(senderId) && message.getReceiverId().equals(receiverId)) ||
                                    (message.getSenderId().equals(receiverId) && message.getReceiverId().equals(senderId)))) {
                        messageList.add(message);
                    }
                }
                messageAdapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(messageList.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ChatActivity.this, "Failed to load messages.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
