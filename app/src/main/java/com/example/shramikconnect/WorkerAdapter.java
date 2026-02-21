package com.example.shramikconnect;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class WorkerAdapter extends ArrayAdapter<User> {

    public WorkerAdapter(@NonNull Context context, @NonNull List<User> workers) {
        super(context, 0, workers);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_worker, parent, false);
        }

        User worker = getItem(position);

        TextView tvWorkerName = convertView.findViewById(R.id.tvWorkerName);
        TextView tvWorkerSkill = convertView.findViewById(R.id.tvWorkerSkill);
        Button btnChat = convertView.findViewById(R.id.btnChat);
        Button btnRate = convertView.findViewById(R.id.btnRate);

        if (worker != null) {
            tvWorkerName.setText(worker.name);
            tvWorkerSkill.setText("Skill: " + worker.role);

            btnChat.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), ChatActivity.class);
                intent.putExtra("receiverId", worker.uid); // Assuming User object has uid
                getContext().startActivity(intent);
            });

            btnRate.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), RateWorkerActivity.class);
                intent.putExtra("workerId", worker.uid); // Assuming User object has uid
                intent.putExtra("workerName", worker.name);
                getContext().startActivity(intent);
            });
        }

        return convertView;
    }
}
