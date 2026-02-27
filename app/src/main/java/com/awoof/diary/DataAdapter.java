package com.awoof.diary;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

import com.awoof.diary.R;

public class DataAdapter extends ArrayAdapter<DataItem> {
    private boolean isPaid;

    public DataAdapter(Context context, List<DataItem> items, boolean isPaid) {
        super(context, 0, items);
        this.isPaid = isPaid;
    }
    
    public void setPaidStatus(boolean isPaid) {
        this.isPaid = isPaid;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
        }

        DataItem item = getItem(position);
        TextView tvTitle = (TextView) convertView.findViewById(R.id.tvTitle);
        TextView tvCode = (TextView) convertView.findViewById(R.id.tvCode);
        TextView tvStatus = (TextView) convertView.findViewById(R.id.tvStatus);
        ImageView ivAction = (ImageView) convertView.findViewById(R.id.ivAction);

        tvTitle.setText(item.title);
        
        boolean isPremium = item.status.trim().equalsIgnoreCase("PREMIUM");
        
        if (isPremium) {
            tvStatus.setText("PREMIUM");
            tvStatus.setTextColor(Color.parseColor("#F59E0B"));
            if (!isPaid) {
                tvCode.setText("****");
                ivAction.setImageResource(R.drawable.ic_lock);
            } else {
                tvCode.setText(item.code);
                ivAction.setImageResource(R.drawable.ic_call);
            }
        } else {
            tvStatus.setText("FREE");
            tvStatus.setTextColor(Color.parseColor("#818CF8"));
            tvCode.setText(item.code);
            ivAction.setImageResource(R.drawable.ic_call);
        }

        return convertView;
    }
}
