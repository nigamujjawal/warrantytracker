package com.uj.warrantytrackerapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class WarrantyAdapter extends RecyclerView.Adapter<WarrantyAdapter.ViewHolder> {

    List<WarrantyItem> warrantyList;

    public WarrantyAdapter(List<WarrantyItem> warrantyList) {
        this.warrantyList = warrantyList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView productName, purchaseDate, duration, expiryDate;

        public ViewHolder(View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.itemProductName);
            purchaseDate = itemView.findViewById(R.id.itemPurchaseDate);
            duration = itemView.findViewById(R.id.itemDuration);
            expiryDate = itemView.findViewById(R.id.itemExpiryDate);
        }
    }

    @NonNull
    @Override
    public WarrantyAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_warranty_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WarrantyAdapter.ViewHolder holder, int position) {
        WarrantyItem item = warrantyList.get(position);
        holder.productName.setText("Product: " + item.getProductName());
        holder.purchaseDate.setText("Purchased on: " + item.getPurchaseDate());
        holder.duration.setText("Duration: " + item.getWarrantyDuration() + " months");
        holder.expiryDate.setText("Expires: " + item.getExpiryDate());
    }

    @Override
    public int getItemCount() {
        return warrantyList.size();
    }
}
